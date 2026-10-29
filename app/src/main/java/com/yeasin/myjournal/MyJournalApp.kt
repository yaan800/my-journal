package com.yeasin.myjournal

import android.app.Application
import com.yeasin.myjournal.data.db.AppDatabase
import com.yeasin.myjournal.data.db.NoteWithItems
import com.yeasin.myjournal.data.repo.AppSettings
import com.yeasin.myjournal.data.repo.CatalogRepository
import com.yeasin.myjournal.data.repo.NoteRepository
import com.yeasin.myjournal.data.repo.SettingsRepository
import com.yeasin.myjournal.ui.Catalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dependencies, wired by hand. Shared live data (active notes, catalog, settings) is held here
 * once, so every screen reads the same up-to-date lists without its own database query.
 */
class AppContainer(app: Application) {
    private val db = AppDatabase.build(app)
    val notes = NoteRepository(db)
    val catalogRepo = CatalogRepository(db)
    val settingsRepo = SettingsRepository(app)

    /** Outlives screens, so a save that starts as you leave a note still finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val activeNotes: StateFlow<List<NoteWithItems>> =
        notes.observeActive().stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val catalog: StateFlow<Catalog> =
        combine(catalogRepo.observeTags(), catalogRepo.observeFolders(), catalogRepo.observeProjects()) { t, f, p -> Catalog(t, f, p) }
            .stateIn(appScope, SharingStarted.Eagerly, Catalog.EMPTY)

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(appScope, SharingStarted.Eagerly, AppSettings())

    /** Set by the editor when a note is deleted; the screen you return to offers Undo. */
    val lastDeleted = MutableStateFlow<String?>(null)
}

class MyJournalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            container.catalogRepo.ensureDefaultTags()
            container.notes.purgeOldTrash()   // Phase 5 moves this to a daily background job
        }
    }
}
