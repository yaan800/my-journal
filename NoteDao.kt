package com.yeasin.myjournal.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // ---------- lists ----------

    /** Notes screen: not deleted, not archived. Pinned first, newest first. */
    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteWithItems>>

    /** Timeline: everything not deleted, archived included, by when it was written. */
    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeLive(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: String): Flow<NoteWithItems?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteWithItems?

    /**
     * Local search over title, text, checklist lines, tag names, project and folder names.
     * Plain LIKE: works offline, with Bangla, and is fast for many thousands of notes.
     * An empty query returns every non-deleted note, so filters work on their own.
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT n.* FROM notes n
        LEFT JOIN checklist_items c ON c.noteId = n.id
        LEFT JOIN note_tags nt ON nt.noteId = n.id
        LEFT JOIN tags t ON t.id = nt.tagId
        LEFT JOIN projects p ON p.id = n.projectId
        LEFT JOIN folders f ON f.id = n.folderId
        WHERE n.isDeleted = 0 AND (
            :q = '' OR
            n.title LIKE '%' || :q || '%' OR
            n.content LIKE '%' || :q || '%' OR
            c.text LIKE '%' || :q || '%' OR
            t.name LIKE '%' || :q || '%' OR
            p.name LIKE '%' || :q || '%' OR
            f.name LIKE '%' || :q || '%'
        )
        ORDER BY n.updatedAt DESC
        """,
    )
    fun search(q: String): Flow<List<NoteWithItems>>

    // ---------- writing ----------

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun clearItems(noteId: String)

    @Query("SELECT COUNT(*) FROM notes WHERE isPinned = 1 AND isDeleted = 0 AND isArchived = 0")
    suspend fun pinnedCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE id IN (:ids) AND isPinned = 0")
    suspend fun unpinnedAmong(ids: List<String>): Int

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinned(ids: List<String>, pinned: Boolean)

    @Query("UPDATE notes SET isArchived = 1, isPinned = 0 WHERE id IN (:ids)")
    suspend fun archive(ids: List<String>)

    @Query("UPDATE notes SET isArchived = 0 WHERE id IN (:ids)")
    suspend fun unarchive(ids: List<String>)

    @Query("UPDATE notes SET isDeleted = 1, isPinned = 0, deletedAt = :at WHERE id IN (:ids)")
    suspend fun moveToTrash(ids: List<String>, at: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restore(ids: List<String>)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteForever(ids: List<String>)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    /** Trash keeps notes for 30 days. */
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long): Int

    @Query("UPDATE notes SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun setFolder(ids: List<String>, folderId: String?)

    @Query("UPDATE notes SET projectId = :projectId WHERE id IN (:ids)")
    suspend fun setProject(ids: List<String>, projectId: String?)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tags WHERE tagId = :tagId AND noteId IN (:ids)")
    suspend fun removeTagRefs(ids: List<String>, tagId: String)

    /** Notes left pointing at a deleted folder or project fall back to "none". */
    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)

    @Query("UPDATE notes SET projectId = NULL WHERE projectId = :projectId")
    suspend fun clearProject(projectId: String)
}
