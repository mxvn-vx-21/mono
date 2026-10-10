package com.mono.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mono.app.data.TaskEntity
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.util.Dates

/** Shared by Today and Done. [trailing] is an optional extra control (e.g. a menu). */
@Composable
fun TaskRow(
    task: TaskEntity,
    today: Long,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor by animateColorAsState(if (task.isCompleted) MonoColors.Muted else MonoColors.Ink, tween(MonoMotion.Normal), label = "title")
    val overdue = !task.isCompleted && task.dueDate != null && task.dueDate < today
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircleCheck(task.isCompleted, onToggle, (if (task.isCompleted) "Mark incomplete: " else "Mark complete: ") + task.title)
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    task.title, style = MaterialTheme.typography.titleSmall, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
                val due = Dates.dueLabel(task.dueDate, task.dueTimeMinutes, today)
                Text(
                    (if (overdue) "Overdue · " else "") + due, style = MaterialTheme.typography.bodyMedium,
                    color = if (overdue) MonoColors.Ink else MonoColors.Muted,
                )
            }
            if (!task.isCompleted) PriorityBars(task.priority, Modifier.padding(horizontal = 8.dp))
            trailing?.invoke()
        }
        HorizontalDivider(color = MonoColors.Divider)
    }
}
