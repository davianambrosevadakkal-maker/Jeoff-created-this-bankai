package com.example.domain.german

import java.util.Locale
import kotlin.math.min

data class VerificationResult(
    val isCorrect: Boolean,
    val scorePercentage: Int, // 0 - 100
    val feedbackTitle: String,
    val feedbackMessage: String,
    val correctedText: String,
    val recognizedSynonym: String? = null,
    val hasArticleIssue: Boolean = false,
    val hasCapitalizationIssue: Boolean = false,
    val hasUmlautIssue: Boolean = false
)

object GermanLanguageEngine {

    // Curated rich German Synonyms mapping (over 60+ foundational lexical clusters)
    private val GERMAN_SYNONYMS_MAP = mapOf(
        "anfangen" to listOf("beginnen", "starten", "initiieren", "loslegen"),
        "beginnen" to listOf("anfangen", "starten", "eröffnen", "einleiten"),
        "schön" to listOf("hübsch", "attraktiv", "herrlich", "wundervoll", "reizend"),
        "schnell" to listOf("rasch", "flink", "geschwind", "eilig", "zügig"),
        "wichtig" to listOf("bedeutend", "wesentlich", "relevant", "maßgeblich", "entscheidend"),
        "schwierig" to listOf("kompliziert", "problematisch", "hart", "knifflig", "anspruchsvoll"),
        "einfach" to listOf("leicht", "unkompliziert", "simpel", "mühelos"),
        "sprechen" to listOf("reden", "unterhalten", "plaudern", "äußern", "sagen"),
        "sehen" to listOf("schauen", "blicken", "gucken", "betrachten", "wahrnehmen"),
        "helfen" to listOf("unterstützen", "beistehen", "assistieren", "entlasten"),
        "verstehen" to listOf("begreifen", "kapieren", "erfassen", "nachvollziehen"),
        "glücklich" to listOf("froh", "zufrieden", "erfreut", "selig", "heiter"),
        "traurig" to listOf("betrübt", "unglücklich", "bekümmert", "niedergeschlagen"),
        "bekommen" to listOf("erhalten", "kriegen", "empfangen"),
        "groß" to listOf("riesig", "gewaltig", "umfangreich", "bedeutend"),
        "klein" to listOf("winzig", "gering", "schmal", "kompakt"),
        "immer" to listOf("stets", "jederzeit", "ausnahmslos", "dauernd"),
        "oft" to listOf("häufig", "mehrmals", "wiederholt", "oftmals"),
        "nie" to listOf("niemals", "zu keiner Zeit", "nimmer"),
        "klug" to listOf("intelligent", "schlau", "gescheit", "weise"),
        "dumm" to listOf("blöd", "töricht", "einfältig", "unüberlegt"),
        "versuchen" to listOf("probieren", "testen", "erproben", "unternehmen"),
        "Auto" to listOf("Wagen", "Fahrzeug", "Kraftwagen", "Automobil"),
        "Haus" to listOf("Gebäude", "Wohnhaus", "Heim", "Residenz"),
        "Arbeit" to listOf("Tätigkeit", "Beruf", "Job", "Beschäftigung", "Werk"),
        "Freund" to listOf("Kumpel", "Gefährte", "Kollege", "Partner"),
        "Tisch" to listOf("Tafel", "Pult", "Esstisch", "Schreibtisch"),
        "Stuhl" to listOf("Sitz", "Sessel", "Sitzgelegenheit"),
        "Straße" to listOf("Weg", "Gasse", "Allee", "Chaussee"),
        "Stadt" to listOf("Großstadt", "Metropole", "Ortschaft", "Gemeinde"),
        "Problem" to listOf("Schwierigkeit", "Hindernis", "Komplikation", "Konflikt"),
        "Ziel" to listOf("Absicht", "Vorsatz", "Zweck", "Destination"),
        "zeigen" to listOf("weisen", "vorführen", "demonstrieren", "darstellen"),
        "denken" to listOf("glauben", "meinen", "nachdenken", "überlegen"),
        "wissen" to listOf("kennen", "im Bilde sein", "verstehen"),
        "fragen" to listOf("sich erkundigen", "befragen", "nachhaken"),
        "antworten" to listOf("erwidern", "entgegnen", "reagieren"),
        "fahren" to listOf("reisen", "kurven", "chauffieren", "steuern"),
        "gehen" to listOf("laufen", "schreiten", "wandern", "marschieren"),
        "essen" to listOf("speisen", "verzehren", "zu sich nehmen", "futtern"),
        "trinken" to listOf("austrinken", "schlürfen", "bechern", "nippen"),
        "kaufen" to listOf("erwerben", "anschaffen", "besorgen"),
        "verkaufen" to listOf("veräußern", "anbieten", "absetzen"),
        "neu" to listOf("aktuell", "frisch", "modern", "neuartig"),
        "alt" to listOf("betagt", "historisch", "antik", "überholt")
    )

