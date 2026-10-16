package killua.dev.privateclipboard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import killua.dev.arsenal.material.components.application.AppPill
import killua.dev.privateclipboard.domain.model.ClipConfig

@Composable
fun ConfigPill(config: ClipConfig, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when {
        config.blockRead || config.blockWrite -> scheme.errorContainer to scheme.onErrorContainer
        config.isConfigured -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }
    AppPill(
        text = configSummary(config),
        modifier = modifier,
        container = container,
        content = content,
    )
}