package com.mono.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class TaskDraft(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val dueTimeMinutes: Int? = null,
    val priority: Priority = Priority.Medium,
    val completed: Boolean = false,
)

data class CountdownDraft(val id: Long = 0, val title: String = "", val targetDate: Long? = null)

enum class FolderResult { Ok, Blank, Duplicate }

class MonoRepository(
    private val db: MonoDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val tasks = db.taskDao()
    private val notes = db.noteDao()
    private val folders = db.folderDao()
    private val countdowns = db.countdownDao()

    // ---- Tasks
    fun observeTasks(): Flow<List<TaskEntity>> = tasks.observeAll()

    suspend fun saveTask(d: TaskDraft): Long {
        val title = d.title.trim()
        require(title.isNotEmpty()) { "Task title is required" }
        val now = clock()
        val existing = if (d.id != 0L) tasks.getById(d.id) else null
        val entity = TaskEntity(
            id = d.id, title = title, description = d.description.trim(),
            dueDate = d.dueDate,
            dueTimeMinutes = if (d.dueDate == null) null else d.dueTimeMinutes,
            priority = d.priority.level, isCompleted = d.completed,
            createdAt = existing?.createdAt ?: now, updatedAt = now,
        )
        val r = tasks.upsert(entity)
        return if (d.id == 0L) r else d.id
    }

    suspend fun setTaskCompleted(id: Long, done: Boolean) = tasks.setCompleted(id, done, clock())
    suspend fun deleteTask(id: Long) = tasks.delete(id)

    // ---- Notes
    fun observeNotes(): Flow<List<NoteEntity>> = notes.observeAll()
    suspend fun getNote(id: Long): NoteEntity? = notes.getById(id)

    suspend fun saveNote(id: Long, title: String, content: String, pinned: Boolean, folderId: Long?): Long {
        val now = clock()
        val existing = if (id != 0L) notes.getById(id) else null
        val entity = NoteEntity(
            id = id, title = title.trim(), content = content, isPinned = pinned, folderId = folderId,
            createdAt = existing?.createdAt ?: now, updatedAt = now,
            contentFormatVersion = com.mono.app.logic.RichContent.VERSION,
        )
        val r = notes.upsert(entity)
        return if (id == 0L) r else id
    }

    suspend fun setNotePinned(id: Long, pinned: Boolean) = notes.setPinned(id, pinned)
    suspend fun deleteNote(id: Long) = notes.delete(id)

    // ---- Folders
    fun observeFolders(): Flow<List<FolderEntity>> = folders.observeAll()

    suspend fun createFolder(name: String): FolderResult {
        val n = name.trim()
        if (n.isEmpty()) return FolderResult.Blank
        if (folders.countByName(n, 0) > 0) return FolderResult.Duplicate
        val now = clock()
        return if (folders.insert(FolderEntity(name = n, createdAt = now, updatedAt = now)) == -1L)
            FolderResult.Duplicate else FolderResult.Ok
    }

    suspend fun renameFolder(id: Long, name: String): FolderResult {
        val n = name.trim()
        if (n.isEmpty()) return FolderResult.Blank
        if (folders.countByName(n, id) > 0) return FolderResult.Duplicate
        folders.rename(id, n, clock())
        return FolderResult.Ok
    }

    /** Notes in the folder are kept and become folder-less (FK SET_NULL). */
    suspend fun deleteFolder(id: Long) = folders.delete(id)

    // ---- Countdowns
    fun observeCountdowns(): Flow<List<CountdownEntity>> = countdowns.observeAll()

    suspend fun saveCountdown(d: CountdownDraft): Long {
        val title = d.title.trim()
        val date = d.targetDate
        require(title.isNotEmpty() && date != null) { "Countdown needs a title and a date" }
        val now = clock()
        val r = countdowns.upsert(CountdownEntity(d.id, title, date, now, now))
        return if (d.id == 0L) r else d.id
    }

    suspend fun deleteCountdown(id: Long) = countdowns.delete(id)

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
