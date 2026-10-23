package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.ReadProgressRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContinueReading(val postId: Int, val title: String, val percent: Int)

/**
 * Tracks how far the user got in the article they read last.
 *
 * Always stored locally (so "Pokračovat ve čtení" works anonymously too); additionally reported to
 * `user/read-progress` when signed in. The server keeps the max percent per post and credits the
 * daily read bonus on any report, so we throttle to meaningful steps.
 */
@Singleton
class ReadingProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiService
) {
    private val prefs = context.getSharedPreferences("reading_progress", Context.MODE_PRIVATE)
    private val lastSent = mutableMapOf<Int, Int>()

    fun lastUnfinished(): ContinueReading? {
        val id = prefs.getInt(KEY_POST_ID, -1)
        val title = prefs.getString(KEY_TITLE, null)
        val percent = prefs.getInt(KEY_PERCENT, 0)
        return if (id >= 0 && !title.isNullOrBlank()) ContinueReading(id, title, percent) else null
    }

    suspend fun report(postId: Int, title: String, percent: Int) {
        val p = percent.coerceIn(0, 100)
        when {
            p >= FINISHED -> if (prefs.getInt(KEY_POST_ID, -1) == postId) prefs.edit().clear().apply()
            p >= MIN_TO_TRACK -> prefs.edit()
                .putInt(KEY_POST_ID, postId).putString(KEY_TITLE, title).putInt(KEY_PERCENT, p).apply()
        }
        if (!AuthSession.isSignedIn || p < MIN_TO_TRACK) return
        val sent = lastSent[postId] ?: 0
        if (p - sent >= SERVER_STEP || (p >= FINISHED && sent < FINISHED)) {
            if (runCatching { api.reportReadProgress(ReadProgressRequest(postId, p)) }.getOrNull()?.isSuccessful == true) {
                lastSent[postId] = p
            }
        }
    }

    private companion object {
        const val KEY_POST_ID = "post_id"
        const val KEY_TITLE = "title"
        const val KEY_PERCENT = "percent"
        const val MIN_TO_TRACK = 5
        const val FINISHED = 95
        const val SERVER_STEP = 10
    }
}
