package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.tts.GermanTtsHelper
import com.example.ui.components.CardInfoViewerDialog
import com.example.ui.components.DatabaseMaintenanceDialog
import com.example.ui.components.ImportModalDialog
import com.example.ui.components.SrsSettingsSheet
import com.example.ui.components.StatsDashboardDialog
import com.example.ui.components.TimeboxSummaryDialog
import com.example.ui.screens.CardListScreen
import com.example.ui.screens.CustomAppLoadingScreen
import com.example.ui.screens.DeckListScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var ttsHelper: GermanTtsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ttsHelper = GermanTtsHelper(this)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnkiBankaiApp(
                        viewModel = viewModel,
                        ttsHelper = ttsHelper,
                        onShareCsv = { csvContent ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "AnkiDroidBANKAI Cards Export")
                                putExtra(Intent.EXTRA_TEXT, csvContent)
                            }
                            startActivity(Intent.createChooser(intent, "Export Flashcards CSV"))
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}

@Composable
fun AnkiBankaiApp(
    viewModel: MainViewModel,
    ttsHelper: GermanTtsHelper,
    onShareCsv: (String) -> Unit
) {
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val selectedDeckId by viewModel.selectedDeckId.collectAsStateWithLifecycle()
    val displayedCards by viewModel.displayedCards.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val rangeState by viewModel.rangeSelection.collectAsStateWithLifecycle()
    val selectedCardIds by viewModel.selectedCardIds.collectAsStateWithLifecycle()
    val studySession by viewModel.studySession.collectAsStateWithLifecycle()
    val csvImportState by viewModel.csvImportState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val cardDisplayDirection by viewModel.cardDisplayDirection.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyLearningGoal.collectAsStateWithLifecycle()
    val srsSettings by viewModel.srsSettings.collectAsStateWithLifecycle()
    val showSrsSettingsSheet by viewModel.showSrsSettingsSheet.collectAsStateWithLifecycle()
    val cardForDiagnostics by viewModel.cardForDiagnostics.collectAsStateWithLifecycle()
    val cardDiagnosticsLogs by viewModel.cardDiagnosticsLogs.collectAsStateWithLifecycle()
    val showTimeboxAlert by viewModel.showTimeboxAlert.collectAsStateWithLifecycle()
    val timeboxCardsReviewed by viewModel.timeboxCardsReviewed.collectAsStateWithLifecycle()
    val allStudyLogs by viewModel.allStudyLogs.collectAsStateWithLifecycle()
    val showStatsDashboard by viewModel.showStatsDashboard.collectAsStateWithLifecycle()
    val showDbMaintenance by viewModel.showDbMaintenance.collectAsStateWithLifecycle()
    val backupList by viewModel.backupList.collectAsStateWithLifecycle()

    var showIntroAnimation by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Display user feedback toasts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (showIntroAnimation) {
        CustomAppLoadingScreen(
            ttsHelper = ttsHelper,
            onFinished = { showIntroAnimation = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // Study Mode Screen
                studySession != null -> {
                    StudyScreen(
                        session = studySession!!,
                        ttsHelper = ttsHelper,
                        srsSettings = srsSettings,
                        onBack = { viewModel.endStudySession() },
                        onRevealAnswer = { viewModel.revealAnswer() },
                        onToggleTypingMode = { viewModel.toggleTypingMode() },
                        onDirectionChanged = { viewModel.setStudyDirection(it) },
                        onTypedInputChanged = { viewModel.updateTypedInput(it) },
                        onVerifyAnswer = { viewModel.verifyAnswer() },
                        onSubmitRating = { viewModel.submitRating(it) },
                        onUndo = { viewModel.undoLastReview() },
                        onToggleWhiteboard = { viewModel.toggleWhiteboard() },
                        onOpenCardDiagnostics = { viewModel.openCardDiagnostics(it) },
                        onSuspendCard = { viewModel.suspendCurrentCard() },
                        onBuryCard = { viewModel.buryCurrentCard() },
                        onToggleFlag = { flag ->
                            studySession?.currentCard?.let { viewModel.toggleCardFlag(it.id, flag) }
                        },
                        onOpenSettings = { viewModel.openSrsSettings() }
                    )
                }

            // Deck Card Browser & Range Selection Screen
            selectedDeckId != null -> {
                val currentDeck = decks.find { it.id == selectedDeckId }
                if (currentDeck != null) {
                    CardListScreen(
                        deck = currentDeck,
                        cards = displayedCards,
                        totalCardsInDeck = currentDeck.cardCount,
                        searchQuery = searchQuery,
                        sortOption = sortOption,
                        rangeState = rangeState,
                        selectedCardIds = selectedCardIds,
                        ttsHelper = ttsHelper,
                        displayDirection = cardDisplayDirection,
                        onDisplayDirectionChanged = { viewModel.setCardDisplayDirection(it) },
                        onBack = { viewModel.selectDeck(-1) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onSortSelected = { viewModel.setSortOption(it) },
                        onToggleRange = { viewModel.toggleRangeSelection(it) },
                        onSetRange = { start, end -> viewModel.setRange(start, end) },
                        onToggleCardSelect = { viewModel.toggleCardSelection(it) },
                        onStudyMicroSet = { viewModel.studyRangeMicroSet() },
                        onStudyWholeDeck = { viewModel.startDeckStudy(currentDeck.id) },
                        onBatchSuspend = { viewModel.batchSuspendSelected(it) },
                        onBatchResetSrs = { viewModel.batchResetSelectedProgress() },
                        onBatchFlag = { viewModel.batchFlagSelected(it) },
                        onBatchExportCsv = {
                            viewModel.exportCurrentCardsToCsv { csv ->
                                onShareCsv(csv)
                            }
                        },
                        onAddCard = { front, back, notes, gender, tags ->
                            viewModel.addCardToCurrentDeck(front, back, notes, gender, tags)
                        },
                        onOpenImport = {
                            viewModel.openCsvImportDialog(targetDeckId = currentDeck.id)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onBatchDeleteSelected = { viewModel.batchDeleteSelected() },
                        onClearSelection = { viewModel.clearCardSelection() },
                        onDeleteDeck = { viewModel.deleteDeck(currentDeck) },
                        onCardInfoClick = { viewModel.openCardDiagnostics(it) }
                    )
                } else {
                    // Fallback to deck list if deck is not found
                    DeckListScreen(
                        decks = decks,
                        cards = allCards,
                        dailyGoal = dailyGoal,
                        srsSettings = srsSettings,
                        onSelectDeck = { viewModel.selectDeck(it) },
                        onStudyDeck = { viewModel.startDeckStudy(it) },
                        onCramDeck = { viewModel.startCramStudy(it) },
                        onOpenImport = { chosenDeckId ->
                            viewModel.openCsvImportDialog(targetDeckId = chosenDeckId ?: decks.firstOrNull()?.id ?: 1L)
                        },
                        onCreateDeck = { name, desc, color ->
                            viewModel.createDeck(name, desc, color)
                        },
                        onDeleteDeck = { viewModel.deleteDeck(it) },
                        onPlayIntroAnimation = { showIntroAnimation = true },
                        onStartGoalStudy = { viewModel.startDailyGoalStudy() },
                        onUpdateDailyGoal = { words, rate -> viewModel.updateDailyGoalTargets(words, rate) },
                        onOpenSrsSettings = { viewModel.openSrsSettings() },
                        onOpenStatsDashboard = { viewModel.openStatsDashboard() },
                        onOpenDbMaintenance = { viewModel.openDbMaintenance() }
                    )
                }
            }

            // Primary Decks List Screen
            else -> {
                DeckListScreen(
                    decks = decks,
                    cards = allCards,
                    dailyGoal = dailyGoal,
                    srsSettings = srsSettings,
                    onSelectDeck = { viewModel.selectDeck(it) },
                    onStudyDeck = { viewModel.startDeckStudy(it) },
                    onCramDeck = { viewModel.startCramStudy(it) },
                    onOpenImport = { chosenDeckId ->
                        viewModel.openCsvImportDialog(targetDeckId = chosenDeckId ?: decks.firstOrNull()?.id ?: 1L)
                    },
                    onCreateDeck = { name, desc, color ->
                        viewModel.createDeck(name, desc, color)
                    },
                    onDeleteDeck = { viewModel.deleteDeck(it) },
                    onPlayIntroAnimation = { showIntroAnimation = true },
                    onStartGoalStudy = { viewModel.startDailyGoalStudy() },
                    onUpdateDailyGoal = { words, rate -> viewModel.updateDailyGoalTargets(words, rate) },
                    onOpenSrsSettings = { viewModel.openSrsSettings() },
                    onOpenStatsDashboard = { viewModel.openStatsDashboard() },
                    onOpenDbMaintenance = { viewModel.openDbMaintenance() }
                )
            }
        }

        // CSV and Anki (.apkg) Import Modal Dialog with Full Deck Selection
        if (csvImportState.isOpen) {
            ImportModalDialog(
                state = csvImportState,
                decks = decks,
                onCsvContentChanged = { viewModel.updateCsvContent(it) },
                onMappingChanged = { viewModel.updateCsvMapping(it) },
                onTargetDeckChanged = { viewModel.setImportTargetDeck(it) },
                onCreateNewDeck = { name, desc, color ->
                    viewModel.createDeckForImport(name, desc, color)
                },
                onExecuteCsvImport = { viewModel.executeCsvImport() },
                onImportAnkiUri = { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.let { stream ->
                            viewModel.importAnkiPackageStream(
                                context = context,
                                inputStream = stream,
                                targetDeckId = csvImportState.targetDeckId
                            )
                            viewModel.closeCsvImportDialog()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot read file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { viewModel.closeCsvImportDialog() }
            )
        }

        // SRS Settings Sheet (Algorithm, Desired Retention, Limits, Timebox)
        if (showSrsSettingsSheet) {
            SrsSettingsSheet(
                currentSettings = srsSettings,
                onDismiss = { viewModel.closeSrsSettings() },
                onSaveSettings = { viewModel.updateSrsSettings(it) },
                onOptimizeFsrsWeights = { viewModel.optimizeFsrsWeights() }
            )
        }

        // Card SRS Diagnostics & Info Dialog
        if (cardForDiagnostics != null) {
            CardInfoViewerDialog(
                card = cardForDiagnostics!!,
                logs = cardDiagnosticsLogs,
                settings = srsSettings,
                onDismiss = { viewModel.closeCardDiagnostics() },
                onToggleSuspend = { isSuspended ->
                    viewModel.suspendCurrentCard()
                },
                onBuryCard = {
                    viewModel.buryCurrentCard()
                },
                onResetProgress = {
                    viewModel.batchResetSelectedProgress()
                }
            )
        }

        // Timeboxing Session Break Dialog
        if (showTimeboxAlert) {
            TimeboxSummaryDialog(
                cardsReviewed = timeboxCardsReviewed,
                timeboxMinutes = srsSettings.timeboxMinutes,
                onTakeBreak = {
                    viewModel.dismissTimeboxAlert()
                    viewModel.endStudySession()
                },
                onContinue = {
                    viewModel.dismissTimeboxAlert()
                }
            )
        }

        // Study Analytics & Heatmap Dashboard
        if (showStatsDashboard) {
            StatsDashboardDialog(
                cards = allCards,
                studyLogs = allStudyLogs,
                onDismiss = { viewModel.closeStatsDashboard() }
            )
        }

        // Database Maintenance & Backup Dialog
        if (showDbMaintenance) {
            DatabaseMaintenanceDialog(
                onDismiss = { viewModel.closeDbMaintenance() },
                onRunIntegrityCheck = { onRes -> viewModel.runDatabaseIntegrityCheck(onRes) },
                onOptimizeDatabase = { onMsg -> viewModel.optimizeDatabase(onMsg) },
                onCreateBackup = { onB -> viewModel.createDatabaseBackup(onB) },
                backups = backupList
            )
        }

        }
    }
}
