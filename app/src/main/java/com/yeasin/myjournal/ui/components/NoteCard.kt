package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.db.items
import com.yeasin.myjournal.ui.theme.Journal
import com.yeasin.myjournal.util.TextFormat

/**
 * One note on the Notes screen. [compact] is the two-column grid version.
 * Type labels are subtle text + a line icon, never emoji.
 */
@Composable
fun NoteCard(item: NoteWithItems, compact: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val n = item.note
    val c = Journal.colors
    val isQuick = n.captureType == CaptureType.QUICK
    val untitled = n.title.isBlank()
    val preview = TextFormat.preview(n.content)
    val lines = item.items.filter { it.text.isNotBlank() }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.border),
        shadowElevation = if (c.isDark) 0.dp else 0.5.dp,
    ) {
        Column(Modifier.padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 12.dp else 14.dp)) {
            if (isQuick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = c.kQuick, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Quick thought", style = MaterialTheme.typography.labelMedium, color = c.kQuick)
                }
                Spacer(Modifier.padding(top = 6.dp))
            }

            Row(verticalAlignment = Alignment.Top) {
                val headline = when {
                    !untitled -> n.title.trim()
                    isQuick -> preview.lineSequence().firstOrNull().orEmpty()
                    else -> ""
                }
                if (headline.isNotEmpty()) {
                    Text(
                        headline,
                        style = if (compact) MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize) else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (n.isPinned) {
                    Icon(
                        Icons.Filled.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp).size(15.dp).semantics { contentDescription = "Pinned" },
                    )
                }
            }

            val body = if (untitled && isQuick) preview.lineSequence().drop(1).joinToString("\n") else preview
            if (n.type == NoteType.TEXT && body.isNotBlank()) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.sub,
                    maxLines = if (compact) 5 else 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (n.type == NoteType.CHECKLIST && lines.isNotEmpty()) {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    lines.take(if (compact) 4 else 3).forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (line.done) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = if (line.done) "Done" else "Not done",
                                tint = c.sub, modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                line.text, style = MaterialTheme.typography.bodyMedium, color = c.sub,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textDecoration = if (line.done) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                    if (lines.size > (if (compact) 4 else 3)) {
                        Text("+ ${lines.size - (if (compact) 4 else 3)} more", style = MaterialTheme.typography.bodySmall, color = c.sub, modifier = Modifier.padding(start = 24.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Spacer(Modifier.weight(1f))
                Text(TextFormat.whenLabel(n.updatedAt), style = MaterialTheme.typography.bodySmall, color = c.sub)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Journal.colors.sub,
        modifier = modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp),
    )
}
