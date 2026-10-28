package com.yeasin.myjournal.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.yeasin.myjournal.data.db.NoteWithItems

@Composable
fun rememberSelection(): SnapshotStateList<String> {
    val s = remember { mutableStateListOf<String>() }
    BackHandler(enabled = s.isNotEmpty()) { s.clear() }
    return s
}

/**
 * Pin · Archive · Delete · More for the selected notes, plus the More menu and its pickers.
 * [visible] is every note on screen (for "Select all" and for knowing what is already pinned/tagged).
 */
class SelectionMenus(
    val selection: SnapshotStateList<String>,
    val ops: NoteOps,
) {
    var open by mutableStateOf<String?>(null)   // null | menu | project | folder | tag
    /** Every note on screen; refreshed on each recomposition (plain field, not state). */
    var visible: List<NoteWithItems> = emptyList()
    val selectedNotes: List<NoteWithItems> get() = visible.filter { it.note.id in selection }
}

@Composable
fun RowScope.SelectionActions(menus: SelectionMenus) {
    val sel = menus.selectedNotes
    val ids = menus.selection.toList()
    val allPinned = sel.isNotEmpty() && sel.all { it.note.isPinned }
    val allArchived = sel.isNotEmpty() && sel.all { it.note.isArchived }
    IconButton(onClick = { menus.ops.pin(ids, !allPinned); menus.selection.clear() }) {
        Icon(if (allPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, contentDescription = if (allPinned) "Unpin" else "Pin")
    }
    IconButton(onClick = { if (allArchived) menus.ops.unarchive(ids) else menus.ops.archive(ids); menus.selection.clear() }) {
        Icon(if (allArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, contentDescription = if (allArchived) "Unarchive" else "Archive")
    }
    IconButton(onClick = { menus.ops.trash(ids); menus.selection.clear() }) {
        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
    }
    IconButton(onClick = { menus.open = "menu" }) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")
    }
}

/** The sheets behind the More button. Place once in the screen. */
@Composable
fun SelectionSheets(menus: SelectionMenus) {
    val ids = menus.selection.toList()
    val sel = menus.selectedNotes
    val n = ids.size
    val done = { menus.open = null; menus.selection.clear() }
    when (menus.open) {
        "menu" -> OptionsSheet(
            title = "$n selected",
            options = listOf(
                "Add to project" to { menus.open = "project" },
                "Move to folder" to { menus.open = "folder" },
                "Add or remove tags" to { menus.open = "tag" },
                "Select all on this screen" to {
                    menus.visible.forEach { if (it.note.id !in menus.selection) menus.selection.add(it.note.id) }
                    menus.open = null
                },
            ),
            onDismiss = { menus.open = null },
        )
        "project" -> ProjectPickerSheet(
            title = if (n == 1) "Project" else "Add $n notes to project",
            currentId = sel.map { it.note.projectId }.distinct().singleOrNull(),
            onPick = { menus.ops.setProject(ids, it); done() },
            onDismiss = { menus.open = null },
        )
        "folder" -> FolderPickerSheet(
            title = if (n == 1) "Move to folder" else "Move $n notes to folder",
            currentId = sel.map { it.note.folderId }.distinct().singleOrNull(),
            onPick = { menus.ops.setFolder(ids, it); done() },
            onDismiss = { menus.open = null },
        )
        "tag" -> TagPickerSheet(
            title = if (n == 1) "Tags" else "Tags for $n notes",
            isOn = { tag -> sel.isNotEmpty() && sel.all { note -> note.tags.any { it.id == tag.id } } },
            onToggle = { tag, on -> menus.ops.setTag(ids, tag.id, on) },
            onDismiss = done,
        )
    }
}

/** Remembered once per screen; [visible] is refreshed on every recomposition. */
@Composable
fun rememberSelectionMenus(selection: SnapshotStateList<String>, ops: NoteOps, visible: List<NoteWithItems>): SelectionMenus {
    val m = remember(selection, ops) { SelectionMenus(selection, ops) }
    m.visible = visible
    return m
}
