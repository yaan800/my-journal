package com.yeasin.myjournal

import android.app.Application
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.repo.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Phase 1 wires dependencies by hand to keep the build simple. Hilt can replace
 * [AppContainer] in Phase 3, when the AI service and its providers arrive.
 */
class AppContainer(app: Application) {
    private val db = AppDatabase.build(app)
    val notes = NoteRepository(db)

    /** Outlives screens, so a save that starts as you leave a note still finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class MyJournalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Phase 5 moves this to a daily WorkManager job; on launch is enough for now.
        container.appScope.launch { container.notes.purgeOldTrash() }
    }
}
