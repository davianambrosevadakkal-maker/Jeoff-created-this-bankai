package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FlashcardEntity
import com.example.domain.srs.SrsDeckSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class DayForecast(
    val dayLabel: String,
    val count: Int,
    val isToday: Boolean
)

@Composable
fun SrsForecastCard(
    cards: List<FlashcardEntity>,
    settings: SrsDeckSettings = SrsDeckSettings(),
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Maturity counts
    val newCount = cards.count { it.repetitions == 0 && it.state != 3 }
    val learningCount = cards.count { it.repetitions > 0 && it.intervalDays <= 1 && it.state != 3 }
    val youngCount = cards.count { it.intervalDays in 2..20 && it.state != 3 }
    val matureCount = cards.count { it.intervalDays >= 21 && it.state != 3 }
    val suspendedCount = cards.count { it.state == 3 }
    val totalCount = max(1, cards.size)

    // Calculate 7-day forecast
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("EEE", Locale.US)
    val now = System.currentTimeMillis()

    val forecastList = remember(cards, now) {
        (0..6).map { dayOffset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1)

            val dueCount = if (dayOffset == 0) {
                // Today: past due + due today
                cards.count { it.state != 3 && (it.state == 0 || it.dueTimestamp <= endOfDay) }
            } else {
                cards.count { it.state != 3 && it.dueTimestamp in (startOfDay until endOfDay) }
            }

            DayForecast(
                dayLabel = if (dayOffset == 0) "Today" else sdf.format(cal.time),
                count = dueCount,
                isToday = dayOffset == 0
            )
        }
    }

    val maxDayDue = max(1, forecastList.maxOf { it.count })

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SRS Forecast & Card Maturity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${settings.algorithm.shortName} • ${ (settings.desiredRetentionRate * 100).toInt() }% Retention Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Forecast Details",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Maturity Segmented Stack Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1E293B))
            ) {
                if (matureCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(matureCount.toFloat() / totalCount)
                            .fillMaxHeight()
                            .background(Color(0xFF10B981)) // Green
                    )
                }
                if (youngCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(youngCount.toFloat() / totalCount)
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6)) // Blue
                    )
                }
                if (learningCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(learningCount.toFloat() / totalCount)
                            .fillMaxHeight()
                            .background(Color(0xFFF59E0B)) // Amber
                    )
                }
                if (newCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(newCount.toFloat() / totalCount)
                            .fillMaxHeight()
                            .background(Color(0xFFA855F7)) // Purple
                    )
                }
                if (suspendedCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(suspendedCount.toFloat() / totalCount)
                            .fillMaxHeight()
                            .background(Color(0xFF64748B)) // Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Maturity Counts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MaturityChip(label = "Mature", count = matureCount, color = Color(0xFF10B981))
                MaturityChip(label = "Young", count = youngCount, color = Color(0xFF3B82F6))
                MaturityChip(label = "Learn", count = learningCount, color = Color(0xFFF59E0B))
                MaturityChip(label = "New", count = newCount, color = Color(0xFFA855F7))
            }

            // Expandable 7-Day Forecast Bar Graph
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "7-DAY REVIEW FORECAST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        forecastList.forEach { df ->
                            val barHeightFraction = (df.count.toFloat() / maxDayDue).coerceIn(0.12f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${df.count}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (df.isToday) Color(0xFF38BDF8) else Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height((70 * barHeightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (df.isToday) Color(0xFF0284C7) else Color(0xFF334155)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = df.dayLabel,
                                    fontSize = 10.sp,
                                    color = if (df.isToday) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    fontWeight = if (df.isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaturityChip(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Medium
        )
    }
}
