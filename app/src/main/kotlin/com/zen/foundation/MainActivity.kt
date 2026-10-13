package com.zen.foundation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import com.zen.foundation.ui.FoundationApp
import dagger.hilt.android.AndroidEntryPoint

/** The only activity. Screens are composables navigated with Navigation 3; see FoundationApp. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash until settings load, so the first frame already has the right theme.
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value is MainActivityUiState.Loading
        }
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            when (val state = uiState) {
                MainActivityUiState.Loading -> Unit

                is MainActivityUiState.Success -> {
                    val darkTheme = shouldUseDarkTheme(state.settings)
                    DisposableEffect(darkTheme) {
                        // System bar icons follow the app theme, not only the system setting.
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.auto(
                                Color.TRANSPARENT,
                                Color.TRANSPARENT
                            ) {
                                darkTheme
                            },
                            navigationBarStyle = SystemBarStyle.auto(
                                Color.TRANSPARENT,
                                Color.TRANSPARENT
                            ) {
                                darkTheme
                            }
                        )
                        onDispose {}
                    }
                    FoundationTheme(
                        darkTheme = darkTheme,
                        dynamicColor = state.settings.useDynamicColor
                    ) {
                        FoundationApp()
                    }
                }
            }
        }
    }
}

@Composable
private fun shouldUseDarkTheme(settings: UserSettings): Boolean = when (settings.themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
