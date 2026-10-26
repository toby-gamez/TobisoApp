package com.tobiso.tobisoappnative.gamesync

import android.content.Context
import android.content.SharedPreferences
import com.tobiso.tobisoappnative.BackpackManager
import com.tobiso.tobisoappnative.PetManager
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.ShopManager
import com.tobiso.tobisoappnative.StreakFreezeManager
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.GameStatePutRequest
import com.tobiso.tobisoappnative.model.GameStateResponse
import com.tobiso.tobisoappnative.model.PointsClaimRequest
import com.tobiso.tobisoappnative.model.PointsClaimResponse
import com.tobiso.tobisoappnative.model.PointsEarnRequest
import com.tobiso.tobisoappnative.model.QuizAnswerSubmission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the gamification prefs (points, shop, backpack, streak, pets, question stats) in sync with
 * `user/game-state` while a student is signed in. Anonymous users are never touched.
 *
 * Any change to the synced prefs marks the device "dirty" and schedules a debounced sync; a sync pulls, merges
 * ([mergeGameState]), applies the merge locally and pushes it back (retrying on 409).
 */
@Singleton
class GameSyncManager @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiService
) {
    private val appContext = context.applicationContext
    private val store = GamePrefsStore(context)
    private val meta: SharedPreferences = context.getSharedPreferences("game_sync_meta", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    // Values we wrote ourselves. SharedPreferences reports changes asynchronously on the main thread, so a flag
    // would already be cleared; instead a change event is ignored when the key still holds what we wrote.
    private val written = java.util.concurrent.ConcurrentHashMap<String, PrefValue>()
    private var debounce: Job? = null
    private var started = false

    // SharedPreferences keeps listeners weakly, so hold on to them.
    private val listeners = SYNCED_PREFS.associateWith { file ->
        SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == null || !AuthSession.isSignedIn) return@OnSharedPreferenceChangeListener
            if (written["$file/$k"] == store.valueOf(file, k)) return@OnSharedPreferenceChangeListener
            meta.edit().putBoolean(dirtyKey(), true).apply()
            debounce?.cancel()
            debounce = scope.launch { delay(DEBOUNCE_MS); sync() }
        }
    }

    fun start() {
        if (started) return
        started = true
        instance = this
        listeners.forEach { (file, l) -> store.prefs(file).registerOnSharedPreferenceChangeListener(l) }
        scope.launch {
            AuthSession.user.map { AuthSession.ownerKey }.distinctUntilChanged().collect { key ->
                if (key != null) { reportActivity(); refreshExchangePoints(); sync() } else _exchangePoints.value = null
            }
        }
    }

    private fun key(name: String) = "${AuthSession.ownerKey}.$name"
    private fun dirtyKey() = key("dirty")

    /** Pull → merge → apply → push. Quietly gives up on network errors; the next change or start retries. */
    suspend fun sync(): Boolean = mutex.withLock {
        if (!AuthSession.isSignedIn) return false
        try {
            repeat(MAX_ATTEMPTS) {
                val pulled = pull() ?: return false
                val serverSnap = pulled.takeIf { it.revision > 0 || it.state.isNotEmpty() }?.state?.let(::parseStateJson)
                val local = store.read()
                val merged = mergeGameState(
                    local, serverSnap,
                    MergeContext(
                        firstSync = !meta.contains(key("revision")),
                        preferLocal = meta.getBoolean(dirtyKey(), false),
                        basePoints = if (meta.contains(key("points"))) meta.getFloat(key("points"), 0f) else null
                    )
                )
                if (merged != local) apply(merged)
                // Also push when the server copy lacks an up-to-date `streak` mirror (older app versions never wrote it).
                if (serverSnap != null && merged == serverSnap && pulled.state["streak"] == merged.streakJson()) {
                    remember(pulled.revision, merged)
                    return true
                }
                when (val r = push(pulled.revision, merged)) {
                    is Push.Ok -> { remember(r.revision, merged); return true }
                    Push.Conflict -> Unit // pull again and re-merge
                    Push.Failed -> return false
                }
            }
            false
        } catch (e: Exception) {
            Timber.w(e, "Game state sync failed")
            false
        }
    }

    /**
     * Tells the server the account was active today (feeds the shared "Řada" streak). Once per local day; a failed
     * call is retried on the next start. Runs before the game-state sync so the freshly recorded day is counted.
     */
    private suspend fun reportActivity() {
        val today = java.time.LocalDate.now().toString()
        if (meta.getString(key("activity"), null) == today) return
        try {
            if (api.reportActivity().isSuccessful) meta.edit().putString(key("activity"), today).apply()
        } catch (e: Exception) {
            Timber.w(e, "Reporting activity failed")
        }
    }

    /** True when everything on this device is safely on the server (nothing changed since the last sync, or a sync just worked). */
    suspend fun flushBeforeSignOut(): Boolean {
        if (!AuthSession.isSignedIn) return true
        return !meta.getBoolean(dirtyKey(), false) || sync()
    }

    /**
     * Erases the account's gamification data from this device (points, shop, backpack, streak, pets, question stats)
     * plus its sync bookkeeping, then reloads the managers so nothing stale stays in memory. Call after signing out
     * ([ownerKey] is the key of the account that just left).
     */
    fun wipeLocalState(ownerKey: String?) {
        debounce?.cancel()
        SYNCED_PREFS.forEach { store.prefs(it).edit().clear().commit() }
        written.clear()
        if (ownerKey != null) {
            val e = meta.edit()
            meta.all.keys.filter { it.startsWith("$ownerKey.") }.forEach { e.remove(it) }
            e.commit()
        }
        runCatching { PointsManager.instance.reloadFromPrefs() }
        runCatching { ShopManager.instance.init() }
        runCatching { BackpackManager.instance.reloadFromPrefs() }
        runCatching { StreakFreezeManager.instance.reloadFromPrefs() }
        runCatching { PetManager.initialize(appContext) }
    }

    /** Server-owned exchange points (earned from graded quiz answers); null until loaded or when signed out. */
    private val _exchangePoints = MutableStateFlow<Int?>(null)
    val exchangePoints: StateFlow<Int?> = _exchangePoints

    suspend fun refreshExchangePoints() {
        if (!AuthSession.isSignedIn) { _exchangePoints.value = null; return }
        try {
            _exchangePoints.value = api.getPoints().balance
        } catch (e: Exception) {
            Timber.w(e, "Loading exchange points failed")
        }
    }

    /** Sends finished quiz answers for server-side grading; the server decides whether and how much to award. */
    fun reportQuiz(answers: List<QuizAnswerSubmission>) {
        if (!AuthSession.isSignedIn || answers.isEmpty()) return
        scope.launch {
            try {
                _exchangePoints.value = api.earnPoints(PointsEarnRequest(answers.take(50))).balance
            } catch (e: Exception) {
                Timber.w(e, "Reporting quiz answers failed")
            }
        }
    }

    /** Buys AI credits with exchange points; the server owns the price list and the balance. */
    suspend fun claimAiCredits(itemId: Int): Result<PointsClaimResponse> = try {
        val response = api.claimPoints(PointsClaimRequest("ai_credits", itemId.toString()))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            _exchangePoints.value = body.points
            Result.success(body)
        } else {
            Result.failure(ClaimFailure(response.code(), errorMessage(response)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    class ClaimFailure(val code: Int, message: String?) : Exception(message)

    // ---- internals ----------------------------------------------------------------------------------------

    private sealed interface Push {
        data class Ok(val revision: Int) : Push
        object Conflict : Push
        object Failed : Push
    }

    /** null = could not reach the server / unexpected status. */
    private suspend fun pull(): GameStateResponse? {
        val r = api.getGameState()
        return when {
            r.code() == 204 -> GameStateResponse(0, null, JsonObject(emptyMap()))
            r.isSuccessful -> r.body()
            else -> { Timber.w("game-state GET -> ${r.code()}"); null }
        }
    }

    private suspend fun push(baseRevision: Int, snapshot: PrefsSnapshot): Push {
        val r = api.putGameState(GameStatePutRequest(baseRevision, snapshot.toStateJson()))
        return when {
            r.isSuccessful -> Push.Ok(r.body()?.revision ?: (baseRevision + 1))
            r.code() == 409 -> Push.Conflict
            else -> { Timber.w("game-state PUT -> ${r.code()}"); Push.Failed }
        }
    }

    private fun apply(merged: PrefsSnapshot) {
        merged.forEach { (file, values) -> values.forEach { (k, v) -> written["$file/$k"] = v } }
        store.write(merged)
        runCatching { PointsManager.instance.reloadFromPrefs() }
        runCatching { ShopManager.instance.init() }
        runCatching { BackpackManager.instance.reloadFromPrefs() }
        runCatching { StreakFreezeManager.instance.reloadFromPrefs() }
        runCatching { PetManager.initialize(appContext) }
    }

    private fun remember(revision: Int, snapshot: PrefsSnapshot) {
        meta.edit()
            .putInt(key("revision"), revision)
            .putFloat(key("points"), snapshot.points() ?: 0f)
            .putBoolean(dirtyKey(), false)
            .apply()
    }

    private fun errorMessage(r: Response<*>): String? = runCatching {
        r.errorBody()?.string()?.let { json.parseToJsonElement(it).jsonObject["message"]?.toString()?.trim('"') }
    }.getOrNull()

    companion object {
        /** Set by [start]; lets screens that aren't injected report quiz answers. */
        @Volatile
        var instance: GameSyncManager? = null
            private set

        private const val DEBOUNCE_MS = 5_000L
        private const val MAX_ATTEMPTS = 3
    }
}
