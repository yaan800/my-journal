package com.yeasin.myjournal.data.repo

import androidx.room.withTransaction
import com.yeasin.myjournal.data.db.AiStatus
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.ChecklistItemEntity
import com.yeasin.myjournal.data.db.NoteEntity
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.db.items
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/** A checklist line as the editor holds it. */
data class ChecklistLine(val id: String, val text: String, val done: Boolean)

/** The only place screens read and write notes. Phase 2 adds tag, folder and project repositories. */
class NoteRepository(private val db: AppDatabase) {
    private val dao = db.noteDao()

    fun observeActive(): Flow<List<NoteWithItems>> = dao.observeActive()

    fun search(query: String): Flow<List<NoteWithItems>> = dao.search(query.trim())

    suspend fun get(id: String): NoteWithItems? = dao.get(id)

    suspend fun create(type: String = NoteType.TEXT): String {
        val note = NoteEntity(type = type)
        dao.upsert(note)
        return note.id
    }

    /**
     * Quick capture: called on the first keystroke and then as typing continues.
     * The thought is saved immediately with no title, folder or tags, and waits in the
     * AI Inbox. [originalContent] tracks the raw text while it is being captured.
     */
    suspend fun saveQuickThought(id: String, createdAt: Long, text: String) {
        dao.upsert(
            NoteEntity(
                id = id,
                content = text,
                originalContent = text,
                captureType = CaptureType.QUICK,
                aiStatus = AiStatus.UNPROCESSED,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Saves the editor's state. Does nothing if nothing changed, so opening a note never reorders the list. */
    suspend fun save(id: String, title: String, content: String, type: String, lines: List<ChecklistLine>) {
        db.withTransaction {
            val current = dao.get(id) ?: return@withTransaction
            val oldLines = current.items.map { ChecklistLine(it.id, it.text, it.done) }
            val changed = current.note.title != title || current.note.content != content ||
                current.note.type != type || oldLines != lines
            if (!changed) return@withTransaction
            dao.upsert(
                current.note.copy(
                    title = title,
                    content = content,
                    type = type,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            dao.clearItems(id)
            if (type == NoteType.CHECKLIST && lines.isNotEmpty()) {
                dao.upsertItems(
                    lines.mapIndexed { i, l -> ChecklistItemEntity(id = l.id, noteId = id, text = l.text, done = l.done, position = i) },
                )
            }
        }
    }

    /** Removes a note that was opened and left empty, like Keep does. Returns true if it was removed. */
    suspend fun discardIfEmpty(id: String): Boolean {
        val n = dao.get(id) ?: return false
        val empty = n.note.title.isBlank() && n.note.content.isBlank() && n.items.all { it.text.isBlank() }
        if (empty) dao.deleteForever(id)
        return empty
    }

    /** Returns false when the pin limit is reached. */
    suspend fun setPinned(id: String, pinned: Boolean): Boolean {
        if (pinned && dao.pinnedCount() >= PIN_LIMIT) return false
        dao.setPinned(id, pinned)
        return true
    }

    suspend fun moveToTrash(id: String) = dao.moveToTrash(id, System.currentTimeMillis())

    suspend fun restore(id: String) = dao.restore(id)

    /** Permanent delete. Only used when the user discards a quick thought they just typed. */
    suspend fun delete(id: String) = dao.deleteForever(id)

    suspend fun purgeOldTrash(now: Long = System.currentTimeMillis()): Int =
        dao.purgeTrash(now - TimeUnit.DAYS.toMillis(TRASH_DAYS))

    companion object {
        const val PIN_LIMIT = 10      // becomes a setting (5 or 10) in Phase 2
        const val TRASH_DAYS = 30L
    }
}
