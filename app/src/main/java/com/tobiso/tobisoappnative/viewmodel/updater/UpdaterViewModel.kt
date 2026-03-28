package com.tobiso.tobisoappnative.viewmodel.updater

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class ReleaseInfo(
    val version: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val author: String,
    val downloadCount: Int,
    val htmlUrl: String
)

@HiltViewModel
class UpdaterViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _currentVersion = MutableStateFlow("?")
    val currentVersion: StateFlow<String> = _currentVersion

    private val _latestVersion = MutableStateFlow<String?>(null)
    val latestVersion: StateFlow<String?> = _latestVersion

    private val _releaseInfo = MutableStateFlow<ReleaseInfo?>(null)
    val releaseInfo: StateFlow<ReleaseInfo?> = _releaseInfo

    private val _isUpToDate = MutableStateFlow<Boolean?>(null)
    val isUpToDate: StateFlow<Boolean?> = _isUpToDate

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    fun init(currentVersion: String) {
        _currentVersion.value = currentVersion
        checkForUpdates(currentVersion)
    }

    private fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOfflineMode.value = !NetworkUtils.isOnline(getApplication())
            if (_isOfflineMode.value) return@launch
            try {
                val info = fetchLatestVersionFromGithub()
                _latestVersion.value = info.version
                _releaseInfo.value = info
                _isUpToDate.value = info.version == currentVersion
            } catch (e: Exception) {
                _error.value = "Chyba při kontrole verze: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun fetchLatestVersionFromGithub(): ReleaseInfo = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repositories/1041297894/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.connect()
        if (connection.responseCode != 200) {
            throw Exception("HTTP ${connection.responseCode}")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)

        val tag = json.getString("tag_name").removePrefix("v")
        val name = json.optString("name", "")
        val bodyRaw = json.optString("body", "Žádné poznámky k vydání.")
        val body = bodyRaw.lines().drop(1).joinToString("\n").trim()
        val publishedAtRaw = json.optString("published_at", "").substringBefore("T")
        val publishedAt = if (publishedAtRaw.isNotEmpty()) {
            val parts = publishedAtRaw.split("-")
            if (parts.size == 3) "${parts[2]}. ${parts[1]}. ${parts[0]}" else publishedAtRaw
        } else ""
        val authorRaw = json.optJSONObject("author")?.optString("login", "") ?: ""
        val author = if (authorRaw == "toby-gamez") "Taneq" else authorRaw

        var downloadCount = 0
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                downloadCount += asset.optInt("download_count", 0)
            }
        }

        val htmlUrl = json.optString("html_url", "")

        return@withContext ReleaseInfo(
            version = tag,
            name = name,
            body = body,
            publishedAt = publishedAt,
            author = author,
            downloadCount = downloadCount,
            htmlUrl = htmlUrl
        )
    }
}
