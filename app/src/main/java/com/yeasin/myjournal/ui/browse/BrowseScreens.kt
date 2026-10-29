package com.yeasin.myjournal.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.CreateDialog
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.MainBottomBar
import com.yeasin.myjournal.ui.components.Tabs
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A row with a coloured icon tile, a name, a secondary line and a chevron. */
@Composable
private fun BrowseRow(
    icon: ImageVector,
    tileBg: Color,
    tileFg: Color,
    name: String,
    sub: String?,
    trailing: String? = null,
    indent: Int = 0,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick)
            .padding(start = (20 + indent * 24).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(tileBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tileFg, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = Journal.colors.sub)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Journal.colors.sub)
    }
}

private fun count(n: Int) = if (n == 1) "1 note" else "$n notes"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabScaffold(
    title: String,
    tab: String,
    navigateTab: (String) -> Unit,
    onAdd: (() -> Unit)?,
    addLabel: String,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                actions = { if (onAdd != null) IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, contentDescription = addLabel) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { MainBottomBar(tab, navigateTab) },
        content = content,
    )
}

// ---------------- Projects ----------------

@Composable
fun ProjectsScreen(openProject: (String) -> Unit, navigateTab: (String) -> Unit) {
    val c = appContainer()
    val catalog = LocalCatalog.current
    val notes by c.activeNotes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    val counts = notes.groupingBy { it.note.projectId }.eachCount()

    TabScaffold("Projects", Tabs.PROJECTS, navigateTab, { creating = true }, "New project") { padding ->
        if (catalog.projects.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState("Projects help group related thinking.", "You don\u2019t need one for every note.", "Create project") { creating = true }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(catalog.projects.sortedByDescending { counts[it.id] ?: 0 }, key = { it.id }) { p ->
                    val (bg, fg) = Journal.colors.project(catalog.projectColorSlot(p.id))
                    BrowseRow(Icons.Outlined.Layers, bg, fg, p.name, count(counts[p.id] ?: 0)) { openProject(p.id) }
                }
                item {
                    Text(
                        "Projects help group related thinking. You don\u2019t need one for every note.",
                        style = MaterialTheme.typography.bodySmall, color = Journal.colors.sub,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
    if (creating) {
        CreateDialog("New project", "e.g. Cement Sector Report", onDismiss = { creating = false }, keywords = true, onCreate = { name, kw ->
            creating = false
            scope.launch { withContext(Dispatchers.IO) { c.catalogRepo.createProject(name, kw.orEmpty()) } }
        })
    }
}

// ---------------- Tags ----------------

@Composable
fun TagsScreen(openTag: (String) -> Unit, navigateTab: (String) -> Unit) {
    val c = appContainer()
    val catalog = LocalCatalog.current
    val notes by c.activeNotes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    val counts = remember(notes) { notes.flatMap { n -> n.tags.map { it.id } }.groupingBy { it }.eachCount() }

    TabScaffold("Tags", Tabs.TAGS, navigateTab, { creating = true }, "New tag") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(catalog.tags.sortedWith(compareByDescending<com.yeasin.myjournal.data.db.TagEntity> { counts[it.id] ?: 0 }.thenBy { it.name.lowercase() }), key = { it.id }) { t ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { openTag(t.id) }.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, tint = Journal.colors.sub, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(t.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text((counts[t.id] ?: 0).toString(), style = MaterialTheme.typography.bodyMedium, color = Journal.colors.sub)
                }
            }
            item {
                TextButton(onClick = { creating = true }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New tag")
                }
            }
        }
    }
    if (creating) {
        CreateDialog("New tag", "e.g. ENVOYTEX", onDismiss = { creating = false }, onCreate = { name, _ ->
            creating = false
            scope.launch { withContext(Dispatchers.IO) { c.catalogRepo.tagId(name) } }
        })
    }
}

// ---------------- More ----------------

@Composable
fun MoreScreen(
    navigateTab: (String) -> Unit,
    openFolders: () -> Unit,
    openTimeline: () -> Unit,
    openArchive: () -> Unit,
    openTrash: () -> Unit,
    openSettings: () -> Unit,
) {
    val c = appContainer()
    val catalog = LocalCatalog.current
    val archived by remember { c.notes.observeArchived() }.collectAsStateWithLifecycle(emptyList())
    val trash by remember { c.notes.observeTrash() }.collectAsStateWithLifecycle(emptyList())
    val tile = Journal.colors.chip
    val ink = Journal.colors.chipText

    TabScaffold("More", Tabs.MORE, navigateTab, null, "") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BrowseRow(Icons.Outlined.Folder, tile, ink, "Folders", null, catalog.folders.size.takeIf { it > 0 }?.toString(), onClick = openFolders)
            BrowseRow(Icons.Outlined.CalendarMonth, tile, ink, "Timeline", "Notes by the day you wrote them", onClick = openTimeline)
            HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 20.dp), color = Journal.colors.border)
            BrowseRow(Icons.Outlined.Archive, tile, ink, "Archive", null, archived.size.takeIf { it > 0 }?.toString(), onClick = openArchive)
            BrowseRow(Icons.Outlined.Delete, tile, ink, "Trash", "Kept for 30 days", trash.size.takeIf { it > 0 }?.toString(), onClick = openTrash)
            HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 20.dp), color = Journal.colors.border)
            BrowseRow(Icons.Outlined.Settings, tile, ink, "Settings", null, onClick = openSettings)
        }
    }
}

// ---------------- Folders ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(onBack: () -> Unit, openFolder: (String) -> Unit) {
    val c = appContainer()
    val catalog = LocalCatalog.current
    val notes by c.activeNotes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, contentDescription = "New folder") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (catalog.folders.isEmpty()) {
            Box(Modifier.padding(padding)) { EmptyState("No folders yet", "Folders are optional. A note can live without one.", "New folder") { creating = true } }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.Top) {
                items(catalog.folderTree(), key = { it.first.id }) { (f, depth) ->
                    val total = catalog.folderAndChildren(f.id).let { ids -> notes.count { it.note.folderId in ids } }
                    BrowseRow(Icons.Outlined.Folder, Journal.colors.chip, Journal.colors.chipText, f.name, count(total), indent = depth) { openFolder(f.id) }
                }
            }
        }
    }
    if (creating) {
        CreateDialog(
            "New folder", "e.g. Books",
            onDismiss = { creating = false },
            parents = catalog.folders.filter { it.parentId == null },
            onCreate = { name, parent ->
                creating = false
                scope.launch { withContext(Dispatchers.IO) { c.catalogRepo.createFolder(name, parent) } }
            },
        )
    }
}
