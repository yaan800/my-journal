package com.yeasin.myjournal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1 = Phase 1. Every later schema change bumps the version and adds a tested
 * Migration — never fallbackToDestructiveMigration(), which would delete notes.
 */
@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "my_journal.db").build()
    }
}
