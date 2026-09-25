package com.TeacherTinkl.myapplication.ui.screens

import android.app.Application
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
import com.TeacherTinkl.myapplication.data.TinklDatabase
import com.TeacherTinkl.myapplication.voice.TinklVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeakingTestViewModel(
    application: Application,
    val lessonId: Int
) : AndroidViewModel(application) {

    private val db = TinklDatabase.getDatabase(application)
    private val dao = db.tinklDao()
    private val speakingEngine = SpeakingEngine()
    private val voiceManager = TinklVoiceManager(application)

    var lessonState by mutableStateOf<SpeakingLesson?>(null)
        private set

    var sentencesList by mutableStateOf<List<LessonSentence>>(emptyList())
        private set

    var currentSentenceIndex by mutableIntStateOf(0)
        private set

    var isHintUnlocked by mutableStateOf(false)
        private set

    var isPassed by mutableStateOf(false)
        private set

    var isTestCompleted by mutableStateOf(false)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var liveSpokenText by mutableStateOf("")
        private set

    var evaluationResult by mutableStateOf<SpeakingEvaluationResult?>(null)
        private set

    var statusMessage by mutableStateOf("Translate Hindi to English! Tap 🎤 to speak, then tap Send.")
        private set

    var passedCount by mutableIntStateOf(0)
        private set

    val currentSentence: LessonSentence?
        get() = sentencesList.getOrNull(currentSentenceIndex)

    init {
        voiceManager.init()
        loadTestData()
    }

    private fun loadTestData() {
        viewModelScope.launch(Dispatchers.IO) {
            val lesson = dao.getSpeakingLessonById(lessonId)
            lessonState = lesson

            dao.getSentencesForLesson(lessonId).collectLatest { sentences ->
                sentencesList = sentences
                if (sentences.isNotEmpty()) {
                    resetSentenceState()
                }
            }
        }
    }

    private fun resetSentenceState() {
        isHintUnlocked = false
        isPassed = false
        liveSpokenText = ""
        evaluationResult = null
        statusMessage = "Sentence ${currentSentenceIndex + 1} of ${sentencesList.size}: Translate in English! 🎤"
    }

    fun revealHint() {
        isHintUnlocked = true
        statusMessage = "💡 Hint Unlocked. Read the correct answer and speak it to pass."
    }

    fun startListeningUserSpeech() {
        voiceManager.stopSpeaking()
        isRecording = true
        liveSpokenText = ""
        statusMessage = "Recording active... Speak English translation now, then tap 'Send & Check' 🎤"

        voiceManager.startListening(
            onResult = { text, isFinal ->
                liveSpokenText = text
                if (isFinal) {
                    isRecording = false
                    statusMessage = "Tap 'Send & Evaluate' or tap Mic to re-speak."
                }
            },
            onError = { error ->
                if (liveSpokenText.isBlank()) {
                    statusMessage = "Mic error. Tap 🎤 to speak."
                }
            }
        )
    }

    fun stopListeningUserSpeech() {
        isRecording = false
        voiceManager.stopListening()
    }

    fun sendAndEvaluateTestAnswer() {
        if (isRecording) {
            stopListeningUserSpeech()
        }

        if (liveSpokenText.isBlank()) {
            statusMessage = "Please speak the English sentence first, then tap Send! 🎤"
            return
        }

        val expected = currentSentence?.expectedText ?: return
        val result = speakingEngine.evaluateSpeech(liveSpokenText, expected)
        evaluationResult = result

        if (result.matchPercentage >= 80 || result.isUnderstood) {
            isPassed = true
            passedCount++
            val scoreLabel = if (result.matchPercentage >= 95) "PERFECT 100%" else "${result.matchPercentage}% Match"
            statusMessage = "🎉 $scoreLabel! Correct sentence! Tap 'Next' to continue."
            voiceManager.speak("Great job! $scoreLabel!")
        } else {
            statusMessage = "❌ Incorrect. Try again or tap to reveal the hint! 🎤"
        }
    }

    fun nextSentence() {
        if (!isPassed) {
            statusMessage = "You must speak the sentence correctly to unlock Next! 🔒"
            return
        }

        if (currentSentenceIndex < sentencesList.size - 1) {
            currentSentenceIndex++
            resetSentenceState()
        } else {
            isTestCompleted = true
            statusMessage = "🏆 Speech Test Completed! Great Job!"
        }
    }

    override fun onCleared() {
        voiceManager.destroy()
    }
}
