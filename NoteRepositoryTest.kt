package com.yeasin.myjournal

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yeasin.myjournal.data.db.AiStatus
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
import com.yeasin.myjournal.data.repo.CatalogRepository
import com.yeasin.myjournal.data.repo.ChecklistLine
import com.yeasin.myjournal.data.repo.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Runs on a phone or emulator: Android Studio → right-click this file → Run. */
@RunWith(AndroidJUnit4::class)
class NoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: NoteRepository

    private lateinit var catalog: CatalogRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = NoteRepository(db)
        catalog = CatalogRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun quickThoughtIsSavedWithOriginalTextAndWaitsInInbox() = runTest {
        repo.saveQuickThought("q1", 1000L, "maybe DSEX momentum")
        val n = repo.get("q1")!!.note
        assertEquals(CaptureType.QUICK, n.captureType)
        assertEquals(AiStatus.UNPROCESSED, n.aiStatus)
        assertEquals("maybe DSEX momentum", n.originalContent)
        assertEquals("", n.title)
    }

    @Test fun editingANoteNeverChangesTheOriginalThought() = runTest {
        repo.saveQuickThought("q1", 1000L, "raw thought")
        repo.save("q1", "A title", "edited text", NoteType.TEXT, emptyList())
        val n = repo.get("q1")!!.note
        assertEquals("edited text", n.content)
        assertEquals("raw thought", n.originalContent)
    }

    @Test fun searchFindsEnglishBanglaAndChecklistText() = runTest {
        val a = repo.create(); repo.save(a, "Cement demand", "clinker imports falling", NoteType.TEXT, emptyList())
        val b = repo.create(); repo.save(b, "", "সিমেন্টের চাহিদা কম হতে পারে", NoteType.TEXT, emptyList())
        val c = repo.create(NoteType.CHECKLIST)
        repo.save(c, "Tasks", "", NoteType.CHECKLIST, listOf(ChecklistLine("l1", "check WACC sensitivity", false)))

        assertEquals(listOf(a), repo.search("clinker").first().map { it.note.id })
        assertEquals(listOf(b), repo.search("চাহিদা").first().map { it.note.id })
        assertEquals(listOf(c), repo.search("WACC").first().map { it.note.id })
    }

    @Test fun emptyNoteIsDiscardedOnClose() = runTest {
        val id = repo.create()
        assertTrue(repo.discardIfEmpty(id))
        assertNull(repo.get(id))
    }

    @Test fun pinLimitIsRespected() = runTest {
        val ids = (1..11).map { repo.create().also { id -> repo.save(id, "n$it", "x", NoteType.TEXT, emptyList()) } }
        ids.take(10).forEach { assertTrue(repo.setPinned(listOf(it), true, 10)) }
        assertFalse(repo.setPinned(listOf(ids.last()), true, 10))
        assertFalse(repo.setPinned(listOf(ids.last()), true, 5))
        assertTrue(repo.setPinned(listOf(ids.first()), false, 5))   // unpinning always works
    }

    @Test fun trashKeepsNotesThirtyDays() = runTest {
        val id = repo.create(); repo.save(id, "old", "x", NoteType.TEXT, emptyList())
        repo.moveToTrash(listOf(id))
        assertEquals(0, repo.purgeOldTrash(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(29)))
        assertEquals(1, repo.purgeOldTrash(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31)))
    }

    // ---------- Phase 2 ----------

    @Test fun archivingUnpinsAndHidesFromNotes() = runTest {
        val id = repo.create(); repo.save(id, "a", "x", NoteType.TEXT, emptyList())
        repo.setPinned(listOf(id), true, 10)
        repo.archive(listOf(id))
        val n = repo.get(id)!!.note
        assertTrue(n.isArchived)
        assertFalse(n.isPinned)
        assertTrue(repo.observeActive().first().none { it.note.id == id })
        assertEquals(listOf(id), repo.observeArchived().first().map { it.note.id })
    }

    @Test fun tagsAreCaseInsensitiveAndManyPerNote() = runTest {
        val t1 = catalog.tagId("DSE")
        assertEquals(t1, catalog.tagId("dse"))
        assertEquals(t1, catalog.tagId("#DSE"))
        val t2 = catalog.tagId("Research")
        val id = repo.create(); repo.save(id, "a", "x", NoteType.TEXT, emptyList())
        repo.addTag(listOf(id), t1); repo.addTag(listOf(id), t2); repo.addTag(listOf(id), t1)
        assertEquals(setOf("DSE", "Research"), repo.get(id)!!.tags.map { it.name }.toSet())
        repo.removeTag(listOf(id), t1)
        assertEquals(listOf("Research"), repo.get(id)!!.tags.map { it.name })
    }

    @Test fun searchMatchesTagProjectAndFolderNames() = runTest {
        val p = catalog.createProject("Cement Sector Report")
        val f = catalog.createFolder("Stock Market", null)
        val t = catalog.tagId("ENVOYTEX")
        val a = repo.create(projectId = p); repo.save(a, "one", "x", NoteType.TEXT, emptyList())
        val b = repo.create(folderId = f); repo.save(b, "two", "x", NoteType.TEXT, emptyList())
        val c = repo.create(tagId = t); repo.save(c, "three", "x", NoteType.TEXT, emptyList())
        assertEquals(listOf(a), repo.search("cement").first().map { it.note.id })
        assertEquals(listOf(b), repo.search("stock market").first().map { it.note.id })
        assertEquals(listOf(c), repo.search("envoy").first().map { it.note.id })
        assertEquals(3, repo.search("").first().size)   // empty query + filters = everything
    }

    @Test fun deletingAFolderKeepsItsNotesAndLiftsSubfolders() = runTest {
        val parent = catalog.createFolder("Stock Market", null)
        val child = catalog.createFolder("DSE Watchlist", parent)
        val id = repo.create(folderId = parent); repo.save(id, "kept", "x", NoteType.TEXT, emptyList())
        catalog.deleteFolder(catalog.observeFolders().first().first { it.id == parent })
        assertNull(repo.get(id)!!.note.folderId)
        assertNull(catalog.observeFolders().first().first { it.id == child }.parentId)
    }
}
