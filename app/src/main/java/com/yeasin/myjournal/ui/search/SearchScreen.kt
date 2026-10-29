package com.yeasin.myjournal.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.FolderPickerSheet
import com.yeasin.myjournal.ui.components.NoteSection
import com.yeasin.myjournal.ui.components.NotesPane
import com.yeasin.myjournal.ui.components.OptionsSheet
import com.yeasin.myjournal.ui.components.ProjectPickerSheet
import com.yeasin.myjournal.ui.components.TagPickerSheet
import com.yeasin.myjournal.ui.components.rememberSelection
import com.yeasin.myjournal.ui.theme.Journal
import com.yeasin.myjournal.util.TextFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onBack: () -> Unit, openNote: (String) -> Unit) {
    val container = appContainer()
    val vm: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(container) } })
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val catalog = LocalCatalog.current
    val focus = remember { FocusRequester() }
    val noSelection = rememberSelection()
    var sheet by remember { mutableStateOf<String?>(null) }
    val c = Journal.colors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Surface(
                    shape = RoundedCornerShape(24.dp), color = c.input,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().height(48.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                        BasicTextField(
                            value = query, onValueChange = vm::onQuery, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).focusRequester(focus),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("Search anything\u2026", style = MaterialTheme.typography.bodyLarge, color = c.sub)
                                inner()
                            },
                        )
                        if (query.isNotEmpty()) IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = c.sub) }
                    }
                }
                // Filter chips: can be combined, e.g. #AI + Research project + this month
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val tagLabel = when (filter.tagIds.size) {
                        0 -> "Tags"
                        1 -> "#" + (catalog.tagById[filter.tagIds.first()]?.name ?: "tag")
                        else -> "${filter.tagIds.size} tags"
                    }
                    FilterChip(filter.tagIds.isNotEmpty(), { sheet = "tag" }, label = { Text(tagLabel) })
                    FilterChip(filter.projectId != null, { sheet = "project" }, label = { Text(filter.projectId?.let { catalog.projectById[it]?.name } ?: "Project") })
                    FilterChip(filter.folderId != null, { sheet = "folder" }, label = { Text(filter.folderId?.let { catalog.folderById[it]?.name } ?: "Folder") })
                    FilterChip(filter.date != null, { sheet = "date" }, label = { Text(filter.date?.label ?: "Date") })
                    FilterChip(filter.pinnedOnly, { vm.update { it.copy(pinnedOnly = !it.pinnedOnly) } }, label = { Text("Pinned") })
                    if (filter.active) TextButton(onClick = { vm.setFilter(SearchFilter()) }) { Text("Clear all") }
                }
            }
        },
    ) { padding ->
        val list = results
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                list == null -> Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("BROWSE BY TAG", style = MaterialTheme.typography.labelSmall, color = c.sub, modifier = Modifier.padding(bottom = 10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        catalog.tags.forEach { t -> AssistChip(onClick = { vm.update { it.copy(tagIds = setOf(t.id)) } }, label = { Text("#" + t.name) }) }
                    }
                    Text(
                        "Search runs on your phone and works offline. It looks through titles, notes, checklists, tags, projects and folders \u2014 archived notes included.",
                        style = MaterialTheme.typography.bodyMedium, color = c.sub, modifier = Modifier.padding(top = 20.dp),
                    )
                }
                list.isEmpty() -> EmptyState(title = "No matching notes", body = "Try fewer words or remove a filter.")
                else -> NotesPane(
                    sections = listOf(NoteSection(if (list.size == 1) "1 NOTE" else "${list.size} NOTES", list)),
                    grid = false,
                    selection = noSelection,
                    selectable = false,
                    onOpen = openNote,
                    bottomPadding = 32.dp,
                    dateText = { n ->
                        (if (n.note.isArchived) "Archived \u00b7 " else "") +
                            TextFormat.whenLabel(n.note.updatedAt).let { if (it.startsWith("Today") || it.startsWith("Yesterday") || it.length > 7) it else it + " " + LocalDate.now().year }
                    },
                )
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    when (sheet) {
        "tag" -> TagPickerSheet(
            title = "Filter by tags",
            isOn = { it.id in filter.tagIds },
            onToggle = { tag, on -> vm.update { f -> f.copy(tagIds = if (on) f.tagIds + tag.id else f.tagIds - tag.id) } },
            onDismiss = { sheet = null },
        )
        "project" -> ProjectPickerSheet(
            title = "Filter by project", currentId = filter.projectId, noneLabel = "Any project", allowCreate = false,
            onPick = { id -> vm.update { it.copy(projectId = id) }; sheet = null }, onDismiss = { sheet = null },
        )
        "folder" -> FolderPickerSheet(
            title = "Filter by folder", currentId = filter.folderId, noneLabel = "Any folder", allowCreate = false,
            onPick = { id -> vm.update { it.copy(folderId = id) }; sheet = null }, onDismiss = { sheet = null },
        )
        "date" -> {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            fun start(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
            fun end(d: LocalDate) = start(d.plusDays(1)) - 1
            fun pick(f: DateFilter?) { vm.update { it.copy(date = f) }; sheet = null }
            OptionsSheet(
                title = "Filter by date",
                options = listOf(
                    "Any time" to { pick(null) },
                    "Today" to { pick(DateFilter("Today", start(today), end(today))) },
                    "Yesterday" to { pick(DateFilter("Yesterday", start(today.minusDays(1)), end(today.minusDays(1)))) },
                    "Last 7 days" to { pick(DateFilter("Last 7 days", start(today.minusDays(6)), end(today))) },
                    "Last 30 days" to { pick(DateFilter("Last 30 days", start(today.minusDays(29)), end(today))) },
                    "This month" to { pick(DateFilter("This month", start(today.withDayOfMonth(1)), end(today))) },
                    "Custom range\u2026" to { sheet = "range" },
                ),
                onDismiss = { sheet = null },
            )
        }
        "range" -> {
            val state = rememberDateRangePickerState()
            val zone = ZoneId.systemDefault()
            DatePickerDialog(
                onDismissRequest = { sheet = null },
                confirmButton = {
                    TextButton(
                        enabled = state.selectedStartDateMillis != null,
                        onClick = {
                            val s = Instant.ofEpochMilli(state.selectedStartDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate()
                            val e = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: s
                            val label = "${s.dayOfMonth}/${s.monthValue}\u2013${e.dayOfMonth}/${e.monthValue}"
                            vm.update {
                                it.copy(date = DateFilter(label, s.atStartOfDay(zone).toInstant().toEpochMilli(), e.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1))
                            }
                            sheet = null
                        },
                    ) { Text("Apply") }
                },
                dismissButton = { TextButton(onClick = { sheet = null }) { Text("Cancel") } },
            ) { DateRangePicker(state = state, modifier = Modifier.weight(1f)) }
        }
    }
}
