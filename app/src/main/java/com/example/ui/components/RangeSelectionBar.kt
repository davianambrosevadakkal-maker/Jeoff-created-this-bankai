package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.RangeSelectionState

@Composable
fun RangeSelectionBar(
    state: RangeSelectionState,
    onToggleRange: (Boolean) -> Unit,
    onSetRange: (Int, Int) -> Unit,
    onStudyMicroSet: () -> Unit,
    onBatchSuspend: (Boolean) -> Unit,
    onBatchResetSrs: () -> Unit,
    onBatchExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCustomRangeExpanded by remember { mutableStateOf(false) }
    var inputStart by remember(state.startIndex) { mutableStateOf(state.startIndex.toString()) }
    var inputEnd by remember(state.endIndex) { mutableStateOf(state.endIndex.toString()) }
    var showBatchMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("range_selection_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Toggle & Active Range Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Range Selection",
                        tint = if (state.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isEnabled) {
                            "Micro-Set: #${state.startIndex} – #${state.endIndex} (${state.count} cards)"
                        } else {
                            "Range Selection (Micro-Sets)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isEnabled) {
                        IconButton(
                            onClick = { onToggleRange(false) },
                            modifier = Modifier.size(32.dp).testTag("clear_range_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Range",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Batch Actions Menu
                    IconButton(
                        onClick = { showBatchMenu = true },
                        modifier = Modifier.size(32.dp).testTag("batch_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Batch Actions",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showBatchMenu,
                        onDismissRequest = { showBatchMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Suspend Micro-Set") },
                            onClick = {
                                showBatchMenu = false
                                onBatchSuspend(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unsuspend Micro-Set") },
                            onClick = {
                                showBatchMenu = false
                                onBatchSuspend(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset SRS Progress") },
                            onClick = {
                                showBatchMenu = false
                                onBatchResetSrs()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Micro-Set to CSV") },
                            onClick = {
                                showBatchMenu = false
                                onBatchExportCsv()
                            }
                        )
                    }
                }
            }

            // Quick Micro-set Preset Chips (e.g. [1-25], [26-50], [50-75], [76-100], [Custom...])
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Preset 1: 1 - 25
                FilterChip(
                    selected = state.isEnabled && state.startIndex == 1 && state.endIndex == 25.coerceAtMost(state.maxDeckIndex),
                    onClick = {
                        onSetRange(1, 25.coerceAtMost(state.maxDeckIndex))
                        isCustomRangeExpanded = false
                    },
                    label = { Text("#1 – #25", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_1_25")
                )

                // Preset 2: 26 - 50
                if (state.maxDeckIndex >= 26) {
                    FilterChip(
                        selected = state.isEnabled && state.startIndex == 26 && state.endIndex == 50.coerceAtMost(state.maxDeckIndex),
                        onClick = {
                            onSetRange(26, 50.coerceAtMost(state.maxDeckIndex))
                            isCustomRangeExpanded = false
                        },
                        label = { Text("#26 – #50", fontSize = 12.sp) },
                        modifier = Modifier.testTag("preset_26_50")
                    )
                }

                // Preset 3: 50 - 75 (Highlighted as specified in requirements!)
                if (state.maxDeckIndex >= 50) {
                    FilterChip(
                        selected = state.isEnabled && state.startIndex == 50 && state.endIndex == 75.coerceAtMost(state.maxDeckIndex),
                        onClick = {
                            onSetRange(50, 75.coerceAtMost(state.maxDeckIndex))
                            isCustomRangeExpanded = false
                        },
                        label = { Text("⭐ #50 – #75", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("preset_50_75")
                    )
                }

                // Preset 4: 76 - 100
                if (state.maxDeckIndex >= 76) {
                    FilterChip(
                        selected = state.isEnabled && state.startIndex == 76 && state.endIndex == 100.coerceAtMost(state.maxDeckIndex),
                        onClick = {
                            onSetRange(76, 100.coerceAtMost(state.maxDeckIndex))
                            isCustomRangeExpanded = false
                        },
                        label = { Text("#76 – #100", fontSize = 12.sp) }
                    )
                }

                // Custom Range Chip
                FilterChip(
                    selected = isCustomRangeExpanded,
                    onClick = { isCustomRangeExpanded = !isCustomRangeExpanded },
                    label = { Text("Custom Range...", fontSize = 12.sp) },
                    modifier = Modifier.testTag("custom_range_chip")
                )
            }

            // Expandable Custom Numeric Range Input
            AnimatedVisibility(
                visible = isCustomRangeExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputStart,
                        onValueChange = { inputStart = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Start #") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_start_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("to", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = inputEnd,
                        onValueChange = { inputEnd = it.filter { ch -> ch.isDigit() } },
                        label = { Text("End #") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_end_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            val s = inputStart.toIntOrNull() ?: 1
                            val e = inputEnd.toIntOrNull() ?: state.maxDeckIndex
                            onSetRange(s, e)
                            isCustomRangeExpanded = false
                        },
                        modifier = Modifier.testTag("apply_custom_range_button")
                    ) {
                        Text("Apply")
                    }
                }
            }

            // Prominent Action Button: Study Micro-Set
            if (state.isEnabled) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                Button(
                    onClick = onStudyMicroSet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("study_microset_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Study Micro-Set",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study Micro-Set (#${state.startIndex} – #${state.endIndex})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
