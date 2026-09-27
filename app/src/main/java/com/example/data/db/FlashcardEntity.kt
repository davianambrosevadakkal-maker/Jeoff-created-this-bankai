package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["orderIndex"]),
        Index(value = ["dueTimestamp"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val notes: String = "",
    val gender: String = "", // "der", "die", "das", or empty
    val partOfSpeech: String = "", // "noun", "verb", "adjective", etc.
    val synonyms: String = "", // comma-separated German synonyms
    val tags: String = "", // comma-separated tags
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val orderIndex: Int = 1,
    // Spaced repetition fields (Anki SM-2)
    val dueTimestamp: Long = 0L,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val state: Int = 0, // 0 = New, 1 = Learning, 2 = Review, 3 = Suspended
    val flag: Int = 0, // 0 = None, 1 = Red, 2 = Orange, 3 = Green, 4 = Blue
    val createdAt: Long = System.currentTimeMillis()
)
