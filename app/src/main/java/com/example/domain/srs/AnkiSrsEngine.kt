package com.example.domain.srs

import com.example.data.db.FlashcardEntity
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class SrsResult(
    val updatedCard: FlashcardEntity,
    val nextIntervalDays: Int,
    val nextIntervalLabel: String,
    val memoryState: CardMemoryState
)

object AnkiSrsEngine {

    private const val MIN_EASE_FACTOR = 1.30f
    private const val DEFAULT_EASE_FACTOR = 2.50f

    fun calculateNextReview(
        card: FlashcardEntity,
        rating: SrsRating,
        settings: SrsDeckSettings = SrsDeckSettings(),
        now: Long = System.currentTimeMillis()
    ): SrsResult {
        return when (settings.algorithm) {
            SrsAlgorithmType.SM2 -> scheduleSm2(card, rating, settings, now)
            SrsAlgorithmType.FSRS -> scheduleFsrs(card, rating, settings, now)
        }
    }

    private fun scheduleSm2(
        card: FlashcardEntity,
        rating: SrsRating,
        settings: SrsDeckSettings,
        now: Long
    ): SrsResult {
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
                intervalDays = if (settings.lapseNewIntervalPercent > 0f) {
                    max(1, (intervalDays * settings.lapseNewIntervalPercent).roundToInt())
                } else {
                    0 // Due immediately/today
                }
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.20f)
            }
            SrsRating.HARD -> {
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - 0.15f)
                intervalDays = if (intervalDays <= 1) {
                    settings.graduatingIntervalDays
                } else {
                    max(1, (intervalDays * settings.hardIntervalMultiplier).roundToInt())
                }
                state = 2 // Review
            }
            SrsRating.GOOD -> {
                intervalDays = if (intervalDays == 0) {
                    settings.graduatingIntervalDays
                } else if (intervalDays == 1) {
                    3
                } else {
                    max(2, (intervalDays * easeFactor).roundToInt())
                }
                state = 2 // Review
            }
            SrsRating.EASY -> {
                easeFactor += 0.15f
                intervalDays = if (intervalDays == 0) {
                    settings.easyIntervalDays
                } else {
                    max(settings.easyIntervalDays, (intervalDays * easeFactor * settings.easyBonusMultiplier).roundToInt())
                }
                state = 2 // Review
            }
        }

        // Apply retention factor modifier
        // Anki SM-2 default assumes 85-90% retention. If desired retention is higher, compress interval.
        val retentionScale = (0.90f / settings.desiredRetentionRate.coerceIn(0.70f, 0.97f))
        if (intervalDays > 1) {
            intervalDays = (intervalDays * retentionScale).roundToInt()
        }

        // Apply Fuzz factor (prevent interval clumping)
        if (settings.enableFuzz && intervalDays >= 3) {
            intervalDays = applyFuzz(intervalDays)
        }

        // Bound intervals
        if (intervalDays > 0) {
            intervalDays = intervalDays.coerceIn(settings.minimumIntervalDays, settings.maximumIntervalDays)
        }

        val dueTimestamp = if (intervalDays == 0) {
            val stepMinutes = if (card.repetitions == 0) {
                settings.learningStepsMinutes.firstOrNull() ?: 10
            } else {
                settings.relearningStepsMinutes.firstOrNull() ?: 10
            }
            now + TimeUnit.MINUTES.toMillis(stepMinutes.toLong())
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

        val memoryState = getCardMemoryState(updated, settings, now)

        return SrsResult(
            updatedCard = updated,
            nextIntervalDays = intervalDays,
            nextIntervalLabel = formatInterval(intervalDays),
            memoryState = memoryState
        )
    }

    private fun scheduleFsrs(
        card: FlashcardEntity,
        rating: SrsRating,
        settings: SrsDeckSettings,
        now: Long
    ): SrsResult {
        val (nextS, nextD) = FsrsEngine.scheduleFsrs(card, rating, settings, now)
        var reps = card.repetitions + 1
        var lapses = card.lapses
        var state = card.state

        var intervalDays = if (rating == SrsRating.AGAIN) {
            lapses += 1
            reps = 0
            state = 1
            0 // Again triggers learning step
        } else {
            state = 2
            FsrsEngine.calculateNextInterval(nextS, settings.desiredRetentionRate)
        }

        // Apply fuzz
        if (settings.enableFuzz && intervalDays >= 3) {
            intervalDays = applyFuzz(intervalDays)
        }

        // Bound intervals
        if (intervalDays > 0) {
            intervalDays = intervalDays.coerceIn(settings.minimumIntervalDays, settings.maximumIntervalDays)
        }

        val dueTimestamp = if (intervalDays == 0) {
            now + TimeUnit.MINUTES.toMillis(settings.relearningStepsMinutes.firstOrNull()?.toLong() ?: 10L)
        } else {
            now + TimeUnit.DAYS.toMillis(intervalDays.toLong())
        }

        // Map FSRS difficulty back to ease for compatibility
        val mappedEase = (3.0f - (nextD - 1.0f) * (1.7f / 9.0f)).coerceIn(1.3f, 3.0f)

        val updated = card.copy(
            intervalDays = intervalDays,
            easeFactor = mappedEase,
            repetitions = reps,
            lapses = lapses,
            state = state,
            dueTimestamp = dueTimestamp
        )

        val memoryState = CardMemoryState(
            stability = nextS,
            difficulty = nextD,
            retrievability = if (intervalDays == 0) 0.5f else 1.0f,
            algorithmUsed = SrsAlgorithmType.FSRS,
            intervalDays = intervalDays,
            lapses = lapses,
            repetitions = reps,
            easeFactor = mappedEase
        )

        return SrsResult(
            updatedCard = updated,
            nextIntervalDays = intervalDays,
            nextIntervalLabel = formatInterval(intervalDays),
            memoryState = memoryState
        )
    }

    private fun applyFuzz(interval: Int): Int {
        val fuzzRange = max(1, (interval * 0.05f).roundToInt())
        val delta = Random.nextInt(-fuzzRange, fuzzRange + 1)
        return max(1, interval + delta)
    }

    fun getPreviewLabel(
        card: FlashcardEntity,
        rating: SrsRating,
        settings: SrsDeckSettings = SrsDeckSettings()
    ): String {
        val result = calculateNextReview(card, rating, settings)
        return result.nextIntervalLabel
    }

    fun getCardMemoryState(
        card: FlashcardEntity,
        settings: SrsDeckSettings = SrsDeckSettings(),
        now: Long = System.currentTimeMillis()
    ): CardMemoryState {
        val lastReviewTimestamp = if (card.dueTimestamp > 0) {
            card.dueTimestamp - TimeUnit.DAYS.toMillis(card.intervalDays.toLong())
        } else {
            card.createdAt
        }
        val elapsedDays = max(0f, (now - lastReviewTimestamp).toFloat() / TimeUnit.DAYS.toMillis(1))

        val stability = FsrsEngine.estimateCurrentStability(card)
        val difficulty = FsrsEngine.estimateCurrentDifficulty(card)
        val retrievability = FsrsEngine.calculateRetrievability(elapsedDays, stability)

        return CardMemoryState(
            stability = stability,
            difficulty = difficulty,
            retrievability = retrievability,
            algorithmUsed = settings.algorithm,
            intervalDays = card.intervalDays,
            lapses = card.lapses,
            repetitions = card.repetitions,
            easeFactor = card.easeFactor
        )
    }

    fun formatInterval(days: Int): String {
        return when {
            days == 0 -> "< 10m"
            days == 1 -> "1d"
            days < 30 -> "${days}d"
            days < 365 -> {
                val months = days / 30f
                if (months < 10) String.format("%.1fm", months) else "${months.roundToInt()}m"
            }
            else -> {
                val years = days / 365f
                String.format("%.1fy", years)
            }
        }
    }
}
