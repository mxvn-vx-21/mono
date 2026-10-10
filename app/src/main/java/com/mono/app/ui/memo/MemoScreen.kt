@file:OptIn(ExperimentalFoundationApi::class)

package com.mono.app.ui.memo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.app.data.FolderEntity
import com.mono.app.data.FolderResult
import com.mono.app.data.NoteEntity
import com.mono.app.logic.Block
import com.mono.app.logic.BlockType
import com.mono.app.logic.RichContent
import com.mono.app.ui.NoteViewModel
import com.mono.app.ui.components.EmptyState
import com.mono.app.ui.components.MonoCard
import com.mono.app.ui.components.MonoChip
import com.mono.app.ui.components.MonoIconButton
import com.mono.app.ui.components.MonoTextField
import com.mono.app.ui.components.ScreenHeader
import com.mono.app.ui.navigation.NavBarClearance
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.ui.theme.NoteBodyStyle
import com.mono.app.util.Dates
import kotlinx.coroutines.launch

private sealed interface FolderDialog {
    data object Create : FolderDialog
    data class Rename(val folder: FolderEntity) : FolderDialog
    data class Delete(val folder: FolderEntity) : FolderDialog
}

@Composable
fun MemoScreen(vm: NoteViewModel, onOpen: (Long) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.folderFilter.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }
    var deleteNote by remember { mutableStateOf<NoteEntity?>(null) }
    val bottom = NavBarClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val selectedFolder = folders.firstOrNull { it.id == filter }

    Column(Modifier.statusBarsPadding().padding(horizontal = MonoSpace.ScreenH)) {
        ScreenHeader("Memo", "Capture your thoughts.") { MonoIconButton(Icons.Outlined.Add, "New note", { onOpen(0) }) }
        SearchField(query) { vm.query.value = it }
        Row(
            Modifier.padding(vertical = MonoSpace.Sm).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm),
        ) {
            MonoChip("All", filter == null, { vm.folderFilter.value = null })
            folders.forEach { f -> MonoChip(f.name, filter == f.id, { vm.folderFilter.value = f.id }) }
            MonoChip("+ Folder", false, { dialog = FolderDialog.Create })
        }
        if (selectedFolder != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(MonoSpace.Md), modifier = Modifier.padding(bottom = MonoSpace.Sm)) {
                Text("RENAME FOLDER", style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickableText { dialog = FolderDialog.Rename(selectedFolder) })
                Text("DELETE FOLDER", style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickableText { dialog = FolderDialog.Delete(selectedFolder) })
            }
        }

        val data = ui
        if (data != null && data.notes.isEmpty()) {
            if (data.totalCount == 0) {
                EmptyState("No notes yet", "Capture an idea, a list or a thought. It stays on this device.", actionLabel = "New note", onAction = { onOpen(0) })
            } else if (query.isNotBlank()) {
                EmptyState("No matches", "Nothing found for \"${query.trim()}\".")
            } else {
                EmptyState("This folder is empty", "Open a note and choose this folder to file it here.")
            }
        } else if (data != null) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalItemSpacing = 12.dp,
            ) {
                items(data.notes, key = { it.id }) { note ->
                    NoteCard(
                        note, folders.firstOrNull { it.id == note.folderId }?.name,
                        onOpen = { onOpen(note.id) },
                        onTogglePin = { vm.setPinned(note, !note.isPinned) },
                        onDelete = { deleteNote = note },
                    )
                }
            }
        }
    }

    dialog?.let { d ->
        when (d) {
            is FolderDialog.Create -> FolderNameDialog("New folder", "", { dialog = null }) { name -> vm.createFolder(name) }
            is FolderDialog.Rename -> FolderNameDialog("Rename folder", d.folder.name, { dialog = null }) { name -> vm.renameFolder(d.folder.id, name) }
            is FolderDialog.Delete -> AlertDialog(
                onDismissRequest = { dialog = null }, containerColor = MonoColors.Surface,
                title = { Text("Delete \"${d.folder.name}\"?") },
                text = { Text("Notes in this folder are kept. They'll simply have no folder.") },
                confirmButton = { TextButton(onClick = { vm.deleteFolder(d.folder.id); dialog = null }) { Text("Delete", color = MonoColors.Ink) } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel", color = MonoColors.Muted) } },
            )
        }
    }
    deleteNote?.let { n ->
        AlertDialog(
            onDismissRequest = { deleteNote = null }, containerColor = MonoColors.Surface,
            title = { Text("Delete this note?") }, text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.delete(n.id); deleteNote = null }) { Text("Delete", color = MonoColors.Ink) } },
            dismissButton = { TextButton(onClick = { deleteNote = null }) { Text("Cancel", color = MonoColors.Muted) } },
        )
    }
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick)).padding(vertical = 6.dp)

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = query, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MonoColors.Ink), cursorBrush = SolidColor(MonoColors.Ink),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(shape).background(MonoColors.Surface).border(1.dp, MonoColors.Border, shape).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = MonoColors.Muted)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search notes", style = MaterialTheme.typography.bodyLarge, color = MonoColors.Muted)
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(Icons.Outlined.Close, "Clear search", Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).clickable { onChange("") })
                }
            }
        },
    )
}

