package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.goal.DailyLearningGoal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalTrackerCard(
    goal: DailyLearningGoal,
    modifier: Modifier = Modifier,
    onStartStudy: () -> Unit,
    onUpdateTarget: (targetWords: Int, targetRetentionPercent: Int) -> Unit
) {
    var showGoalSettingsDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressFraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    val animatedRetention by animateFloatAsState(
        targetValue = goal.actualRetentionRate,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "retentionProgress"
    )

    val progressRingColor by animateColorAsState(
        targetValue = when {
            goal.isGoalAchieved -> Color(0xFF10B981) // Green
            goal.progressFraction >= 0.5f -> Color(0xFF38BDF8) // Cyan
            else -> Color(0xFF2563EB) // Blue
        },
        label = "ringColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("daily_goal_tracker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    if (goal.isGoalAchieved) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF3B82F6).copy(alpha = 0.4f),
                    Color(0xFF8B5CF6).copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Goal Title, Streak Badge, and Settings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Target",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Retention Target",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (goal.isGoalAchieved) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Goal Achieved",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Target: ${goal.targetWordsCount} Words @ ${goal.targetRatePercent}% Accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Day Streak Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${goal.streakDays}d",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Settings / Edit Goal button
                    IconButton(
                        onClick = { showGoalSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_daily_goal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expand / Collapse Toggle
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Details",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Metrics Section: Circular Retention Gauge + Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Progress Gauge
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(95.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val arcSize = size.width - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Background track
                        drawArc(
                            color = Color(0xFF334155),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Target retention threshold marker (dashed/subtle background indicator)
                        val targetSweep = 360f * goal.targetRetentionRate
                        drawArc(
                            color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                            startAngle = -90f,
                            sweepAngle = targetSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Butt)
                        )

                        // Actual progress sweep
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF38BDF8),
                                    progressRingColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${goal.retainedCount}/${goal.targetWordsCount}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Key Retention Progress Cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Retention Accuracy Status Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    goal.reviewedCount == 0 -> Color(0xFF1E293B)
                                    goal.actualRetentionRate >= goal.targetRetentionRate -> Color(0xFF064E3B)
                                    else -> Color(0xFF451A03)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Actual Retention",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = if (goal.reviewedCount > 0) "${goal.retentionRatePercent}%" else "-- %",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    goal.reviewedCount == 0 -> Color.White
                                    goal.actualRetentionRate >= goal.targetRetentionRate -> Color(0xFF34D399)
                                    else -> Color(0xFFFBBF24)
                                }
                            )
                        }

                        // Delta badge
                        if (goal.reviewedCount > 0) {
                            val delta = goal.retentionRatePercent - goal.targetRatePercent
                            val isAbove = delta >= 0
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAbove) Color(0xFF059669) else Color(0xFFD97706))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAbove) "+$delta%" else "$delta%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Target ${goal.targetRatePercent}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Linear Words Retained Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Words Retained",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${goal.retainedCount} of ${goal.targetWordsCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressRingColor,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            // Expandable Detailed SRS Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Multi-Segment Quality Bar
                    if (goal.reviewedCount > 0) {
                        Text(
                            text = "TODAY'S SRS REVIEWS BREAKDOWN (${goal.reviewedCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Stacked horizontal quality bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF334155))
                        ) {
                            val total = goal.reviewedCount.toFloat()
                            if (goal.easyCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(goal.easyCount / total)
                                        .height(10.dp)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            if (goal.goodCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(goal.goodCount / total)
                                        .height(10.dp)
                                        .background(Color(0xFF3B82F6))
                                )
                            }
                            if (goal.hardCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(goal.hardCount / total)
                                        .height(10.dp)
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                            if (goal.againCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(goal.againCount / total)
                                        .height(10.dp)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SRS Breakdown Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SrsMiniPill(label = "Easy", count = goal.easyCount, color = Color(0xFF10B981))
                            SrsMiniPill(label = "Good", count = goal.goodCount, color = Color(0xFF3B82F6))
                            SrsMiniPill(label = "Hard", count = goal.hardCount, color = Color(0xFFF59E0B))
                            SrsMiniPill(label = "Again", count = goal.againCount, color = Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Motivational Advice Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = goal.motivationalFeedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button: "Continue Towards Goal →"
                    Button(
                        onClick = onStartStudy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (goal.isGoalAchieved) Color(0xFF10B981) else Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_goal_study_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (goal.isGoalAchieved) Icons.Default.EmojiEvents else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (goal.isGoalAchieved) "Review More Flashcards" else "Study Towards Daily Goal (${goal.remainingWords} left)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Goal Configuration Dialog
    if (showGoalSettingsDialog) {
        GoalSettingsDialog(
            currentWords = goal.targetWordsCount,
            currentRetentionPercent = goal.targetRatePercent,
            onDismiss = { showGoalSettingsDialog = false },
            onSave = { words, rate ->
                onUpdateTarget(words, rate)
                showGoalSettingsDialog = false
            }
        )
    }
}

@Composable
private fun SrsMiniPill(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $count",
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GoalSettingsDialog(
    currentWords: Int,
    currentRetentionPercent: Int,
    onDismiss: () -> Unit,
    onSave: (targetWords: Int, targetRetentionPercent: Int) -> Unit
) {
    var wordsTarget by remember { mutableIntStateOf(currentWords) }
    var retentionTarget by remember { mutableIntStateOf(currentRetentionPercent) }

    val wordPresets = listOf(10, 15, 20, 25, 30, 50)
    val retentionPresets = listOf(75, 80, 85, 90, 95)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Customize Daily Learning Goal")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set your daily vocabulary retention target. Regular daily goals ensure optimal Anki spaced repetition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Words Target Selection
                Text(
                    text = "Daily Retained Words: $wordsTarget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wordPresets.take(3).forEach { count ->
                        FilterChip(
                            selected = (wordsTarget == count),
                            onClick = { wordsTarget = count },
                            label = { Text("$count words") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wordPresets.drop(3).forEach { count ->
                        FilterChip(
                            selected = (wordsTarget == count),
                            onClick = { wordsTarget = count },
                            label = { Text("$count words") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Retention Accuracy Target Selection
                Text(
                    text = "Target Retention Accuracy: $retentionTarget%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Anki standard target is typically 85% to 90% retention.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    retentionPresets.forEach { pct ->
                        FilterChip(
                            selected = (retentionTarget == pct),
                            onClick = { retentionTarget = pct },
                            label = { Text("$pct%") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(wordsTarget, retentionTarget) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
