package com.yeasin.myjournal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.data.repo.AppSettings
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val s by c.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Header("Appearance")
            Choice("Light", s.theme == AppSettings.THEME_LIGHT) { scope.launch { c.settingsRepo.setTheme(AppSettings.THEME_LIGHT) } }
            Choice("Dark", s.theme == AppSettings.THEME_DARK) { scope.launch { c.settingsRepo.setTheme(AppSettings.THEME_DARK) } }
            Choice("System default", s.theme == AppSettings.THEME_SYSTEM) { scope.launch { c.settingsRepo.setTheme(AppSettings.THEME_SYSTEM) } }
            Divider()
            Header("Default view")
            Choice("List", s.defaultView == AppSettings.VIEW_LIST) { scope.launch { c.settingsRepo.setDefaultView(AppSettings.VIEW_LIST) } }
            Choice("Grid", s.defaultView == AppSettings.VIEW_GRID) { scope.launch { c.settingsRepo.setDefaultView(AppSettings.VIEW_GRID) } }
            Divider()
            Header("Pin limit")
            Choice("5 notes", s.pinLimit == 5) { scope.launch { c.settingsRepo.setPinLimit(5) } }
            Choice("10 notes", s.pinLimit == 10) { scope.launch { c.settingsRepo.setPinLimit(10) } }
            Divider()
            Header("About")
            Text(
                "My Journal \u00b7 Phase 2\nNotes are stored on this phone only and work offline.\nBackup and export arrive in a later phase.",
                style = MaterialTheme.typography.bodyMedium, color = Journal.colors.sub,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(top = 8.dp), color = Journal.colors.border)

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
