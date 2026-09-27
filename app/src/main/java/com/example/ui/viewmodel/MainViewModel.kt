package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.data.repository.FlashcardRepository
import com.example.domain.ai.GeminiGermanService
import com.example.domain.german.GermanLanguageEngine
import com.example.domain.german.VerificationResult
import com.example.domain.importer.AnkiImportResult
import com.example.domain.importer.CsvColumnMapping
import com.example.domain.importer.CsvImporter
import com.example.domain.importer.CsvPreview
import com.example.domain.srs.SrsRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

enum class CardSortOption(val displayName: String) {
    INDEX_ASC("Index # (1 → N)"),
    INDEX_DESC("Index # (N → 1)"),
    FRONT_AZ("Front (German A → Z)"),
    FRONT_ZA("Front (German Z → A)"),
    BACK_AZ("Back (English A → Z)"),
    BACK_ZA("Back (English Z → A)"),
    DUE_DATE("Due Date (Past due first)"),
    EASE_LOWEST("Difficulty (Lowest ease first)"),
    LAPSES_HIGHEST("Lapses (Most forgotten first)"),
    GENDER("Gender (der → die → das)")
}

data class RangeSelectionState(
    val isEnabled: Boolean = false,
    val startIndex: Int = 1,
    val endIndex: Int = 25,
    val maxDeckIndex: Int = 1
) {
    val count: Int
        get() = if (endIndex >= startIndex) endIndex - startIndex + 1 else 0
}

data class StudySessionState(
    val deckName: String,
    val cards: List<FlashcardEntity>,
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val isTypingMode: Boolean = true,
    val userTypedInput: String = "",
    val verificationResult: VerificationResult? = null,
    val dynamicSynonyms: List<String> = emptyList(),
    val isLoadingSynonyms: Boolean = false,
    val isFinished: Boolean = false,
    val isMicroSetSession: Boolean = false,
    val microSetLabel: String = ""
) {
    val currentCard: FlashcardEntity?
        get() = cards.getOrNull(currentIndex)
    val progress: Float
        get() = if (cards.isNotEmpty()) currentIndex.toFloat() / cards.size else 0f
}

