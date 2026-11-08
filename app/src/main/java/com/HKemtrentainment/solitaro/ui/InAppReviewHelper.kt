package com.HKemtrentainment.solitaro.ui

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Triggers the official Google Play In-App Review flow at high-satisfaction moments
 * (e.g., after the player's 2nd or 5th victory, or upon reaching a 3-day streak)
 * to boost rating velocity and maintain a >4.5★ average on Google Play.
 */
object InAppReviewHelper {

    fun requestReviewIfAppropriate(context: Context, totalWins: Int, currentStreak: Int) {
        val shouldTrigger = (totalWins == 2 || totalWins == 6 || totalWins == 15 || currentStreak == 3)
        if (!shouldTrigger) return

        val activity = context as? Activity ?: return
        try {
            val manager = ReviewManagerFactory.create(context.applicationContext)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(activity, reviewInfo)
                }
            }
        } catch (_: Exception) {
            // Safe fallback if Google Play Store is unavailable on emulator/device
        }
    }
}
