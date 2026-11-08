package com.HKemtrentainment.solitaro.ui

import android.app.Activity
import android.content.Context
import com.HKemtrentainment.solitaro.analytics.AnalyticsManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages AdMob interstitial ads with a retention-friendly frequency cap (cooldown).
 * Showing interstitial ads when a user restarts a deal after only a few seconds triggers
 * early 7-day uninstalls—the #1 negative ranking signal in Google Play's algorithm.
 */
object AdMobInterstitialHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastAdShownTimestampMs: Long = 0L
    private const val MIN_COOLDOWN_MS = 120_000L // Minimum 2 minutes between interstitials
    private const val MIN_MOVES_BEFORE_AD = 12   // Never punish quick deal restarts with an ad
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoading = false
                    }
                }
            )
        } catch (_: Exception) {
            isLoading = false
        }
    }

    fun showInterstitialIfEligible(
        context: Context,
        movesInCurrentGame: Int = 20,
        onComplete: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        val cooldownElapsed = (now - lastAdShownTimestampMs) >= MIN_COOLDOWN_MS
        val enoughEngagement = movesInCurrentGame >= MIN_MOVES_BEFORE_AD
        val activity = context as? Activity

        if (interstitialAd != null && activity != null && cooldownElapsed && enoughEngagement) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    lastAdShownTimestampMs = System.currentTimeMillis()
                    interstitialAd = null
                    AnalyticsManager.logAdInterstitialShown(context)
                    loadAd(context)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadAd(context)
                    onComplete()
                }
            }
            interstitialAd?.show(activity)
        } else {
            loadAd(context)
            onComplete()
        }
    }

    // Backwards-compatible overload
    fun showInterstitial(context: Context, onAdClosed: () -> Unit) {
        showInterstitialIfEligible(context = context, movesInCurrentGame = 20, onComplete = onAdClosed)
    }
}
