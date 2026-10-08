package com.yeasin.myjournal.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeasin.myjournal.ui.appContainer
import com.yeasin.myjournal.ui.components.EmptyState
import com.yeasin.myjournal.ui.components.NoteCard
import com.yeasin.myjournal.ui.theme.Journal

@Composable
fun SearchScreen(onBack: () -> Unit, openNote: (String) -> Unit) {
    val container = appContainer()
    val vm: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(container) } })
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val c = Journal.colors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = c.input,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().height(48.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    BasicTextField(
                        value = query,
                        onValueChange = vm::onQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search anything\u2026", style = MaterialTheme.typography.bodyLarge, color = c.sub)
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = c.sub) }
                    }
                }
            }
        },
    ) { padding ->
        val list = results
        Box(Modifier.fillMaxSize().padding(padding)) {
        when {
            list == null -> EmptyState(
                title = "Search your notes",
                body = "Titles, note text and checklist items. Works offline, in English and Bangla.",
            )
            list.isEmpty() -> EmptyState(title = "No matching notes", body = "Try fewer or different words.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${list.size} ${if (list.size == 1) "note" else "notes"}",
                        style = MaterialTheme.typography.bodySmall, color = c.sub, modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(list, key = { it.note.id }) { NoteCard(it, compact = false, onClick = { openNote(it.note.id) }) }
            }
        }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
