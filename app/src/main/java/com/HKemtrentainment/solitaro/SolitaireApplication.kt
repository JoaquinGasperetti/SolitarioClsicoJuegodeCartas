package com.HKemtrentainment.solitaro

import android.app.Application
import com.HKemtrentainment.solitaro.ads.AdMobSdkInitializer

class SolitaireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize ads asynchronously; the game remains usable if the SDK is unavailable.
        AdMobSdkInitializer.initialize(this)
    }
}
