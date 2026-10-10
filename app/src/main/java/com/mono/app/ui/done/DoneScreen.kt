package com.mono.app.ui.done

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.app.data.TaskEntity
import com.mono.app.logic.TaskFilter
import com.mono.app.logic.TaskLogic
import com.mono.app.ui.TaskViewModel
import com.mono.app.ui.components.EmptyState
import com.mono.app.ui.components.MonoChip
import com.mono.app.ui.components.MonoIconButton
import com.mono.app.ui.components.ScreenHeader
import com.mono.app.ui.components.TaskRow
import com.mono.app.ui.navigation.NavBarClearance
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.ui.today.rememberNow

@Composable
fun DoneScreen(vm: TaskViewModel, onAddTask: () -> Unit, onEditTask: (TaskEntity) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = rememberNow().value.toLocalDate().toEpochDay()
    var filter by rememberSaveable { mutableStateOf(TaskFilter.All) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<TaskEntity?>(null) }

    val sections = remember(tasks, today, filter) { TaskLogic.sections(tasks.orEmpty(), today, filter) }
    val bottom = NavBarClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.statusBarsPadding().padding(horizontal = MonoSpace.ScreenH)) {
        ScreenHeader("Done", "Get things done.") { MonoIconButton(Icons.Outlined.Add, "Add task", onAddTask) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm)) {
            TaskFilter.values().forEach { f -> MonoChip(f.label, filter == f, { filter = f }) }
        }
        LazyColumn(contentPadding = PaddingValues(top = MonoSpace.Sm, bottom = bottom)) {
            if (tasks != null && sections.isEmpty) {
                item {
                    when (filter) {
                        TaskFilter.All -> EmptyState("No tasks yet", "Add your first task and give your day some shape.", actionLabel = "Add task", onAction = onAddTask)
                        TaskFilter.Today -> EmptyState("Nothing due today", "Enjoy the quiet, or plan something.")
                        TaskFilter.Upcoming -> EmptyState("Nothing upcoming", "Tasks with a future due date appear here.")
                        TaskFilter.Completed -> EmptyState("Nothing completed yet", "Finished tasks will collect here.")
                    }
                }
            }
            if (sections.overdue.isNotEmpty()) {
                item(key = "h-overdue") { Label("Overdue (${sections.overdue.size})") }
                items(sections.overdue, key = { "o${it.id}" }) { t ->
                    TaskRow(t, today, { vm.setCompleted(t, it) }, { onEditTask(t) }, trailing = { TaskMenu({ onEditTask(t) }, { deleteTarget = t }) })
                }
            }
            if (sections.pending.isNotEmpty()) {
                item(key = "h-pending") { Label("Pending (${sections.pending.size})") }
                items(sections.pending, key = { "p${it.id}" }) { t ->
                    TaskRow(t, today, { vm.setCompleted(t, it) }, { onEditTask(t) }, trailing = { TaskMenu({ onEditTask(t) }, { deleteTarget = t }) })
                }
            }
            if (sections.completed.isNotEmpty()) {
                item(key = "h-completed") {
                    val rot by animateFloatAsState(if (showCompleted) 0f else -90f, tween(MonoMotion.Normal), label = "chev")
                    Row(
                        Modifier.fillMaxWidth().clickable { showCompleted = !showCompleted }.padding(top = MonoSpace.Lg, bottom = MonoSpace.Xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("COMPLETED (${sections.completed.size})", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.KeyboardArrowDown, if (showCompleted) "Hide completed" else "Show completed",
                            Modifier.size(28.dp).graphicsLayer { rotationZ = rot },
                        )
                    }
                }
                item(key = "completed-body") {
                    AnimatedVisibility(
                        showCompleted,
                        enter = expandVertically(tween(MonoMotion.Normal)) + fadeIn(tween(MonoMotion.Normal)),
                        exit = shrinkVertically(tween(MonoMotion.Normal)) + fadeOut(tween(MonoMotion.Fast)),
                    ) {
                        Column {
                            sections.completed.forEach { t ->
                                TaskRow(t, today, { vm.setCompleted(t, it) }, { onEditTask(t) }, trailing = { TaskMenu({ onEditTask(t) }, { deleteTarget = t }) })
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null }, containerColor = MonoColors.Surface,
            title = { Text("Delete this task?") }, text = { Text("\"${t.title}\" will be removed permanently.") },
            confirmButton = { TextButton(onClick = { vm.delete(t); deleteTarget = null }) { Text("Delete", color = MonoColors.Ink) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = MonoColors.Muted) } },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = MonoSpace.Lg, bottom = MonoSpace.Xs))
}

@Composable
private fun TaskMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, "Task options", Modifier.size(22.dp)) }
        DropdownMenu(open, { open = false }, containerColor = MonoColors.Surface) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}
