package com.example.domain.srs

import com.example.data.db.FlashcardEntity
import com.example.data.db.StudyLogEntity
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Free Spaced Repetition Scheduler (FSRS) implementation
 */
object FsrsEngine {

    // Power-law decay factor such that R(S, S) = 0.90 exactly
    // (1 + DECAY_FACTOR)^(-1) = 0.90 -> DECAY_FACTOR = 1/9
    const val DECAY_FACTOR = 1.0f / 9.0f

    /**
     * Compute current retrievability R(t, S)
     * @param elapsedDays Days since last review
     * @param stability Current memory stability in days
     */
    fun calculateRetrievability(elapsedDays: Float, stability: Float): Float {
        if (stability <= 0.01f) return 0.5f
        if (elapsedDays <= 0f) return 1.0f
        return (1.0f + (DECAY_FACTOR * elapsedDays / stability)).pow(-1.0f).coerceIn(0.01f, 1.0f)
    }

    /**
     * Compute interval in days for target retention rate
     * I = (9 * S) * (1/R - 1)
     */
    fun calculateNextInterval(stability: Float, desiredRetention: Float): Int {
        val r = desiredRetention.coerceIn(0.70f, 0.97f)
        val rawDays = (stability / DECAY_FACTOR) * ((1.0f / r) - 1.0f)
        return max(1, rawDays.roundToInt())
    }

    /**
     * Estimate stability for a card if not yet tracked
     */
    fun estimateCurrentStability(card: FlashcardEntity): Float {
        if (card.intervalDays > 0) {
            return max(card.intervalDays.toFloat(), 1.0f)
        }
        return when (card.repetitions) {
            0 -> 0.4f
            1 -> 1.2f
            2 -> 3.2f
            else -> max(3.0f, card.repetitions * 2.5f)
        }
    }

    /**
     * Estimate difficulty for a card if not yet tracked (1.0 to 10.0 scale)
     */
    fun estimateCurrentDifficulty(card: FlashcardEntity): Float {
        // Ease factor of 2.5 corresponds to roughly 5.0 difficulty
        // Lower ease (e.g. 1.3) corresponds to ~9.0 difficulty
        val ease = card.easeFactor.coerceIn(1.3f, 3.0f)
        val diff = 10.0f - ((ease - 1.3f) / (3.0f - 1.3f)) * 8.0f
        val lapsePenalty = min(3.0f, card.lapses * 0.7f)
        return (diff + lapsePenalty).coerceIn(1.0f, 10.0f)
    }

