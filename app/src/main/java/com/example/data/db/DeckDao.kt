package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllDecksFlow(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllDecks(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("UPDATE decks SET cardCount = (SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId) WHERE id = :deckId")
    suspend fun refreshDeckCardCount(deckId: Long)

    @Query("UPDATE decks SET cardCount = (SELECT COUNT(*) FROM flashcards WHERE deckId = decks.id)")
    suspend fun refreshAllDeckCounts()
}
