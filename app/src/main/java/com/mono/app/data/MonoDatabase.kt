package com.mono.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema version 1. When the schema changes: bump version, add a Migration to
 * addMigrations(...) below. Never use fallbackToDestructiveMigration().
 */
@Database(
    entities = [TaskEntity::class, NoteEntity::class, FolderEntity::class, CountdownEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MonoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun countdownDao(): CountdownDao

    companion object {
        fun build(context: Context): MonoDatabase =
            Room.databaseBuilder(context.applicationContext, MonoDatabase::class.java, "mono.db").build()
    }
}
