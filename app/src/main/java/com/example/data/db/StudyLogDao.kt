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

    @Query("SELECT * FROM study_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getLogsSinceFlow(sinceTimestamp: Long): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getLogsForCardFlow(cardId: Long): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE cardId = :cardId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLogForCard(cardId: Long): StudyLogEntity?

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT * FROM study_logs WHERE deckId = :deckId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsForDeck(deckId: Long): Flow<List<StudyLogEntity>>
}
