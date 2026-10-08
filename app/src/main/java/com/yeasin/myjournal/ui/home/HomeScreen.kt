package com.yeasin.myjournal.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.NoteCard
import com.yeasin.myjournal.ui.components.QuickCaptureSheet
import com.yeasin.myjournal.ui.components.SectionLabel
import com.yeasin.myjournal.ui.theme.Journal
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    openNote: (String) -> Unit,
    openSearch: () -> Unit,
    deletedId: StateFlow<String?>,
    clearDeleted: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
    val lists by vm.lists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val c = Journal.colors

    // "Note deleted · Undo" after deleting from the editor
    val deleted by deletedId.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) {
        val id = deleted ?: return@LaunchedEffect
        clearDeleted()
        val r = snackbar.showSnackbar("Note deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (r == SnackbarResult.ActionPerformed) vm.restore(id)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Journal", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = vm::toggleGrid) {
                        Icon(
                            if (vm.grid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                            contentDescription = if (vm.grid) "List view" else "Grid view",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = vm::openQuick,
                containerColor = c.fab, contentColor = c.fabIcon,
                shape = RoundedCornerShape(18.dp),
            ) { Icon(Icons.Outlined.Add, contentDescription = "Quick thought") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (vm.grid) 2 else 1),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 104.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
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
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Quick thought", Icons.Outlined.Bolt, c.heroBg, c.heroText, c.heroBg, Modifier.weight(1f), vm::openQuick)
                    ActionButton("Checklist", Icons.Outlined.CheckBox, c.card, MaterialTheme.colorScheme.primary, c.border, Modifier.weight(1f)) {
                        vm.createNote(NoteType.CHECKLIST, openNote)
                    }
                }
            }

            if (lists.pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionLabel("PINNED") }
                items(lists.pinned, key = { it.note.id }) { NoteCard(it, compact = vm.grid, onClick = { openNote(it.note.id) }) }
            }
            if (lists.recent.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionLabel("RECENT") }
                items(lists.recent, key = { it.note.id }) { NoteCard(it, compact = vm.grid, onClick = { openNote(it.note.id) }) }
            }
            if (lists.loaded && lists.pinned.isEmpty() && lists.recent.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptyState(
                        title = "Nothing here yet.",
                        body = "Capture a thought now.\nYou don\u2019t need to organize it.",
                        actionLabel = "+ Quick thought",
                        onAction = vm::openQuick,
                    )
                }
            }
        }
    }

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
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    border: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
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
