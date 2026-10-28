package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.ui.theme.Journal

@Composable
fun EmptyState(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), textAlign = TextAlign.Center)
        Text(
            body, style = MaterialTheme.typography.bodyMedium, color = Journal.colors.sub,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        if (actionLabel != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) { Text(actionLabel) }
        }
    }
}
