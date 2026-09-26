package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_logs",
    indices = [Index(value = ["cardId"]), Index(value = ["timestamp"])]
)
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val rating: Int, // 1 = Again, 2 = Hard, 3 = Good, 4 = Easy
    val intervalDays: Int,
    val timestamp: Long = System.currentTimeMillis()
)
