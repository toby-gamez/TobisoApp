package com.example.tobisoappnative.viewmodel.tts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tobisoappnative.tts.TtsManager

class TtsViewModel(application: Application) : AndroidViewModel(application) {

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

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TtsViewModel(application) as T
    }
}
