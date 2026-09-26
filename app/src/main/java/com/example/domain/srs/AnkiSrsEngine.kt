package com.example.domain.srs

import com.example.data.db.FlashcardEntity
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

enum class SrsRating(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}

data class SrsResult(
    val updatedCard: FlashcardEntity,
    val nextIntervalDays: Int,
    val nextIntervalLabel: String
)

object AnkiSrsEngine {

    private const val MIN_EASE_FACTOR = 1.30f
    private const val DEFAULT_EASE_FACTOR = 2.50f
    private const val EASY_BONUS = 1.30f
    private const val HARD_INTERVAL_FACTOR = 1.20f

    fun calculateNextReview(card: FlashcardEntity, rating: SrsRating, now: Long = System.currentTimeMillis()): SrsResult {
        var intervalDays = card.intervalDays
        var easeFactor = card.easeFactor
        var reps = card.repetitions + 1
        var lapses = card.lapses
        var state = card.state

        when (rating) {
            SrsRating.AGAIN -> {
                lapses += 1
                reps = 0
                state = 1 // Learning
                intervalDays = 0 // Due immediately/today
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.20f)
            }
            SrsRating.HARD -> {
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.15f)
                intervalDays = if (intervalDays <= 1) {
                    1
                } else {
                    max(1, (intervalDays * HARD_INTERVAL_FACTOR).roundToInt())
                }
                state = 2 // Review
            }
            SrsRating.GOOD -> {
                if (intervalDays == 0) {
                    intervalDays = 1
                } else if (intervalDays == 1) {
                    intervalDays = 3
                } else {
                    intervalDays = max(2, (intervalDays * easeFactor).roundToInt())
                }
                state = 2 // Review
            }
            SrsRating.EASY -> {
                easeFactor += 0.15f
                if (intervalDays == 0) {
                    intervalDays = 4
                } else {
                    intervalDays = max(4, (intervalDays * easeFactor * EASY_BONUS).roundToInt())
                }
                state = 2 // Review
            }
        }

        val dueTimestamp = if (intervalDays == 0) {
            now + TimeUnit.MINUTES.toMillis(10) // 10 minutes from now
        } else {
            now + TimeUnit.DAYS.toMillis(intervalDays.toLong())
        }

        val updated = card.copy(
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            repetitions = reps,
            lapses = lapses,
            state = state,
            dueTimestamp = dueTimestamp
        )

        return SrsResult(
            updatedCard = updated,
            nextIntervalDays = intervalDays,
            nextIntervalLabel = formatInterval(intervalDays)
        )
    }

    fun getPreviewLabel(card: FlashcardEntity, rating: SrsRating): String {
        val result = calculateNextReview(card, rating)
        return result.nextIntervalLabel
    }

    fun formatInterval(days: Int): String {
        return when {
            days <= 0 -> "< 10m"
            days == 1 -> "1 day"
            days < 30 -> "$days days"
            days < 365 -> "${(days / 30.0).roundToInt()} mo"
            else -> "${(days / 365.0).roundToInt()} yr"
        }
    }
}
