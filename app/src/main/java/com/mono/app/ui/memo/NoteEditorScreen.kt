package com.mono.app.ui.memo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import com.mono.app.logic.BlockType
import com.mono.app.ui.NoteViewModel
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.ui.theme.NoteBodyStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NoteEditorScreen(noteId: Long, vm: NoteViewModel, onClose: () -> Unit) {
    val state = remember { NoteEditorState() }
    var ready by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf(false) }
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(noteId) {
        state.load(if (noteId != 0L) vm.getNote(noteId) else null)
        ready = true
    }
    // Debounced autosave: every change is written ~0.6s after you stop typing.
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { state.snapshot() }.drop(1).collectLatest { delay(600); vm.save(state) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (ready) vm.saveInBackground(state) }

    fun saveAndClose() {
        scope.launch {
            // Only leave once the save succeeded, so a failed save never silently drops text.
            if (!ready || vm.save(state)) onClose()
        }
    }
    BackHandler { saveAndClose() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::saveAndClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Save and go back") }
            Box(Modifier.weight(1f)) {
                val folderName = folders.firstOrNull { it.id == state.folderId }?.name ?: "No folder"
                Text(
                    folderName.uppercase(), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, MonoColors.Border, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { folderMenu = true }.padding(horizontal = 14.dp, vertical = 8.dp),
                    maxLines = 1,
                )
                DropdownMenu(folderMenu, { folderMenu = false }, containerColor = MonoColors.Surface) {
                    DropdownMenuItem(text = { Text("No folder") }, onClick = { state.folderId = null; folderMenu = false })
                    folders.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { state.folderId = f.id; folderMenu = false }) }
                }
            }
            IconButton(onClick = { state.pinned = !state.pinned }) {
                Icon(
                    if (state.pinned) Icons.Filled.Star else Icons.Outlined.Star,
                    if (state.pinned) "Unpin note" else "Pin note",
                )
            }
            if (state.id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete note") }
        }

        if (!ready) Box(Modifier.weight(1f)) else LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = MonoSpace.ScreenH, vertical = MonoSpace.Sm),
        ) {
            item(key = "title") {
                BasicTextField(
                    value = state.title, onValueChange = { state.title = it.replace("\n", " ") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = MonoSpace.Sm),
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = MonoColors.Ink),
                    cursorBrush = SolidColor(MonoColors.Ink),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box { if (state.title.isEmpty()) Text("Title", style = MaterialTheme.typography.displayMedium, color = MonoColors.Border); inner() }
                    },
                )
            }
            itemsIndexed(state.blocks, key = { _, b -> b.key }) { index, block ->
                BlockRow(state, block, index)
            }
            item(key = "tail") {
                Spacer(
                    Modifier.fillMaxWidth().height(240.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.focusEnd() }
                )
            }
        }

        FormatBar(state)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, containerColor = MonoColors.Surface,
            title = { Text("Delete this note?") }, text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(state.id) { onClose() } }) { Text("Delete", color = MonoColors.Ink) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = MonoColors.Muted) } },
        )
    }
}

@Composable
private fun BlockRow(state: NoteEditorState, block: BlockState, index: Int) {
    LaunchedEffect(state.pendingFocus) {
        if (state.pendingFocus == block.key) {
            runCatching { block.requester.requestFocus() }
            state.pendingFocus = null
        }
    }
    val checkedLook = block.type == BlockType.Check && block.checked
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        when (block.type) {
            BlockType.Bullet -> Text("•", style = NoteBodyStyle, modifier = Modifier.width(24.dp).padding(start = 6.dp))
            BlockType.Numbered -> Text("${state.numberOf(index)}.", style = NoteBodyStyle, modifier = Modifier.width(30.dp))
            BlockType.Check -> Box(
                Modifier.padding(top = 2.dp, end = 10.dp).size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (block.checked) MonoColors.Ink else MonoColors.Surface)
                    .border(1.5.dp, MonoColors.Ink, RoundedCornerShape(7.dp))
                    .clickable(role = Role.Checkbox) { block.checked = !block.checked }
                    .semantics { contentDescription = if (block.checked) "Checked" else "Unchecked" },
                contentAlignment = Alignment.Center,
            ) { if (block.checked) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = MonoColors.Surface) }
            BlockType.Paragraph -> {}
        }
        BasicTextField(
            value = block.value, onValueChange = { state.onTextChange(block, it) },
            modifier = Modifier.weight(1f)
                .focusRequester(block.requester)
                .onFocusChanged { if (it.isFocused) state.focusKey = block.key }
                .onPreviewKeyEvent { e ->
                    e.type == KeyEventType.KeyDown && e.key == Key.Backspace &&
                        block.value.selection.collapsed && block.value.selection.start == 0 &&
                        state.backspaceAtStart(block)
                },
            textStyle = NoteBodyStyle.copy(
                color = if (checkedLook) MonoColors.Muted else MonoColors.Ink,
                fontWeight = if (block.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (block.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (checkedLook) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(MonoColors.Ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box(Modifier.padding(vertical = 2.dp)) {
                    if (index == 0 && state.blocks.size == 1 && block.value.text.isEmpty() && block.type == BlockType.Paragraph) {
                        Text("Start writing…", style = NoteBodyStyle, color = MonoColors.Muted)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun FormatBar(state: NoteEditorState) {
    val f = state.focusedBlock()
    Row(
        Modifier.fillMaxWidth().background(MonoColors.Surface).border(1.dp, MonoColors.Border)
            .padding(horizontal = MonoSpace.Sm, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        FormatButton("B", "Bold", f?.bold == true, bold = true) { state.toggleBold() }
        FormatButton("I", "Italic", f?.italic == true, italic = true) { state.toggleItalic() }
        FormatButton("•", "Bullet list", f?.type == BlockType.Bullet) { state.toggleType(BlockType.Bullet) }
        FormatButton("1.", "Numbered list", f?.type == BlockType.Numbered) { state.toggleType(BlockType.Numbered) }
        FormatButton("☑", "Checklist", f?.type == BlockType.Check) { state.toggleType(BlockType.Check) }
    }
}

@Composable
private fun FormatButton(label: String, description: String, active: Boolean, bold: Boolean = false, italic: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(if (active) MonoColors.Ink else MonoColors.Surface)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = NoteBodyStyle.copy(
                fontSize = 18.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            ),
            color = if (active) MonoColors.Surface else MonoColors.Ink,
        )
    }
}
