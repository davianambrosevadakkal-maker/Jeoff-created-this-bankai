package com.example.domain.srs

import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * SRS Rating grades (1 = Again, 2 = Hard, 3 = Good, 4 = Easy)
 */
enum class SrsRating(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}

/**
 * Spaced Repetition Scheduling Algorithm Type
 */
enum class SrsAlgorithmType(val title: String, val shortName: String, val description: String) {
    SM2(
        title = "SuperMemo 2 (SM-2)",
        shortName = "SM-2",
        description = "Classic multiplier-based interval expansion with ease factors."
    ),
    FSRS(
        title = "Free Spaced Repetition Scheduler (FSRS)",
        shortName = "FSRS",
        description = "Modern machine-learning inspired model using Memory Stability & Difficulty."
    )
}

/**
 * Global and Deck-level SRS Scheduling Parameters
 */
data class SrsDeckSettings(
    val algorithm: SrsAlgorithmType = SrsAlgorithmType.FSRS,
    val desiredRetentionRate: Float = 0.90f, // 90% default target retention
    val dailyNewLimit: Int = 20,
    val dailyReviewLimit: Int = 100,
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    val relearningStepsMinutes: List<Int> = listOf(10),
    val graduatingIntervalDays: Int = 1,
    val easyIntervalDays: Int = 4,
    val startingEase: Float = 2.50f,
    val hardIntervalMultiplier: Float = 1.20f,
    val easyBonusMultiplier: Float = 1.30f,
    val minimumIntervalDays: Int = 1,
    val maximumIntervalDays: Int = 36500, // 100 years
    val lapseNewIntervalPercent: Float = 0.0f, // 0 = complete reset, 0.20 = 20% preserved
    val enableFuzz: Boolean = true,
    val timeboxMinutes: Int = 15,
    // FSRS 17-parameter weights (standard default weights optimized from millions of reviews)
    val fsrsWeights: List<Float> = listOf(
        0.4072f, 1.1827f, 3.1262f, 15.4722f, // w0-w3: initial stabilities for Again, Hard, Good, Easy
        7.2102f, 0.5316f, 1.0651f, 0.0234f,  // w4-w7: difficulty parameters & mean reversion
        1.6160f, 0.1544f, 1.0824f,           // w8-w10: recall stability parameters
        1.9813f, 0.0953f, 0.2975f, 0.2242f,  // w11-w14: forget stability parameters
        0.2407f, 2.9466f                     // w15-w16: hard & easy stability multipliers
    )
)

/**
 * Current Memory State for a card
 */
data class CardMemoryState(
    val stability: Float,        // Days until retrievability drops to 90%
    val difficulty: Float,       // Card inherent difficulty (1.0 to 10.0)
    val retrievability: Float,   // Estimated recall probability right now (0.0 to 1.0)
    val algorithmUsed: SrsAlgorithmType,
    val intervalDays: Int,
    val lapses: Int,
    val repetitions: Int,
    val easeFactor: Float
) {
    val retrievabilityPercent: Int
        get() = (retrievability * 100).coerceIn(0f, 100f).roundToInt()

    val maturityCategory: String
        get() = when {
            repetitions == 0 -> "New"
            intervalDays <= 1 -> "Learning"
            intervalDays < 21 -> "Young"
            else -> "Mature"
        }
}

/**
 * Result of FSRS historical weight optimization
 */
data class FsrsOptimizationResult(
    val analyzedReviewsCount: Int,
    val historicalAccuracy: Float,
    val originalWeights: List<Float>,
    val optimizedWeights: List<Float>,
    val averageStabilityDays: Float,
    val statusMessage: String
)
