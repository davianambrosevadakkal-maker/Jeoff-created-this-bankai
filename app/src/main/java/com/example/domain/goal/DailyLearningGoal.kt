package com.example.domain.goal

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyLearningGoal(
    val targetWordsCount: Int = 20,           // User's target number of words to retain today
    val targetRetentionRate: Float = 0.85f,    // Target retention rate (e.g. 0.85 = 85%)
    val reviewedCount: Int = 0,               // Total card reviews completed today
    val retainedCount: Int = 0,               // Words successfully retained (rated Good [3] or Easy [4])
    val lapsedCount: Int = 0,                 // Words needing review (rated Again [1] or Hard [2])
    val streakDays: Int = 1,                  // Consecutive active learning days
    val againCount: Int = 0,                  // Rating = 1
    val hardCount: Int = 0,                   // Rating = 2
    val goodCount: Int = 0,                   // Rating = 3
    val easyCount: Int = 0                    // Rating = 4
) {
    /**
     * Actual retention rate achieved today:
     * (retainedCount / reviewedCount) * 100
     */
    val actualRetentionRate: Float
        get() = if (reviewedCount > 0) retainedCount.toFloat() / reviewedCount else 0f

    /**
     * Progress towards target word retention (0.0 to 1.0)
     */
    val progressFraction: Float
        get() = (retainedCount.toFloat() / targetWordsCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    /**
     * Accuracy difference vs target (positive = above target, negative = below)
     */
    val retentionDelta: Float
        get() = actualRetentionRate - targetRetentionRate

    /**
     * Percentage integer of actual retention
     */
    val retentionRatePercent: Int
        get() = (actualRetentionRate * 100).toInt()

    /**
     * Percentage integer of target retention
     */
    val targetRatePercent: Int
        get() = (targetRetentionRate * 100).toInt()

    /**
     * Remaining words needed to hit the daily goal
     */
    val remainingWords: Int
        get() = (targetWordsCount - retainedCount).coerceAtLeast(0)

    /**
     * Whether the target retention count has been reached and accuracy is on track
     */
    val isGoalAchieved: Boolean
        get() = retainedCount >= targetWordsCount && (reviewedCount == 0 || actualRetentionRate >= targetRetentionRate)

    /**
     * Status message for the learner
     */
    val motivationalFeedback: String
        get() = when {
            reviewedCount == 0 -> "Begin today's session to reach your $targetRatePercent% retention goal!"
            retainedCount >= targetWordsCount && actualRetentionRate >= targetRetentionRate ->
                "🎉 Bankai Target Achieved! Retention accuracy at $retentionRatePercent%!"
            retainedCount >= targetWordsCount ->
                "⚡ Word count target met! Review hard words to bring accuracy to $targetRatePercent%."
            actualRetentionRate >= targetRetentionRate ->
                "🔥 On track! $remainingWords words left to lock in today's retention target."
            else ->
                "💪 Keep going! Retain $remainingWords more words to boost retention above $targetRatePercent%."
        }

    companion object {
        fun getStartOfTodayMillis(): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
    }
}