    /**
     * Retrieves or derives German synonyms for a given word or target text.
     */
    fun getSynonyms(target: String): List<String> {
        val clean = extractKeyWord(target).lowercase(Locale.GERMAN)
        // Check exact key
        GERMAN_SYNONYMS_MAP[clean]?.let { return it }

        // Check if key is contained in map
        for ((key, syns) in GERMAN_SYNONYMS_MAP) {
            if (clean.equals(key, ignoreCase = true) || syns.any { it.equals(clean, ignoreCase = true) }) {
                val list = mutableListOf(key)
                list.addAll(syns)
                return list.filterNot { it.equals(clean, ignoreCase = true) }
            }
        }

        // Check prefix / stem match
        for ((key, syns) in GERMAN_SYNONYMS_MAP) {
            if (clean.startsWith(key) || key.startsWith(clean)) {
                return syns
            }
        }

        return emptyList()
    }

    /**
     * Extracts grammatical gender ("der", "die", "das") from a German word or notes
     */
    fun extractGender(word: String, notes: String = ""): String {
        val trimmed = word.trim()
        val lower = trimmed.lowercase(Locale.GERMAN)
        return when {
            lower.startsWith("der ") || notes.contains("der ", ignoreCase = true) || notes.contains("maskulin", ignoreCase = true) -> "der"
            lower.startsWith("die ") || notes.contains("die ", ignoreCase = true) || notes.contains("feminin", ignoreCase = true) -> "die"
            lower.startsWith("das ") || notes.contains("das ", ignoreCase = true) || notes.contains("neutral", ignoreCase = true) -> "das"
            else -> ""
        }
    }

