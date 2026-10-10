package com.mono.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Priority(val level: Int, val label: String) {
    Low(0, "Low"), Medium(1, "Medium"), High(2, "High");

    companion object {
        fun fromLevel(level: Int) = values().firstOrNull { it.level == level } ?: Medium
    }
}

/** dueDate = LocalDate epoch day (timezone-free). dueTimeMinutes = minutes after midnight, local. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val dueTimeMinutes: Int? = null,
    val priority: Int = Priority.Medium.level,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "folders", indices = [Index(value = ["name"], unique = true)])
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Deleting a folder sets folderId to NULL: notes are kept, never destroyed. */
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["folderId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("folderId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    /** Versioned JSON, see RichContent. */
    val content: String,
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val contentFormatVersion: Int = 1,
)

/** targetDate = LocalDate epoch day (timezone-free). */
@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
