@file:OptIn(ExperimentalFoundationApi::class)

package com.mono.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mono.app.R
import com.mono.app.ui.theme.MonoColors
import com.mono.app.ui.theme.MonoElevation
import com.mono.app.ui.theme.MonoMotion
import com.mono.app.ui.theme.MonoRadius
import com.mono.app.ui.theme.MonoSpace
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.toggleable

@Composable
fun MonoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(MonoRadius.Card)
    var m = modifier
        .shadow(MonoElevation.Card, shape, ambientColor = MonoColors.Shadow, spotColor = MonoColors.Shadow)
        .clip(shape).background(MonoColors.Surface).border(1.dp, MonoColors.Border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m, content = content)
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = MonoSpace.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MonoSpace.Sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.displayMedium)
            Text(subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = MonoColors.Muted)
        }
        actions()
    }
}

@Composable
fun MonoIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(46.dp).shadow(2.dp, CircleShape, ambientColor = MonoColors.Shadow, spotColor = MonoColors.Shadow)
            .clip(CircleShape).background(MonoColors.Surface).border(1.dp, MonoColors.Border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(22.dp), tint = MonoColors.Ink) }
}

@Composable
fun SectionHeader(title: String, actions: List<Pair<String, () -> Unit>> = emptyList()) {
    Row(
        Modifier.fillMaxWidth().padding(top = MonoSpace.Lg, bottom = MonoSpace.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        actions.forEach { (label, action) ->
            Text(
                label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MonoColors.Muted,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = action)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun CircleCheck(checked: Boolean, onToggle: (Boolean) -> Unit, description: String) {
    val fill by animateColorAsState(if (checked) MonoColors.Ink else MonoColors.Surface, tween(MonoMotion.Fast), label = "fill")
    val scale by animateFloatAsState(if (checked) 1f else 0f, tween(MonoMotion.Fast), label = "tick")
    Box(
        Modifier.size(46.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onToggle)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(fill).border(1.5.dp, MonoColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check, null, tint = MonoColors.Surface,
                modifier = Modifier.size(16.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

/** Three small bars; filled up to the priority level. Monochrome. */
@Composable
fun PriorityBars(level: Int, modifier: Modifier = Modifier) {
    Row(modifier.semantics { contentDescription = "Priority ${listOf("low", "medium", "high")[level.coerceIn(0, 2)]}" },
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0..2) {
            Box(
                Modifier.width(3.dp).height((6 + i * 4).dp).clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) MonoColors.Ink else MonoColors.Border)
            )
        }
    }
}

@Composable
fun MonoButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = true) {
    val shape = RoundedCornerShape(MonoRadius.Chip)
    Box(
        modifier.height(50.dp).clip(shape)
            .background(if (filled) MonoColors.Ink else MonoColors.Surface)
            .border(1.dp, if (filled) MonoColors.Ink else MonoColors.Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(), style = MaterialTheme.typography.labelLarge,
            color = if (filled) MonoColors.Surface else MonoColors.Ink,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
fun MonoChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(MonoRadius.Chip)
    val bg by animateColorAsState(if (selected) MonoColors.Ink else MonoColors.Surface, tween(MonoMotion.Fast), label = "chip")
    Box(
        modifier.height(40.dp).clip(shape).background(bg).border(1.dp, if (selected) MonoColors.Ink else MonoColors.Border, shape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = if (selected) MonoColors.Surface else MonoColors.Ink,
            modifier = Modifier.padding(horizontal = 16.dp), maxLines = 1,
        )
    }
}

@Composable
fun MonoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
) {
    val shape = RoundedCornerShape(MonoRadius.Field)
    BasicTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        singleLine = singleLine, minLines = if (singleLine) 1 else minLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MonoColors.Ink),
        cursorBrush = SolidColor(MonoColors.Ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(shape).background(MonoColors.SurfaceAlt)
                    .border(if (isError) 1.5.dp else 1.dp, if (isError) MonoColors.Ink else MonoColors.Border, shape)
                    .padding(14.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MonoColors.Muted)
                inner()
            }
        },
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = MonoSpace.Xl, horizontal = MonoSpace.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.logo), contentDescription = null,
            modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit, alpha = 0.85f,
        )
        Spacer(Modifier.height(MonoSpace.Md))
        Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MonoSpace.Xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MonoColors.Muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(MonoSpace.Md))
            MonoButton(actionLabel, onAction)
        }
    }
}