@Composable
private fun NoteCard(note: NoteEntity, folderName: String?, onOpen: () -> Unit, onTogglePin: () -> Unit, onDelete: () -> Unit) {
    val preview = remember(note.content) { RichContent.preview(note.content, 5) }
    var menu by remember { mutableStateOf(false) }
    MonoCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(start = 14.dp, top = 8.dp, end = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (note.title.isBlank()) MonoColors.Muted else MonoColors.Ink,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
                AnimatedVisibility(note.isPinned, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    Icon(Icons.Filled.Star, "Pinned", Modifier.padding(top = 12.dp).size(16.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoreVert, "Note options", Modifier.size(20.dp))
                    }
                    DropdownMenu(menu, { menu = false }, containerColor = MonoColors.Surface) {
                        DropdownMenuItem(text = { Text(if (note.isPinned) "Unpin" else "Pin") }, onClick = { menu = false; onTogglePin() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Column(Modifier.padding(end = 10.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                preview.forEachIndexed { i, b -> PreviewLine(b, preview, i) }
            }
            Spacer(Modifier.height(10.dp))
            Text(Dates.formatStamp(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted)
            if (folderName != null) {
                Text(folderName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PreviewLine(b: Block, all: List<Block>, index: Int) {
    val done = b.type == BlockType.Check && b.checked
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        when (b.type) {
            BlockType.Bullet -> Text("•", style = NoteBodyStyle.copy(fontSize = 13.sp))
            BlockType.Numbered -> {
                var n = 1; var i = index - 1
                while (i >= 0 && all[i].type == BlockType.Numbered) { n++; i-- }
                Text("$n.", style = NoteBodyStyle.copy(fontSize = 13.sp))
            }
            BlockType.Check -> Box(
                Modifier.padding(top = 2.dp).size(13.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (b.checked) MonoColors.Ink else MonoColors.Surface).border(1.2.dp, MonoColors.Ink, RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) { if (b.checked) Icon(Icons.Outlined.Check, null, Modifier.size(10.dp), tint = MonoColors.Surface) }
            BlockType.Paragraph -> {}
        }
        Text(
            b.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = NoteBodyStyle.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (b.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (b.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MonoColors.Muted else MonoColors.Ink.copy(alpha = 0.75f),
            ),
        )
    }
}

@Composable
private fun FolderNameDialog(title: String, initial: String, onDismiss: () -> Unit, submit: suspend (String) -> FolderResult) {
    var name by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MonoColors.Surface,
        title = { Text(title) },
        text = {
            Column {
                MonoTextField(name, { name = it; error = null }, "Folder name", isError = error != null)
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    when (submit(name)) {
                        FolderResult.Ok -> onDismiss()
                        FolderResult.Blank -> error = "Please enter a name."
                        FolderResult.Duplicate -> error = "A folder with that name already exists."
                    }
                }
            }) { Text("Save", color = MonoColors.Ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MonoColors.Muted) } },
    )
}
