package com.mono.app.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mono.app.R
import com.mono.app.data.CountdownEntity
import com.mono.app.data.TaskEntity
import com.mono.app.logic.DayStats
import com.mono.app.logic.TaskLogic
import com.mono.app.ui.CountdownViewModel
import com.mono.app.ui.TaskViewModel
import com.mono.app.ui.components.EmptyState
import com.mono.app.ui.components.MonoCard
import com.mono.app.ui.components.SectionHeader
import com.mono.app.ui.components.TaskRow
import com.mono.app.ui.navigation.NavBarClearance
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.util.Dates
import com.mono.app.util.UserProfile
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ticks once a second, only while the screen is STARTED (no background updates). */
@Composable
fun rememberNow(): State<LocalDateTime> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now.value = LocalDateTime.now()
                delay(1000L - System.currentTimeMillis() % 1000L)
            }
        }
    }
    return now
}

@Composable
fun TodayScreen(
    taskVm: TaskViewModel,
    countdownVm: CountdownViewModel,
    onAddTask: () -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onAddCountdown: () -> Unit,
    onEditCountdown: (CountdownEntity) -> Unit,
    onSeeAllTasks: () -> Unit,
) {
    val tasks by taskVm.tasks.collectAsStateWithLifecycle()
    val countdowns by countdownVm.countdowns.collectAsStateWithLifecycle()
    val now by rememberNow()
    val today = now.toLocalDate().toEpochDay()
    var showAllCountdowns by rememberSaveable { mutableStateOf(false) }

    val all = tasks
    val todays = remember(all, today) { TaskLogic.todayList(all.orEmpty(), today) }
    val stats = remember(all, today) { TaskLogic.dayStats(all.orEmpty(), today) }
    val ordered = remember(countdowns, today) {
        val (upcoming, past) = countdowns.orEmpty().partition { it.targetDate >= today }
        upcoming.sortedBy { it.targetDate } + past.sortedByDescending { it.targetDate }
    }
    val shownCountdowns = if (showAllCountdowns) ordered else ordered.take(3)
    val bottom = NavBarClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        Modifier.statusBarsPadding().padding(horizontal = MonoSpace.ScreenH),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        item { Header(now) }
        item { DayCard(stats) }
        item {
            SectionHeader("Today's tasks", listOf("Add" to onAddTask, "See all" to onSeeAllTasks))
        }
        if (all != null && todays.isEmpty()) {
            item {
                EmptyState(
                    "Nothing planned", "Tasks due today show up here. Add one to start your day.",
                    actionLabel = "Add task", onAction = onAddTask,
                )
            }
        }
        items(todays, key = { "t${it.id}" }) { task ->
            TaskRow(task, today, onToggle = { taskVm.setCompleted(task, it) }, onClick = { onEditTask(task) })
        }
        item {
            SectionHeader(
                "Countdowns",
                buildList<Pair<String, () -> Unit>> {
                    add("Add" to onAddCountdown)
                    if (ordered.size > 3) add((if (showAllCountdowns) "Less" else "All ${ordered.size}") to { showAllCountdowns = !showAllCountdowns })
                },
            )
        }
        if (countdowns != null && ordered.isEmpty()) {
            item {
                EmptyState(
                    "No countdowns yet", "Track a birthday, a deadline or any date worth looking forward to.",
                    actionLabel = "Add countdown", onAction = onAddCountdown,
                )
            }
        }
        items(shownCountdowns, key = { "c${it.id}" }) { c ->
            CountdownRow(c, today) { onEditCountdown(c) }
            Spacer(Modifier.height(MonoSpace.Sm))
        }
    }
}

@Composable
private fun Header(now: LocalDateTime) {
    val clock = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    val seconds = now.format(DateTimeFormatter.ofPattern(":ss", Locale.getDefault()))
    val date = now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
    Row(Modifier.fillMaxWidth().padding(top = MonoSpace.Md, bottom = MonoSpace.Md), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("MONO", style = MaterialTheme.typography.displayMedium)
            Text(Dates.greeting(now.hour) + ", " + UserProfile.NAME + ".", style = MaterialTheme.typography.labelMedium, color = MonoColors.Muted)
            Spacer(Modifier.height(MonoSpace.Md))
            Text(date.uppercase(), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(clock, style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { contentDescription = "Time $clock" })
                Text(seconds, style = MaterialTheme.typography.titleMedium, color = MonoColors.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            }
        }
        Image(
            painterResource(R.drawable.logo), contentDescription = "MONO mascot",
            modifier = Modifier.size(84.dp), contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun DayCard(stats: DayStats) {
    val progress by animateFloatAsState(stats.progress, tween(MonoMotion.Slow), label = "progress")
    MonoCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MonoSpace.Md)) {
            Text("YOUR DAY", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.padding(top = MonoSpace.Sm), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(72.dp)) {
                        val w = 7.dp.toPx()
                        val inset = w / 2
                        val arcSize = Size(size.width - w, size.height - w)
                        drawArc(MonoColors.Border, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
                        drawArc(MonoColors.Ink, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
                    }
                    Text("${stats.percent}%", style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.padding(start = MonoSpace.Md)) {
                    Text("${stats.done} / ${stats.total}", style = MaterialTheme.typography.displayMedium)
                    Text(
                        if (stats.total == 0) "NO TASKS TODAY" else "TASKS COMPLETED",
                        style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted,
                    )
                }
            }
            Box(Modifier.padding(top = MonoSpace.Md).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MonoColors.Border)) {
                Box(Modifier.fillMaxWidth(progress).height(4.dp).background(MonoColors.Ink))
            }
        }
    }
}

@Composable
private fun CountdownRow(c: CountdownEntity, today: Long, onClick: () -> Unit) {
    val days = Dates.daysUntil(c.targetDate, today)
    val label = when {
        days == 0L -> "TODAY"
        days == 1L -> "1 DAY"
        days > 1 -> "$days DAYS"
        days == -1L -> "1 DAY AGO"
        else -> "${-days} DAYS AGO"
    }
    MonoCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(MonoSpace.Md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MonoSpace.Md)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MonoColors.SurfaceAlt), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DateRange, null, Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (days < 0) MonoColors.Muted else MonoColors.Ink)
                    Text(Dates.formatDay(c.targetDate).uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted)
                }
            }
        }
    }
}
