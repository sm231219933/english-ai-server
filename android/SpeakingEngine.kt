package com.TeacherTinkl.myapplication.ai

data class SpeakingEvaluationResult(
    val transcribedText: String,
    val expectedText: String?,
    val matchPercentage: Int, // 0 to 100%
    val grammarResult: GrammarCheckResult,
    val isUnderstood: Boolean,
    val feedbackMessage: String
)

class SpeakingEngine {

    private val grammarEngine = LanguageToolGrammarEngine()

    fun evaluateSpeech(
        userSpokenText: String,
        expectedText: String? = null
    ): SpeakingEvaluationResult {
        val grammarResult = grammarEngine.check(userSpokenText)

        val matchScore = if (!expectedText.isNullOrBlank()) {
            calculateSimilarityScore(userSpokenText, expectedText)
        } else {
            100
        }

        val isUnderstood = matchScore >= 65 && grammarResult.matches.size <= 2

        val feedbackMessage = when {
            matchScore >= 90 && grammarResult.matches.isEmpty() ->
                "🌟 Excellent! Perfect pronunciation & grammar!"
            grammarResult.matches.isNotEmpty() ->
                "💡 Good effort! Check the grammar correction below."
            matchScore >= 70 ->
                "👍 Understandable sentence! Practice once more for perfection."
            else ->
                "🔄 Try saying it again clearly."
        }

        return SpeakingEvaluationResult(
            transcribedText = userSpokenText,
            expectedText = expectedText,
            matchPercentage = matchScore,
            grammarResult = grammarResult,
            isUnderstood = isUnderstood,
            feedbackMessage = feedbackMessage
        )
    }

    private fun calculateSimilarityScore(spoken: String, expected: String): Int {
        val cleanSpoken = spoken.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex()).filter { it.isNotBlank() }
        val cleanExpected = expected.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (cleanExpected.isEmpty()) return 100
        if (cleanSpoken.isEmpty()) return 0

        val expectedSet = cleanExpected.toSet()
        val commonWords = cleanSpoken.count { expectedSet.contains(it) }

        val matchRatio = (commonWords.toDouble() / cleanExpected.size) * 100
        return matchRatio.toInt().coerceIn(0, 100)
    }
}
