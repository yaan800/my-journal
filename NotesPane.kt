package com.yeasin.myjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yeasin.myjournal.data.db.NoteWithItems

data class NoteSection(val label: String?, val notes: List<NoteWithItems>)

/** Swipe right / swipe left actions. Labels can depend on the note (Pin vs Unpin). */
class SwipeActions(
    val rightLabel: (NoteWithItems) -> String = { "" },
    val onRight: ((NoteWithItems) -> Unit)? = null,
    val leftLabel: (NoteWithItems) -> String = { "" },
    val onLeft: ((NoteWithItems) -> Unit)? = null,
)

/**
 * The note list used on every screen: sections, list or 2-column grid, swipe (list only),
 * and long-press multi-select. While anything is selected, a tap toggles selection.
 */
@Composable
fun NotesPane(
    sections: List<NoteSection>,
    grid: Boolean,
    selection: SnapshotStateList<String>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    swipe: SwipeActions? = null,
    selectOnTap: Boolean = false,
    selectable: Boolean = true,
    showProject: Boolean = true,
    showFolder: Boolean = true,
    dateText: ((NoteWithItems) -> String)? = null,
    bottomPadding: Dp = 104.dp,
    header: LazyStaggeredGridScope.() -> Unit = {},
    empty: (@Composable () -> Unit)? = null,
) {
    val toggle = { id: String -> if (id in selection) selection.remove(id) else selection.add(id) }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (grid) 2 else 1),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = bottomPadding),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        header()
        val visible = sections.filter { it.notes.isNotEmpty() }
        visible.forEach { sec ->
            if (sec.label != null) item(span = StaggeredGridItemSpan.FullLine, key = "label-" + sec.label) { SectionLabel(sec.label) }
            items(sec.notes, key = { it.note.id }) { item ->
                val id = item.note.id
                val selected = id in selection
                val card = @Composable {
                    NoteCard(
                        item = item,
                        compact = grid,
                        selected = selected,
                        onClick = { if (selection.isNotEmpty() || selectOnTap) toggle(id) else onOpen(id) },
                        onLongClick = if (selectable) ({ if (id !in selection) selection.add(id) }) else null,
                        showProject = showProject,
                        showFolder = showFolder,
                        dateText = dateText?.invoke(item),
                    )
                }
                if (swipe != null && !grid && selection.isEmpty()) SwipeCard(item, swipe, card) else card()
            }
        }
        if (visible.isEmpty() && empty != null) item(span = StaggeredGridItemSpan.FullLine, key = "empty") { empty() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeCard(item: NoteWithItems, swipe: SwipeActions, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> swipe.onRight?.invoke(item)
                SwipeToDismissBoxValue.EndToStart -> swipe.onLeft?.invoke(item)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false   // always spring back; the list itself updates
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = swipe.onRight != null,
        enableDismissFromEndToStart = swipe.onLeft != null,
        backgroundContent = {
            val dir = state.dismissDirection
            val label = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> swipe.rightLabel(item)
                SwipeToDismissBoxValue.EndToStart -> swipe.leftLabel(item)
                SwipeToDismissBoxValue.Settled -> ""
            }
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    .background(if (dir == SwipeToDismissBoxValue.Settled) Color.Transparent else MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 22.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
    ) { content() }
}
