package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.FlashcardEntity
import com.example.data.db.StudyLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun StatsDashboardDialog(
    cards: List<FlashcardEntity>,
    studyLogs: List<StudyLogEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Heatmap", "Forecast", "Leeches")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("stats_dashboard_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Study Analytics & Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Local statistics & review consistency",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("stats_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> StatsOverviewSection(cards = cards, logs = studyLogs)
                        1 -> StatsHeatmapSection(logs = studyLogs)
                        2 -> StatsForecastSection(cards = cards)
                        3 -> StatsLeechSection(cards = cards)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsOverviewSection(cards: List<FlashcardEntity>, logs: List<StudyLogEntity>) {
    val totalCards = cards.size
    val newCards = cards.count { it.state == 0 }
    val learningCards = cards.count { it.state == 1 }
    val youngCards = cards.count { it.state == 2 && it.intervalDays < 21 }
    val matureCards = cards.count { it.state == 2 && it.intervalDays >= 21 }
    val suspendedCards = cards.count { it.state == 3 }

    val totalReviews = logs.size
    val passedReviews = logs.count { it.rating > 1 }
    val accuracy = if (totalReviews > 0) (passedReviews * 100f / totalReviews) else 0f
    val estimatedSeconds = totalReviews * 8L
    val totalMinutes = TimeUnit.SECONDS.toMinutes(estimatedSeconds)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // High-Level KPIs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    title = "Total Cards",
                    value = "$totalCards",
                    subtitle = "${cards.map { it.deckId }.distinct().size} Decks",
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Reviews",
                    value = "$totalReviews",
                    subtitle = "$passedReviews Passed",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Retention",
                    value = String.format(Locale.US, "%.1f%%", accuracy),
                    subtitle = "Historical",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Card Maturity Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Card Maturity Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stacked bar visual
                    if (totalCards > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF334155))
                        ) {
                            if (matureCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(matureCards.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFF10B981))
                                )
                            }
                            if (youngCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(youngCards.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFF38BDF8))
                                )
                            }
                            if (learningCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(learningCards.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                            if (newCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(newCards.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFF6366F1))
                                )
                            }
                            if (suspendedCards > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(suspendedCards.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFF64748B))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MaturityRow(label = "Mature (≥21 days)", count = matureCards, total = totalCards, color = Color(0xFF10B981))
                    MaturityRow(label = "Young (<21 days)", count = youngCards, total = totalCards, color = Color(0xFF38BDF8))
                    MaturityRow(label = "Learning", count = learningCards, total = totalCards, color = Color(0xFFF59E0B))
                    MaturityRow(label = "New (Unseen)", count = newCards, total = totalCards, color = Color(0xFF6366F1))
                    MaturityRow(label = "Suspended", count = suspendedCards, total = totalCards, color = Color(0xFF64748B))
                }
            }
        }

        item {
            // Study Time Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Estimated Active Study Time",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$totalMinutes mins logged across $totalReviews card reviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaturityRow(label: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) (count * 100f / total) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 12.sp, color = Color(0xFFE2E8F0))
        }
        Text(
            text = "$count (${String.format(Locale.US, "%.1f%%", pct)})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun StatsHeatmapSection(logs: List<StudyLogEntity>) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val reviewsPerDay = remember(logs) {
        logs.groupBy { sdf.format(Date(it.timestamp)) }
            .mapValues { it.value.size }
    }

    // Generate last 60 days
    val daysList = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -59)
        for (i in 0 until 60) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Daily Review Heatmap",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Study consistency over past 60 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF10B981))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Heatmap Grid: 10 columns x 6 rows
                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(daysList) { dayStr ->
                        val count = reviewsPerDay[dayStr] ?: 0
                        val boxColor = when {
                            count >= 25 -> Color(0xFF10B981)
                            count >= 10 -> Color(0xFF34D399)
                            count >= 5 -> Color(0xFF6EE7B7)
                            count > 0 -> Color(0xFFA7F3D0)
                            else -> Color(0xFF334155)
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(boxColor)
                                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = if (count > 99) "99+" else "$count",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (count >= 10) Color.White else Color(0xFF064E3B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Less", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(
                        Color(0xFF334155),
                        Color(0xFFA7F3D0),
                        Color(0xFF6EE7B7),
                        Color(0xFF34D399),
                        Color(0xFF10B981)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text("More", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun StatsForecastSection(cards: List<FlashcardEntity>) {
    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 86_400_000L

    val dueToday = cards.count { it.state != 3 && it.dueTimestamp <= now && it.state != 0 }
    val dueTomorrow = cards.count { it.state != 3 && it.dueTimestamp in (now + 1)..(now + oneDayMs) }
    val dueIn3Days = cards.count { it.state != 3 && it.dueTimestamp in (now + oneDayMs + 1)..(now + 3 * oneDayMs) }
    val dueIn7Days = cards.count { it.state != 3 && it.dueTimestamp in (now + 3 * oneDayMs + 1)..(now + 7 * oneDayMs) }
    val dueIn30Days = cards.count { it.state != 3 && it.dueTimestamp in (now + 7 * oneDayMs + 1)..(now + 30 * oneDayMs) }
    val maxDue = maxOf(1, dueToday, dueTomorrow, dueIn3Days, dueIn7Days, dueIn30Days)

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Review Scheduling Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Projected cards due in upcoming days",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                ForecastBar(label = "Due Today", count = dueToday, max = maxDue, color = Color(0xFFEF4444))
                ForecastBar(label = "Tomorrow", count = dueTomorrow, max = maxDue, color = Color(0xFFF59E0B))
                ForecastBar(label = "In 2-3 Days", count = dueIn3Days, max = maxDue, color = Color(0xFF38BDF8))
                ForecastBar(label = "In 4-7 Days", count = dueIn7Days, max = maxDue, color = Color(0xFF10B981))
                ForecastBar(label = "In 8-30 Days", count = dueIn30Days, max = maxDue, color = Color(0xFF818CF8))
            }
        }
    }
}

@Composable
fun ForecastBar(label: String, count: Int, max: Int, color: Color) {
    val fraction = (count.toFloat() / max).coerceIn(0.04f, 1f)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFFCBD5E1))
            Text("$count cards", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF334155))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color)
            )
        }
    }
}

@Composable
fun StatsLeechSection(cards: List<FlashcardEntity>) {
    val leechCards = cards.filter { it.lapses >= 8 || it.tags.contains("leech", ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Leech Cards (${leechCards.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                        Text(
                            text = "Cards repeatedly lapsed (≥8 lapses)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171))
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (leechCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉 No leeches detected! You are retaining words effectively.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF34D399)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(leechCards.size) { idx ->
                            val card = leechCards[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.front,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = card.back,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7F1D1D))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${card.lapses} lapses",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}
