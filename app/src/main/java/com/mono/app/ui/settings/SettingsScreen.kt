package com.mono.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.app.R
import com.mono.app.ui.SettingsViewModel
import com.mono.app.ui.components.MonoCard
import com.mono.app.ui.components.ScreenHeader
import com.mono.app.ui.navigation.NavBarClearance
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoSpace

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
    }
    var confirm by remember { mutableStateOf(false) }
    val bottom = NavBarClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        Modifier.statusBarsPadding().padding(horizontal = MonoSpace.ScreenH),
        contentPadding = PaddingValues(bottom = bottom), verticalArrangement = Arrangement.spacedBy(MonoSpace.Md),
    ) {
        item { ScreenHeader("Settings", "Make it yours.") }
        item {
            MonoCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(MonoSpace.Lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MonoSpace.Lg)) {
                    Image(painterResource(R.drawable.logo), "MONO mascot", Modifier.size(92.dp), contentScale = ContentScale.Fit)
                    Column {
                        Text("MONO", style = MaterialTheme.typography.displayMedium)
                        Text("A SIMPLE PLACE FOR A BETTER YOU.", style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted)
                        Text("v$version", style = MaterialTheme.typography.labelMedium, color = MonoColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item {
            MonoCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MonoSpace.Md)) {
                    Text("ABOUT", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "MONO brings your day, your tasks and your notes together in one calm, monochrome place.",
                        style = MaterialTheme.typography.bodyLarge, color = MonoColors.Muted, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        item {
            MonoCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(MonoSpace.Md)) {
                    Text("LOCAL STORAGE", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Everything lives only on this device. MONO has no accounts, no cloud and no internet access.",
                        style = MaterialTheme.typography.bodyLarge, color = MonoColors.Muted, modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(Modifier.padding(top = MonoSpace.Md), horizontalArrangement = Arrangement.spacedBy(MonoSpace.Lg)) {
                        Stat(counts.tasks, "Tasks"); Stat(counts.notes, "Notes"); Stat(counts.countdowns, "Countdowns")
                    }
                }
            }
        }
        item {
            MonoCard(Modifier.fillMaxWidth(), onClick = { confirm = true }) {
                Row(Modifier.padding(MonoSpace.Md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MonoSpace.Md)) {
                    Icon(Icons.Outlined.Delete, null, Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Clear all data", style = MaterialTheme.typography.titleSmall)
                        Text("Delete all tasks, notes, folders and countdowns", style = MaterialTheme.typography.bodyMedium, color = MonoColors.Muted)
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, containerColor = MonoColors.Surface,
            title = { Text("Delete everything?") },
            text = { Text("All tasks, notes, folders and countdowns will be permanently erased from this device. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.clearAll() }) { Text("Delete all", color = MonoColors.Ink) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel", color = MonoColors.Muted) } },
        )
    }
}

@Composable
private fun Stat(n: Int, label: String) {
    Column {
        Text("$n", style = MaterialTheme.typography.displayMedium)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoColors.Muted)
    }
}
