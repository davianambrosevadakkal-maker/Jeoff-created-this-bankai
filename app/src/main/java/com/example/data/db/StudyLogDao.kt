package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudyLogEntity): Long

    @Query("SELECT COUNT(*) FROM study_logs WHERE timestamp >= :sinceTimestamp")
    suspend fun getReviewsSince(sinceTimestamp: Long): Int

    @Query("SELECT * FROM study_logs WHERE deckId = :deckId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsForDeck(deckId: Long): Flow<List<StudyLogEntity>>
}
