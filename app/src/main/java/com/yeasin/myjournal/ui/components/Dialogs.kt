package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.data.db.FolderEntity
import com.yeasin.myjournal.ui.theme.Journal

/** "New tag", "New project" (with optional keywords) and "New folder" (with a parent). */
@Composable
fun CreateDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, extra: String?) -> Unit,
    keywords: Boolean = false,
    parents: List<FolderEntity>? = null,
) {
    var name by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }, placeholder = { Text(placeholder) }, modifier = Modifier.fillMaxWidth())
                if (keywords) {
                    OutlinedTextField(
                        extra, { extra = it }, singleLine = true,
                        label = { Text("Keywords (optional)") }, placeholder = { Text("e.g. cement, clinker") },
                        supportingText = { Text("Help the app spot related notes later.") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (parents != null) {
                    Text("Inside", style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub, modifier = Modifier.padding(top = 12.dp))
                    (listOf<FolderEntity?>(null) + parents).forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { parent = f?.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = parent == f?.id, onClick = null, modifier = Modifier.padding(8.dp))
                            Text(f?.name ?: "Top level")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim(), if (parents != null) parent else extra.trim()) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
