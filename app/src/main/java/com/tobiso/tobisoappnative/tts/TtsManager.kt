package com.tobiso.tobisoappnative.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import com.tobiso.tobisoappnative.utils.TextUtils

enum class TtsState {
    IDLE,
    INITIALIZING,
    SPEAKING,
    PAUSED,
    ERROR,
    STOPPED
}

data class TtsStatus(
    val state: TtsState = TtsState.IDLE,
    val isInitialized: Boolean = false,
    val currentText: String = "",
    val errorMessage: String? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentSegment: Int = 0,
    val totalSegments: Int = 0,
    val currentSegmentText: String = "", // Text aktuálního segmentu
    val currentWordIndex: Int = 0, // Index aktuálního slova v segmentu
    val segmentWords: List<String> = emptyList(), // Slova aktuálního segmentu
    val visibleSegments: List<String> = emptyList() // Sliding window: current + next 2 segments
)

class TtsManager(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var currentUtteranceId: String? = null
    
    private val _status = MutableStateFlow(TtsStatus())
    val status: StateFlow<TtsStatus> = _status.asStateFlow()
    
    private var currentTextSegments: List<String> = emptyList()
    private var currentSegmentIndex = 0
    private var pausedPosition = 0
    private var resumeOffset = 0 // offset v segmentu při resume
    private var isReallyPaused = false
    private var savedProgress = 0f // Uložený progress při pozastavení
    private var currentSegmentWords: List<String> = emptyList()
    private var currentWordIndex = 0
    // Counters for sliding window based on words spoken
    private var totalWordsSpoken = 0
    private var lastAbsoluteWordIndex = -1
    private var visibleShiftCount = 0
    
    init {
        initializeTts()
    }
    
    private fun initializeTts() {
        _status.value = _status.value.copy(state = TtsState.INITIALIZING)
        
        textToSpeech = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    textToSpeech?.let { tts ->
                        // Nastavíme češtinu
                        val localeResult = tts.setLanguage(Locale("cs", "CZ"))
                        
                        if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                            localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            // Fallback na angličtinu pokud čeština není dostupná
                            tts.setLanguage(Locale.US)
                        }
                        
                        // Nastavíme listener pro sledování pokroku
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) {
                                if (!isReallyPaused) {
                                    val segmentText = if (currentSegmentIndex < currentTextSegments.size) {
                                        currentTextSegments[currentSegmentIndex]
                                    } else ""
                                    
                                    currentSegmentWords = segmentText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                                    currentWordIndex = 0
                                    
                                    _status.value = _status.value.copy(
                                        state = TtsState.SPEAKING,
                                        currentSegment = currentSegmentIndex + 1,
                                        totalSegments = currentTextSegments.size,
                                        currentSegmentText = segmentText,
                                        segmentWords = currentSegmentWords,
                                        currentWordIndex = 0,
                                        progress = if (!isReallyPaused) savedProgress else _status.value.progress,
                                        visibleSegments = currentTextSegments.drop(currentSegmentIndex).take(3)
                                    )
                                }
                            }
                            
                            override fun onDone(utteranceId: String) {
                                if (utteranceId.startsWith(currentUtteranceId ?: "") && !isReallyPaused) {
                                    // Dokončili jsme aktuální segment - nastavíme poslední slovo jako přečtené
                                    if (currentSegmentWords.isNotEmpty()) {
                                        currentWordIndex = currentSegmentWords.size - 1
                                        _status.value = _status.value.copy(currentWordIndex = currentWordIndex)
                                    }
                                    
                                    // Pokud jsou další segmenty, pokračujeme
                                    if (currentSegmentIndex < currentTextSegments.size - 1) {
                                        currentSegmentIndex++
                                        currentWordIndex = 0
                                        savedProgress = _status.value.progress // Uložíme progress
                                        updateProgress()
                                        // Update visible segments window anchored at the new currentSegmentIndex
                                        _status.value = _status.value.copy(
                                            visibleSegments = currentTextSegments.drop(currentSegmentIndex).take(3),
                                            currentSegmentText = if (currentSegmentIndex < currentTextSegments.size) currentTextSegments[currentSegmentIndex] else "",
                                            segmentWords = if (currentSegmentIndex < currentTextSegments.size) currentTextSegments[currentSegmentIndex].split("\\s+".toRegex()).filter { it.isNotBlank() } else emptyList(),
                                            currentWordIndex = 0
                                        )
                                        speakNextSegment()
                                    } else {
                                        // Dokončeno
                                        _status.value = _status.value.copy(
                                            state = TtsState.IDLE,
                                            progress = 1f,
                                            currentSegment = currentTextSegments.size,
                                            totalSegments = currentTextSegments.size,
                                            currentWordIndex = if (currentSegmentWords.isNotEmpty()) currentSegmentWords.size - 1 else 0
                                        )
                                        resetState()
                                    }
                                }
                            }
                            
                            override fun onError(utteranceId: String) {
                                _status.value = _status.value.copy(
                                    state = TtsState.ERROR,
                                    errorMessage = "Chyba při přehrávání"
                                )
                            }
                            
                            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                                super.onRangeStart(utteranceId, start, end, frame)
                                if (!isReallyPaused) {
                                    // Uložíme pozici pro případné pozastavení
                                    pausedPosition = start
                                    
                                    // Vypočítáme aktuální slovo
                                    updateCurrentWordFromPosition(start)
                                    updateProgress()
                                    // Shift visible window by one line for every 3 words spoken.
                                    // Compute absolute word index across segments.
                                    val wordsBefore = currentTextSegments.take(currentSegmentIndex).sumOf { seg ->
                                        seg.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                    }
                                    val absoluteWordIndex = wordsBefore + currentWordIndex

                                    if (absoluteWordIndex > lastAbsoluteWordIndex) {
                                        val delta = absoluteWordIndex - maxOf(lastAbsoluteWordIndex, -1)
                                        totalWordsSpoken += delta
                                        lastAbsoluteWordIndex = absoluteWordIndex

                                        val newShiftCount = totalWordsSpoken / 3
                                        if (newShiftCount > visibleShiftCount) {
                                            visibleShiftCount = newShiftCount.coerceAtMost(maxOf(0, currentTextSegments.size - 1))
                                                    val newVisible = currentTextSegments.drop(visibleShiftCount).take(3)
                                                    // Ensure currentSegmentText/segmentWords reflect the actual
                                                    // current segment (currentSegmentIndex) rather than the
                                                    // first element of the visible window which may differ.
                                                    val currentSegText = if (currentSegmentIndex in currentTextSegments.indices) {
                                                        currentTextSegments[currentSegmentIndex]
                                                    } else {
                                                        newVisible.firstOrNull() ?: _status.value.currentSegmentText
                                                    }
                                                    val currentSegWords = if (currentSegmentIndex in currentTextSegments.indices) {
                                                        currentTextSegments[currentSegmentIndex].split("\\s+".toRegex()).filter { it.isNotBlank() }
                                                    } else {
                                                        if (newVisible.isNotEmpty()) newVisible[0].split("\\s+".toRegex()).filter { it.isNotBlank() } else emptyList()
                                                    }

                                                    _status.value = _status.value.copy(
                                                        visibleSegments = newVisible,
                                                        currentSegmentText = currentSegText,
                                                        segmentWords = currentSegWords,
                                                        currentWordIndex = currentWordIndex
                                                    )
                                        }
                                    }
                                }
                            }
                        })
                        
                        _status.value = _status.value.copy(
                            state = TtsState.IDLE,
                            isInitialized = true
                        )
                    }
                }
                else -> {
                    _status.value = _status.value.copy(
                        state = TtsState.ERROR,
                        errorMessage = "TTS inicializace selhala"
                    )
                }
            }
        }
    }
    
    fun speak(text: String) {
        val tts = textToSpeech ?: return
        if (!_status.value.isInitialized) return
        
        // Zastavíme případné předchozí přehrávání
        stop()
        
        // Sanitize input (remove addendum markers and other markdown) so TTS
        // always receives clean plain text regardless of caller.
        val cleanedText = TextUtils.extractPlainTextForTts(text)

        // Rozdělíme text na menší segmenty pro lepší kontrolu pokroku
        currentTextSegments = TextUtils.splitTextForTts(cleanedText)
        currentSegmentIndex = 0
        currentUtteranceId = "utterance_${System.currentTimeMillis()}"

        _status.value = _status.value.copy(
            currentText = cleanedText,
            progress = 0f,
            currentSegment = 1,
            totalSegments = currentTextSegments.size,
            currentSegmentText = if (currentTextSegments.isNotEmpty()) currentTextSegments[0] else "",
            segmentWords = emptyList(),
            visibleSegments = currentTextSegments.drop(0).take(3),
            currentWordIndex = 0
        )
        
        isReallyPaused = false
        pausedPosition = 0
        savedProgress = 0f
        totalWordsSpoken = 0
        lastAbsoluteWordIndex = -1
        visibleShiftCount = 0
        speakNextSegment()
    }
    
    private fun speakNextSegment() {
        val tts = textToSpeech ?: return
        if (currentSegmentIndex >= currentTextSegments.size) return

        var segment = currentTextSegments[currentSegmentIndex]

        // Pokud pokračujeme z pozastavení, ořežeme začátek segmentu a uložíme offset
        if (isReallyPaused && pausedPosition > 0 && pausedPosition < segment.length) {
            segment = segment.substring(pausedPosition)
            resumeOffset = pausedPosition
            isReallyPaused = false
            pausedPosition = 0
        } else {
            resumeOffset = 0
        }

        val segmentId = "${currentUtteranceId}_$currentSegmentIndex"

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, segmentId)
        }

        tts.speak(segment, TextToSpeech.QUEUE_FLUSH, params, segmentId)
    }
    
    private fun updateProgress() {
        val totalLength = currentTextSegments.sumOf { it.length }
        val currentProgress = if (totalLength > 0) {
            val segmentsBeforeLength = currentTextSegments.take(currentSegmentIndex).sumOf { it.length }
            val currentSegmentProgress = if (currentTextSegments.size > currentSegmentIndex && currentTextSegments[currentSegmentIndex].length > 0) {
                pausedPosition.toFloat() / currentTextSegments[currentSegmentIndex].length
            } else 0f
            
            (segmentsBeforeLength + (currentSegmentProgress * currentTextSegments[currentSegmentIndex].length)) / totalLength
        } else 0f
        
        // Při obnovení z pozastavení zachováváme progress
        val finalProgress = if (isReallyPaused) {
            maxOf(savedProgress, currentProgress)
        } else {
            maxOf(_status.value.progress, currentProgress) // Zabráníme couvání progressu
        }
        
        _status.value = _status.value.copy(
            progress = finalProgress,
            currentSegment = currentSegmentIndex + 1,
            totalSegments = currentTextSegments.size,
            currentWordIndex = currentWordIndex,
            visibleSegments = _status.value.visibleSegments.ifEmpty { currentTextSegments.drop(currentSegmentIndex).take(3) }
        )
    }
    
    private fun updateCurrentWordFromPosition(charPosition: Int) {
        if (currentSegmentIndex < currentTextSegments.size) {
            val segmentText = currentTextSegments[currentSegmentIndex]
            val effectivePosition = charPosition + resumeOffset
            // Najdeme aktuální slovo podle pozice znaku v původním segmentu
            val words = segmentText.split("\\s+".toRegex()).filter { it.isNotBlank() }
            var currentPos = 0
            for ((index, word) in words.withIndex()) {
                val wordEnd = currentPos + word.length
                if (effectivePosition >= currentPos && effectivePosition <= wordEnd) {
                    currentWordIndex = index
                    break
                }
                currentPos = wordEnd + 1 // +1 pro mezeru
            }
            currentWordIndex = currentWordIndex.coerceIn(0, words.size - 1)
        }
    }
    
    fun pause() {
        if (_status.value.state == TtsState.SPEAKING) {
            textToSpeech?.stop()
            isReallyPaused = true
            savedProgress = _status.value.progress // Uložíme aktuální progress
            _status.value = _status.value.copy(state = TtsState.PAUSED)
        }
    }
    
    fun resume() {
        if (_status.value.state == TtsState.PAUSED && isReallyPaused) {
            // Při resume nastavíme offset pro highlight
            resumeOffset = pausedPosition
            updateCurrentWordFromPosition(0)
            speakNextSegment()
        }
    }
    
    fun stop() {
        textToSpeech?.stop()
        resetState()
        _status.value = _status.value.copy(
            state = TtsState.IDLE,
            currentText = "",
            progress = 0f,
            currentSegment = 0,
            totalSegments = 0,
            currentSegmentText = "",
            segmentWords = emptyList(),
            visibleSegments = emptyList(),
            currentWordIndex = 0
        )
    }
    
    private fun resetState() {
        currentUtteranceId = null
        currentSegmentIndex = 0
        currentTextSegments = emptyList()
        pausedPosition = 0
        resumeOffset = 0
        isReallyPaused = false
        savedProgress = 0f
        currentSegmentWords = emptyList()
        currentWordIndex = 0
        totalWordsSpoken = 0
        lastAbsoluteWordIndex = -1
        visibleShiftCount = 0
    }
    
    fun setSpeed(speed: Float) {
        textToSpeech?.setSpeechRate(speed)
    }
    
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }
    
    fun isAvailable(): Boolean {
        return _status.value.isInitialized
    }
    
    fun skipToNext() {
        if (currentSegmentIndex < currentTextSegments.size - 1) {
            textToSpeech?.stop()
            savedProgress = _status.value.progress // Uložíme progress před skipem
            currentSegmentIndex++
            pausedPosition = 0
            isReallyPaused = false
            currentWordIndex = 0
            updateProgress()
            speakNextSegment()
        }
    }
    
    fun skipToPrevious() {
        if (currentSegmentIndex > 0) {
            textToSpeech?.stop()
            currentSegmentIndex--
            pausedPosition = 0
            isReallyPaused = false
            currentWordIndex = 0
            // Pro předchozí segment nesnižujeme progress
            updateProgress()
            speakNextSegment()
        }
    }
    
    fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        resetState()
        _status.value = TtsStatus()
    }
}