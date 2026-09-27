package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.tts.GermanTtsHelper
import com.example.ui.components.ImportModalDialog
import com.example.ui.screens.CardListScreen
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
    val selectedDeckId by viewModel.selectedDeckId.collectAsStateWithLifecycle()
    val displayedCards by viewModel.displayedCards.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val rangeState by viewModel.rangeSelection.collectAsStateWithLifecycle()
    val selectedCardIds by viewModel.selectedCardIds.collectAsStateWithLifecycle()
    val studySession by viewModel.studySession.collectAsStateWithLifecycle()
    val csvImportState by viewModel.csvImportState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Display user feedback toasts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Study Mode Screen
            studySession != null -> {
                StudyScreen(
                    session = studySession!!,
                    ttsHelper = ttsHelper,
                    onBack = { viewModel.endStudySession() },
                    onRevealAnswer = { viewModel.revealAnswer() },
                    onToggleTypingMode = { viewModel.toggleTypingMode() },
                    onTypedInputChanged = { viewModel.updateTypedInput(it) },
                    onVerifyAnswer = { viewModel.verifyAnswer() },
                    onSubmitRating = { viewModel.submitRating(it) }
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
                        onDeleteDeck = { viewModel.deleteDeck(currentDeck) }
                    )
                } else {
                    // Fallback to deck list if deck is not found
                    DeckListScreen(
                        decks = decks,
                        onSelectDeck = { viewModel.selectDeck(it) },
                        onStudyDeck = { viewModel.startDeckStudy(it) },
                        onOpenImport = { chosenDeckId ->
                            viewModel.openCsvImportDialog(targetDeckId = chosenDeckId ?: decks.firstOrNull()?.id ?: 1L)
                        },
                        onCreateDeck = { name, desc, color ->
                            viewModel.createDeck(name, desc, color)
                        },
                        onDeleteDeck = { viewModel.deleteDeck(it) }
                    )
                }
            }

            // Primary Decks List Screen
            else -> {
                DeckListScreen(
                    decks = decks,
                    onSelectDeck = { viewModel.selectDeck(it) },
                    onStudyDeck = { viewModel.startDeckStudy(it) },
                    onOpenImport = { chosenDeckId ->
                        viewModel.openCsvImportDialog(targetDeckId = chosenDeckId ?: decks.firstOrNull()?.id ?: 1L)
                    },
                    onCreateDeck = { name, desc, color ->
                        viewModel.createDeck(name, desc, color)
                    },
                    onDeleteDeck = { viewModel.deleteDeck(it) }
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
    }
}
