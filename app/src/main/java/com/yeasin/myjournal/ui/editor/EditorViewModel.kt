package com.yeasin.myjournal.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeasin.myjournal.AppContainer
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.db.items
import com.yeasin.myjournal.data.repo.ChecklistLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Holds the note being edited. Every change schedules a save 400 ms later on the app scope,
 * so leaving the screen never loses the last few keystrokes. There is no Save button.
 */
class EditorViewModel(private val noteId: String, private val c: AppContainer) : ViewModel() {
    private val repo = c.notes

    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var type by mutableStateOf(NoteType.TEXT)
        private set
    var isPinned by mutableStateOf(false)
        private set
    var isQuick by mutableStateOf(false)
        private set
    var updatedAt by mutableStateOf(0L)
        private set
    var message by mutableStateOf<String?>(null)
    val lines = mutableStateListOf<ChecklistLine>()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { repo.get(noteId) } ?: return@launch
            title = n.note.title
            content = n.note.content
            type = n.note.type
            isPinned = n.note.isPinned
            isQuick = n.note.captureType == CaptureType.QUICK
            updatedAt = n.note.updatedAt
            lines.clear()
            lines.addAll(n.items.map { ChecklistLine(it.id, it.text, it.done) })
            if (type == NoteType.CHECKLIST && lines.isEmpty()) lines.add(newLine())
            loaded = true
        }
    }

    fun onTitle(v: String) { title = v; scheduleSave() }
    fun onContent(v: String) { content = v; scheduleSave() }

    fun onLineText(index: Int, v: String) { if (index in lines.indices) { lines[index] = lines[index].copy(text = v); scheduleSave() } }
    fun toggleLine(index: Int) { if (index in lines.indices) { lines[index] = lines[index].copy(done = !lines[index].done); scheduleSave() } }
    fun removeLine(index: Int) { if (index in lines.indices) { lines.removeAt(index); scheduleSave() } }
    fun addLine() { lines.add(newLine()); scheduleSave() }

    /** Text ⇄ checklist. Each non-empty line becomes an item; items become "- " lines. */
    fun toggleChecklist() {
        if (type == NoteType.TEXT) {
            val items = content.lines()
                .map { it.replace(Regex("""^\s*([-*]|\d+\.)\s+"""), "").trim() }
                .filter { it.isNotEmpty() }
            lines.clear()
            lines.addAll(if (items.isEmpty()) listOf(newLine()) else items.map { ChecklistLine(UUID.randomUUID().toString(), it, false) })
            content = ""
            type = NoteType.CHECKLIST
        } else {
            content = lines.filter { it.text.isNotBlank() }.joinToString("\n") { "- " + it.text }
            lines.clear()
            type = NoteType.TEXT
        }
        scheduleSave()
    }

    /** Project, folder and tags change instantly (no autosave needed); the screen observes them here. */
    val meta: StateFlow<NoteWithItems?> = repo.observe(noteId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun togglePin() {
        val want = !isPinned
        val limit = c.settings.value.pinLimit
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { flushNow(); repo.setPinned(listOf(noteId), want, limit) }
            if (ok) isPinned = want else message = "You can pin up to $limit notes."
        }
    }

    fun setProject(id: String?) { c.appScope.launch { repo.setProject(listOf(noteId), id) } }
    fun setFolder(id: String?) { c.appScope.launch { repo.setFolder(listOf(noteId), id) } }
    fun setTag(tagId: String, on: Boolean) {
        c.appScope.launch { if (on) repo.addTag(listOf(noteId), tagId) else repo.removeTag(listOf(noteId), tagId) }
    }

    /** Archive and leave. */
    fun archive(done: () -> Unit) {
        saveJob?.cancel()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { flushNow(); repo.archive(listOf(noteId)) }
            done()
        }
    }

    /** Move to Trash and leave; the screen you return to offers Undo. */
    fun delete(done: () -> Unit) {
        saveJob?.cancel()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { flushNow(); repo.moveToTrash(listOf(noteId)) }
            c.lastDeleted.value = noteId
            done()
        }
    }

    /** Called on back: save now, drop the note if it was left empty, then leave. */
    fun close(done: () -> Unit) {
        saveJob?.cancel()
        c.appScope.launch {
            flushNow()
            repo.discardIfEmpty(noteId)
            withContext(Dispatchers.Main) { done() }
        }
    }

    private fun scheduleSave() {
        if (!loaded) return
        saveJob?.cancel()
        saveJob = c.appScope.launch {
            delay(400)
            flushNow()
        }
    }

    private suspend fun flushNow() {
        if (!loaded) return
        repo.save(noteId, title, content, type, lines.toList())
        val now = System.currentTimeMillis()
        withContext(Dispatchers.Main) { updatedAt = now }
    }

    private fun newLine() = ChecklistLine(UUID.randomUUID().toString(), "", false)
}
