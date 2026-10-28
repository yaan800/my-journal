package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.db.items
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.theme.Journal
import com.yeasin.myjournal.util.TextFormat

/**
 * One note. Shows: type label (Quick thought), title, project · folder, preview or checklist,
 * #tags and the date. Tap opens; long-press starts selection. [compact] is the grid version.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    item: NoteWithItems,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    showProject: Boolean = true,
    showFolder: Boolean = true,
    dateText: String? = null,
) {
    val n = item.note
    val c = Journal.colors
    val catalog = LocalCatalog.current
    val isQuick = n.captureType == CaptureType.QUICK
    val untitled = n.title.isBlank()
    val preview = TextFormat.preview(n.content)
    val lines = item.items.filter { it.text.isNotBlank() }
    val project = n.projectId?.let { catalog.projectById[it] }?.takeIf { showProject }
    val folder = catalog.folderPath(n.folderId).takeIf { showFolder && it.isNotEmpty() }

    Box(modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .semantics { this.selected = selected },
            shape = RoundedCornerShape(16.dp),
            color = if (selected) c.selBg else c.card,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else c.border),
        ) {
            Column(Modifier.padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 12.dp else 14.dp)) {
                if (isQuick) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = c.kQuick, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quick thought", style = MaterialTheme.typography.labelMedium, color = c.kQuick)
                    }
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
                            style = MaterialTheme.typography.titleMedium,
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

                if (project != null || folder != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                        if (project != null) {
                            Box(Modifier.size(8.dp).background(c.project(catalog.projectColorSlot(project.id)).second, CircleShape))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            listOfNotNull(project?.name, folder).joinToString(" \u00b7 "),
                            style = MaterialTheme.typography.bodySmall, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                val body = if (untitled && isQuick) preview.lineSequence().drop(1).joinToString("\n") else preview
                if (n.type == NoteType.TEXT && body.isNotBlank()) {
                    Text(
                        body, style = MaterialTheme.typography.bodyMedium, color = c.sub,
                        maxLines = if (compact) 5 else 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }

                if (n.type == NoteType.CHECKLIST && lines.isNotEmpty()) {
                    val shown = if (compact) 4 else 3
                    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        lines.take(shown).forEach { line ->
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
                        if (lines.size > shown) {
                            Text("+ ${lines.size - shown} more", style = MaterialTheme.typography.bodySmall, color = c.sub, modifier = Modifier.padding(start = 24.dp))
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item.tags.sortedBy { it.name.lowercase() }.take(if (compact) 2 else 4).forEach { TagChip(it.name) }
                }
                Text(
                    dateText ?: TextFormat.whenLabel(n.updatedAt),
                    style = MaterialTheme.typography.bodySmall, color = c.sub,
                    modifier = Modifier.fillMaxWidth().padding(top = if (item.tags.isEmpty()) 0.dp else 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
        if (selected) {
            Box(
                Modifier.offset(x = (-6).dp, y = (-8).dp).size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp)) }
        }
    }
}

@Composable
fun TagChip(name: String, modifier: Modifier = Modifier) {
    Text(
        "#$name",
        style = MaterialTheme.typography.labelMedium,
        color = Journal.colors.chipText,
        modifier = modifier
            .background(Journal.colors.chip, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
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
