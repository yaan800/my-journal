package com.yeasin.myjournal.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Tags, folders and projects. */
@Dao
interface CatalogDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun tagByName(name: String): TagEntity?

    @Query("SELECT * FROM projects WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun projectByName(name: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun tagCount(): Int

    @Upsert suspend fun upsertTag(tag: TagEntity)
    @Upsert suspend fun upsertTags(tags: List<TagEntity>)
    @Upsert suspend fun upsertFolder(folder: FolderEntity)
    @Upsert suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM tags WHERE id = :id") suspend fun deleteTag(id: String)
    @Query("DELETE FROM folders WHERE id = :id") suspend fun deleteFolder(id: String)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun deleteProject(id: String)

    /** Sub-folders move up one level when their parent is deleted. */
    @Query("UPDATE folders SET parentId = :newParent WHERE parentId = :oldParent")
    suspend fun reparent(oldParent: String, newParent: String?)
}
