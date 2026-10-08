package com.yeasin.myjournal.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeasin.myjournal.AppContainer
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID

data class HomeLists(val pinned: List<NoteWithItems>, val recent: List<NoteWithItems>, val loaded: Boolean)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.notes

    val lists: StateFlow<HomeLists> = repo.observeActive()
        .map { all -> HomeLists(all.filter { it.note.isPinned }, all.filterNot { it.note.isPinned }, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLists(emptyList(), emptyList(), loaded = false))

    var grid by mutableStateOf(false)
        private set

    fun toggleGrid() { grid = !grid }

    fun createNote(type: String = NoteType.TEXT, open: (String) -> Unit) {
        viewModelScope.launch {
            val id = repo.create(type)
            open(id)
        }
    }

    fun restore(id: String) {
        c.appScope.launch { repo.restore(id) }
    }

    // ---- Quick capture: the first keystroke saves at once, later ones every 300 ms ----

    var quickOpen by mutableStateOf(false)
        private set
    var quickSaved by mutableStateOf(false)
        private set
    private var quickId: String? = null
    private var quickCreatedAt = 0L
    private var quickText = ""
    private var quickJob: Job? = null

    fun openQuick() {
        quickId = null; quickText = ""; quickSaved = false; quickOpen = true
    }

    fun quickType(text: String) {
        quickText = text
        if (quickId == null) {
            if (text.isBlank()) return
            quickId = UUID.randomUUID().toString()
            quickCreatedAt = System.currentTimeMillis()
            flushQuick()
            return
        }
        quickJob?.cancel()
        quickJob = c.appScope.launch { delay(300); writeQuick() }
    }

    private fun flushQuick() {
        quickJob?.cancel()
        quickJob = c.appScope.launch { writeQuick() }
    }

    private suspend fun writeQuick() {
        val id = quickId ?: return
        repo.saveQuickThought(id, quickCreatedAt, quickText)
        withContext(Dispatchers.Main) { quickSaved = true }
    }

    /** Returns true if a thought was kept, so the screen can confirm it. */
    fun closeQuick(keep: Boolean): Boolean {
        quickOpen = false
        val id = quickId ?: return false
        if (!keep || quickText.isBlank()) {
            val pending = quickJob
            c.appScope.launch { pending?.join(); repo.delete(id) }   // wait for any write in flight, then remove
            quickId = null
            return false
        }
        flushQuick()
        quickId = null
        return true
    }

    /** "Open as full note": keep what was typed and hand the id to the editor. */
    fun quickToFull(open: (String) -> Unit) {
        val id = quickId
        quickOpen = false
        if (id == null || quickText.isBlank()) {
            quickId = null
            createNote(open = open)
            return
        }
        quickJob?.cancel()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.saveQuickThought(id, quickCreatedAt, quickText) }
            quickId = null
            open(id)
        }
    }
}
