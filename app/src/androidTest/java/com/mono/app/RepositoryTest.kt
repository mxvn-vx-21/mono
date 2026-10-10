package com.mono.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mono.app.data.CountdownDraft
import com.mono.app.data.FolderResult
import com.mono.app.data.MonoDatabase
import com.mono.app.data.MonoRepository
import com.mono.app.data.Priority
import com.mono.app.data.TaskDraft
import com.mono.app.logic.Block
import com.mono.app.logic.BlockType
import com.mono.app.logic.NoteLogic
import com.mono.app.logic.RichContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests: run on a device/emulator with `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: MonoDatabase
    private lateinit var repo: MonoRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MonoDatabase::class.java).allowMainThreadQueries().build()
        repo = MonoRepository(db)
    }
    @After fun tearDown() = db.close()

    @Test fun taskLifecyclePersistsDueDateAndPriority() = runBlocking {
        val id = repo.saveTask(TaskDraft(title = "  Write  ", dueDate = 20000, dueTimeMinutes = 615, priority = Priority.High))
        var t = repo.observeTasks().first().single()
        assertEquals("Write", t.title); assertEquals(20000L, t.dueDate); assertEquals(615, t.dueTimeMinutes); assertEquals(2, t.priority)
        repo.saveTask(TaskDraft(id = id, title = "Write more", dueDate = null, dueTimeMinutes = 615, priority = Priority.Low))
        t = repo.observeTasks().first().single()
        assertNull(t.dueDate); assertNull(t.dueTimeMinutes); assertEquals(0, t.priority)
        repo.setTaskCompleted(id, true)
        assertTrue(repo.observeTasks().first().single().isCompleted)
        repo.deleteTask(id)
        assertTrue(repo.observeTasks().first().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class) fun blankTaskTitleRejected() = runBlocking<Unit> {
        repo.saveTask(TaskDraft(title = "   "))
    }

    @Test fun notesKeepFormattingChecklistPinAndFolder() = runBlocking {
        val folder = repo.createFolder("College"); assertEquals(FolderResult.Ok, folder)
        val fid = db.folderDao().observeAll().first().single().id
        val blocks = listOf(Block(BlockType.Check, "milk", checked = true), Block(BlockType.Paragraph, "bold", bold = true))
        val id = repo.saveNote(0, "List", RichContent.encode(blocks), pinned = false, folderId = fid)
        repo.setNotePinned(id, true)
        val n = repo.getNote(id)!!
        assertEquals(blocks, RichContent.decode(n.content)); assertTrue(n.isPinned); assertEquals(fid, n.folderId)
        assertEquals(listOf(id), NoteLogic.filter(repo.observeNotes().first(), fid, "milk").map { it.id })
    }

    @Test fun deletingFolderKeepsNotes() = runBlocking {
        repo.createFolder("Temp")
        val fid = db.folderDao().observeAll().first().single().id
        val id = repo.saveNote(0, "Keep me", RichContent.encode(listOf(Block(text = "x"))), false, fid)
        repo.deleteFolder(fid)
        assertNull(repo.getNote(id)!!.folderId)
    }

    @Test fun duplicateFolderNamesRejectedCaseInsensitively() = runBlocking {
        assertEquals(FolderResult.Ok, repo.createFolder("Ideas"))
        assertEquals(FolderResult.Duplicate, repo.createFolder("ideas"))
        assertEquals(FolderResult.Blank, repo.createFolder("  "))
    }

    @Test fun countdownDatePersistsExactly() = runBlocking {
        repo.saveCountdown(CountdownDraft(title = "Exhibition", targetDate = 20742))
        assertEquals(20742L, repo.observeCountdowns().first().single().targetDate)
    }

    @Test fun clearAllWipesEverything() = runBlocking {
        repo.saveTask(TaskDraft(title = "x")); repo.createFolder("f"); repo.saveCountdown(CountdownDraft(title = "c", targetDate = 1))
        repo.clearAll()
        assertTrue(repo.observeTasks().first().isEmpty()); assertTrue(repo.observeCountdowns().first().isEmpty())
        assertFalse(db.folderDao().observeAll().first().isNotEmpty())
    }
}
