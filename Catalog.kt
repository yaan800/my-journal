package com.yeasin.myjournal.ui

import androidx.compose.runtime.staticCompositionLocalOf

import com.yeasin.myjournal.data.db.FolderEntity
import com.yeasin.myjournal.data.db.ProjectEntity
import com.yeasin.myjournal.data.db.TagEntity

/** A snapshot of tags, folders and projects with the lookups screens need. */
data class Catalog(
    val tags: List<TagEntity>,
    val folders: List<FolderEntity>,
    val projects: List<ProjectEntity>,
) {
    private val projectIndex: Map<String, Int> = projects.sortedBy { it.createdAt }.mapIndexed { i, p -> p.id to i }.toMap()
    val projectById: Map<String, ProjectEntity> = projects.associateBy { it.id }
    val folderById: Map<String, FolderEntity> = folders.associateBy { it.id }
    val tagById: Map<String, TagEntity> = tags.associateBy { it.id }

    /** Stable colour slot for a project: the order it was created in. */
    fun projectColorSlot(id: String): Int = projectIndex[id] ?: 0

    /** "Stock Market / DSE Watchlist" */
    fun folderPath(id: String?): String {
        val f = id?.let { folderById[it] } ?: return ""
        val parent = f.parentId?.let { folderById[it] }
        return if (parent != null) parent.name + " / " + f.name else f.name
    }

    /** Folders in tree order with their depth, for nested lists and pickers. */
    fun folderTree(): List<Pair<FolderEntity, Int>> {
        val out = mutableListOf<Pair<FolderEntity, Int>>()
        fun walk(parent: String?, depth: Int) {
            folders.filter { it.parentId == parent && (parent == null || it.id != parent) }
                .sortedBy { it.name.lowercase() }
                .forEach { out.add(it to depth); if (depth < 6) walk(it.id, depth + 1) }
        }
        walk(null, 0)
        // Folders whose parent no longer exists still show, at the top level.
        folders.filter { f -> out.none { it.first.id == f.id } }.forEach { out.add(it to 0) }
        return out
    }

    /** A folder and everything inside it. */
    fun folderAndChildren(id: String): Set<String> {
        val out = mutableSetOf(id)
        var grew = true
        while (grew) {
            grew = false
            folders.forEach { if (it.parentId in out && out.add(it.id)) grew = true }
        }
        return out
    }

    companion object {
        val EMPTY = Catalog(emptyList(), emptyList(), emptyList())
    }
}

/** The current catalog, provided once at the root (see AppNav). */
val LocalCatalog = staticCompositionLocalOf { Catalog.EMPTY }
