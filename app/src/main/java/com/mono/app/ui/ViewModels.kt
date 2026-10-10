package com.mono.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mono.app.data.AppContainer
import com.mono.app.data.CountdownDraft
import com.mono.app.data.CountdownEntity
import com.mono.app.data.FolderEntity
import com.mono.app.data.FolderResult
import com.mono.app.data.NoteEntity
import com.mono.app.data.TaskDraft
import com.mono.app.data.TaskEntity
import com.mono.app.logic.NoteLogic
import com.mono.app.logic.RichContent
import com.mono.app.ui.memo.NoteEditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Lazily = SharingStarted.WhileSubscribed(5_000)

/** null = still loading (first emission), so empty states don't flash. */
class TaskViewModel(private val c: AppContainer) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>?> = c.repo.observeTasks().stateIn(viewModelScope, Lazily, null)

    fun save(draft: TaskDraft, onDone: () -> Unit) {
        viewModelScope.launch {
            if (c.guarded("Couldn't save the task. Please try again.") { c.repo.saveTask(draft) }) onDone()
        }
    }

    fun setCompleted(task: TaskEntity, done: Boolean) {
        viewModelScope.launch { c.guarded("Couldn't update the task.") { c.repo.setTaskCompleted(task.id, done) } }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch { c.guarded("Couldn't delete the task.") { c.repo.deleteTask(task.id) } }
    }
}

class CountdownViewModel(private val c: AppContainer) : ViewModel() {
    val countdowns: StateFlow<List<CountdownEntity>?> = c.repo.observeCountdowns().stateIn(viewModelScope, Lazily, null)

    fun save(draft: CountdownDraft, onDone: () -> Unit) {
        viewModelScope.launch {
            if (c.guarded("Couldn't save the countdown.") { c.repo.saveCountdown(draft) }) onDone()
        }
    }

    fun delete(item: CountdownEntity) {
        viewModelScope.launch { c.guarded("Couldn't delete the countdown.") { c.repo.deleteCountdown(item.id) } }
    }
}

data class NotesUi(val notes: List<NoteEntity>, val totalCount: Int)

class NoteViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    /** null = all folders */
    val folderFilter = MutableStateFlow<Long?>(null)
    private val saveMutex = Mutex()

    val folders: StateFlow<List<FolderEntity>> =
        c.repo.observeFolders().stateIn(viewModelScope, Lazily, emptyList())

    val ui: StateFlow<NotesUi?> = combine(c.repo.observeNotes(), folderFilter, query) { all, f, q ->
        NotesUi(NoteLogic.filter(all, f, q), all.size)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, Lazily, null)

    suspend fun getNote(id: Long): NoteEntity? = c.repo.getNote(id)

    fun setPinned(note: NoteEntity, pinned: Boolean) {
        viewModelScope.launch { c.guarded("Couldn't update the note.") { c.repo.setNotePinned(note.id, pinned) } }
    }

    fun delete(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (c.guarded("Couldn't delete the note.") { c.repo.deleteNote(id) }) onDone()
        }
    }

    /** Serialised so two quick saves can never create duplicate notes. Returns true on success. */
    suspend fun save(state: NoteEditorState): Boolean = saveMutex.withLock {
        if (state.id == 0L && state.isBlank()) return@withLock true
        val snap = state.snapshot()
        if (snap == state.lastSaved) return@withLock true
        c.guarded("Couldn't save the note. Your text is still on screen.") {
            state.id = c.repo.saveNote(
                state.id, snap.title, RichContent.encode(snap.blocks), snap.pinned, snap.folderId,
            )
            state.lastSaved = snap
        }
    }

    fun saveInBackground(state: NoteEditorState) {
        viewModelScope.launch { save(state) }
    }

    suspend fun createFolder(name: String) = c.repo.createFolder(name)
    suspend fun renameFolder(id: Long, name: String): FolderResult = c.repo.renameFolder(id, name)
    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            if (c.guarded("Couldn't delete the folder.") { c.repo.deleteFolder(id) } && folderFilter.value == id) {
                folderFilter.value = null
            }
        }
    }
}

data class StorageCounts(val tasks: Int, val notes: Int, val countdowns: Int)

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val counts: StateFlow<StorageCounts> = combine(
        c.repo.observeTasks(), c.repo.observeNotes(), c.repo.observeCountdowns(),
    ) { t, n, d -> StorageCounts(t.size, n.size, d.size) }
        .stateIn(viewModelScope, Lazily, StorageCounts(0, 0, 0))

    fun clearAll() {
        viewModelScope.launch {
            if (c.guarded("Couldn't delete your data.") { c.repo.clearAll() }) c.messages.tryEmit("All local data deleted")
        }
    }
}

class MonoViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        TaskViewModel::class.java -> TaskViewModel(c)
        CountdownViewModel::class.java -> CountdownViewModel(c)
        NoteViewModel::class.java -> NoteViewModel(c)
        SettingsViewModel::class.java -> SettingsViewModel(c)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}
