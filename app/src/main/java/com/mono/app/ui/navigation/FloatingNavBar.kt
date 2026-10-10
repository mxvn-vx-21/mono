package com.mono.app.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mono.app.R
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoElevation
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.ui.theme.MonoSpace

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Today", Icons.Outlined.Home),
    Done("done", "Done", Icons.Outlined.CheckCircle),
    Memo("memo", "Memo", Icons.Outlined.Create),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}

private val PillHeight = 68.dp
private val CenterSize = 68.dp
private val Overhang = 28.dp

/** Total height this bar occupies (excluding system nav inset). Screens pad by this plus margin. */
val NavBarClearance = PillHeight + Overhang + 40.dp

@Composable
fun FloatingNavBar(
    selected: Dest?,
    quickAddOpen: Boolean,
    onSelect: (Dest) -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = MonoSpace.ScreenH).padding(bottom = MonoSpace.Sm)
            .height(PillHeight + Overhang),
    ) {
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(PillHeight),
            shape = RoundedCornerShape(PillHeight / 2), color = MonoColors.Surface,
            shadowElevation = MonoElevation.Nav, border = BorderStroke(1.dp, MonoColors.Border),
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                NavItem(Dest.Today, selected == Dest.Today, onSelect)
                NavItem(Dest.Done, selected == Dest.Done, onSelect)
                Spacer(Modifier.weight(1f)) // slot under the raised mascot button: exact centre
                NavItem(Dest.Memo, selected == Dest.Memo, onSelect)
                NavItem(Dest.Settings, selected == Dest.Settings, onSelect)
            }
        }
        val tilt by animateFloatAsState(if (quickAddOpen) 12f else 0f, tween(MonoMotion.Normal), label = "tilt")
        Box(
            Modifier.align(Alignment.TopCenter).size(CenterSize)
                .shadow(MonoElevation.Fab, CircleShape, ambientColor = MonoColors.Shadow, spotColor = MonoColors.Shadow)
                .clip(CircleShape).background(MonoColors.Ink).border(4.dp, MonoColors.Surface, CircleShape)
                .clickable(role = Role.Button, onClick = onCenter)
                .semantics { contentDescription = "Quick add" },
            contentAlignment = Alignment.Center,
        ) {
            // logo.png: drawable-nodpi, shown with original proportions & transparency
            Image(
                painterResource(R.drawable.logo), contentDescription = null,
                modifier = Modifier.size(38.dp).graphicsLayer { rotationZ = tilt },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(dest: Dest, selected: Boolean, onSelect: (Dest) -> Unit) {
    val tint = if (selected) MonoColors.Ink else MonoColors.Muted
    val bar by animateDpAsState(if (selected) 18.dp else 0.dp, tween(MonoMotion.Normal), label = "bar")
    Column(
        Modifier.weight(1f).fillMaxSize()
            .clickable(role = Role.Tab) { onSelect(dest) }
            .semantics { this.selected = selected; contentDescription = dest.label },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(dest.icon, null, Modifier.size(24.dp), tint = tint)
        Text(dest.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
        Box(Modifier.padding(top = 3.dp).height(2.dp).size(width = bar, height = 2.dp).background(MonoColors.Ink, RoundedCornerShape(1.dp)))
    }
}
