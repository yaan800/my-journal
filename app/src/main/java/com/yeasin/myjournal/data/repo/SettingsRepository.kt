package com.yeasin.myjournal.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val theme: String = THEME_SYSTEM,       // system | light | dark
    val defaultView: String = VIEW_LIST,    // list | grid
    val pinLimit: Int = 10,                 // 5 or 10
) {
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val VIEW_LIST = "list"
        const val VIEW_GRID = "grid"
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small settings kept with Jetpack DataStore (not in the notes database). */
class SettingsRepository(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val view = stringPreferencesKey("default_view")
    private val pinLimit = intPreferencesKey("pin_limit")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            theme = p[theme] ?: AppSettings.THEME_SYSTEM,
            defaultView = p[view] ?: AppSettings.VIEW_LIST,
            pinLimit = p[pinLimit] ?: 10,
        )
    }

    suspend fun setTheme(value: String) { context.settingsStore.edit { it[theme] = value } }
    suspend fun setDefaultView(value: String) { context.settingsStore.edit { it[view] = value } }
    suspend fun setPinLimit(value: Int) { context.settingsStore.edit { it[pinLimit] = value } }
}
