package com.yeasin.myjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.data.repo.AppSettings
import com.yeasin.myjournal.ui.AppNav
import com.yeasin.myjournal.ui.theme.MyJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MyJournalApp).container
        setContent {
            val settings by container.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                AppSettings.THEME_DARK -> true
                AppSettings.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            // Keep status-bar icons readable when the app theme differs from the phone's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            MyJournalTheme(dark = dark) { AppNav() }
        }
    }
}
