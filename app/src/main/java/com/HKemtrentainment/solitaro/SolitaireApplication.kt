package com.HKemtrentainment.solitaro

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SolitaireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Google Mobile Ads SDK
        try {
            MobileAds.initialize(this) { _ ->
                // AdMob initialization complete stub
            }
        } catch (_: Exception) {
            // Handled for test environments / missing Google Play Services
        }
    }
}
