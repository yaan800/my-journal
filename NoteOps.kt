package com.yeasin.myjournal.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeasin.myjournal.AppContainer
import com.yeasin.myjournal.ui.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pin / archive / delete / organize for one or many notes, with Undo where it matters. */
class NoteOps(private val c: AppContainer, private val snackbar: SnackbarHostState, private val scope: CoroutineScope) {

    private fun io(block: suspend () -> Unit) = scope.launch { withContext(Dispatchers.IO) { block() } }
    private fun plural(n: Int) = if (n == 1) "Note" else "$n notes"
    private fun pinnedAmong(ids: List<String>) = c.activeNotes.value.filter { it.note.id in ids && it.note.isPinned }.map { it.note.id }

    fun pin(ids: List<String>, pinned: Boolean) = scope.launch {
        val limit = c.settings.value.pinLimit
        val ok = withContext(Dispatchers.IO) { c.notes.setPinned(ids, pinned, limit) }
        if (!ok) snackbar.showSnackbar("You can pin up to $limit notes.")
    }

    fun archive(ids: List<String>) = scope.launch {
        val wasPinned = pinnedAmong(ids)
        withContext(Dispatchers.IO) { c.notes.archive(ids) }
        val r = snackbar.showSnackbar("${plural(ids.size)} archived", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (r == SnackbarResult.ActionPerformed) withContext(Dispatchers.IO) {
            c.notes.unarchive(ids)
            if (wasPinned.isNotEmpty()) c.notes.setPinned(wasPinned, true, Int.MAX_VALUE)
        }
    }

    fun unarchive(ids: List<String>) = scope.launch {
        withContext(Dispatchers.IO) { c.notes.unarchive(ids) }
        val r = snackbar.showSnackbar("${plural(ids.size)} unarchived", actionLabel = "Undo")
        if (r == SnackbarResult.ActionPerformed) withContext(Dispatchers.IO) { c.notes.archive(ids) }
    }

    fun trash(ids: List<String>) = scope.launch {
        val wasPinned = pinnedAmong(ids)
        withContext(Dispatchers.IO) { c.notes.moveToTrash(ids) }
        offerUndoDelete(ids, wasPinned)
    }

    suspend fun offerUndoDelete(ids: List<String>, wasPinned: List<String> = emptyList()) {
        val r = snackbar.showSnackbar("${plural(ids.size)} deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (r == SnackbarResult.ActionPerformed) withContext(Dispatchers.IO) {
            c.notes.restore(ids)
            if (wasPinned.isNotEmpty()) c.notes.setPinned(wasPinned, true, Int.MAX_VALUE)
        }
    }

    fun restore(ids: List<String>) = scope.launch {
        withContext(Dispatchers.IO) { c.notes.restore(ids) }
        snackbar.showSnackbar("${plural(ids.size)} restored")
    }

    fun deleteForever(ids: List<String>) = io { c.notes.deleteForever(ids) }
    fun emptyTrash() = io { c.notes.emptyTrash() }
    fun setProject(ids: List<String>, projectId: String?) = io { c.notes.setProject(ids, projectId) }
    fun setFolder(ids: List<String>, folderId: String?) = io { c.notes.setFolder(ids, folderId) }
    fun setTag(ids: List<String>, tagId: String, on: Boolean) = io { if (on) c.notes.addTag(ids, tagId) else c.notes.removeTag(ids, tagId) }
}

@Composable
fun rememberNoteOps(snackbar: SnackbarHostState): NoteOps {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val ops = remember(snackbar) { NoteOps(c, snackbar, scope) }
    // A note deleted inside the editor: the screen you land on offers Undo.
    val deleted by c.lastDeleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) {
        val id = deleted ?: return@LaunchedEffect
        c.lastDeleted.value = null
        ops.offerUndoDelete(listOf(id))
    }
    return ops
}
