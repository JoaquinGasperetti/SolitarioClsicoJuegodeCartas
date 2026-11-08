package com.HKemtrentainment.solitaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HKemtrentainment.solitaro.ads.AdMobSdkInitializer
import com.HKemtrentainment.solitaro.ui.GameScreen
import com.HKemtrentainment.solitaro.ui.theme.SolitarioClásicoJuegoDeCartasTheme
import com.HKemtrentainment.solitaro.viewmodel.SolitaireViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize optional ad services from the activity. The app no longer
        // depends on a custom Application subclass to start successfully.
        AdMobSdkInitializer.initialize(applicationContext)

        enableEdgeToEdge()
        setContent {
            SolitarioClásicoJuegoDeCartasTheme {
                val solitaireViewModel: SolitaireViewModel = viewModel()
                GameScreen(viewModel = solitaireViewModel)
            }
        }
    }
}
