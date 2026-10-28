package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeasin.myjournal.ui.theme.Journal

/**
 * "What are you thinking?" — opens with the keyboard up, saves on the first keystroke
 * (via [onTextChange]) and asks for nothing else. Closing it keeps what was typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    saved: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onOpenFull: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onSave,            // swiping away keeps the thought
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding()) {
            Text("Quick thought", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it; onTextChange(it) },
                textStyle = TextStyle(fontSize = 19.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).focusRequester(focus),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Just start writing\u2026", style = TextStyle(fontSize = 19.sp, lineHeight = 28.sp), color = Journal.colors.sub)
                    }
                    inner()
                },
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenFull) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open as full note", tint = Journal.colors.sub)
                }
                Text(
                    if (saved) "Saved" else "Saves as you type",
                    style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDiscard) { Text("Discard") }
                Button(onClick = onSave) { Text("Save") }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
