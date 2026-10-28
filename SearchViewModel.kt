package com.yeasin.myjournal.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeasin.myjournal.AppContainer
import com.yeasin.myjournal.data.db.NoteWithItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A date window; [from] and [to] are epoch millis, inclusive. */
data class DateFilter(val label: String, val from: Long, val to: Long)

data class SearchFilter(
    val tagIds: Set<String> = emptySet(),
    val projectId: String? = null,
    val folderId: String? = null,
    val date: DateFilter? = null,
    val pinnedOnly: Boolean = false,
) {
    val active: Boolean get() = tagIds.isNotEmpty() || projectId != null || folderId != null || date != null || pinnedOnly
}

/**
 * Local search: text matches title, note, checklist, tag, project and folder names (in SQL);
 * filters then narrow by tags (all must match), project, folder (with sub-folders), date and pinned.
 * No internet needed.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val c: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _filter = MutableStateFlow(SearchFilter())
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    /** null = nothing typed or filtered yet. */
    val results: StateFlow<List<NoteWithItems>?> =
        combine(_query.debounce(150), _filter) { q, f -> q to f }
            .flatMapLatest { (q, f) ->
                if (q.isBlank() && !f.active) {
                    flowOf(null)
                } else {
                    c.notes.search(q).map { list -> list.filter { matches(it, f) } }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun matches(n: NoteWithItems, f: SearchFilter): Boolean {
        val note = n.note
        if (f.tagIds.isNotEmpty() && !f.tagIds.all { id -> n.tags.any { it.id == id } }) return false
        if (f.projectId != null && note.projectId != f.projectId) return false
        if (f.folderId != null && note.folderId !in c.catalog.value.folderAndChildren(f.folderId)) return false
        if (f.pinnedOnly && !note.isPinned) return false
        if (f.date != null && (note.updatedAt < f.date.from || note.updatedAt > f.date.to)) return false
        return true
    }

    fun onQuery(q: String) { _query.value = q }
    fun setFilter(f: SearchFilter) { _filter.value = f }
    fun update(change: (SearchFilter) -> SearchFilter) { _filter.value = change(_filter.value) }
}
