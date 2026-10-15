package killua.dev.privateclipboard.ask

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import killua.dev.arsenal.material.Spacing
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.ui.components.labelRes
import kotlin.math.ceil
import killua.dev.arsenal.material.components.application.AppIcon
import killua.dev.arsenal.material.components.application.rememberAppLabel

private val CardShape = RoundedCornerShape(28.dp)
private val OuterCorner = 16.dp
private val InnerCorner = 4.dp

@Composable
fun AskDialog(
    request: AskRequest,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onAlwaysDeny: () -> Unit,
    onTimeout: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .safeDrawingPadding()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = shown, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
            AskCard(request, onAllow, onDeny, onAlwaysDeny, onTimeout)
        }
    }
}

@Composable
private fun AskCard(
    request: AskRequest,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onAlwaysDeny: () -> Unit,
    onTimeout: () -> Unit,
) {
    val label = rememberAppLabel(request.packageName)
    val isRead = request.category.isRead
    val progress = remember(request.id) { Animatable(1f) }
    LaunchedEffect(request.id) {
        val elapsed = (System.currentTimeMillis() - request.receivedAt).coerceAtLeast(0)
        val remaining = (request.timeoutMs - elapsed).coerceAtLeast(0)
        progress.snapTo(remaining.toFloat() / request.timeoutMs.coerceAtLeast(1))
        progress.animateTo(0f, tween(durationMillis = remaining.toInt(), easing = LinearEasing))
        onTimeout()
    }
    val secondsLeft = ceil(progress.value * request.timeoutMs / 1000f).toInt()

    Surface(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(request.packageName, size = 64.dp)
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(
                        imageVector = if (isRead) Icons.Rounded.ContentPaste else Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.xs).size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            InfoLine(stringResource(R.string.ask_package, request.packageName))
            InfoLine(stringResource(R.string.ask_operation, stringResource(request.category.labelRes())))

            Spacer(Modifier.height(Spacing.md))
            PreviewBox(
                title = stringResource(if (isRead) R.string.ask_preview_read else R.string.ask_preview_write),
                text = request.preview,
            )

            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(Spacing.md))

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ActionButton(
                    text = stringResource(R.string.ask_allow),
                    shape = RoundedCornerShape(topStart = OuterCorner, topEnd = OuterCorner, bottomStart = InnerCorner, bottomEnd = InnerCorner),
                    emphasized = true,
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ActionButton(
                        text = stringResource(R.string.ask_deny_countdown, secondsLeft),
                        shape = RoundedCornerShape(topStart = InnerCorner, topEnd = InnerCorner, bottomStart = OuterCorner, bottomEnd = InnerCorner),
                        onClick = onDeny,
                        modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        text = stringResource(if (isRead) R.string.ask_always_deny_read else R.string.ask_always_deny_write),
                        shape = RoundedCornerShape(topStart = InnerCorner, topEnd = InnerCorner, bottomStart = InnerCorner, bottomEnd = OuterCorner),
                        destructive = true,
                        onClick = onAlwaysDeny,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PreviewBox(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OuterCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = text.ifBlank { stringResource(R.string.ask_preview_unavailable) },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (text.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        emphasized -> colors.primary to colors.onPrimary
        destructive -> colors.errorContainer to colors.onErrorContainer
        else -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
