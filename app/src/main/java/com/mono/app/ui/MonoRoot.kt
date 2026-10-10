package com.mono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mono.app.data.AppContainer
import com.mono.app.data.CountdownEntity
import com.mono.app.data.TaskEntity
import com.mono.app.ui.components.MonoCard
import com.mono.app.ui.done.DoneScreen
import com.mono.app.ui.memo.MemoScreen
import com.mono.app.ui.memo.NoteEditorScreen
import com.mono.app.ui.navigation.Dest
import com.mono.app.ui.navigation.FloatingNavBar
import com.mono.app.ui.settings.SettingsScreen
import com.mono.app.ui.sheets.CountdownSheet
import com.mono.app.ui.sheets.TaskSheet
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.ui.theme.MonoSpace
import com.mono.app.ui.today.TodayScreen

private const val EDITOR = "editor/{id}"
private data class TaskSheetRequest(val task: TaskEntity?)
private data class CountdownSheetRequest(val item: CountdownEntity?)

@Composable
fun MonoRoot(container: AppContainer) {
    val factory = remember { MonoViewModelFactory(container) }
    val taskVm: TaskViewModel = viewModel(factory = factory)
    val countdownVm: CountdownViewModel = viewModel(factory = factory)
    val noteVm: NoteViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)

    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val inEditor = route == EDITOR

    var quickAdd by rememberSaveable { mutableStateOf(false) }
    var taskSheet by remember { mutableStateOf<TaskSheetRequest?>(null) }
    var countdownSheet by remember { mutableStateOf<CountdownSheetRequest?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(container) { container.messages.collect { snackbar.showSnackbar(it) } }

    fun goTo(d: Dest) {
        quickAdd = false
        nav.navigate(d.route) {
            popUpTo(Dest.Today.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun openEditor(id: Long) { quickAdd = false; nav.navigate("editor/$id") { launchSingleTop = true } }

    Surface(Modifier.fillMaxSize(), color = MonoColors.Background, contentColor = MonoColors.Ink) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                nav, startDestination = Dest.Today.route,
                enterTransition = { fadeIn(tween(MonoMotion.Normal)) },
                exitTransition = { fadeOut(tween(MonoMotion.Fast)) },
                popEnterTransition = { fadeIn(tween(MonoMotion.Normal)) },
                popExitTransition = { fadeOut(tween(MonoMotion.Fast)) },
            ) {
                composable(Dest.Today.route) {
                    TodayScreen(
                        taskVm, countdownVm,
                        onAddTask = { taskSheet = TaskSheetRequest(null) },
                        onEditTask = { taskSheet = TaskSheetRequest(it) },
                        onAddCountdown = { countdownSheet = CountdownSheetRequest(null) },
                        onEditCountdown = { countdownSheet = CountdownSheetRequest(it) },
                        onSeeAllTasks = { goTo(Dest.Done) },
                    )
                }
                composable(Dest.Done.route) {
                    DoneScreen(taskVm, onAddTask = { taskSheet = TaskSheetRequest(null) }, onEditTask = { taskSheet = TaskSheetRequest(it) })
                }
                composable(Dest.Memo.route) { MemoScreen(noteVm, onOpen = ::openEditor) }
                composable(Dest.Settings.route) { SettingsScreen(settingsVm) }
                composable(
                    EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    enterTransition = { slideInVertically(tween(MonoMotion.Normal)) { it / 14 } + fadeIn(tween(MonoMotion.Normal)) },
                    exitTransition = { fadeOut(tween(MonoMotion.Fast)) },
                    popExitTransition = { slideOutVertically(tween(MonoMotion.Normal)) { it / 14 } + fadeOut(tween(MonoMotion.Fast)) },
                ) { entry ->
                    NoteEditorScreen(entry.arguments?.getLong("id") ?: 0L, noteVm, onClose = { nav.popBackStack() })
                }
            }

            // Quick Add scrim + menu
            AnimatedVisibility(quickAdd && !inEditor, enter = fadeIn(tween(MonoMotion.Fast)), exit = fadeOut(tween(MonoMotion.Fast))) {
                Box(
                    Modifier.fillMaxSize().background(MonoColors.Scrim)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { quickAdd = false }
                )
            }
            AnimatedVisibility(
                quickAdd && !inEditor,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 112.dp),
                enter = fadeIn(tween(MonoMotion.Fast)) + slideInVertically(tween(MonoMotion.Normal)) { it / 3 } + scaleIn(tween(MonoMotion.Normal), 0.9f),
                exit = fadeOut(tween(MonoMotion.Fast)) + slideOutVertically(tween(MonoMotion.Fast)) { it / 3 } + scaleOut(tween(MonoMotion.Fast), 0.9f),
            ) {
                MonoCard(Modifier.padding(horizontal = MonoSpace.ScreenH)) {
                    Column(Modifier.padding(MonoSpace.Md)) {
                        Text("QUICK ADD", style = MaterialTheme.typography.labelMedium, color = MonoColors.Muted)
                        Row(Modifier.padding(top = MonoSpace.Sm), horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm)) {
                            QuickAction("Add task", Icons.Outlined.CheckCircle, Modifier.weight(1f)) {
                                quickAdd = false; taskSheet = TaskSheetRequest(null)
                            }
                            QuickAction("Create note", Icons.Outlined.Create, Modifier.weight(1f)) { openEditor(0) }
                        }
                    }
                }
            }

            AnimatedVisibility(
                !inEditor, Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(MonoMotion.Normal)) + slideInVertically(tween(MonoMotion.Normal)) { it },
                exit = fadeOut(tween(MonoMotion.Fast)) + slideOutVertically(tween(MonoMotion.Fast)) { it },
            ) {
                FloatingNavBar(
                    selected = Dest.values().firstOrNull { it.route == route }, quickAddOpen = quickAdd,
                    onSelect = ::goTo, onCenter = { quickAdd = !quickAdd },
                )
            }

            SnackbarHost(
                snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (inEditor) 72.dp else 124.dp),
            ) { Snackbar(it, containerColor = MonoColors.Ink, contentColor = MonoColors.Surface, shape = RoundedCornerShape(14.dp)) }
        }
    }

    taskSheet?.let { req ->
        TaskSheet(
            task = req.task, onDismiss = { taskSheet = null },
            onSave = { draft, done -> taskVm.save(draft, done) },
            onDelete = if (req.task != null) ({ taskVm.delete(req.task); }) else null,
        )
    }
    countdownSheet?.let { req ->
        CountdownSheet(
            item = req.item, onDismiss = { countdownSheet = null },
            onSave = { draft, done -> countdownVm.save(draft, done) },
            onDelete = if (req.item != null) ({ countdownVm.delete(req.item) }) else null,
        )
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(MonoColors.SurfaceAlt)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MonoColors.Ink)
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
