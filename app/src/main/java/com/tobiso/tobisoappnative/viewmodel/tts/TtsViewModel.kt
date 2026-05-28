package com.tobiso.tobisoappnative.viewmodel.tts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tobiso.tobisoappnative.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TtsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    val ttsManager: TtsManager = TtsManager(application)

    fun speak(text: String) {
        ttsManager.ensureInitialized()
        ttsManager.speak(text)
    }
    fun pause() {
        ttsManager.ensureInitialized()
        ttsManager.pause()
    }
    fun resume() {
        ttsManager.ensureInitialized()
        ttsManager.resume()
    }
    fun stop() {
        ttsManager.ensureInitialized()
        ttsManager.stop()
    }
    fun skipToNext() {
        ttsManager.ensureInitialized()
        ttsManager.skipToNext()
    }
    fun skipToPrevious() {
        ttsManager.ensureInitialized()
        ttsManager.skipToPrevious()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.destroy()
    }
}
