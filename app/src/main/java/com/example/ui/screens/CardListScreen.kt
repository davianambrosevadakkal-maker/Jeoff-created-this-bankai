package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.domain.tts.GermanTtsHelper
import com.example.ui.components.GenderBadge
import com.example.ui.components.RangeSelectionBar
import com.example.ui.components.SortFilterSheet
import com.example.ui.components.getGenderColor
import com.example.ui.viewmodel.CardSortOption
import com.example.ui.viewmodel.RangeSelectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    deck: DeckEntity,
    cards: List<FlashcardEntity>,
    totalCardsInDeck: Int,
    searchQuery: String,
    sortOption: CardSortOption,
    rangeState: RangeSelectionState,
    selectedCardIds: Set<Long>,
    ttsHelper: GermanTtsHelper,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortSelected: (CardSortOption) -> Unit,
    onToggleRange: (Boolean) -> Unit,
    onSetRange: (Int, Int) -> Unit,
    onToggleCardSelect: (Long) -> Unit,
    onStudyMicroSet: () -> Unit,
    onStudyWholeDeck: () -> Unit,
    onBatchSuspend: (Boolean) -> Unit,
    onBatchResetSrs: () -> Unit,
    onBatchExportCsv: () -> Unit,
    onAddCard: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }

    var isSearchActive by remember { mutableStateOf(false) }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var isAddCardDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search German, English, tags...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_search_input"),
                            trailingIcon = {
                                IconButton(onClick = {
                                    onSearchChanged("")
                                    isSearchActive = false
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = deck.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${cards.size} of $totalCardsInDeck cards shown • ${sortOption.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("card_list_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }, modifier = Modifier.testTag("toggle_search_btn")) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = { isSortSheetOpen = true }, modifier = Modifier.testTag("open_sort_btn")) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort Cards")
                    }
                    IconButton(onClick = onStudyWholeDeck, modifier = Modifier.testTag("study_all_btn")) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Study Deck",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddCardDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Flexible Range Selection Bar (Preset micro-sets e.g. 50-75, custom range inputs, batch actions)
            RangeSelectionBar(
                state = rangeState,
                onToggleRange = onToggleRange,
                onSetRange = onSetRange,
                onStudyMicroSet = onStudyMicroSet,
                onBatchSuspend = onBatchSuspend,
                onBatchResetSrs = onBatchResetSrs,
                onBatchExportCsv = onBatchExportCsv
            )

            // Cards List
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No cards match '$searchQuery'" else "No cards in this range",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardRowItem(
                            card = card,
                            isSelected = selectedCardIds.contains(card.id),
                            onToggleSelect = { onToggleCardSelect(card.id) },
                            onPlayTts = { ttsHelper.speak(card.front) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (isSortSheetOpen) {
        SortFilterSheet(
            currentSort = sortOption,
            onSortSelected = onSortSelected,
            onDismiss = { isSortSheetOpen = false }
        )
    }

    if (isAddCardDialogOpen) {
        AddCardDialog(
            onDismiss = { isAddCardDialogOpen = false },
            onAdd = { front, back, notes, gender, tags ->
                onAddCard(front, back, notes, gender, tags)
                isAddCardDialogOpen = false
            }
        )
    }
}

@Composable
private fun CardRowItem(
    card: FlashcardEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPlayTts: () -> Unit
) {
    val genderColor = getGenderColor(card.gender)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("card_item_${card.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.testTag("card_checkbox_${card.id}")
            )

            // Numeric Order Index Badge (e.g. #50, #75)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#${card.orderIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.front,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (genderColor != Color.Unspecified) genderColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (card.gender.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GenderBadge(gender = card.gender)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = card.back,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (card.synonyms.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "≈ ${card.synonyms}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                if (card.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Audio Pronunciation TTS Button
            IconButton(
                onClick = onPlayTts,
                modifier = Modifier.testTag("tts_play_${card.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Pronounce German",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AddCardDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Flashcard", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (German Word/Phrase)*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_card_front_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (English Meaning)*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_card_back_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (g in listOf("der", "die", "das")) {
                        val isSelected = gender.equals(g, ignoreCase = true)
                        OutlinedButton(
                            onClick = { gender = if (isSelected) "" else g },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(g, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Example Sentence / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (e.g. B1, verbs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (front.isNotBlank() && back.isNotBlank()) {
                        onAdd(front, back, notes, gender, tags)
                    }
                },
                enabled = front.isNotBlank() && back.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_card_btn")
            ) {
                Text("Add Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
