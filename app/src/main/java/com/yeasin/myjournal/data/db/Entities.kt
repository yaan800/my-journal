package com.yeasin.myjournal.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

/** Values stored as text so the database stays readable and later phases can add more. */
object NoteType {
    const val TEXT = "text"
    const val CHECKLIST = "checklist"
}

object CaptureType {
    const val NORMAL = "normal"
    const val QUICK = "quick"
    const val BRAINSTORM = "brainstorm"   // Phase 3
    const val VOICE = "voice"             // Phase 4
    const val SHARED = "shared"           // Phase 4
}

object AiStatus {
    const val NONE = "none"
    const val UNPROCESSED = "unprocessed" // waiting in the AI Inbox
    const val PROCESSING = "processing"
    const val PENDING = "pending"         // suggestions ready to review
    const val PROCESSED = "processed"
}

/**
 * One note. Columns used by later phases (folder, project, AI status) exist from day one,
 * so Phase 2 and 3 add tables instead of changing this one.
 *
 * Rule: [originalContent] holds the raw thought as first captured. AI never writes to it.
 */
@Entity(
    tableName = "notes",
    indices = [Index("updatedAt"), Index("isDeleted"), Index("aiStatus")],
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val originalContent: String? = null,
    val type: String = NoteType.TEXT,
    val captureType: String = CaptureType.NORMAL,
    val aiStatus: String = AiStatus.NONE,
    val folderId: String? = null,
    val projectId: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val text: String,
    val done: Boolean = false,
    val position: Int,
)

// ---------- Phase 2: tags, folders, projects ----------

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("noteId"), Index("tagId")],
)
data class NoteTagCrossRef(val noteId: String, val tagId: String)

/** Folders can nest one inside another through [parentId]. */
@Entity(tableName = "folders", indices = [Index("parentId")])
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** [keywords] is comma-separated; Phase 3 uses it to suggest the project for a new note. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val keywords: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A note together with its checklist lines and its tags. */
data class NoteWithItems(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val itemsUnsorted: List<ChecklistItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(value = NoteTagCrossRef::class, parentColumn = "noteId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity> = emptyList(),
)

/** Checklist lines in the order the user arranged them. Kept outside the class so Room ignores it. */
val NoteWithItems.items: List<ChecklistItemEntity> get() = itemsUnsorted.sortedBy { it.position }
