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

    fun logGameStart(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("game_start", Bundle())
        } catch (_: Exception) {}
    }

    fun logGameWon(context: Context? = null, seconds: Long, moves: Int) {
        try {
            getAnalytics(context)?.logEvent("game_won", Bundle().apply {
                putLong("tiempo_segundos", seconds)
                putInt("movimientos", moves)
            })
        } catch (_: Exception) {}
    }

    fun logUndoUsed(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("undo_used", Bundle())
        } catch (_: Exception) {}
    }

    fun logAdInterstitialShown(context: Context? = null) {
        try {
            getAnalytics(context)?.logEvent("ad_interstitial_shown", Bundle())
        } catch (_: Exception) {}
    }
}