data class CsvImportState(
    val isOpen: Boolean = false,
    val rawCsv: String = "",
    val preview: CsvPreview? = null,
    val currentMapping: CsvColumnMapping = CsvColumnMapping(),
    val delimiter: Char = ',',
    val hasHeader: Boolean = true,
    val targetDeckId: Long = 1L,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashcardRepository(AppDatabase.getDatabase(application))

    val allDecks: StateFlow<List<DeckEntity>> = repository.allDecksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeckId = MutableStateFlow<Long?>(null)
    val selectedDeckId: StateFlow<Long?> = _selectedDeckId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(CardSortOption.INDEX_ASC)
    val sortOption: StateFlow<CardSortOption> = _sortOption.asStateFlow()

    private val _rangeSelection = MutableStateFlow(RangeSelectionState())
    val rangeSelection: StateFlow<RangeSelectionState> = _rangeSelection.asStateFlow()

    private val _selectedCardIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCardIds: StateFlow<Set<Long>> = _selectedCardIds.asStateFlow()

    private val _currentDeckCards = MutableStateFlow<List<FlashcardEntity>>(emptyList())

    // Filtered, sorted, and range-selected cards for display
    val displayedCards: StateFlow<List<FlashcardEntity>> = combine(
        _currentDeckCards,
        _searchQuery,
        _sortOption,
        _rangeSelection
    ) { cards, query, sort, range ->
        var list = cards

        // Filter by search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase(Locale.GERMAN)
            list = list.filter {
                it.front.lowercase(Locale.GERMAN).contains(q) ||
                        it.back.lowercase(Locale.GERMAN).contains(q) ||
                        it.tags.lowercase(Locale.GERMAN).contains(q) ||
                        it.notes.lowercase(Locale.GERMAN).contains(q)
            }
        }

        // Apply range selection filter if active
        if (range.isEnabled) {
            list = list.filter { it.orderIndex in range.startIndex..range.endIndex }
        }

        // Apply sorting
        when (sort) {
            CardSortOption.INDEX_ASC -> list.sortedBy { it.orderIndex }
            CardSortOption.INDEX_DESC -> list.sortedByDescending { it.orderIndex }
            CardSortOption.FRONT_AZ -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.front })
            CardSortOption.FRONT_ZA -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.front })
            CardSortOption.BACK_AZ -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.back })
            CardSortOption.BACK_ZA -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.back })
            CardSortOption.DUE_DATE -> list.sortedBy { it.dueTimestamp }
            CardSortOption.EASE_LOWEST -> list.sortedBy { it.easeFactor }
            CardSortOption.LAPSES_HIGHEST -> list.sortedByDescending { it.lapses }
            CardSortOption.GENDER -> list.sortedBy {
                when (it.gender.lowercase()) {
                    "der" -> 1
                    "die" -> 2
                    "das" -> 3
                    else -> 4
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _studySession = MutableStateFlow<StudySessionState?>(null)
    val studySession: StateFlow<StudySessionState?> = _studySession.asStateFlow()

    private val _csvImportState = MutableStateFlow(CsvImportState())
    val csvImportState: StateFlow<CsvImportState> = _csvImportState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }

    fun selectDeck(deckId: Long) {
        _selectedDeckId.value = deckId
        _selectedCardIds.value = emptySet()
        _searchQuery.value = ""
        viewModelScope.launch {
            repository.getCardsForDeckFlow(deckId).collect { cards ->
                _currentDeckCards.value = cards
                val maxIdx = cards.maxOfOrNull { it.orderIndex } ?: 1
                _rangeSelection.value = _rangeSelection.value.copy(
                    maxDeckIndex = maxIdx,
                    startIndex = 1,
                    endIndex = if (maxIdx >= 25) 25 else maxIdx
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: CardSortOption) {
        _sortOption.value = option
    }

    // --- Range Selection & Micro-sets ---

    fun toggleRangeSelection(enabled: Boolean) {
        _rangeSelection.value = _rangeSelection.value.copy(isEnabled = enabled)
    }

    fun setRange(start: Int, end: Int) {
        val maxIdx = _rangeSelection.value.maxDeckIndex
        val validStart = start.coerceIn(1, maxIdx)
        val validEnd = end.coerceIn(validStart, maxIdx)
        _rangeSelection.value = _rangeSelection.value.copy(
            isEnabled = true,
            startIndex = validStart,
            endIndex = validEnd
        )
    }

    fun setPresetMicroSet(start: Int, end: Int) {
        setRange(start, end)
    }

    fun toggleCardSelection(cardId: Long) {
        val current = _selectedCardIds.value.toMutableSet()
        if (current.contains(cardId)) {
            current.remove(cardId)
        } else {
            current.add(cardId)
        }
        _selectedCardIds.value = current
    }

    fun selectAllVisibleCards() {
        val visibleIds = displayedCards.value.map { it.id }.toSet()
        _selectedCardIds.value = visibleIds
    }

    fun clearCardSelection() {
        _selectedCardIds.value = emptySet()
    }

    // --- Micro-Set & Batch Operations ---

    fun studyRangeMicroSet() {
        val range = _rangeSelection.value
        val deckId = _selectedDeckId.value ?: return
        viewModelScope.launch {
            val cardsInRange = repository.getCardsInRange(deckId, range.startIndex, range.endIndex)
            if (cardsInRange.isEmpty()) {
                _toastMessage.value = "No cards found in range ${range.startIndex}-${range.endIndex}"
                return@launch
            }
            val deck = repository.getDeckById(deckId)
            startStudySession(
                deckName = deck?.name ?: "German Deck",
                cards = cardsInRange,
                isMicroSet = true,
                microSetLabel = "Cards #${range.startIndex} – #${range.endIndex}"
            )
        }
    }

    fun batchSuspendSelected(suspend: Boolean) {
        val ids = getEffectiveSelectedIds()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchSuspend(ids, suspend)
            _toastMessage.value = "${ids.size} cards ${if (suspend) "suspended" else "unsuspended"}"
            clearCardSelection()
        }
    }

    fun batchResetSelectedProgress() {
        val ids = getEffectiveSelectedIds()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchResetProgress(ids)
            _toastMessage.value = "Reset SRS progress for ${ids.size} cards"
            clearCardSelection()
        }
    }

    fun batchMoveSelected(targetDeckId: Long) {
        val ids = getEffectiveSelectedIds()
        val currentDeckId = _selectedDeckId.value ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchMove(ids, currentDeckId, targetDeckId)
            _toastMessage.value = "Moved ${ids.size} cards to new deck"
            clearCardSelection()
        }
    }

    fun batchDeleteSelected() {
        val ids = getEffectiveSelectedIds()
        val currentDeckId = _selectedDeckId.value ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchDelete(ids, currentDeckId)
            _toastMessage.value = "Deleted ${ids.size} cards"
            clearCardSelection()
        }
    }

    private fun getEffectiveSelectedIds(): List<Long> {
        val explicitlySelected = _selectedCardIds.value.toList()
        if (explicitlySelected.isNotEmpty()) {
            return explicitlySelected
        }
        // If range is active and nothing individually checked, target the range
        if (_rangeSelection.value.isEnabled) {
            return displayedCards.value.map { it.id }
        }
        return emptyList()
    }

    // --- Study & SRS Session ---

    fun startDeckStudy(deckId: Long) {
        viewModelScope.launch {
            val dueCards = repository.getDueCardsForStudy(deckId)
            val deck = repository.getDeckById(deckId)
            val allCards = if (dueCards.isEmpty()) {
                repository.getCardsForDeck(deckId).take(20) // Cram practice if no cards due
            } else {
                dueCards
            }

            if (allCards.isEmpty()) {
                _toastMessage.value = "Deck has no cards to study!"
                return@launch
            }

            startStudySession(
                deckName = deck?.name ?: "Study Deck",
                cards = allCards,
                isMicroSet = false,
                microSetLabel = ""
            )
        }
    }

    private fun startStudySession(
        deckName: String,
        cards: List<FlashcardEntity>,
        isMicroSet: Boolean,
        microSetLabel: String
    ) {
        val firstCard = cards.firstOrNull()
        _studySession.value = StudySessionState(
            deckName = deckName,
            cards = cards,
            currentIndex = 0,
            isAnswerRevealed = false,
            isTypingMode = true,
            isMicroSetSession = isMicroSet,
            microSetLabel = microSetLabel,
            dynamicSynonyms = firstCard?.let { GermanLanguageEngine.getSynonyms(it.front) } ?: emptyList()
        )
        firstCard?.let { loadDynamicSynonyms(it.front) }
    }

    fun revealAnswer() {
        _studySession.value = _studySession.value?.copy(isAnswerRevealed = true)
    }

    fun toggleTypingMode() {
        val current = _studySession.value ?: return
        _studySession.value = current.copy(isTypingMode = !current.isTypingMode)
    }

    fun updateTypedInput(input: String) {
        _studySession.value = _studySession.value?.copy(userTypedInput = input)
    }

    fun verifyAnswer() {
        val session = _studySession.value ?: return
        val card = session.currentCard ?: return
        val result = GermanLanguageEngine.verifyGermanAnswer(
            userInput = session.userTypedInput,
            expectedGerman = card.front,
            englishMeaning = card.back
        )
        _studySession.value = session.copy(
            isAnswerRevealed = true,
            verificationResult = result
        )
    }

    fun submitRating(rating: SrsRating) {
        val session = _studySession.value ?: return
        val currentCard = session.currentCard ?: return

        viewModelScope.launch {
            repository.recordReview(currentCard, rating)

            val nextIndex = session.currentIndex + 1
            if (nextIndex >= session.cards.size) {
                _studySession.value = session.copy(isFinished = true)
            } else {
                val nextCard = session.cards[nextIndex]
                _studySession.value = session.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    userTypedInput = "",
                    verificationResult = null,
                    dynamicSynonyms = GermanLanguageEngine.getSynonyms(nextCard.front)
                )
                loadDynamicSynonyms(nextCard.front)
            }
        }
    }

    private fun loadDynamicSynonyms(word: String) {
        viewModelScope.launch {
            val synonyms = GeminiGermanService.fetchDynamicSynonyms(word)
            _studySession.value = _studySession.value?.copy(
                dynamicSynonyms = synonyms,
                isLoadingSynonyms = false
            )
        }
    }

    fun endStudySession() {
        _studySession.value = null
    }

    // --- Deck Creation & Management ---

    fun createDeck(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            val id = repository.createDeck(name, description, colorHex)
            _toastMessage.value = "Deck '$name' created!"
            selectDeck(id)
        }
    }

    fun addCardToCurrentDeck(front: String, back: String, notes: String, gender: String, tags: String) {
        val deckId = _selectedDeckId.value ?: return
        viewModelScope.launch {
            val syns = GermanLanguageEngine.getSynonyms(front).joinToString(", ")
            val card = FlashcardEntity(
                deckId = deckId,
                front = front.trim(),
                back = back.trim(),
                notes = notes.trim(),
                gender = if (gender.isNotBlank()) gender else GermanLanguageEngine.extractGender(front, notes),
                tags = tags.trim(),
                synonyms = syns
            )
            repository.addCard(card)
            _toastMessage.value = "Added card: $front"
        }
    }

    fun deleteCurrentDeck(deck: DeckEntity) {
        deleteDeck(deck)
    }

    fun deleteDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
            if (_selectedDeckId.value == deck.id) {
                _selectedDeckId.value = null
            }
            _toastMessage.value = "Deck '${deck.name}' deleted"
        }
    }

    fun deleteCard(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            _selectedCardIds.value = _selectedCardIds.value - card.id
            _toastMessage.value = "Deleted card '${card.front}'"
        }
    }

    // --- CSV & Anki Import ---

    fun openCsvImportDialog(csvContent: String = "", targetDeckId: Long = 1L) {
        val preview = if (csvContent.isNotBlank()) CsvImporter.analyzeCsv(csvContent) else null
        _csvImportState.value = CsvImportState(
            isOpen = true,
            rawCsv = csvContent,
            preview = preview,
            currentMapping = preview?.suggestedMapping ?: CsvColumnMapping(),
            delimiter = preview?.delimiter ?: ',',
            targetDeckId = targetDeckId
        )
    }

    fun updateCsvContent(content: String) {
        val preview = CsvImporter.analyzeCsv(content)
        _csvImportState.value = _csvImportState.value.copy(
            rawCsv = content,
            preview = preview,
            currentMapping = preview.suggestedMapping,
            delimiter = preview.delimiter
        )
    }

    fun updateCsvMapping(mapping: CsvColumnMapping) {
        _csvImportState.value = _csvImportState.value.copy(currentMapping = mapping)
    }

    fun setImportTargetDeck(deckId: Long) {
        _csvImportState.value = _csvImportState.value.copy(targetDeckId = deckId)
    }

    fun createDeckForImport(name: String, description: String = "", colorHex: String = "#2563EB") {
        viewModelScope.launch {
            val newDeckId = repository.createDeck(name, description, colorHex)
            _csvImportState.value = _csvImportState.value.copy(targetDeckId = newDeckId)
            _toastMessage.value = "Deck '$name' created & selected for import"
        }
    }

    fun closeCsvImportDialog() {
        _csvImportState.value = CsvImportState(isOpen = false)
    }

    fun executeCsvImport() {
        val state = _csvImportState.value
        if (state.rawCsv.isBlank()) return

        viewModelScope.launch {
            _csvImportState.value = state.copy(isProcessing = true)
            try {
                val count = repository.importCsv(
                    content = state.rawCsv,
                    deckId = state.targetDeckId,
                    mapping = state.currentMapping,
                    delimiter = state.delimiter,
                    hasHeader = state.hasHeader
                )
                _toastMessage.value = "Successfully imported $count cards!"
                _csvImportState.value = CsvImportState(isOpen = false)
                selectDeck(state.targetDeckId)
            } catch (e: Exception) {
                _csvImportState.value = state.copy(
                    isProcessing = false,
                    statusMessage = "Import error: ${e.message}"
                )
            }
        }
    }

    fun importAnkiPackageStream(context: Context, inputStream: InputStream, targetDeckId: Long) {
        viewModelScope.launch {
            _toastMessage.value = "Extracting and parsing Anki package (.apkg)..."
            val result = repository.importAnkiPackage(context, inputStream, targetDeckId)
            if (result.success) {
                _toastMessage.value = "Imported ${result.totalCount} cards from '${result.deckName}'!"
                selectDeck(targetDeckId)
            } else {
                _toastMessage.value = result.errorMessage ?: "Failed to import Anki package"
            }
        }
    }

    fun exportCurrentCardsToCsv(onCsvGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportCardsToCsv(displayedCards.value)
            onCsvGenerated(csv)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
