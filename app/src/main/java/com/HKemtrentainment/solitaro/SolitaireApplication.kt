package com.HKemtrentainment.solitaro

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SolitaireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Google Mobile Ads SDK off the main thread to optimize Android Vitals cold start time
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@SolitaireApplication) { _ -> }
            } catch (_: Exception) {
                // Handled for test environments / missing Google Play Services
            }
        }
    }
}
