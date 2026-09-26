package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY orderIndex ASC")
    fun getCardsForDeckFlow(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY orderIndex ASC")
    suspend fun getCardsForDeck(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND orderIndex BETWEEN :minIndex AND :maxIndex ORDER BY orderIndex ASC")
    fun getCardsInRangeFlow(deckId: Long, minIndex: Int, maxIndex: Int): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND orderIndex BETWEEN :minIndex AND :maxIndex ORDER BY orderIndex ASC")
    suspend fun getCardsInRange(deckId: Long, minIndex: Int, maxIndex: Int): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND id IN (:cardIds)")
    suspend fun getCardsByIds(deckId: Long, cardIds: List<Long>): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND (state = 0 OR dueTimestamp <= :now) AND state != 3 ORDER BY dueTimestamp ASC")
    suspend fun getDueCardsForStudy(deckId: Long, now: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): FlashcardEntity?

    @Query("SELECT MAX(orderIndex) FROM flashcards WHERE deckId = :deckId")
    suspend fun getMaxOrderIndex(deckId: Long): Int?

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    fun getCardCountFlow(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardCount(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND state = 0")
    suspend fun getNewCardCount(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND dueTimestamp <= :now AND state != 3 AND state != 0")
    suspend fun getDueReviewCount(deckId: Long, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Update
    suspend fun updateCards(cards: List<FlashcardEntity>)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id IN (:cardIds)")
    suspend fun deleteCardsByIds(cardIds: List<Long>)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteAllCardsInDeck(deckId: Long)

    @Query("UPDATE flashcards SET state = :newState WHERE id IN (:cardIds)")
    suspend fun updateCardsState(cardIds: List<Long>, newState: Int)

    @Query("UPDATE flashcards SET deckId = :targetDeckId WHERE id IN (:cardIds)")
    suspend fun moveCardsToDeck(cardIds: List<Long>, targetDeckId: Long)

    @Query("UPDATE flashcards SET state = 0, intervalDays = 0, easeFactor = 2.5, repetitions = 0, lapses = 0, dueTimestamp = 0 WHERE id IN (:cardIds)")
    suspend fun resetCardsProgress(cardIds: List<Long>)
}
