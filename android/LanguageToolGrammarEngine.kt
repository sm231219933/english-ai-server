package com.TeacherTinkl.myapplication.ai

import org.languagetool.JLanguageTool
import org.languagetool.language.AmericanEnglish

data class RuleMatch(
    val fromPos: Int,
    val toPos: Int,
    val errorText: String,
    val message: String,
    val replacements: List<String>,
    val ruleId: String
)

data class GrammarCheckResult(
    val originalText: String,
    val correctedText: String,
    val matches: List<RuleMatch>
)

class LanguageToolGrammarEngine {

    private var jLanguageTool: JLanguageTool? = null
    private var isJLtInitializationAttempted = false

    private fun getOrInitJLanguageTool(): JLanguageTool? {
        if (!isJLtInitializationAttempted) {
            isJLtInitializationAttempted = true
            try {
                jLanguageTool = JLanguageTool(AmericanEnglish())
            } catch (_: Throwable) {
                jLanguageTool = null
            }
        }
        return jLanguageTool
    }

    private val commonSpellingMap = mapOf(
        "teh" to "the",
        "recieve" to "receive",
        "becuase" to "because",
        "definately" to "definitely",
        "seperate" to "separate",
        "tomorow" to "tomorrow"
    )

    private val singularSubjects = listOf(
        "my father", "my mother", "my brother", "my sister",
        "the man", "the woman", "the boy", "the girl"
    )

