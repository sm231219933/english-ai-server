package com.TeacherTinkl.myapplication.ui.screens

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.TeacherTinkl.myapplication.ai.SpeakingEngine
import com.TeacherTinkl.myapplication.ai.SpeakingEvaluationResult
import com.TeacherTinkl.myapplication.data.SpeakingGymSentence
import com.TeacherTinkl.myapplication.data.TinklDatabase
import com.TeacherTinkl.myapplication.voice.TinklVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeakingGymViewModel(
    application: Application,
    val initialLevel: Int = 1
) : AndroidViewModel(application) {

    private val db = TinklDatabase.getDatabase(application)
    private val dao = db.tinklDao()
    private val speakingEngine = SpeakingEngine()
    private val voiceManager = TinklVoiceManager(application)

    var currentLevel by mutableIntStateOf(initialLevel)
        private set

    var gymSentences by mutableStateOf<List<SpeakingGymSentence>>(emptyList())
        private set

    var currentIndex by mutableIntStateOf(0)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var isTtsPlaying by mutableStateOf(false)
        private set

    var evaluationResult by mutableStateOf<SpeakingEvaluationResult?>(null)
        private set

    var timerSeconds by mutableIntStateOf(60)
        private set

    var isTimerRunning by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf("Welcome to English Speaking Gym! 🏋️")
        private set

    val currentGymSentence: SpeakingGymSentence?
        get() = gymSentences.getOrNull(currentIndex)

    init {
        voiceManager.init()
        loadSentencesForLevel(initialLevel)
    }

    fun selectLevel(level: Int) {
        currentLevel = level
        currentIndex = 0
        evaluationResult = null
        isTimerRunning = false
        timerSeconds = 60
        loadSentencesForLevel(level)
    }

    private fun loadSentencesForLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getGymSentencesByLevel(level).collectLatest { list ->
                gymSentences = list
                if (list.isNotEmpty()) {
                    statusMessage = "Level $level: Ready for Sentence ${currentIndex + 1} 🎯"
                }
            }
        }
    }

    fun playTtsPrompt() {
        val sentence = currentGymSentence ?: return
        isTtsPlaying = true
        statusMessage = "Tinkl is speaking... 🎧"

        voiceManager.speakWithCallback(sentence.promptQuestion) {
            isTtsPlaying = false
            statusMessage = "Now speak your response! 🎤"
        }
    }

    fun startSpeaking() {
        isRecording = true
        statusMessage = "Listening... Speak now! 🎤"

        if (currentLevel == 5 && !isTimerRunning) {
            start60SecondTimer()
        }

        voiceManager.startListening(
            onResult = { spokenText, isFinal ->
                if (isFinal) {
                    isRecording = false
                    statusMessage = "Evaluating your speech..."
                    processSpokenText(spokenText)
                }
            },
            onError = { error ->
                isRecording = false
                statusMessage = "Mic error: $error. Tap 🎤 to try again."
            }
        )
    }

    fun stopSpeaking() {
        isRecording = false
        voiceManager.stopListening()
    }

    private fun processSpokenText(spokenText: String) {
        val expected = currentGymSentence?.expectedSentence
        val result = speakingEngine.evaluateSpeech(spokenText, expected)
        evaluationResult = result
        statusMessage = result.feedbackMessage
    }

    private fun start60SecondTimer() {
        isTimerRunning = true
        timerSeconds = 60
        viewModelScope.launch {
            while (timerSeconds > 0 && isTimerRunning) {
                delay(1000)
                timerSeconds--
            }
            isTimerRunning = false
            statusMessage = "⏱️ Time's up! Review your speaking report below."
        }
    }

    fun nextGymSentence() {
        if (currentIndex < gymSentences.size - 1) {
            currentIndex++
            evaluationResult = null
            statusMessage = "Level $currentLevel: Sentence ${currentIndex + 1} of ${gymSentences.size}"
        } else if (currentLevel < 5) {
            selectLevel(currentLevel + 1)
        } else {
            statusMessage = "🎉 Gym Workout Completed! Outstanding job!"
        }
    }

    fun retryGymSentence() {
        evaluationResult = null
        statusMessage = "Tap 🎤 to try again!"
    }

    override fun onCleared() {
        voiceManager.destroy()
    }
}
