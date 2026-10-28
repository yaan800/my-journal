package com.yeasin.myjournal.data.repo

import androidx.room.withTransaction
import com.yeasin.myjournal.data.db.AiStatus
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.ChecklistItemEntity
import com.yeasin.myjournal.data.db.NoteEntity
import com.yeasin.myjournal.data.db.NoteTagCrossRef
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.db.items
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/** A checklist line as the editor holds it. */
data class ChecklistLine(val id: String, val text: String, val done: Boolean)

/** The only place screens read and write notes. */
class NoteRepository(private val db: AppDatabase) {
    private val dao = db.noteDao()

    fun observeActive(): Flow<List<NoteWithItems>> = dao.observeActive()
    fun observeArchived(): Flow<List<NoteWithItems>> = dao.observeArchived()
    fun observeTrash(): Flow<List<NoteWithItems>> = dao.observeTrash()
    fun observeLive(): Flow<List<NoteWithItems>> = dao.observeLive()
    fun observe(id: String): Flow<NoteWithItems?> = dao.observe(id)
    fun search(query: String): Flow<List<NoteWithItems>> = dao.search(query.trim())

    suspend fun get(id: String): NoteWithItems? = dao.get(id)

    /** New note, optionally already placed in a project, folder or tag (when created from that screen). */
    suspend fun create(
        type: String = NoteType.TEXT,
        projectId: String? = null,
        folderId: String? = null,
        tagId: String? = null,
    ): String {
        val note = NoteEntity(type = type, projectId = projectId, folderId = folderId)
        db.withTransaction {
            dao.upsert(note)
            if (tagId != null) dao.addTagRefs(listOf(NoteTagCrossRef(note.id, tagId)))
        }
        return note.id
    }

    /**
     * Quick capture: saved on the first keystroke with no title, folder or tag, waiting in
     * the AI Inbox. [originalContent] keeps the raw thought while it is being typed.
     */
    suspend fun saveQuickThought(id: String, createdAt: Long, text: String) {
        val existing = dao.get(id)?.note
        dao.upsert(
            (existing ?: NoteEntity(id = id, captureType = CaptureType.QUICK, aiStatus = AiStatus.UNPROCESSED, createdAt = createdAt))
                .copy(content = text, originalContent = text, updatedAt = System.currentTimeMillis()),
        )
    }

    /** Saves the editor's text. Does nothing if nothing changed, so opening a note never reorders the list. */
    suspend fun save(id: String, title: String, content: String, type: String, lines: List<ChecklistLine>) {
        db.withTransaction {
            val current = dao.get(id) ?: return@withTransaction
            val oldLines = current.items.map { ChecklistLine(it.id, it.text, it.done) }
            val changed = current.note.title != title || current.note.content != content ||
                current.note.type != type || oldLines != lines
            if (!changed) return@withTransaction
            dao.upsert(current.note.copy(title = title, content = content, type = type, updatedAt = System.currentTimeMillis()))
            dao.clearItems(id)
            if (type == NoteType.CHECKLIST && lines.isNotEmpty()) {
                dao.upsertItems(lines.mapIndexed { i, l -> ChecklistItemEntity(id = l.id, noteId = id, text = l.text, done = l.done, position = i) })
            }
        }
    }

    /** Removes a note that was opened and left empty, like Keep does. Returns true if it was removed. */
    suspend fun discardIfEmpty(id: String): Boolean {
        val n = dao.get(id) ?: return false
        val empty = n.note.title.isBlank() && n.note.content.isBlank() && n.items.all { it.text.isBlank() }
        if (empty) dao.deleteForever(listOf(id))
        return empty
    }

    // ---------- pin, archive, trash (single or many) ----------

    /** Pins all [ids], or returns false if that would go over [limit]. Unpinning always works. */
    suspend fun setPinned(ids: List<String>, pinned: Boolean, limit: Int): Boolean {
        if (pinned && dao.pinnedCount() + dao.unpinnedAmong(ids) > limit) return false
        dao.setPinned(ids, pinned)
        return true
    }

    suspend fun archive(ids: List<String>) = dao.archive(ids)
    suspend fun unarchive(ids: List<String>) = dao.unarchive(ids)
    suspend fun moveToTrash(ids: List<String>) = dao.moveToTrash(ids, System.currentTimeMillis())
    suspend fun restore(ids: List<String>) = dao.restore(ids)
    suspend fun deleteForever(ids: List<String>) = dao.deleteForever(ids)
    suspend fun emptyTrash() = dao.emptyTrash()

    /** Permanent delete of one note. Used when a just-typed quick thought is discarded. */
    suspend fun delete(id: String) = dao.deleteForever(listOf(id))

    suspend fun purgeOldTrash(now: Long = System.currentTimeMillis()): Int =
        dao.purgeTrash(now - TimeUnit.DAYS.toMillis(TRASH_DAYS))

    // ---------- organizing ----------

    suspend fun setFolder(ids: List<String>, folderId: String?) = dao.setFolder(ids, folderId)
    suspend fun setProject(ids: List<String>, projectId: String?) = dao.setProject(ids, projectId)
    suspend fun addTag(ids: List<String>, tagId: String) = dao.addTagRefs(ids.map { NoteTagCrossRef(it, tagId) })
    suspend fun removeTag(ids: List<String>, tagId: String) = dao.removeTagRefs(ids, tagId)

    companion object {
        const val TRASH_DAYS = 30L
    }
}
