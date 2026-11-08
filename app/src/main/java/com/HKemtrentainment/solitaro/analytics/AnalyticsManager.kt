package com.HKemtrentainment.solitaro.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {
    private fun getAnalytics(context: Context? = null): FirebaseAnalytics? {
        return try {
            Firebase.analytics
        } catch (_: Exception) {
            null
        }
    }

    fun logGameStart(
        context: Context? = null,
        drawCount: Int = 1,
        isDailyChallenge: Boolean = false,
        isWinningDeal: Boolean = false
    ) {
        try {
            getAnalytics(context)?.logEvent("game_start", Bundle().apply {
                putInt("draw_count", drawCount)
                putBoolean("daily_challenge", isDailyChallenge)
                putBoolean("winning_deal", isWinningDeal)
            })
        } catch (_: Exception) {}
    }

    fun logGameWon(
        context: Context? = null,
        seconds: Long,
        moves: Int,
        score: Int = 0,
        drawCount: Int = 1,
        isDailyChallenge: Boolean = false,
        streak: Int = 1
    ) {
        try {
            getAnalytics(context)?.logEvent("game_won", Bundle().apply {
                putLong("tiempo_segundos", seconds)
                putInt("movimientos", moves)
                putInt("puntuacion", score)
                putInt("draw_count", drawCount)
                putBoolean("daily_challenge", isDailyChallenge)
                putInt("racha_dias", streak)
            })
        } catch (_: Exception) {}
    }

    fun logUndoUsed(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("undo_used", Bundle())
        } catch (_: Exception) {}
    }

    fun logHintUsed(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("hint_used", Bundle())
        } catch (_: Exception) {}
    }

    fun logAutoCompleteUsed(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("autocomplete_used", Bundle())
        } catch (_: Exception) {}
    }

    fun logAchievementUnlocked(context: Context? = null, achievementId: String) {
        try {
            getAnalytics(context)?.logEvent("unlock_achievement", Bundle().apply {
                putString("achievement_id", achievementId)
            })
        } catch (_: Exception) {}
    }

    fun logAdInterstitialShown(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("ad_interstitial_shown", Bundle())
        } catch (_: Exception) {}
    }
}
