package com.yeasin.myjournal.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.MainBottomBar
import com.yeasin.myjournal.ui.components.NoteSection
import com.yeasin.myjournal.ui.components.NotesPane
import com.yeasin.myjournal.ui.components.QuickCaptureSheet
import com.yeasin.myjournal.ui.components.SelectionActions
import com.yeasin.myjournal.ui.components.rememberSelectionMenus
import com.yeasin.myjournal.ui.components.SelectionSheets
import com.yeasin.myjournal.ui.components.SelectionTopBar
import com.yeasin.myjournal.ui.components.SwipeActions
import com.yeasin.myjournal.ui.components.Tabs
import com.yeasin.myjournal.ui.components.rememberNoteOps
import com.yeasin.myjournal.ui.components.rememberSelection
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    openNote: (String) -> Unit,
    openSearch: () -> Unit,
    openTimeline: () -> Unit,
    navigateTab: (String) -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
    val all by container.activeNotes.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ops = rememberNoteOps(snackbar)
    val scope = rememberCoroutineScope()
    val selection = rememberSelection()
    val menus = rememberSelectionMenus(selection, ops, all)
    val c = Journal.colors

    val pinned = all.filter { it.note.isPinned }
    val recent = all.filterNot { it.note.isPinned }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, onClose = { selection.clear() }) { SelectionActions(menus) }
            } else {
                TopAppBar(
                    title = { Text("My Journal", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = openTimeline) { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Timeline") }
                        IconButton(onClick = vm::toggleGrid) {
                            Icon(
                                if (vm.grid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                                contentDescription = if (vm.grid) "List view" else "Grid view",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = { if (selection.isEmpty()) MainBottomBar(Tabs.NOTES, navigateTab) },
        floatingActionButton = {
            if (selection.isEmpty()) {
                FloatingActionButton(
                    onClick = vm::openQuick,
                    containerColor = c.fab, contentColor = c.fabIcon,
                    shape = RoundedCornerShape(18.dp),
                ) { Icon(Icons.Outlined.Add, contentDescription = "Quick thought") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NotesPane(
            sections = listOf(NoteSection(if (pinned.isNotEmpty()) "PINNED" else null, pinned), NoteSection("RECENT", recent)),
            grid = vm.grid,
            selection = selection,
            onOpen = openNote,
            modifier = Modifier.fillMaxSize().padding(padding),
            swipe = SwipeActions(
                rightLabel = { if (it.note.isPinned) "Unpin" else "Pin" },
                onRight = { ops.pin(listOf(it.note.id), !it.note.isPinned) },
                leftLabel = { "Archive" },
                onLeft = { ops.archive(listOf(it.note.id)) },
            ),
            header = {
                item(span = StaggeredGridItemSpan.FullLine, key = "search") {
                    Surface(
                        onClick = openSearch,
                        shape = RoundedCornerShape(25.dp),
                        color = c.input,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = c.sub)
                            Spacer(Modifier.width(12.dp))
                            Text("Search your notes\u2026", color = c.sub, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine, key = "actions") {
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Quick thought", Icons.Outlined.Bolt, c.heroBg, c.heroText, c.heroBg, Modifier.weight(1f), vm::openQuick)
                        ActionButton("Checklist", Icons.Outlined.CheckBox, c.card, MaterialTheme.colorScheme.primary, c.border, Modifier.weight(1f)) {
                            vm.createNote(NoteType.CHECKLIST, openNote)
                        }
                    }
                }
            },
            empty = {
                EmptyState(
                    title = "Nothing here yet.",
                    body = "Capture a thought now.\nYou don\u2019t need to organize it.",
                    actionLabel = "+ Quick thought",
                    onAction = vm::openQuick,
                )
            },
        )
    }

    SelectionSheets(menus)

    if (vm.quickOpen) {
        QuickCaptureSheet(
            saved = vm.quickSaved,
            onTextChange = vm::quickType,
            onSave = {
                if (vm.closeQuick(keep = true)) scope.launch { snackbar.showSnackbar("Saved \u2014 organize it whenever you like") }
            },
            onDiscard = { vm.closeQuick(keep = false) },
            onOpenFull = { vm.quickToFull(openNote) },
        )
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, bg: Color, fg: Color, border: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
