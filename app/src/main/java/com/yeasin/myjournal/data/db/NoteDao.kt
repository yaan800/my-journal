package com.yeasin.myjournal.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** Everything on the Notes screen: not deleted, not archived. Pinned first, newest first. */
    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteWithItems?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getEntity(id: String): NoteEntity?

    /**
     * Plain local search over title, text and checklist lines. Works offline and with Bangla,
     * because LIKE compares the stored characters directly. Phase 2 replaces this with FTS5.
     * Archived notes are included, as in the prototype; deleted notes are not.
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT n.* FROM notes n
        LEFT JOIN checklist_items c ON c.noteId = n.id
        WHERE n.isDeleted = 0 AND (
            n.title LIKE '%' || :q || '%' OR
            n.content LIKE '%' || :q || '%' OR
            c.text LIKE '%' || :q || '%'
        )
        ORDER BY n.updatedAt DESC
        """,
    )
    fun search(q: String): Flow<List<NoteWithItems>>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun clearItems(noteId: String)

    @Query("SELECT COUNT(*) FROM notes WHERE isPinned = 1 AND isDeleted = 0 AND isArchived = 0")
    suspend fun pinnedCount(): Int

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, isPinned = 0, deletedAt = :at WHERE id = :id")
    suspend fun moveToTrash(id: String, at: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: String)

    /** Trash keeps notes for 30 days. */
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long): Int
}
