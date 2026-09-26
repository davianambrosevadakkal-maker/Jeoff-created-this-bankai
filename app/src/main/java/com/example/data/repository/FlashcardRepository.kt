package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.StudyLogEntity
import com.example.domain.importer.AnkiImportResult
import com.example.domain.importer.AnkiPackageImporter
import com.example.domain.importer.CsvColumnMapping
import com.example.domain.importer.CsvImporter
import com.example.domain.importer.DefaultGermanDecks
import com.example.domain.srs.AnkiSrsEngine
import com.example.domain.srs.SrsRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

class FlashcardRepository(private val db: AppDatabase) {

    private val deckDao = db.deckDao()
    private val cardDao = db.flashcardDao()
    private val studyLogDao = db.studyLogDao()

    val allDecksFlow: Flow<List<DeckEntity>> = deckDao.getAllDecksFlow()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingDecks = deckDao.getAllDecks()
        if (existingDecks.isEmpty()) {
            for (deck in DefaultGermanDecks.starterDecks) {
                val deckId = deckDao.insertDeck(deck)
                val cards = DefaultGermanDecks.getCardsForDeck(deck.id)
                cardDao.insertCards(cards.map { it.copy(deckId = deckId) })
                deckDao.refreshDeckCardCount(deckId)
            }
        }
    }

    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?> = deckDao.getDeckByIdFlow(deckId)

    suspend fun getDeckById(deckId: Long): DeckEntity? = withContext(Dispatchers.IO) {
        deckDao.getDeckById(deckId)
    }

    suspend fun createDeck(name: String, description: String = "", colorHex: String = "#2563EB"): Long =
        withContext(Dispatchers.IO) {
            deckDao.insertDeck(
                DeckEntity(
                    name = name.trim(),
                    description = description.trim(),
                    colorHex = colorHex
                )
            )
        }

    suspend fun deleteDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        cardDao.deleteAllCardsInDeck(deck.id)
        deckDao.deleteDeck(deck)
    }

    fun getCardsForDeckFlow(deckId: Long): Flow<List<FlashcardEntity>> =
        cardDao.getCardsForDeckFlow(deckId)

    suspend fun getCardsForDeck(deckId: Long): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        cardDao.getCardsForDeck(deckId)
    }

    suspend fun getCardsInRange(deckId: Long, minIndex: Int, maxIndex: Int): List<FlashcardEntity> =
        withContext(Dispatchers.IO) {
            cardDao.getCardsInRange(deckId, minIndex, maxIndex)
        }

    suspend fun getDueCardsForStudy(deckId: Long, now: Long = System.currentTimeMillis()): List<FlashcardEntity> =
        withContext(Dispatchers.IO) {
            cardDao.getDueCardsForStudy(deckId, now)
        }

    suspend fun addCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        val maxIndex = cardDao.getMaxOrderIndex(card.deckId) ?: 0
        val newCard = card.copy(orderIndex = maxIndex + 1)
        val id = cardDao.insertCard(newCard)
        deckDao.refreshDeckCardCount(card.deckId)
        id
    }

    suspend fun updateCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        cardDao.deleteCard(card)
        deckDao.refreshDeckCardCount(card.deckId)
    }

    suspend fun recordReview(card: FlashcardEntity, rating: SrsRating): FlashcardEntity =
        withContext(Dispatchers.IO) {
            val result = AnkiSrsEngine.calculateNextReview(card, rating)
            cardDao.updateCard(result.updatedCard)
            studyLogDao.insertLog(
                StudyLogEntity(
                    cardId = card.id,
                    deckId = card.deckId,
                    rating = rating.value,
                    intervalDays = result.nextIntervalDays
                )
            )
            result.updatedCard
        }

    // Batch operations
    suspend fun batchSuspend(cardIds: List<Long>, suspend: Boolean) = withContext(Dispatchers.IO) {
        val state = if (suspend) 3 else 0
        cardDao.updateCardsState(cardIds, state)
    }

    suspend fun batchMove(cardIds: List<Long>, fromDeckId: Long, toDeckId: Long) =
        withContext(Dispatchers.IO) {
            cardDao.moveCardsToDeck(cardIds, toDeckId)
            deckDao.refreshDeckCardCount(fromDeckId)
            deckDao.refreshDeckCardCount(toDeckId)
        }

    suspend fun batchResetProgress(cardIds: List<Long>) = withContext(Dispatchers.IO) {
        cardDao.resetCardsProgress(cardIds)
    }

    suspend fun batchDelete(cardIds: List<Long>, deckId: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardsByIds(cardIds)
        deckDao.refreshDeckCardCount(deckId)
    }

    // Import operations
    suspend fun importCsv(
        content: String,
        deckId: Long,
        mapping: CsvColumnMapping,
        delimiter: Char = ',',
        hasHeader: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        val maxIndex = cardDao.getMaxOrderIndex(deckId) ?: 0
        val parsed = CsvImporter.parseToFlashcards(
            content = content,
            deckId = deckId,
            mapping = mapping,
            delimiter = delimiter,
            hasHeaderRow = hasHeader,
            startIndex = maxIndex + 1
        )
        if (parsed.isNotEmpty()) {
            cardDao.insertCards(parsed)
            deckDao.refreshDeckCardCount(deckId)
        }
        parsed.size
    }

    suspend fun importAnkiPackage(context: Context, inputStream: InputStream, targetDeckId: Long): AnkiImportResult =
        withContext(Dispatchers.IO) {
            val result = AnkiPackageImporter.importPackage(context, inputStream, targetDeckId)
            if (result.success && result.cards.isNotEmpty()) {
                val maxIndex = cardDao.getMaxOrderIndex(targetDeckId) ?: 0
                val reindexed = result.cards.mapIndexed { idx, c ->
                    c.copy(deckId = targetDeckId, orderIndex = maxIndex + 1 + idx)
                }
                cardDao.insertCards(reindexed)
                deckDao.refreshDeckCardCount(targetDeckId)
            }
            result
        }

    suspend fun exportCardsToCsv(cards: List<FlashcardEntity>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("Front,Back,Gender,Notes,Synonyms,Tags,IntervalDays,EaseFactor\n")
        for (c in cards) {
            val frontEscaped = "\"${c.front.replace("\"", "\"\"")}\""
            val backEscaped = "\"${c.back.replace("\"", "\"\"")}\""
            val genderEscaped = "\"${c.gender.replace("\"", "\"\"")}\""
            val notesEscaped = "\"${c.notes.replace("\"", "\"\"")}\""
            val synsEscaped = "\"${c.synonyms.replace("\"", "\"\"")}\""
            val tagsEscaped = "\"${c.tags.replace("\"", "\"\"")}\""
            sb.append("$frontEscaped,$backEscaped,$genderEscaped,$notesEscaped,$synsEscaped,$tagsEscaped,${c.intervalDays},${c.easeFactor}\n")
        }
        sb.toString()
    }
}
