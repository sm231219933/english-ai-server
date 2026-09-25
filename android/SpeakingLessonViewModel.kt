package com.TeacherTinkl.myapplication.ui.screens

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.TeacherTinkl.myapplication.ai.SpeakingEngine
import com.TeacherTinkl.myapplication.ai.SpeakingEvaluationResult
import com.TeacherTinkl.myapplication.data.LessonSentence
import com.TeacherTinkl.myapplication.data.SpeakingLesson
import com.TeacherTinkl.myapplication.data.SpeakingReport
import com.TeacherTinkl.myapplication.data.TinklDatabase
import com.TeacherTinkl.myapplication.voice.TinklVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeakingLessonViewModel(
    application: Application,
    val lessonId: Int
) : AndroidViewModel(application) {

    private val db = TinklDatabase.getDatabase(application)
    private val dao = db.tinklDao()
    private val speakingEngine = SpeakingEngine()
    private val voiceManager = TinklVoiceManager(application)

    var lessonState by mutableStateOf<SpeakingLesson?>(null)
        private set

    var allSentences by mutableStateOf<List<LessonSentence>>(emptyList())
        private set

    var selectedPersonFilter by mutableStateOf("All")
        private set

    val sentencesList: List<LessonSentence>
        get() {
            return if (selectedPersonFilter == "All") {
                allSentences
            } else {
                allSentences.filter { it.personTag.equals(selectedPersonFilter, ignoreCase = true) }
            }
        }

    var currentSentenceIndex by mutableIntStateOf(0)
        private set

    var isTtsPlaying by mutableStateOf(false)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var liveSpokenText by mutableStateOf("")
        private set

    var evaluationResult by mutableStateOf<SpeakingEvaluationResult?>(null)
        private set

    var statusMessage by mutableStateOf("Tap 🎤 to speak, then tap Send")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentSentence: LessonSentence?
        get() = sentencesList.getOrNull(currentSentenceIndex)

    init {
        voiceManager.init()
        loadLessonData()
    }

    fun setPersonFilter(filter: String) {
        stopSpeaking()
        stopListeningUserSpeech()
        selectedPersonFilter = filter
        currentSentenceIndex = 0
        evaluationResult = null
        liveSpokenText = ""
        statusMessage = "Filter: $filter sentences"
        autoSpeakSentence2Times()
    }

    private fun loadLessonData() {
        viewModelScope.launch(Dispatchers.IO) {
            val lesson = dao.getSpeakingLessonById(lessonId)
            lessonState = lesson

            dao.getSentencesForLesson(lessonId).collectLatest { sentences ->
                allSentences = sentences
                if (sentences.isNotEmpty() && evaluationResult == null) {
                    statusMessage = "Ready for Sentence ${currentSentenceIndex + 1} of ${sentences.size} 🌸"
                    autoSpeakSentence2Times()
                }
            }
        }
    }

    fun autoSpeakSentence2Times() {
        val sentence = currentSentence ?: return
        isTtsPlaying = true
        statusMessage = "Tinkl is speaking (2 times)... Listen carefully 🎧"
        errorMessage = null

        voiceManager.speakWithCallback(sentence.expectedText) {
            if (isTtsPlaying) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isTtsPlaying) {
                        voiceManager.speakWithCallback(sentence.expectedText) {
                            isTtsPlaying = false
                            statusMessage = "Now your turn! Tap 🎤 to speak, then tap Send."
                        }
                    }
                }, 400L)
            }
        }
    }

    fun playCurrentSentenceTts() {
        autoSpeakSentence2Times()
    }

    fun stopSpeaking() {
        isTtsPlaying = false
        voiceManager.stopSpeaking()
    }

    fun startListeningUserSpeech() {
        stopSpeaking() // STOP TTS INSTANTLY WHEN USER TOUCHES MIC!
        isRecording = true
        liveSpokenText = ""
        errorMessage = null
        statusMessage = "Recording active... Speak now, then tap 'Send & Check' 🎤"

        voiceManager.startListening(
            onResult = { text, isFinal ->
                liveSpokenText = text
                if (isFinal) {
                    isRecording = false
                    statusMessage = "Tap 'Send & Check' or tap Mic to re-speak."
                }
            },
            onError = { error ->
                if (liveSpokenText.isBlank()) {
                    errorMessage = error
                    statusMessage = "Mic error. Tap 🎤 to try again."
                }
            }
        )
    }

    fun stopListeningUserSpeech() {
        isRecording = false
        voiceManager.stopListening()
    }

    fun sendAndEvaluateSpeech() {
        stopSpeaking()
        if (isRecording) {
            stopListeningUserSpeech()
        }
        if (liveSpokenText.isNotBlank()) {
            statusMessage = "Evaluating speech..."
            processSpokenText(liveSpokenText)
        } else {
            statusMessage = "Please speak something first, then tap Send! 🎤"
        }
    }

    private fun processSpokenText(spokenText: String) {
        val expected = currentSentence?.expectedText
        val result = speakingEngine.evaluateSpeech(spokenText, expected)
        evaluationResult = result

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSpeakingReport(
                SpeakingReport(
                    lessonId = lessonId,
                    rawTranscribedText = spokenText,
                    correctedText = result.grammarResult.correctedText,
                    matchScore = result.matchPercentage,
                    mistakesCount = result.grammarResult.matches.size
                )
            )
        }

        statusMessage = result.feedbackMessage
    }

    fun retryCurrentSentence() {
        stopSpeaking()
        stopListeningUserSpeech()
        evaluationResult = null
        liveSpokenText = ""
        errorMessage = null
        statusMessage = "Tap 🎤 to speak again!"
        autoSpeakSentence2Times()
    }

    fun nextSentence() {
        stopSpeaking()
        stopListeningUserSpeech()
        if (currentSentenceIndex < sentencesList.size - 1) {
            currentSentenceIndex++
            evaluationResult = null
            liveSpokenText = ""
            errorMessage = null
            statusMessage = "Sentence ${currentSentenceIndex + 1} of ${sentencesList.size} 🌸"
            autoSpeakSentence2Times()
        } else {
            statusMessage = "🎉 Lesson Completed! Great job!"
        }
    }

    override fun onCleared() {
        voiceManager.destroy()
    }
}
