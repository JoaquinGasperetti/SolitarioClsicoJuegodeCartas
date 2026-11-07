package com.HKemtrentainment.solitaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                viewModel.initializeAdMob(applicationContext)
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
