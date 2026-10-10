package com.mono.app.ui.memo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mono.app.data.NoteEntity
import com.mono.app.logic.Block
import com.mono.app.logic.BlockType
import com.mono.app.logic.RichContent

/** One editable line. Each block owns its own text field so structure is never lost. */
class BlockState(val key: Long, type: BlockType, text: String, checked: Boolean, bold: Boolean, italic: Boolean) {
    var type by mutableStateOf(type)
    var value by mutableStateOf(TextFieldValue(text))
    var checked by mutableStateOf(checked)
    var bold by mutableStateOf(bold)
    var italic by mutableStateOf(italic)
    val requester = FocusRequester()
    fun toBlock() = Block(type, value.text, checked, bold, italic)
}

data class EditorSnapshot(val title: String, val blocks: List<Block>, val pinned: Boolean, val folderId: Long?)

class NoteEditorState {
    var id by mutableLongStateOf(0L)
    var title by mutableStateOf("")
    var pinned by mutableStateOf(false)
    var folderId by mutableStateOf<Long?>(null)
    val blocks = mutableStateListOf<BlockState>()
    var focusKey by mutableStateOf<Long?>(null)
    var pendingFocus by mutableStateOf<Long?>(null)
    private var nextKey = 1L
    /** Last successfully persisted snapshot; lets us skip no-op saves (which would bump updatedAt). */
    var lastSaved: EditorSnapshot? = null

    private fun make(b: Block = Block()) = BlockState(nextKey++, b.type, b.text, b.checked, b.bold, b.italic)

    fun load(note: NoteEntity?) {
        blocks.clear()
        if (note != null) {
            id = note.id; title = note.title; pinned = note.isPinned; folderId = note.folderId
            RichContent.decode(note.content).forEach { blocks.add(make(it)) }
        } else blocks.add(make())
        lastSaved = snapshot()
    }

    fun snapshotBlocks(): List<Block> = blocks.map { it.toBlock() }
    fun snapshot() = EditorSnapshot(title, snapshotBlocks(), pinned, folderId)
    fun isBlank() = title.isBlank() && RichContent.isBlank(snapshotBlocks())

    private fun focused(): BlockState? = blocks.firstOrNull { it.key == focusKey } ?: blocks.lastOrNull()

    fun toggleType(t: BlockType) { focused()?.let { it.type = if (it.type == t) BlockType.Paragraph else t } }
    fun toggleBold() { focused()?.let { it.bold = !it.bold } }
    fun toggleItalic() { focused()?.let { it.italic = !it.italic } }
    fun focusedBlock() = focused()

    /** Handles typing, Enter (splits into a new block), and pasted multi-line text. */
    fun onTextChange(block: BlockState, nv: TextFieldValue) {
        if (!nv.text.contains('\n')) { block.value = nv; return }
        val parts = nv.text.split('\n')
        val idx = blocks.indexOf(block)
        if (idx < 0) return
        // Enter on an empty list item exits the list.
        if (parts.size == 2 && parts[0].isEmpty() && parts[1].isEmpty() && block.type != BlockType.Paragraph) {
            block.type = BlockType.Paragraph; block.value = TextFieldValue(""); return
        }
        block.value = TextFieldValue(parts[0], TextRange(parts[0].length))
        var last: BlockState = block
        parts.drop(1).forEachIndexed { i, text ->
            val nb = make(Block(block.type, text, false, block.bold, block.italic))
            blocks.add(idx + 1 + i, nb)
            last = nb
        }
        val lastText = parts.last()
        last.value = TextFieldValue(lastText, TextRange(if (parts.size == 2) 0 else lastText.length))
        pendingFocus = last.key
    }

    /** Backspace at the start of a block: drop list formatting first, then merge into previous. */
    fun backspaceAtStart(block: BlockState): Boolean {
        if (block.type != BlockType.Paragraph) { block.type = BlockType.Paragraph; return true }
        val idx = blocks.indexOf(block)
        if (idx <= 0) return false
        val prev = blocks[idx - 1]
        val junction = prev.value.text.length
        prev.value = TextFieldValue(prev.value.text + block.value.text, TextRange(junction))
        blocks.removeAt(idx)
        pendingFocus = prev.key
        return true
    }

    fun focusEnd() {
        val lastBlock = blocks.lastOrNull()
        if (lastBlock != null && lastBlock.value.text.isEmpty() && lastBlock.type == BlockType.Paragraph) pendingFocus = lastBlock.key
        else { val nb = make(); blocks.add(nb); pendingFocus = nb.key }
    }

    fun numberOf(index: Int): Int {
        var n = 1; var i = index - 1
        while (i >= 0 && blocks[i].type == BlockType.Numbered) { n++; i-- }
        return n
    }
}
