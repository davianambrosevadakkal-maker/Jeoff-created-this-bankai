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
import com.example.data.db.StudyLogEntity
import com.example.domain.ai.GeminiGermanService
import com.example.domain.german.GermanLanguageEngine
import com.example.domain.german.VerificationResult
import com.example.domain.goal.DailyLearningGoal
import com.example.domain.importer.AnkiImportResult
import com.example.domain.importer.CsvColumnMapping
import com.example.domain.importer.CsvImporter
import com.example.domain.importer.CsvPreview
import com.example.domain.db.BackupInfo
import com.example.domain.db.DatabaseMaintenanceHelper
import com.example.domain.db.DbCheckResult
import com.example.domain.srs.ClozeHelper
import com.example.domain.srs.FsrsOptimizationResult
import com.example.domain.srs.SrsAlgorithmType
import com.example.domain.srs.SrsDeckSettings
import com.example.domain.srs.SrsRating
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
    GENDER("Gender (der → die → das)"),
    FLAG_FIRST("Flagged Cards (Top priority)"),
    LEECH_FIRST("Leeches (Lapses ≥ 8 first)")
}

enum class StudyDirection(val displayName: String, val badge: String) {
    GERMAN_TO_ENGLISH("German → English", "🇩🇪 → 🇬🇧"),
    ENGLISH_TO_GERMAN("English → German", "🇬🇧 → 🇩🇪"),
    ALTERNATING("Alternating (Mixed)", "🔄 🇩🇪 ⇄ 🇬🇧")
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
    val direction: StudyDirection = StudyDirection.GERMAN_TO_ENGLISH,
    val isAnswerRevealed: Boolean = false,
    val isTypingMode: Boolean = true,
    val userTypedInput: String = "",
    val verificationResult: VerificationResult? = null,
    val dynamicSynonyms: List<String> = emptyList(),
    val isLoadingSynonyms: Boolean = false,
    val isFinished: Boolean = false,
    val isMicroSetSession: Boolean = false,
    val microSetLabel: String = "",
    val showWhiteboard: Boolean = false,
    val cardStartTime: Long = System.currentTimeMillis(),
    val canUndo: Boolean = false,
    val isCramMode: Boolean = false
) {
    val currentCard: FlashcardEntity?
        get() = cards.getOrNull(currentIndex)
    val progress: Float
        get() = if (cards.isNotEmpty()) currentIndex.toFloat() / cards.size else 0f
    val isCurrentPromptGerman: Boolean
        get() = when (direction) {
            StudyDirection.GERMAN_TO_ENGLISH -> true
            StudyDirection.ENGLISH_TO_GERMAN -> false
            StudyDirection.ALTERNATING -> (currentIndex % 2 == 0)
        }
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

    val allCards: StateFlow<List<FlashcardEntity>> = repository.allCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _srsSettings = MutableStateFlow(SrsDeckSettings())
    val srsSettings: StateFlow<SrsDeckSettings> = _srsSettings.asStateFlow()

    private val _showSrsSettingsSheet = MutableStateFlow(false)
    val showSrsSettingsSheet: StateFlow<Boolean> = _showSrsSettingsSheet.asStateFlow()

    private val _cardForDiagnostics = MutableStateFlow<FlashcardEntity?>(null)
    val cardForDiagnostics: StateFlow<FlashcardEntity?> = _cardForDiagnostics.asStateFlow()

    private val _cardDiagnosticsLogs = MutableStateFlow<List<StudyLogEntity>>(emptyList())
    val cardDiagnosticsLogs: StateFlow<List<StudyLogEntity>> = _cardDiagnosticsLogs.asStateFlow()

    private val _timeboxCardsReviewed = MutableStateFlow(0)
    val timeboxCardsReviewed: StateFlow<Int> = _timeboxCardsReviewed.asStateFlow()

    private val _showTimeboxAlert = MutableStateFlow(false)
    val showTimeboxAlert: StateFlow<Boolean> = _showTimeboxAlert.asStateFlow()

    val allStudyLogs: StateFlow<List<StudyLogEntity>> = repository.allStudyLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showStatsDashboard = MutableStateFlow(false)
    val showStatsDashboard: StateFlow<Boolean> = _showStatsDashboard.asStateFlow()

    private val _showDbMaintenance = MutableStateFlow(false)
    val showDbMaintenance: StateFlow<Boolean> = _showDbMaintenance.asStateFlow()

    private val _backupList = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backupList: StateFlow<List<BackupInfo>> = _backupList.asStateFlow()

    private var sessionStartTime: Long = System.currentTimeMillis()
    private var lastReviewedCardSnapshot: FlashcardEntity? = null
    private var lastSessionCardIndex: Int = 0

    fun openSrsSettings() {
        _showSrsSettingsSheet.value = true
    }

    fun closeSrsSettings() {
        _showSrsSettingsSheet.value = false
    }

    fun updateSrsSettings(settings: SrsDeckSettings) {
        _srsSettings.value = settings
        _toastMessage.value = "SRS updated: ${settings.algorithm.shortName} @ ${ (settings.desiredRetentionRate * 100).toInt() }% target"
    }

    fun optimizeFsrsWeights() {
        viewModelScope.launch {
            val result = repository.optimizeFsrsWeights(_srsSettings.value)
            _srsSettings.value = _srsSettings.value.copy(fsrsWeights = result.optimizedWeights)
            _toastMessage.value = result.statusMessage
        }
    }

    fun openCardDiagnostics(card: FlashcardEntity) {
        _cardForDiagnostics.value = card
        viewModelScope.launch {
            repository.getLogsForCardFlow(card.id).collect { logs ->
                _cardDiagnosticsLogs.value = logs
            }
        }
    }

    fun closeCardDiagnostics() {
        _cardForDiagnostics.value = null
        _cardDiagnosticsLogs.value = emptyList()
    }

    fun dismissTimeboxAlert() {
        _showTimeboxAlert.value = false
        // Reset timer for next timebox
        sessionStartTime = System.currentTimeMillis()
    }

    private val _selectedDeckId = MutableStateFlow<Long?>(null)
    val selectedDeckId: StateFlow<Long?> = _selectedDeckId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(CardSortOption.INDEX_ASC)
    val sortOption: StateFlow<CardSortOption> = _sortOption.asStateFlow()

    private val _cardDisplayDirection = MutableStateFlow(StudyDirection.GERMAN_TO_ENGLISH)
    val cardDisplayDirection: StateFlow<StudyDirection> = _cardDisplayDirection.asStateFlow()

    fun setCardDisplayDirection(direction: StudyDirection) {
        _cardDisplayDirection.value = direction
    }

    fun cycleCardDisplayDirection() {
        val next = when (_cardDisplayDirection.value) {
            StudyDirection.GERMAN_TO_ENGLISH -> StudyDirection.ENGLISH_TO_GERMAN
            StudyDirection.ENGLISH_TO_GERMAN -> StudyDirection.ALTERNATING
            StudyDirection.ALTERNATING -> StudyDirection.GERMAN_TO_ENGLISH
        }
        _cardDisplayDirection.value = next
    }

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

        // Filter by search query with Anki search syntax support
        if (query.isNotBlank()) {
            val q = query.trim().lowercase(Locale.GERMAN)
            list = when {
                q == "is:leech" -> list.filter { it.lapses >= 8 || it.tags.contains("leech", ignoreCase = true) }
                q == "is:suspended" -> list.filter { it.state == 3 }
                q == "is:new" -> list.filter { it.state == 0 }
                q == "is:learning" -> list.filter { it.state == 1 }
                q == "is:review" -> list.filter { it.state == 2 }
                q == "is:flagged" -> list.filter { it.flag > 0 }
                q == "flag:1" || q == "flag:red" -> list.filter { it.flag == 1 }
                q == "flag:2" || q == "flag:orange" -> list.filter { it.flag == 2 }
                q == "flag:3" || q == "flag:green" -> list.filter { it.flag == 3 }
                q == "flag:4" || q == "flag:blue" -> list.filter { it.flag == 4 }
                q.startsWith("tag:") -> {
                    val tagQuery = q.removePrefix("tag:").trim()
                    list.filter { it.tags.lowercase(Locale.GERMAN).contains(tagQuery) }
                }
                else -> {
                    list.filter {
                        it.front.lowercase(Locale.GERMAN).contains(q) ||
                                it.back.lowercase(Locale.GERMAN).contains(q) ||
                                it.tags.lowercase(Locale.GERMAN).contains(q) ||
                                it.notes.lowercase(Locale.GERMAN).contains(q)
                    }
                }
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
            CardSortOption.FLAG_FIRST -> list.sortedByDescending { it.flag }
            CardSortOption.LEECH_FIRST -> list.sortedByDescending { it.lapses }
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

    private val _dailyTargetWords = MutableStateFlow(20)
    val dailyTargetWords: StateFlow<Int> = _dailyTargetWords.asStateFlow()

    private val _dailyTargetRetention = MutableStateFlow(0.85f)
    val dailyTargetRetention: StateFlow<Float> = _dailyTargetRetention.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyLearningGoal: StateFlow<DailyLearningGoal> = combine(
        _dailyTargetWords,
        _dailyTargetRetention
    ) { words, rate ->
        Pair(words, rate)
    }.flatMapLatest { (words, rate) ->
        repository.getDailyLearningGoalFlow(words, rate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyLearningGoal())

    fun updateDailyGoalTargets(targetWords: Int, targetRetentionPercent: Int) {
        _dailyTargetWords.value = targetWords.coerceIn(5, 200)
        _dailyTargetRetention.value = (targetRetentionPercent.coerceIn(50, 100) / 100f)
        _toastMessage.value = "Daily goal updated: $targetWords words at $targetRetentionPercent% target!"
    }

    fun startDailyGoalStudy() {
        val decks = allDecks.value
        val targetDeck = decks.firstOrNull() ?: return
        startDeckStudy(targetDeck.id)
    }

    private val _preferredStudyDirection = MutableStateFlow(StudyDirection.GERMAN_TO_ENGLISH)
    val preferredStudyDirection: StateFlow<StudyDirection> = _preferredStudyDirection.asStateFlow()

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
        microSetLabel: String,
        isCram: Boolean = false
    ) {
        val firstCard = cards.firstOrNull()
        sessionStartTime = System.currentTimeMillis()
        _timeboxCardsReviewed.value = 0
        lastReviewedCardSnapshot = null

        _studySession.value = StudySessionState(
            deckName = deckName,
            cards = cards,
            currentIndex = 0,
            direction = _preferredStudyDirection.value,
            isAnswerRevealed = false,
            isTypingMode = true,
            isMicroSetSession = isMicroSet,
            microSetLabel = microSetLabel,
            showWhiteboard = false,
            cardStartTime = System.currentTimeMillis(),
            canUndo = false,
            isCramMode = isCram,
            dynamicSynonyms = firstCard?.let { GermanLanguageEngine.getSynonyms(it.front) } ?: emptyList()
        )
        firstCard?.let { loadDynamicSynonyms(it.front) }
    }

    fun startCramStudy(deckId: Long) {
        viewModelScope.launch {
            val deck = repository.getDeckById(deckId)
            val allCards = repository.getCardsForDeck(deckId)
            if (allCards.isEmpty()) {
                _toastMessage.value = "No cards available to cram!"
                return@launch
            }
            startStudySession(
                deckName = "${deck?.name ?: "Deck"} (Cram Mode)",
                cards = allCards.shuffled(),
                isMicroSet = false,
                microSetLabel = "Cram Mode",
                isCram = true
            )
        }
    }

    fun toggleWhiteboard() {
        val current = _studySession.value ?: return
        _studySession.value = current.copy(showWhiteboard = !current.showWhiteboard)
    }

    fun suspendCurrentCard() {
        val session = _studySession.value ?: return
        val currentCard = session.currentCard ?: return
        viewModelScope.launch {
            repository.suspendCard(currentCard.id, true)
            _toastMessage.value = "Card '${currentCard.front}' suspended"
            // Advance to next card
            advanceToNextCard(session)
        }
    }

    fun buryCurrentCard() {
        val session = _studySession.value ?: return
        val currentCard = session.currentCard ?: return
        viewModelScope.launch {
            repository.buryCard(currentCard.id)
            _toastMessage.value = "Card buried until tomorrow"
            advanceToNextCard(session)
        }
    }

    fun undoLastReview() {
        val snapshot = lastReviewedCardSnapshot ?: return
        val session = _studySession.value ?: return
        viewModelScope.launch {
            repository.undoLastReview(snapshot)
            // Roll back study session index
            val prevIndex = max(0, session.currentIndex - 1)
            val restoredCards = session.cards.toMutableList()
            restoredCards[prevIndex] = snapshot

            _studySession.value = session.copy(
                cards = restoredCards,
                currentIndex = prevIndex,
                isAnswerRevealed = false,
                userTypedInput = "",
                verificationResult = null,
                canUndo = false,
                dynamicSynonyms = GermanLanguageEngine.getSynonyms(snapshot.front)
            )
            lastReviewedCardSnapshot = null
            _toastMessage.value = "Undid last review on '${snapshot.front}'"
        }
    }

    private fun advanceToNextCard(session: StudySessionState) {
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
                cardStartTime = System.currentTimeMillis(),
                dynamicSynonyms = GermanLanguageEngine.getSynonyms(nextCard.front)
            )
            loadDynamicSynonyms(nextCard.front)
        }
    }

    fun setStudyDirection(direction: StudyDirection) {
        _preferredStudyDirection.value = direction
        _studySession.value = _studySession.value?.copy(direction = direction)
        _toastMessage.value = "Study mode: ${direction.displayName}"
    }

    fun cycleStudyDirection() {
        val next = when (_preferredStudyDirection.value) {
            StudyDirection.GERMAN_TO_ENGLISH -> StudyDirection.ENGLISH_TO_GERMAN
            StudyDirection.ENGLISH_TO_GERMAN -> StudyDirection.ALTERNATING
            StudyDirection.ALTERNATING -> StudyDirection.GERMAN_TO_ENGLISH
        }
        setStudyDirection(next)
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
        val result = if (session.isCurrentPromptGerman) {
            val expectedEnglish = if (ClozeHelper.hasCloze(card.front)) {
                ClozeHelper.extractClozeAnswer(card.front) ?: card.back
            } else {
                card.back
            }
            GermanLanguageEngine.verifyEnglishAnswer(
                userInput = session.userTypedInput,
                expectedEnglish = expectedEnglish
            )
        } else {
            val expectedGerman = if (ClozeHelper.hasCloze(card.front)) {
                ClozeHelper.extractClozeAnswer(card.front) ?: card.front
            } else {
                card.front
            }
            GermanLanguageEngine.verifyGermanAnswer(
                userInput = session.userTypedInput,
                expectedGerman = expectedGerman,
                englishMeaning = card.back
            )
        }
        _studySession.value = session.copy(
            isAnswerRevealed = true,
            verificationResult = result
        )
    }

    fun toggleCardFlag(cardId: Long, flag: Int) {
        viewModelScope.launch {
            repository.updateCardFlag(cardId, flag)
            _studySession.value?.let { session ->
                val updatedCards = session.cards.map { if (it.id == cardId) it.copy(flag = flag) else it }
                _studySession.value = session.copy(cards = updatedCards)
            }
        }
    }

    fun batchFlagSelected(flag: Int) {
        val selected = _selectedCardIds.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.updateCardsFlag(selected, flag)
            _selectedCardIds.value = emptySet()
            _toastMessage.value = "Updated flags on ${selected.size} cards"
        }
    }

    fun manuallyRescheduleCard(cardId: Long, intervalDays: Int) {
        viewModelScope.launch {
            val due = System.currentTimeMillis() + (intervalDays * 86_400_000L)
            repository.manuallyRescheduleCard(cardId, intervalDays, due)
            _toastMessage.value = "Card rescheduled for $intervalDays days"
        }
    }

    fun openStatsDashboard() {
        _showStatsDashboard.value = true
    }

    fun closeStatsDashboard() {
        _showStatsDashboard.value = false
    }

    fun openDbMaintenance() {
        val helper = DatabaseMaintenanceHelper(getApplication())
        _backupList.value = helper.listBackups()
        _showDbMaintenance.value = true
    }

    fun closeDbMaintenance() {
        _showDbMaintenance.value = false
    }

    fun runDatabaseIntegrityCheck(onResult: (DbCheckResult) -> Unit) {
        viewModelScope.launch {
            val helper = DatabaseMaintenanceHelper(getApplication())
            val res = helper.checkDatabaseIntegrity()
            onResult(res)
        }
    }

    fun optimizeDatabase(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val helper = DatabaseMaintenanceHelper(getApplication())
            val msg = helper.optimizeDatabase()
            onResult(msg)
        }
    }

    fun createDatabaseBackup(onResult: (BackupInfo) -> Unit) {
        viewModelScope.launch {
            val helper = DatabaseMaintenanceHelper(getApplication())
            val backup = helper.createBackupSnapshot()
            _backupList.value = helper.listBackups()
            onResult(backup)
        }
    }

    fun submitRating(rating: SrsRating) {
        val session = _studySession.value ?: return
        val currentCard = session.currentCard ?: return

        viewModelScope.launch {
            // Store snapshot for Undo
            lastReviewedCardSnapshot = currentCard
            lastSessionCardIndex = session.currentIndex

            val srsResult = repository.recordReview(currentCard, rating, _srsSettings.value)

            // Update timebox stats
            val count = _timeboxCardsReviewed.value + 1
            _timeboxCardsReviewed.value = count

            val timeboxMins = _srsSettings.value.timeboxMinutes
            if (timeboxMins > 0) {
                val elapsedMinutes = (System.currentTimeMillis() - sessionStartTime) / (60 * 1000)
                if (elapsedMinutes >= timeboxMins && count % 5 == 0) {
                    _showTimeboxAlert.value = true
                }
            }

            val nextIndex = session.currentIndex + 1
            if (nextIndex >= session.cards.size) {
                _studySession.value = session.copy(isFinished = true, canUndo = true)
            } else {
                val nextCard = session.cards[nextIndex]
                _studySession.value = session.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    userTypedInput = "",
                    verificationResult = null,
                    cardStartTime = System.currentTimeMillis(),
                    canUndo = true,
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
