package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FlashcardEntity
import com.example.data.db.StudyLogEntity
import com.example.domain.srs.AnkiSrsEngine
import com.example.domain.srs.CardMemoryState
import com.example.domain.srs.SrsDeckSettings
import com.example.ui.theme.SrsAgainColor
import com.example.ui.theme.SrsEasyColor
import com.example.ui.theme.SrsGoodColor
import com.example.ui.theme.SrsHardColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardInfoViewerDialog(
    card: FlashcardEntity,
    logs: List<StudyLogEntity> = emptyList(),
    settings: SrsDeckSettings = SrsDeckSettings(),
    onDismiss: () -> Unit,
    onToggleSuspend: (Boolean) -> Unit,
    onBuryCard: () -> Unit,
    onResetProgress: () -> Unit
) {
    val memoryState: CardMemoryState = AnkiSrsEngine.getCardMemoryState(card, settings)
    val isSuspended = card.state == 3
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Card Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Card SRS Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Algorithm badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = settings.algorithm.shortName,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                item {
                    // Front & Back summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (card.gender.isNotBlank()) {
                                    GenderBadge(gender = card.gender)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = card.front,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.back,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Memory Model & Retrievability Grid
                    Text(
                        text = "FSRS & MEMORY STABILITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoMetricBox(
                            label = "Retrievability (R)",
                            value = "${memoryState.retrievabilityPercent}%",
                            subtitle = "Recall probability",
                            highlightColor = if (memoryState.retrievabilityPercent >= 85) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )

                        InfoMetricBox(
                            label = "Stability (S)",
                            value = String.format(Locale.US, "%.1fd", memoryState.stability),
                            subtitle = "Days to 90% retention",
                            highlightColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoMetricBox(
                            label = "Difficulty (D)",
                            value = String.format(Locale.US, "%.1f / 10", memoryState.difficulty),
                            subtitle = "Intrinsic complexity",
                            highlightColor = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f)
                        )

                        InfoMetricBox(
                            label = "Maturity State",
                            value = memoryState.maturityCategory,
                            subtitle = "${card.repetitions} reps • ${card.lapses} lapses",
                            highlightColor = when (memoryState.maturityCategory) {
                                "Mature" -> Color(0xFF10B981)
                                "Young" -> Color(0xFF3B82F6)
                                "Learning" -> Color(0xFFF59E0B)
                                else -> Color(0xFF94A3B8)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scheduling Metrics
                    Text(
                        text = "SCHEDULING PARAMETERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyValueRow("Current Interval", "${card.intervalDays} days (${AnkiSrsEngine.formatInterval(card.intervalDays)})")
                        KeyValueRow("SM-2 Ease Factor", String.format(Locale.US, "%.2f (starting: %.2f)", card.easeFactor, settings.startingEase))
                        KeyValueRow("Due Timestamp", if (card.dueTimestamp > 0) sdf.format(Date(card.dueTimestamp)) else "Due Now (New)")
                        KeyValueRow("Card Status", when (card.state) {
                            3 -> "Suspended (Skipped from study)"
                            2 -> "Review (In SRS rotation)"
                            1 -> "Learning (Step queue)"
                            else -> "New (Unseen)"
                        })
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Review History Log
                    Text(
                        text = "HISTORICAL REVIEW LOGS (${logs.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "No prior study logs recorded for this card yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(logs) { log ->
                        val ratingLabel = when (log.rating) {
                            1 -> "Again"
                            2 -> "Hard"
                            3 -> "Good"
                            4 -> "Easy"
                            else -> "Rating ${log.rating}"
                        }
                        val ratingColor = when (log.rating) {
                            1 -> SrsAgainColor
                            2 -> SrsHardColor
                            3 -> SrsGoodColor
                            4 -> SrsEasyColor
                            else -> Color.Gray
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ratingColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ratingLabel,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${log.intervalDays}d int",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Card Management Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onToggleSuspend(!isSuspended)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSuspended) "Unsuspend" else "Suspend", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onBuryCard()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bury Today", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoMetricBox(
    label: String,
    value: String,
    subtitle: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = highlightColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
