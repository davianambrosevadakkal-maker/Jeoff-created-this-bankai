package com.example.domain.srs

object ClozeHelper {

    private val CLOZE_REGEX = Regex("""\{\{c(\d+)::(.*?)(?:::([^}]*))?\}\}""")

    fun hasCloze(text: String): Boolean {
        return CLOZE_REGEX.containsMatchIn(text)
    }

    /**
     * Extracts the target answer for a specific cloze deletion (e.g. c1)
     */
    fun extractClozeAnswer(text: String, clozeNum: Int = 1): String? {
        val matches = CLOZE_REGEX.findAll(text)
        for (match in matches) {
            val num = match.groupValues[1].toIntOrNull() ?: 1
            if (num == clozeNum) {
                return match.groupValues[2].trim()
            }
        }
        return matches.firstOrNull()?.groupValues?.get(2)?.trim()
    }

    /**
     * Renders the cloze prompt for the question side.
     * Target cloze is rendered as "[...]" or "[hint]".
     * Other clozes (e.g. c2 if reviewing c1) are shown as plain text.
     */
    fun renderClozeQuestion(text: String, clozeNum: Int = 1): String {
        return CLOZE_REGEX.replace(text) { match ->
            val num = match.groupValues[1].toIntOrNull() ?: 1
            val answer = match.groupValues[2]
            val hint = match.groupValues.getOrNull(3)

            if (num == clozeNum) {
                if (!hint.isNullOrBlank()) "[$hint]" else "[...]"
            } else {
                answer
            }
        }
    }

    /**
     * Renders the cloze with the answer revealed and bracketed.
     */
    fun renderClozeRevealed(text: String, clozeNum: Int = 1): String {
        return CLOZE_REGEX.replace(text) { match ->
            val num = match.groupValues[1].toIntOrNull() ?: 1
            val answer = match.groupValues[2]

            if (num == clozeNum) {
                "【$answer】"
            } else {
                answer
            }
        }
    }
}
