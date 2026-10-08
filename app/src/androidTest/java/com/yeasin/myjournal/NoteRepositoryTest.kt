package com.yeasin.myjournal

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yeasin.myjournal.data.db.AiStatus
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.CaptureType
import com.yeasin.myjournal.data.db.NoteType
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

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = NoteRepository(db)
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

    @Test fun pinLimitIsTen() = runTest {
        val ids = (1..11).map { repo.create().also { id -> repo.save(id, "n$it", "x", NoteType.TEXT, emptyList()) } }
        ids.take(10).forEach { assertTrue(repo.setPinned(it, true)) }
        assertFalse(repo.setPinned(ids.last(), true))
    }

    @Test fun trashKeepsNotesThirtyDays() = runTest {
        val id = repo.create(); repo.save(id, "old", "x", NoteType.TEXT, emptyList())
        repo.moveToTrash(id)
        assertEquals(0, repo.purgeOldTrash(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(29)))
        assertEquals(1, repo.purgeOldTrash(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31)))
    }
}