    fun check(text: String): GrammarCheckResult {
        val matches = mutableListOf<RuleMatch>()
        if (text.isBlank()) {
            return GrammarCheckResult(text, text, emptyList())
        }

        var workingText = text.trim()

        // 1. Safe Official Full JLanguageTool 3,000+ Rules Corpus
        val lt = getOrInitJLanguageTool()
        if (lt != null) {
            try {
                val ltMatches = lt.check(workingText)
                for (match in ltMatches) {
                    val errorSpan = if (match.fromPos < workingText.length && match.toPos <= workingText.length && match.fromPos < match.toPos) {
                        workingText.substring(match.fromPos, match.toPos)
                    } else ""
                    val suggestions = match.suggestedReplacements
                    matches.add(
                        RuleMatch(
                            fromPos = match.fromPos,
                            toPos = match.toPos,
                            errorText = errorSpan,
                            message = match.message ?: "Grammar rule match",
                            replacements = suggestions ?: emptyList(),
                            ruleId = match.rule?.id ?: "OFFICIAL_LT_RULE"
                        )
                    )
                }

                for (match in ltMatches.sortedByDescending { it.fromPos }) {
                    val suggestions = match.suggestedReplacements
                    if (!suggestions.isNullOrEmpty() && match.fromPos >= 0 && match.toPos <= workingText.length && match.fromPos <= match.toPos) {
                        val firstSuggestion = suggestions[0]
                        if (match.rule?.id != "MORFOLOGIK_RULE_EN_US") {
                            workingText = workingText.substring(0, match.fromPos) + firstSuggestion + workingText.substring(match.toPos)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        fun replacePattern(wrong: String, correct: String, ruleId: String, msg: String) {
            val regex = Regex("(?i)\\b${Regex.escape(wrong)}\\b")
            for (m in regex.findAll(workingText)) {
                matches.add(
                    RuleMatch(
                        fromPos = m.range.first,
                        toPos = m.range.last + 1,
                        errorText = m.value,
                        message = msg,
                        replacements = listOf(correct),
                        ruleId = ruleId
                    )
                )
            }
            workingText = workingText.replace(regex) { m ->
                if (m.value.first().isUpperCase()) correct.replaceFirstChar { it.uppercaseChar() } else correct
            }
        }

        // 2. First/Second/Third Person Pronoun & Verb Agreement
        replacePattern("I is", "I am", "SUBJECT_VERB", "Use 'I am' instead of 'I is'")
        replacePattern("I are", "I am", "SUBJECT_VERB", "Use 'I am' instead of 'I are'")
        replacePattern("I has", "I have", "SUBJECT_VERB", "Use 'I have' instead of 'I has'")

        // 'I/You/We/They' + -s verb form mismatch (e.g. "I goes" -> "I go")
        replacePattern("I goes", "I go", "PRONOUN_VERB_S", "Use 'I go' (base form with pronoun 'I').")
        replacePattern("you goes", "you go", "PRONOUN_VERB_S", "Use 'you go'.")
        replacePattern("we goes", "we go", "PRONOUN_VERB_S", "Use 'we go'.")
        replacePattern("they goes", "they go", "PRONOUN_VERB_S", "Use 'they go'.")

        replacePattern("I comes", "I come", "PRONOUN_VERB_S", "Use 'I come'.")
        replacePattern("you comes", "you come", "PRONOUN_VERB_S", "Use 'you come'.")
        replacePattern("we comes", "we come", "PRONOUN_VERB_S", "Use 'we come'.")
        replacePattern("they comes", "they come", "PRONOUN_VERB_S", "Use 'they come'.")

        replacePattern("I wants", "I want", "PRONOUN_VERB_S", "Use 'I want'.")
        replacePattern("you wants", "you want", "PRONOUN_VERB_S", "Use 'you want'.")
        replacePattern("we wants", "we want", "PRONOUN_VERB_S", "Use 'we want'.")
        replacePattern("they wants", "they want", "PRONOUN_VERB_S", "Use 'they want'.")

        replacePattern("I needs", "I need", "PRONOUN_VERB_S", "Use 'I need'.")
        replacePattern("you needs", "you need", "PRONOUN_VERB_S", "Use 'you need'.")
        replacePattern("we needs", "we need", "PRONOUN_VERB_S", "Use 'we need'.")
        replacePattern("they needs", "they need", "PRONOUN_VERB_S", "Use 'they need'.")

        replacePattern("They is", "They are", "SUBJECT_VERB", "Use 'They are' instead of 'They is'")
        replacePattern("We is", "We are", "SUBJECT_VERB", "Use 'We are' instead of 'We is'")
        replacePattern("They was", "They were", "SUBJECT_VERB", "Use 'They were' instead of 'They was'")
        replacePattern("We was", "We were", "SUBJECT_VERB", "Use 'We were' instead of 'We was'")

        // He / She / It + Verb Agreement (3rd person singular)
        replacePattern("he go", "he goes", "HE_SHE_VERB", "Use 'he goes' for third-person singular.")
        replacePattern("she go", "she goes", "HE_SHE_VERB", "Use 'she goes' for third-person singular.")
        replacePattern("it go", "it goes", "HE_SHE_VERB", "Use 'it goes' for third-person singular.")

        replacePattern("he come", "he comes", "HE_SHE_VERB", "Use 'he comes'.")
        replacePattern("she come", "she comes", "HE_SHE_VERB", "Use 'she comes'.")
        replacePattern("it come", "it comes", "HE_SHE_VERB", "Use 'it comes'.")

        replacePattern("he want", "he wants", "HE_SHE_VERB", "Use 'he wants'.")
        replacePattern("she want", "she wants", "HE_SHE_VERB", "Use 'she wants'.")

        replacePattern("he need", "he needs", "HE_SHE_VERB", "Use 'he needs'.")
        replacePattern("she need", "she needs", "HE_SHE_VERB", "Use 'she needs'.")

        replacePattern("he have", "he has", "HE_SHE_VERB", "Use 'he has'.")
        replacePattern("she have", "she has", "HE_SHE_VERB", "Use 'she has'.")
        replacePattern("it have", "it has", "HE_SHE_VERB", "Use 'it has'.")

        replacePattern("he do", "he does", "HE_SHE_VERB", "Use 'he does'.")
        replacePattern("she do", "she does", "HE_SHE_VERB", "Use 'she does'.")
        replacePattern("it do", "it does", "HE_SHE_VERB", "Use 'it does'.")

        replacePattern("he don't", "he doesn't", "HE_SHE_VERB", "Use 'he doesn't'.")
        replacePattern("she don't", "she doesn't", "HE_SHE_VERB", "Use 'she doesn't'.")
        replacePattern("it don't", "it doesn't", "HE_SHE_VERB", "Use 'it doesn't'.")

        // 3. Destination Articles ("to market" -> "to the market", "to office" -> "to the office")
        replacePattern("to market", "to the market", "DESTINATION_ARTICLE", "Use 'to the market'.")
        replacePattern("to office", "to the office", "DESTINATION_ARTICLE", "Use 'to the office'.")

        // 4. Everyday (adjective) vs Every day (adverbial phrase)
        replacePattern("everyday", "every day", "EVERYDAY_EVERY_DAY", "Use 'every day' (two words) as an adverbial phrase at the end of a sentence.")

        // 5. 'There is' + Plural Nouns
        val thereIsPluralRegex = Regex("(?i)\\bthere\\s+is\\s+.*\\b(people|things|cars|books|children|men|women)\\b")
        if (thereIsPluralRegex.containsMatchIn(workingText)) {
            val m = Regex("(?i)\\bthere\\s+is\\b").find(workingText)
            if (m != null) {
                matches.add(
                    RuleMatch(
                        fromPos = m.range.first,
                        toPos = m.range.last + 1,
                        errorText = m.value,
                        message = "Use 'there are' before plural nouns.",
                        replacements = listOf("there are"),
                        ruleId = "THERE_IS_PLURAL"
                    )
                )
                workingText = workingText.replace(Regex("(?i)\\bthere\\s+is\\b"), "there are")
            }
        }

        // 6. Singular Subjects
        for (subject in singularSubjects) {
            replacePattern("$subject were", "$subject was", "SINGULAR_SUBJECT_WAS", "Use '$subject was' for singular subjects.")
            replacePattern("$subject are", "$subject is", "SINGULAR_SUBJECT_IS", "Use '$subject is' for singular subjects.")
            replacePattern("$subject have", "$subject has", "SINGULAR_SUBJECT_HAS", "Use '$subject has' for singular subjects.")
        }

        // 7. Past tense with 'yesterday'
        val goYesterdayRegex = Regex("(?i)\\b(he|she|i|they|we|it|sandeep)\\s+go\\b(.*\\b(yesterday|last week|last year)\\b)")
        for (m in goYesterdayRegex.findAll(workingText)) {
            val subjectStr = m.groupValues[1]
            val rest = m.groupValues[2]
            val replacement = "$subjectStr went$rest"
            matches.add(
                RuleMatch(
                    fromPos = m.range.first,
                    toPos = m.range.last + 1,
                    errorText = m.value,
                    message = "Past time expression requires past tense 'went'.",
                    replacements = listOf(replacement),
                    ruleId = "PAST_TENSE_TIME"
                )
            )
            workingText = workingText.replace(m.value, replacement)
        }

        // 8. Double Past & Auxiliaries
        replacePattern("did went", "did go", "DOUBLE_PAST", "Use base verb 'did go'.")
        replacePattern("doesn't likes", "doesn't like", "AUXILIARY_VERB", "Use base verb 'doesn't like'.")

        // 9. Modals
        val modalPattern = Regex("(?i)\\b(can|should|must)\\s+to\\s+(\\w+)\\b")
        for (m in modalPattern.findAll(workingText)) {
            val modal = m.groupValues[1]
            val verb = m.groupValues[2]
            val replacement = "$modal $verb"
            matches.add(
                RuleMatch(
                    fromPos = m.range.first,
                    toPos = m.range.last + 1,
                    errorText = m.value,
                    message = "Modal verbs don't take 'to': '$replacement'.",
                    replacements = listOf(replacement),
                    ruleId = "MODAL_NO_TO"
                )
            )
            workingText = workingText.replace(m.value, replacement)
        }

        // 10. Article A vs AN rule
        val articleAWithVowelRegex = Regex("(?i)\\ba\\s+([aeiou][a-z]+)\\b")
        for (m in articleAWithVowelRegex.findAll(workingText)) {
            val word = m.groupValues[1]
            if (word.lowercase() != "university" && word.lowercase() != "unit" && word.lowercase() != "user") {
                val replacement = "an $word"
                matches.add(
                    RuleMatch(
                        fromPos = m.range.first,
                        toPos = m.range.last + 1,
                        errorText = m.value,
                        message = "Use 'an' before words starting with a vowel sound: '$replacement'.",
                        replacements = listOf(replacement),
                        ruleId = "ARTICLE_A_AN"
                    )
                )
                workingText = workingText.replace(m.value, replacement)
            }
        }

        // 11. Common Spelling
        for ((misspelled, correct) in commonSpellingMap) {
            replacePattern(misspelled, correct, "SPELLING", "Correct spelling is '$correct'.")
        }

        // 12. Capitalization & Punctuation
        if (workingText.isNotEmpty() && workingText.first().isLowerCase()) {
            val firstChar = workingText.first()
            val capsFirstChar = firstChar.uppercaseChar()
            workingText = capsFirstChar + workingText.substring(1)
        }

        if (workingText.isNotEmpty() && !workingText.endsWith(".") && !workingText.endsWith("?") && !workingText.endsWith("!")) {
            workingText += "."
        }

        return GrammarCheckResult(
            originalText = text,
            correctedText = workingText,
            matches = matches.distinctBy { (it.ruleId + it.errorText).lowercase() }
        )
    }
}
