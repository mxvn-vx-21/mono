package com.mono.app.logic

import com.mono.app.data.NoteEntity

object NoteLogic {
    /** folderId == null means "all folders". Pinned first, then most recently updated. */
    fun filter(notes: List<NoteEntity>, folderId: Long?, query: String): List<NoteEntity> {
        val q = query.trim()
        return notes
            .filter { folderId == null || it.folderId == folderId }
            .filter { q.isEmpty() || it.title.contains(q, true) || RichContent.plainText(it.content).contains(q, true) }
            .sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
    }
}
