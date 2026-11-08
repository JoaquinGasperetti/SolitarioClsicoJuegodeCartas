package com.HKemtrentainment.solitaro.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Initializes Google Mobile Ads once, off the main thread.
 *
 * The game must not depend on the ads SDK being available: if initialization fails,
 * gameplay continues and ad views are simply not displayed.
 */
object AdMobSdkInitializer {
    private const val TAG = "AdMobSdkInitializer"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var initializationStarted = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    fun initialize(context: Context) {
        synchronized(lock) {
            if (initializationStarted || _isInitialized.value) return
            initializationStarted = true
        }

        val appContext = context.applicationContext
        scope.launch {
            try {
                MobileAds.initialize(appContext) {
                    val callbacks = synchronized(lock) {
                        _isInitialized.value = true
                        pendingCallbacks.toList().also { pendingCallbacks.clear() }
                    }

                    // Ad loading and Android View operations are dispatched on the UI thread.
                    mainHandler.post {
                        callbacks.forEach { callback ->
                            runCatching { callback() }
                                .onFailure { Log.w(TAG, "An ad initialization callback failed", it) }
                        }
                    }
                }
            } catch (exception: Exception) {
                // Ads are optional. Do not let an SDK/device issue close the game.
                Log.w(TAG, "Google Mobile Ads could not be initialized; continuing without ads", exception)
                synchronized(lock) {
                    initializationStarted = false
                }
            }
        }
    }

    /** Runs [callback] on the main thread after the SDK is ready. */
    fun runWhenInitialized(callback: () -> Unit) {
        val alreadyInitialized = synchronized(lock) {
            if (_isInitialized.value) {
                true
            } else {
                pendingCallbacks.add(callback)
                false
            }
        }

        if (alreadyInitialized) {
            mainHandler.post {
                runCatching { callback() }
                    .onFailure { Log.w(TAG, "An ad initialization callback failed", it) }
            }
        }
    }
}
