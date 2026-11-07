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

object AdMobInterstitialHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
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
    }

    fun showInterstitial(context: Context, onAdClosed: () -> Unit) {
        val activity = context as? Activity
        if (interstitialAd != null && activity != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    AnalyticsManager.logAdInterstitialShown(context)
                    loadAd(context)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadAd(context)
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            loadAd(context)
            onAdClosed()
        }
    }
}
