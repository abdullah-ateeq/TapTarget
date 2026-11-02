package com.example.taptarget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taptarget.ui.navigation.TapTargetNavGraph
import com.example.taptarget.ui.theme.TapTargetTheme
import com.example.taptarget.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()

            TapTargetTheme(appTheme = settings.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TapTargetNavGraph(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}
