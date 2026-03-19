package com.example.tobisoappnative.viewmodel.tts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tobisoappnative.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TtsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    val ttsManager: TtsManager = TtsManager(application)

    fun speak(text: String) = ttsManager.speak(text)
    fun pause() = ttsManager.pause()
    fun resume() = ttsManager.resume()
    fun stop() = ttsManager.stop()
    fun skipToNext() = ttsManager.skipToNext()
    fun skipToPrevious() = ttsManager.skipToPrevious()

    override fun onCleared() {
        super.onCleared()
        ttsManager.destroy()
    }
}
