package com.mono.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks") fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun getById(id: Long): TaskEntity?
    @Upsert suspend fun upsert(task: TaskEntity): Long
    @Query("UPDATE tasks SET isCompleted = :done, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean, now: Long)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC") fun observeAll(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun getById(id: Long): NoteEntity?
    @Upsert suspend fun upsert(note: NoteEntity): Long
    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id") suspend fun setPinned(id: Long, pinned: Boolean)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<FolderEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(folder: FolderEntity): Long
    @Query("SELECT COUNT(*) FROM folders WHERE name = :name AND id != :exceptId")
    suspend fun countByName(name: String, exceptId: Long): Int
    @Query("UPDATE folders SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)
    @Query("DELETE FROM folders WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetDate") fun observeAll(): Flow<List<CountdownEntity>>
    @Upsert suspend fun upsert(c: CountdownEntity): Long
    @Query("DELETE FROM countdowns WHERE id = :id") suspend fun delete(id: Long)
}
