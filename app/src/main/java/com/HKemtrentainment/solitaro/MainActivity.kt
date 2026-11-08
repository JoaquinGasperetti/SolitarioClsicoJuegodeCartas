package com.HKemtrentainment.solitaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HKemtrentainment.solitaro.ui.GameScreen
import com.HKemtrentainment.solitaro.ui.theme.SolitarioClásicoJuegoDeCartasTheme
import com.HKemtrentainment.solitaro.viewmodel.SolitaireViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolitarioClásicoJuegoDeCartasTheme {
                val viewModel: SolitaireViewModel = viewModel()
                // Run initialization inside LaunchedEffect(Unit) rather than on every recomposition
                // to prevent main-thread jank and protect Android Vitals ANR metrics.
                LaunchedEffect(Unit) {
                    viewModel.attachContext(applicationContext)
                }
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