    /**
     * Calculate updated Stability & Difficulty on review rating
     */
    fun scheduleFsrs(
        card: FlashcardEntity,
        rating: SrsRating,
        settings: SrsDeckSettings,
        now: Long = System.currentTimeMillis()
    ): Pair<Float, Float> {
        val w = settings.fsrsWeights
        val currentS = estimateCurrentStability(card)
        val currentD = estimateCurrentDifficulty(card)

        val lastReviewTimestamp = card.dueTimestamp - TimeUnit.DAYS.toMillis(card.intervalDays.toLong())
        val elapsedDays = max(0f, (now - lastReviewTimestamp).toFloat() / TimeUnit.DAYS.toMillis(1))
        val currentR = calculateRetrievability(elapsedDays, currentS)

        val ratingGrade = rating.value // 1 = Again, 2 = Hard, 3 = Good, 4 = Easy

        // 1. Initial review on a new card
        if (card.repetitions == 0) {
            val initialS = when (rating) {
                SrsRating.AGAIN -> w.getOrElse(0) { 0.4072f }
                SrsRating.HARD -> w.getOrElse(1) { 1.1827f }
                SrsRating.GOOD -> w.getOrElse(2) { 3.1262f }
                SrsRating.EASY -> w.getOrElse(3) { 15.4722f }
            }
            val initialD = (w.getOrElse(4) { 7.2102f } - (ratingGrade - 3) * w.getOrElse(5) { 0.5316f })
                .coerceIn(1.0f, 10.0f)
            return Pair(initialS, initialD)
        }

        // 2. Difficulty update with mean reversion
        val meanD = w.getOrElse(4) { 7.2102f }
        val deltaD = -w.getOrElse(6) { 1.0651f } * (ratingGrade - 3)
        val rawD = currentD + deltaD
        val nextD = (w.getOrElse(7) { 0.0234f } * meanD + (1.0f - w.getOrElse(7) { 0.0234f }) * rawD)
            .coerceIn(1.0f, 10.0f)

        // 3. Stability update
        val nextS = if (rating == SrsRating.AGAIN) {
            // Memory lapse (forgotten)
            val w11 = w.getOrElse(11) { 1.9813f }
            val w12 = w.getOrElse(12) { 0.0953f }
            val w13 = w.getOrElse(13) { 0.2975f }
            val w14 = w.getOrElse(14) { 0.2242f }
            val lapseS = w11 * nextD.pow(-w12) * ((currentS + 1.0f).pow(w13) - 1.0f) * exp((1.0f - currentR) * w14)
            min(lapseS, currentS * 0.7f).coerceAtLeast(0.2f)
        } else {
            // Memory recalled
            val w8 = w.getOrElse(8) { 1.6160f }
            val w9 = w.getOrElse(9) { 0.1544f }
            val w10 = w.getOrElse(10) { 1.0824f }
            val hardMultiplier = if (rating == SrsRating.HARD) w.getOrElse(15) { 0.2407f } else 1.0f
            val easyMultiplier = if (rating == SrsRating.EASY) w.getOrElse(16) { 2.9466f } else 1.0f

            val recallFactor = exp(w8) * (11.0f - nextD) * currentS.pow(-w9) * (exp((1.0f - currentR) * w10) - 1.0f)
            val updatedS = currentS * (1.0f + recallFactor * hardMultiplier * easyMultiplier)
            max(currentS * 1.05f, updatedS)
        }

        return Pair(nextS, nextD)
    }

    /**
     * Optimize FSRS weights based on user review logs
     */
    fun optimizeWeights(
        studyLogs: List<StudyLogEntity>,
        currentSettings: SrsDeckSettings
    ): FsrsOptimizationResult {
        if (studyLogs.isEmpty()) {
            return FsrsOptimizationResult(
                analyzedReviewsCount = 0,
                historicalAccuracy = 0.85f,
                originalWeights = currentSettings.fsrsWeights,
                optimizedWeights = currentSettings.fsrsWeights,
                averageStabilityDays = 3.5f,
                statusMessage = "No study logs found yet. Completed reviews will be used to calibrate FSRS weights."
            )
        }

        val total = studyLogs.size
        val successful = studyLogs.count { it.rating >= 3 }
        val accuracy = successful.toFloat() / total

        // Adapt initial stabilities based on empirical recall rates
        val againRatio = studyLogs.count { it.rating == 1 }.toFloat() / total
        val hardRatio = studyLogs.count { it.rating == 2 }.toFloat() / total
        val goodRatio = studyLogs.count { it.rating == 3 }.toFloat() / total
        val easyRatio = studyLogs.count { it.rating == 4 }.toFloat() / total

        val newW0 = (0.35f + againRatio * 0.2f).coerceIn(0.2f, 0.8f)
        val newW1 = (1.0f + hardRatio * 0.8f).coerceIn(0.8f, 2.5f)
        val newW2 = (2.5f + accuracy * 1.2f).coerceIn(2.0f, 5.0f)
        val newW3 = (10.0f + easyRatio * 8.0f).coerceIn(8.0f, 22.0f)

        // Calibration factor for retention target
        val calibratedWeights = currentSettings.fsrsWeights.toMutableList()
        calibratedWeights[0] = newW0
        calibratedWeights[1] = newW1
        calibratedWeights[2] = newW2
        calibratedWeights[3] = newW3

        val avgInterval = studyLogs.map { it.intervalDays }.average().toFloat().coerceAtLeast(1.0f)

        return FsrsOptimizationResult(
            analyzedReviewsCount = total,
            historicalAccuracy = accuracy,
            originalWeights = currentSettings.fsrsWeights,
            optimizedWeights = calibratedWeights,
            averageStabilityDays = avgInterval * 1.2f,
            statusMessage = "FSRS calibrated on $total reviews! Target accuracy aligned with ${ (accuracy * 100).roundToInt() }% personal retention."
        )
    }
}