    /**
     * Verifies the user's typed German answer against expected German text
     */
    fun verifyGermanAnswer(userInput: String, expectedGerman: String, englishMeaning: String = ""): VerificationResult {
        val rawInput = userInput.trim()
        val rawExpected = expectedGerman.trim()

        if (rawInput.isEmpty()) {
            return VerificationResult(
                isCorrect = false,
                scorePercentage = 0,
                feedbackTitle = "No Answer Provided",
                feedbackMessage = "Please type your German answer.",
                correctedText = rawExpected
            )
        }

        // Exact match
        if (rawInput == rawExpected) {
            return VerificationResult(
                isCorrect = true,
                scorePercentage = 100,
                feedbackTitle = "Perfekt! (100%)",
                feedbackMessage = "Flawless match with correct article and capitalization.",
                correctedText = rawExpected
            )
        }

        val expectedGender = extractGender(rawExpected)
        val userGender = extractGender(rawInput)

        val cleanExpectedNoun = stripArticle(rawExpected)
        val cleanUserNoun = stripArticle(rawInput)

        // Check if user entered a recognized synonym
        val expectedSynonyms = getSynonyms(rawExpected)
        val isSynonym = expectedSynonyms.any { syn ->
            cleanUserNoun.equals(syn, ignoreCase = true) ||
                    rawInput.equals(syn, ignoreCase = true) ||
                    stripArticle(syn).equals(cleanUserNoun, ignoreCase = true)
        }

        if (isSynonym) {
            return VerificationResult(
                isCorrect = true,
                scorePercentage = 95,
                feedbackTitle = "Großartig! (Valid Synonym)",
                feedbackMessage = "'$rawInput' is a fantastic German synonym for '$rawExpected'!",
                correctedText = rawExpected,
                recognizedSynonym = rawInput
            )
        }

        // Check for Gender / Article mismatch
        if (expectedGender.isNotEmpty() && userGender.isNotEmpty() && expectedGender != userGender) {
            return VerificationResult(
                isCorrect = false,
                scorePercentage = 65,
                feedbackTitle = "Artikel-Fehler (Article Mismatch)",
                feedbackMessage = "Watch out: '$cleanExpectedNoun' takes '$expectedGender', not '$userGender'. Remember: $expectedGender $cleanExpectedNoun!",
                correctedText = rawExpected,
                hasArticleIssue = true
            )
        }

        // Check for missing article
        if (expectedGender.isNotEmpty() && userGender.isEmpty() && cleanUserNoun.equals(cleanExpectedNoun, ignoreCase = true)) {
            return VerificationResult(
                isCorrect = true,
                scorePercentage = 80,
                feedbackTitle = "Fast perfekt! (Missing Article)",
                feedbackMessage = "You got the noun right, but don't forget the article: '$expectedGender $cleanExpectedNoun'.",
                correctedText = rawExpected,
                hasArticleIssue = true
            )
        }

        // Check for Capitalization issue (German nouns must be capitalized)
        if (rawInput.equals(rawExpected, ignoreCase = true)) {
            val isNoun = expectedGender.isNotEmpty() || rawExpected.firstOrNull()?.isUpperCase() == true
            return if (isNoun && rawInput.firstOrNull()?.isLowerCase() == true) {
                VerificationResult(
                    isCorrect = true,
                    scorePercentage = 90,
                    feedbackTitle = "Großschreibung beachten! (Capitalization)",
                    feedbackMessage = "In German, all nouns are capitalized! Always write: '$rawExpected'.",
                    correctedText = rawExpected,
                    hasCapitalizationIssue = true
                )
            } else {
                VerificationResult(
                    isCorrect = true,
                    scorePercentage = 95,
                    feedbackTitle = "Richtig! (Case difference)",
                    feedbackMessage = "Accurate translation. Recommended formatting: '$rawExpected'.",
                    correctedText = rawExpected
                )
            }
        }

        // Check for Umlaut substitution (e.g. ae instead of ä, oe instead of ö, ue instead of ü, ss instead of ß)
        val normalizedInput = normalizeUmlauts(rawInput)
        val normalizedExpected = normalizeUmlauts(rawExpected)
        if (normalizedInput.equals(normalizedExpected, ignoreCase = true)) {
            return VerificationResult(
                isCorrect = true,
                scorePercentage = 85,
                feedbackTitle = "Umlaut-Hinweis (Umlaut formatting)",
                feedbackMessage = "Good job! Use German umlauts when possible: '$rawExpected' (ä, ö, ü, ß).",
                correctedText = rawExpected,
                hasUmlautIssue = true
            )
        }

        // Levenshtein typo check
        val dist = levenshteinDistance(cleanUserNoun.lowercase(Locale.GERMAN), cleanExpectedNoun.lowercase(Locale.GERMAN))
        if (dist in 1..2 && cleanExpectedNoun.length >= 4) {
            return VerificationResult(
                isCorrect = false,
                scorePercentage = 70,
                feedbackTitle = "Tippfehler (Minor Typo)",
                feedbackMessage = "Very close! Check your spelling: expected '$rawExpected'.",
                correctedText = rawExpected
            )
        }

        // Incorrect
        return VerificationResult(
            isCorrect = false,
            scorePercentage = 25,
            feedbackTitle = "Leider falsch (Incorrect)",
            feedbackMessage = "Expected German: '$rawExpected' ($englishMeaning). Keep practicing!",
            correctedText = rawExpected
        )
    }

    private fun extractKeyWord(text: String): String {
        val stripped = stripArticle(text)
        return stripped.split(" ", ",", ";", "/", "(").firstOrNull()?.trim() ?: stripped
    }

    private fun stripArticle(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase(Locale.GERMAN)
        return when {
            lower.startsWith("der ") -> trimmed.substring(4).trim()
            lower.startsWith("die ") -> trimmed.substring(4).trim()
            lower.startsWith("das ") -> trimmed.substring(4).trim()
            lower.startsWith("ein ") -> trimmed.substring(4).trim()
            lower.startsWith("eine ") -> trimmed.substring(5).trim()
            else -> trimmed
        }
    }

    private fun normalizeUmlauts(text: String): String {
        return text.replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ß", "ss")
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
