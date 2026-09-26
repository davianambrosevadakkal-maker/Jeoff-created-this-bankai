package com.example

import com.example.data.db.FlashcardEntity
import com.example.domain.german.GermanLanguageEngine
import com.example.domain.importer.CsvColumnMapping
import com.example.domain.importer.CsvImporter
import com.example.domain.srs.AnkiSrsEngine
import com.example.domain.srs.SrsRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGermanVerification_exactMatch() {
        val result = GermanLanguageEngine.verifyGermanAnswer(
            userInput = "der Tisch",
            expectedGerman = "der Tisch",
            englishMeaning = "table"
        )
        assertTrue(result.isCorrect)
        assertEquals(100, result.scorePercentage)
    }

    @Test
    fun testGermanVerification_articleMismatch() {
        val result = GermanLanguageEngine.verifyGermanAnswer(
            userInput = "die Tisch",
            expectedGerman = "der Tisch",
            englishMeaning = "table"
        )
        assertFalse(result.isCorrect)
        assertTrue(result.hasArticleIssue)
        assertTrue(result.feedbackMessage.contains("der"))
    }

    @Test
    fun testGermanVerification_synonymRecognition() {
        val result = GermanLanguageEngine.verifyGermanAnswer(
            userInput = "anfangen",
            expectedGerman = "beginnen",
            englishMeaning = "to begin"
        )
        assertTrue(result.isCorrect)
        assertEquals("anfangen", result.recognizedSynonym)
    }

    @Test
    fun testGermanVerification_capitalizationHint() {
        val result = GermanLanguageEngine.verifyGermanAnswer(
            userInput = "der hund",
            expectedGerman = "der Hund",
            englishMeaning = "dog"
        )
        assertTrue(result.isCorrect)
        assertTrue(result.hasCapitalizationIssue)
    }

    @Test
    fun testAnkiSrsEngine_intervals() {
        val newCard = FlashcardEntity(
            deckId = 1,
            front = "das Buch",
            back = "book",
            orderIndex = 1
        )

        // Good on new card -> 1 day
        val goodResult = AnkiSrsEngine.calculateNextReview(newCard, SrsRating.GOOD)
        assertEquals(1, goodResult.nextIntervalDays)

        // Easy on new card -> 4 days
        val easyResult = AnkiSrsEngine.calculateNextReview(newCard, SrsRating.EASY)
        assertEquals(4, easyResult.nextIntervalDays)

        // Again -> reset to 0 (immediate relearning), lapse incremented
        val againResult = AnkiSrsEngine.calculateNextReview(newCard.copy(repetitions = 3, intervalDays = 10), SrsRating.AGAIN)
        assertEquals(0, againResult.nextIntervalDays)
        assertEquals(1, againResult.updatedCard.lapses)
    }

    @Test
    fun testCsvImporter_customMappingAndQuoting() {
        val csv = """
            "German Word","English Meaning","Gender"
            "die Herausforderung","challenge, great obstacle","die"
            "der Fortschritt","progress","der"
        """.trimIndent()

        val preview = CsvImporter.analyzeCsv(csv)
        assertEquals(3, preview.headers.size)
        assertEquals(',', preview.delimiter)

        val mapping = CsvColumnMapping(frontIndex = 0, backIndex = 1, genderIndex = 2)
        val cards = CsvImporter.parseToFlashcards(csv, deckId = 1, mapping = mapping)
        assertEquals(2, cards.size)
        assertEquals("die Herausforderung", cards[0].front)
        assertEquals("challenge, great obstacle", cards[0].back)
        assertEquals("die", cards[0].gender)
    }
}
