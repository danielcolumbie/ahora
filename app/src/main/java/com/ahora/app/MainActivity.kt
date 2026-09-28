package com.ahora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahora.app.data.ThemeMode
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.MainViewModelFactory
import com.ahora.app.ui.navigation.NavGraph
import com.ahora.app.ui.settings.SettingsViewModel
import com.ahora.app.ui.settings.SettingsViewModelFactory
import com.ahora.app.ui.theme.AhoraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AhoraApplication).container

        setContent {
            val themeMode by container.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            AhoraTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainViewModel: MainViewModel =
                        viewModel(factory = MainViewModelFactory(container))
                    val settingsViewModel: SettingsViewModel =
                        viewModel(factory = SettingsViewModelFactory(container))
                    NavGraph(
                        mainViewModel = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
