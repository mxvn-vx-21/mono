@file:OptIn(ExperimentalMaterial3Api::class)

package com.mono.app.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mono.app.data.CountdownDraft
import com.mono.app.data.CountdownEntity
import com.mono.app.data.Priority
import com.mono.app.data.TaskDraft
import com.mono.app.data.TaskEntity
import com.mono.app.ui.components.CircleCheck
import com.mono.app.ui.components.MonoButton
import com.mono.app.ui.components.MonoChip
import com.mono.app.ui.components.MonoTextField
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoRadius
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.util.Dates
import kotlinx.coroutines.launch

@Composable
private fun MonoSheet(onDismiss: () -> Unit, content: @Composable (sheetState: SheetState, close: (() -> Unit) -> Unit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MonoColors.Surface, shape = RoundedCornerShape(topStart = MonoRadius.Sheet, topEnd = MonoRadius.Sheet),
    ) {
        content(sheetState) { after ->
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); after() }
        }
    }
}

@Composable
private fun DateChip(label: String, hasValue: Boolean, onClick: () -> Unit, onClear: (() -> Unit)?) {
    val shape = RoundedCornerShape(MonoRadius.Chip)
    Row(
        Modifier.clip(shape).background(MonoColors.SurfaceAlt).border(1.dp, MonoColors.Border, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(start = 14.dp, end = if (hasValue && onClear != null) 6.dp else 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.DateRange, null, Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (hasValue) MonoColors.Ink else MonoColors.Muted)
        if (hasValue && onClear != null) {
            Icon(Icons.Outlined.Close, "Clear", Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClear).padding(6.dp))
        }
    }
}

@Composable
fun MonoDatePickerDialog(initialEpochDay: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay?.let(Dates::epochDayToPickerMillis))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { onConfirm(Dates.pickerMillisToEpochDay(it)) }
            }) { Text("OK", color = MonoColors.Ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MonoColors.Muted) } },
    ) { DatePicker(state) }
}

@Composable
private fun MonoTimePickerDialog(initialMinutes: Int?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState((initialMinutes ?: 9 * 60) / 60, (initialMinutes ?: 0) % 60, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MonoColors.Surface,
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK", color = MonoColors.Ink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MonoColors.Muted) } },
    )
}

@Composable
fun TaskSheet(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskDraft, onSaved: () -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(task?.description.orEmpty()) }
    var dueDate by rememberSaveable { mutableStateOf(task?.dueDate) }
    var dueTime by rememberSaveable { mutableStateOf(task?.dueTimeMinutes) }
    var priority by rememberSaveable { mutableStateOf(task?.priority ?: Priority.Medium.level) }
    var completed by rememberSaveable { mutableStateOf(task?.isCompleted ?: false) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    MonoSheet(onDismiss) { _, close ->
        Column(
            Modifier.fillMaxWidth().padding(horizontal = MonoSpace.ScreenH).padding(bottom = MonoSpace.Lg).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(MonoSpace.Sm),
        ) {
            Text((if (task == null) "Add task" else "Edit task").uppercase(), style = MaterialTheme.typography.headlineSmall)
            MonoTextField(title, { title = it; if (it.isNotBlank()) showError = false }, "Task title", isError = showError)
            if (showError) Text("Please give your task a title.", style = MaterialTheme.typography.bodyMedium, color = MonoColors.Ink)
            MonoTextField(description, { description = it }, "Description (optional)", singleLine = false, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm)) {
                DateChip(dueDate?.let { Dates.formatDay(it) } ?: "Due date", dueDate != null, { showDate = true }, { dueDate = null; dueTime = null })
                if (dueDate != null) {
                    DateChip(dueTime?.let { Dates.formatTime(it) } ?: "Time", dueTime != null, { showTime = true }, { dueTime = null })
                }
            }
            Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm)) {
                Priority.values().forEach { p -> MonoChip(p.label, priority == p.level, { priority = p.level }) }
            }
            if (task != null) {
                Row(Modifier.clickable { completed = !completed }, verticalAlignment = Alignment.CenterVertically) {
                    CircleCheck(completed, { completed = it }, "Completed")
                    Text("Completed", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(4.dp))
            MonoButton(if (task == null) "Create" else "Save", {
                if (title.isBlank()) showError = true else {
                    onSave(TaskDraft(task?.id ?: 0, title, description, dueDate, dueTime, Priority.fromLevel(priority), completed)) { close {} }
                }
            }, Modifier.fillMaxWidth())
            if (onDelete != null) {
                MonoButton("Delete task", { confirmDelete = true }, Modifier.fillMaxWidth(), filled = false)
            }
        }
        if (showDate) MonoDatePickerDialog(dueDate, { showDate = false }) { dueDate = it; showDate = false }
        if (showTime) MonoTimePickerDialog(dueTime, { showTime = false }) { dueTime = it; showTime = false }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false }, containerColor = MonoColors.Surface,
                title = { Text("Delete this task?") }, text = { Text("This can't be undone.") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke(); close {} }) { Text("Delete", color = MonoColors.Ink) } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = MonoColors.Muted) } },
            )
        }
    }
}

@Composable
fun CountdownSheet(
    item: CountdownEntity?,
    onDismiss: () -> Unit,
    onSave: (CountdownDraft, onSaved: () -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(item?.title.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(item?.targetDate) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    MonoSheet(onDismiss) { _, close ->
        Column(
            Modifier.fillMaxWidth().padding(horizontal = MonoSpace.ScreenH).padding(bottom = MonoSpace.Lg).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(MonoSpace.Sm),
        ) {
            Text((if (item == null) "Add countdown" else "Edit countdown").uppercase(), style = MaterialTheme.typography.headlineSmall)
            MonoTextField(title, { title = it; if (it.isNotBlank()) showError = false }, "Event title", isError = showError && title.isBlank())
            DateChip(date?.let { Dates.formatDay(it) } ?: "Select date", date != null, { showDate = true }, null)
            if (showError) Text("Add a title and pick a date.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            MonoButton(if (item == null) "Create" else "Save", {
                if (title.isBlank() || date == null) showError = true
                else onSave(CountdownDraft(item?.id ?: 0, title, date)) { close {} }
            }, Modifier.fillMaxWidth())
            if (onDelete != null) MonoButton("Delete countdown", { confirmDelete = true }, Modifier.fillMaxWidth(), filled = false)
        }
        if (showDate) MonoDatePickerDialog(date, { showDate = false }) { date = it; showDate = false; showError = false }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false }, containerColor = MonoColors.Surface,
                title = { Text("Delete this countdown?") }, text = { Text("This can't be undone.") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke(); close {} }) { Text("Delete", color = MonoColors.Ink) } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = MonoColors.Muted) } },
            )
        }
    }
}
