package com.yeasin.myjournal.data.repo

import androidx.room.withTransaction
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.FolderEntity
import com.yeasin.myjournal.data.db.ProjectEntity
import com.yeasin.myjournal.data.db.TagEntity
import kotlinx.coroutines.flow.Flow

/** Tags, folders and projects. Names are matched without regard to case, so "ai" and "AI" are one tag. */
class CatalogRepository(private val db: AppDatabase) {
    private val dao = db.catalogDao()
    private val notes = db.noteDao()

    fun observeTags(): Flow<List<TagEntity>> = dao.observeTags()
    fun observeFolders(): Flow<List<FolderEntity>> = dao.observeFolders()
    fun observeProjects(): Flow<List<ProjectEntity>> = dao.observeProjects()

    /** Returns the id of the tag with this name, creating it if needed. */
    suspend fun tagId(name: String): String {
        val clean = name.trim().removePrefix("#").trim()
        dao.tagByName(clean)?.let { return it.id }
        val tag = TagEntity(name = clean)
        dao.upsertTag(tag)
        return tag.id
    }

    suspend fun createFolder(name: String, parentId: String?): String {
        val f = FolderEntity(name = name.trim(), parentId = parentId)
        dao.upsertFolder(f)
        return f.id
    }

    /** Returns the existing project's id if one already has this name. */
    suspend fun createProject(name: String, keywords: String = ""): String {
        dao.projectByName(name.trim())?.let { return it.id }
        val p = ProjectEntity(name = name.trim(), keywords = keywords.trim())
        dao.upsertProject(p)
        return p.id
    }

    suspend fun deleteTag(id: String) = dao.deleteTag(id)   // note links go with it (cascade)

    suspend fun deleteFolder(folder: FolderEntity) = db.withTransaction {
        dao.reparent(folder.id, folder.parentId)
        notes.clearFolder(folder.id)
        dao.deleteFolder(folder.id)
    }

    suspend fun deleteProject(id: String) = db.withTransaction {
        notes.clearProject(id)
        dao.deleteProject(id)
    }

    /** First run only: the starter tags from the spec. */
    suspend fun ensureDefaultTags() {
        if (dao.tagCount() > 0) return
        dao.upsertTags(
            listOf("Rumor", "AI", "Explanation", "Idea", "Research", "Market", "Personal", "Work", "Learning", "Important")
                .map { TagEntity(name = it) },
        )
    }
}
