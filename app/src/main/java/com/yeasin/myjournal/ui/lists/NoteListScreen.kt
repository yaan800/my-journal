package com.yeasin.myjournal.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.repo.AppSettings
import com.yeasin.myjournal.ui.LocalCatalog
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.ConfirmDialog
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.NoteSection
import com.yeasin.myjournal.ui.components.NotesPane
import com.yeasin.myjournal.ui.components.SelectionActions
import com.yeasin.myjournal.ui.components.SelectionSheets
import com.yeasin.myjournal.ui.components.SelectionTopBar
import com.yeasin.myjournal.ui.components.SwipeActions
import com.yeasin.myjournal.ui.components.rememberNoteOps
import com.yeasin.myjournal.ui.components.rememberSelection
import com.yeasin.myjournal.ui.components.rememberSelectionMenus
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ListKind { PROJECT, TAG, FOLDER, ARCHIVE, TRASH }

/**
 * One screen for: a project, a tag, a folder (with its sub-folders), Archive and Trash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    kind: ListKind,
    id: String?,
    onBack: () -> Unit,
    openNote: (String) -> Unit,
    openFolder: (String) -> Unit = {},
) {
    val c = appContainer()
    val catalog = LocalCatalog.current
    val colors = Journal.colors
    val snackbar = remember { SnackbarHostState() }
    val ops = rememberNoteOps(snackbar)
    val scope = rememberCoroutineScope()
    val selection = rememberSelection()
    var grid by remember { mutableStateOf(c.settings.value.defaultView == AppSettings.VIEW_GRID) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }

    val active by c.activeNotes.collectAsStateWithLifecycle()
    val archived by remember { c.notes.observeArchived() }.collectAsStateWithLifecycle(emptyList())
    val trash by remember { c.notes.observeTrash() }.collectAsStateWithLifecycle(emptyList())

    val notes = when (kind) {
        ListKind.PROJECT -> active.filter { it.note.projectId == id }
        ListKind.TAG -> active.filter { n -> n.tags.any { it.id == id } }
        ListKind.FOLDER -> active.filter { it.note.folderId == id }
        ListKind.ARCHIVE -> archived
        ListKind.TRASH -> trash
    }
    val menus = rememberSelectionMenus(selection, ops, notes)

    val title = when (kind) {
        ListKind.PROJECT -> id?.let { catalog.projectById[it]?.name } ?: "Project"
        ListKind.TAG -> "#" + (id?.let { catalog.tagById[it]?.name } ?: "tag")
        ListKind.FOLDER -> id?.let { catalog.folderById[it]?.name } ?: "Folder"
        ListKind.ARCHIVE -> "Archive"
        ListKind.TRASH -> "Trash"
    }

    val sections = when (kind) {
        ListKind.PROJECT -> {
            val pinned = notes.filter { it.note.isPinned }
            val brain = notes.filter { !it.note.isPinned && it.note.captureType == CaptureType.BRAINSTORM }
            val tasks = notes.filter { !it.note.isPinned && it.note.type == NoteType.CHECKLIST && it !in brain }
            val rest = notes.filter { it !in pinned && it !in brain && it !in tasks }
            listOf(NoteSection("PINNED", pinned), NoteSection("BRAINSTORMS", brain), NoteSection("TASKS", tasks), NoteSection("NOTES", rest))
        }
        ListKind.TAG, ListKind.FOLDER -> {
            val pinned = notes.filter { it.note.isPinned }
            listOf(NoteSection(if (pinned.isNotEmpty()) "PINNED" else null, pinned), NoteSection(if (pinned.isNotEmpty()) "NOTES" else null, notes - pinned.toSet()))
        }
        ListKind.ARCHIVE -> listOf(NoteSection(null, notes))
        ListKind.TRASH -> listOf(NoteSection("NOTES IN TRASH ARE REMOVED AFTER 30 DAYS", notes))
    }

    val canCreate = kind == ListKind.PROJECT || kind == ListKind.TAG || kind == ListKind.FOLDER
    val newNote = {
        scope.launch {
            val newId = withContext(Dispatchers.IO) {
                c.notes.create(
                    projectId = id.takeIf { kind == ListKind.PROJECT },
                    folderId = id.takeIf { kind == ListKind.FOLDER },
                    tagId = id.takeIf { kind == ListKind.TAG },
                )
            }
            openNote(newId)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, onClose = { selection.clear() }) {
                    if (kind == ListKind.TRASH) {
                        val ids = selection.toList()
                        IconButton(onClick = { ops.restore(ids); selection.clear() }) { Icon(Icons.Outlined.RestoreFromTrash, contentDescription = "Restore") }
                        IconButton(onClick = { ops.deleteForever(ids); selection.clear() }) { Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete forever") }
                    } else {
                        SelectionActions(menus)
                    }
                }
            } else {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                    actions = {
                        if (kind == ListKind.TRASH) {
                            if (notes.isNotEmpty()) TextButton(onClick = { confirm = "empty" }) { Text("Empty trash") }
                        } else {
                            IconButton(onClick = { grid = !grid }) {
                                Icon(if (grid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView, contentDescription = if (grid) "List view" else "Grid view")
                            }
                        }
                        if (canCreate) {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete " + kind.name.lowercase()) },
                                    onClick = { menuOpen = false; confirm = "delete" },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        floatingActionButton = {
            if (canCreate && selection.isEmpty()) {
                FloatingActionButton(onClick = { newNote() }, containerColor = colors.fab, contentColor = colors.fabIcon, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "New note here")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val subfolders = if (kind == ListKind.FOLDER) catalog.folders.filter { it.parentId == id } else emptyList()
        NotesPane(
            sections = sections,
            grid = grid && kind != ListKind.TRASH,
            selection = selection,
            onOpen = openNote,
            modifier = Modifier.fillMaxSize().padding(padding),
            selectOnTap = kind == ListKind.TRASH,
            showProject = kind != ListKind.PROJECT,
            showFolder = kind != ListKind.FOLDER,
            swipe = when (kind) {
                ListKind.TRASH -> null
                ListKind.ARCHIVE -> SwipeActions(leftLabel = { "Unarchive" }, onLeft = { ops.unarchive(listOf(it.note.id)) })
                else -> SwipeActions(
                    rightLabel = { if (it.note.isPinned) "Unpin" else "Pin" },
                    onRight = { ops.pin(listOf(it.note.id), !it.note.isPinned) },
                    leftLabel = { "Archive" },
                    onLeft = { ops.archive(listOf(it.note.id)) },
                )
            },
            header = {
                subfolders.forEach { f ->
                    item(span = StaggeredGridItemSpan.FullLine, key = "sub-" + f.id) {
                        Row(
                            Modifier.fillMaxWidth().clickable { openFolder(f.id) }.padding(horizontal = 8.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.sub)
                            Spacer(Modifier.width(14.dp))
                            Text(f.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(active.count { it.note.folderId == f.id }.toString(), color = colors.sub, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            empty = {
                when (kind) {
                    ListKind.ARCHIVE -> EmptyState("No archived notes", "Swipe a note left to archive it.\nArchived notes still show up in search.")
                    ListKind.TRASH -> EmptyState("Trash is empty", "Deleted notes wait here for 30 days, so you can restore them.")
                    ListKind.PROJECT -> EmptyState("No notes in this project yet", "Add a note, or add existing notes by selecting them on any screen.", "New note in project") { newNote() }
                    ListKind.TAG -> EmptyState("No notes with this tag", "Write one \u2014 the tag is added for you.", "New note with this tag") { newNote() }
                    ListKind.FOLDER -> if (subfolders.isEmpty()) EmptyState("This folder is empty", "Add a note here, or move notes in by selecting them.", "New note here") { newNote() }
                }
            },
        )
    }

    SelectionSheets(menus)

    when (confirm) {
        "empty" -> ConfirmDialog(
            title = "Empty trash?",
            body = "${notes.size} notes will be deleted forever. This can\u2019t be undone.",
            confirm = "Delete forever",
            onConfirm = { ops.emptyTrash() },
            onDismiss = { confirm = null },
        )
        "delete" -> ConfirmDialog(
            title = "Delete this ${kind.name.lowercase()}?",
            body = "Your notes are kept. They just won\u2019t be in this ${kind.name.lowercase()} any more.",
            confirm = "Delete",
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        when (kind) {
                            ListKind.PROJECT -> id?.let { c.catalogRepo.deleteProject(it) }
                            ListKind.TAG -> id?.let { c.catalogRepo.deleteTag(it) }
                            ListKind.FOLDER -> id?.let { catalog.folderById[it] }?.let { c.catalogRepo.deleteFolder(it) }
                            else -> Unit
                        }
                    }
                    onBack()
                }
            },
            onDismiss = { confirm = null },
        )
    }
}
