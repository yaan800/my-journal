package com.yeasin.myjournal.ui.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.theme.Journal
import com.yeasin.myjournal.util.TextFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Notes grouped by the day they were written — for when you remember when, not what. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(onBack: () -> Unit, openNote: (String) -> Unit) {
    val c = appContainer()
    val notes by remember { c.notes.observeLive() }.collectAsStateWithLifecycle(emptyList())
    val zone = ZoneId.systemDefault()
    var jumpTo by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now(zone)
    val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    val dayFmtYear = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

    val days = remember(notes, jumpTo) {
        val limit = jumpTo
        notes.filter { n -> limit == null || !Instant.ofEpochMilli(n.note.createdAt).atZone(zone).toLocalDate().isAfter(limit) }
            .groupBy { Instant.ofEpochMilli(it.note.createdAt).atZone(zone).toLocalDate() }
            .toList()
            .take(60)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (jumpTo != null) TextButton(onClick = { jumpTo = null }) { Text("Today") }
                    IconButton(onClick = { picking = true }) { Icon(Icons.Outlined.EditCalendar, contentDescription = "Jump to a date") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (days.isEmpty()) {
            androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                EmptyState("Nothing written yet", "Notes appear here by the day you wrote them.")
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            days.forEach { (date, list) ->
                item(key = "d-$date") {
                    val label = when (date) {
                        today -> "Today \u00b7 " + date.format(dayFmt)
                        today.minusDays(1) -> "Yesterday \u00b7 " + date.format(dayFmt)
                        else -> date.format(if (date.year == today.year) dayFmt else dayFmtYear)
                    }
                    Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(if (list.size == 1) "1 note" else "${list.size} notes", style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub)
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Journal.colors.border)
                }
                items(list, key = { it.note.id }) { n ->
                    val title = n.note.title.ifBlank { TextFormat.preview(n.note.content).lineSequence().firstOrNull().orEmpty() }
                        .ifBlank { n.itemsUnsorted.firstOrNull()?.text ?: "Empty note" }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 50.dp).clickable { openNote(n.note.id) }.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(TextFormat.clockLabel(n.note.createdAt), style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub, modifier = Modifier.width(72.dp))
                        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        val kind = when {
                            n.note.captureType == CaptureType.QUICK -> "Quick"
                            n.note.isArchived -> "Archived"
                            else -> ""
                        }
                        if (kind.isNotEmpty()) Text(kind, style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub)
                    }
                }
            }
        }
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (jumpTo ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { jumpTo = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
