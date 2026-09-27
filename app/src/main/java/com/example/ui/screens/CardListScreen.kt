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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.ui.viewmodel.StudyDirection

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
    displayDirection: StudyDirection = StudyDirection.GERMAN_TO_ENGLISH,
    onDisplayDirectionChanged: (StudyDirection) -> Unit = {},
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
    onBatchFlag: (Int) -> Unit = {},
    onBatchExportCsv: () -> Unit,
    onAddCard: (String, String, String, String, String) -> Unit,
    onOpenImport: () -> Unit,
    onDeleteCard: (FlashcardEntity) -> Unit,
    onBatchDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteDeck: () -> Unit,
    onCardInfoClick: (FlashcardEntity) -> Unit = {}
) {
    BackHandler { onBack() }

    var isSearchActive by remember { mutableStateOf(false) }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var isAddCardDialogOpen by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<FlashcardEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteDeckConfirm by remember { mutableStateOf(false) }
    var locallyFlippedCardIds by remember { mutableStateOf(setOf<Long>()) }

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
                    IconButton(onClick = onOpenImport, modifier = Modifier.testTag("import_to_this_deck_appbar_btn")) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import Cards into ${deck.name}"
                        )
                    }
                    IconButton(onClick = onStudyWholeDeck, modifier = Modifier.testTag("study_all_btn")) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Study Deck",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDeckConfirm = true },
                        modifier = Modifier.testTag("delete_deck_from_card_list_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${deck.name}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
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
                onBatchExportCsv = onBatchExportCsv,
                onBatchDelete = { showBatchDeleteConfirm = true }
            )

            // Multi-selected cards quick action banner
            if (selectedCardIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("selected_cards_action_bar"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedCardIds.size} cards selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onClearSelection,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // Batch Flag dropdown
                            var batchFlagExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { batchFlagExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("batch_flag_btn")
                                ) {
                                    Icon(Icons.Default.Flag, contentDescription = "Flag Selected", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Flag", fontSize = 11.sp)
                                }

                                DropdownMenu(
                                    expanded = batchFlagExpanded,
                                    onDismissRequest = { batchFlagExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Red Flag") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444)) },
                                        onClick = {
                                            onBatchFlag(1)
                                            batchFlagExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Orange Flag") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFF97316)) },
                                        onClick = {
                                            onBatchFlag(2)
                                            batchFlagExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Green Flag") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF10B981)) },
                                        onClick = {
                                            onBatchFlag(3)
                                            batchFlagExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Blue Flag") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        onClick = {
                                            onBatchFlag(4)
                                            batchFlagExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear Flag") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Gray) },
                                        onClick = {
                                            onBatchFlag(0)
                                            batchFlagExpanded = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showBatchDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("batch_delete_selected_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete (${selectedCardIds.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Card Front/Back Orientation Switcher Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Card Direction",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Display:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DirectionPill(
                            label = "🇩🇪 German 1st",
                            isSelected = displayDirection == StudyDirection.GERMAN_TO_ENGLISH,
                            onClick = {
                                onDisplayDirectionChanged(StudyDirection.GERMAN_TO_ENGLISH)
                                locallyFlippedCardIds = emptySet()
                            },
                            testTag = "direction_pill_german"
                        )
                        DirectionPill(
                            label = "🇬🇧 English 1st",
                            isSelected = displayDirection == StudyDirection.ENGLISH_TO_GERMAN,
                            onClick = {
                                onDisplayDirectionChanged(StudyDirection.ENGLISH_TO_GERMAN)
                                locallyFlippedCardIds = emptySet()
                            },
                            testTag = "direction_pill_english"
                        )
                        DirectionPill(
                            label = "🔄 Alt",
                            isSelected = displayDirection == StudyDirection.ALTERNATING,
                            onClick = {
                                onDisplayDirectionChanged(StudyDirection.ALTERNATING)
                                locallyFlippedCardIds = emptySet()
                            },
                            testTag = "direction_pill_alternating"
                        )
                    }
                }
            }

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
                    itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                        val baseIsGermanFirst = when (displayDirection) {
                            StudyDirection.GERMAN_TO_ENGLISH -> true
                            StudyDirection.ENGLISH_TO_GERMAN -> false
                            StudyDirection.ALTERNATING -> (index % 2 == 0)
                        }
                        val isGermanFirst = if (locallyFlippedCardIds.contains(card.id)) !baseIsGermanFirst else baseIsGermanFirst

                        CardRowItem(
                            card = card,
                            isGermanFirst = isGermanFirst,
                            isSelected = selectedCardIds.contains(card.id),
                            onToggleSelect = { onToggleCardSelect(card.id) },
                            onCardInfo = { onCardInfoClick(card) },
                            onFlipCard = {
                                locallyFlippedCardIds = if (locallyFlippedCardIds.contains(card.id)) {
                                    locallyFlippedCardIds - card.id
                                } else {
                                    locallyFlippedCardIds + card.id
                                }
                            },
                            onPlayTts = { ttsHelper.speak(card.front) },
                            onDeleteCard = { cardToDelete = card }
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

    // Single card deletion confirmation dialog
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete Flashcard?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete card #${card.orderIndex}:\n\n\"${card.front}\" (${card.back})?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(card)
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_card_btn")
                ) {
                    Text("Delete Card", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cardToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_card_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch cards deletion confirmation dialog
    if (showBatchDeleteConfirm) {
        val count = if (selectedCardIds.isNotEmpty()) selectedCardIds.size else cards.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete $count Cards?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete $count cards from '${deck.name}'? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBatchDeleteSelected()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_batch_delete_btn")
                ) {
                    Text("Delete $count Cards", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteConfirm = false },
                    modifier = Modifier.testTag("cancel_batch_delete_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Entire Deck deletion confirmation dialog
    if (showDeleteDeckConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteDeckConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Entire Deck?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${deck.name}' and all ${deck.cardCount} cards inside it? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDeckConfirm = false
                        onDeleteDeck()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_deck_from_cards_btn")
                ) {
                    Text("Delete Deck", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDeckConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_deck_from_cards_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CardRowItem(
    card: FlashcardEntity,
    isGermanFirst: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCardInfo: () -> Unit,
    onFlipCard: () -> Unit,
    onPlayTts: () -> Unit,
    onDeleteCard: () -> Unit
) {
    val genderColor = getGenderColor(card.gender)
    val primaryText = if (isGermanFirst) card.front else card.back
    val secondaryText = if (isGermanFirst) card.back else card.front
    val primaryColor = if (isGermanFirst && genderColor != Color.Unspecified) {
        genderColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }

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
                        text = primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    if (isGermanFirst && card.gender.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GenderBadge(gender = card.gender)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!isGermanFirst && genderColor != Color.Unspecified) genderColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isGermanFirst && card.gender.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GenderBadge(gender = card.gender)
                    }
                }

                // SRS status indicator pill
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val srsBadge = when {
                        card.state == 3 -> "Suspended"
                        card.repetitions == 0 -> "New"
                        card.intervalDays <= 1 -> "Learning (1d)"
                        card.intervalDays < 21 -> "Young (${card.intervalDays}d)"
                        else -> "Mature (${card.intervalDays}d)"
                    }
                    val badgeColor = when {
                        card.state == 3 -> Color(0xFF64748B)
                        card.repetitions == 0 -> Color(0xFFA855F7)
                        card.intervalDays <= 1 -> Color(0xFFF59E0B)
                        card.intervalDays < 21 -> Color(0xFF3B82F6)
                        else -> Color(0xFF10B981)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(srsBadge, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                    }

                    // Flag indicator
                    if (card.flag > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val flagColor = when (card.flag) {
                            1 -> Color(0xFFEF4444)
                            2 -> Color(0xFFF97316)
                            3 -> Color(0xFF10B981)
                            4 -> Color(0xFF3B82F6)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(flagColor.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = "Flagged",
                                tint = flagColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    // Leech indicator
                    if (card.lapses >= 8) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Leech (${card.lapses})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

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

            // Card Diagnostics Info Button
            IconButton(
                onClick = onCardInfo,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("card_info_btn_${card.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "SRS Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Flip Individual Card Sides Button
            IconButton(
                onClick = onFlipCard,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("flip_card_btn_${card.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Flip Card Sides",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
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

            // Delete Single Card Button
            IconButton(
                onClick = onDeleteCard,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_card_btn_${card.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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
