package killua.dev.privateclipboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.templates.settings.SettingsAccent
import killua.dev.arsenal.material.templates.settings.SettingsItem
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import killua.dev.privateclipboard.shared.ClipShared.Keys

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun configSummary(config: ClipConfig): String {
    val parts = buildList {
        if (config.blockRead) add(stringResource(R.string.summary_block_read))
        if (config.blockWrite) add(stringResource(R.string.summary_block_write))
        if (config.guardRead && !config.blockRead) add(stringResource(R.string.summary_guard_read))
        if (config.guardWrite && !config.blockWrite) add(stringResource(R.string.summary_guard_write))
    }
    return if (parts.isEmpty()) stringResource(R.string.summary_unrestricted) else parts.joinToString(" · ")
}

@Composable
fun TemplatePickerSheet(
    templates: List<ClipTemplate>,
    onPick: (ClipTemplate) -> Unit,
    onManage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SheetTitle(stringResource(R.string.template_pick_title))
        if (templates.isEmpty()) {
            Text(
                stringResource(R.string.template_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                itemsIndexed(templates, key = { _, it -> it.id }) { index, template ->
                    SettingsItem(
                        title = template.name,
                        description = configSummary(template.config),
                        leadingIcon = Icons.Rounded.Dashboard,
                        leadingAccent = SettingsAccent.Primary,
                        shape = groupedItemShape(itemPosition(index, templates.size)),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { onPick(template) },
                    )
                }
            }
        }
        FilledTonalButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.template_manage))
        }
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
fun NameInputSheet(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SheetTitle(title)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.template_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_save))
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
fun BatchActionsSheet(
    selectedCount: Int,
    onSet: (key: String, value: Boolean) -> Unit,
    onApplyTemplate: () -> Unit,
    onReset: () -> Unit,
) {
    val rows = listOf(
        Keys.BLOCK_READ to R.string.config_block_read,
        Keys.BLOCK_WRITE to R.string.config_block_write,
        Keys.GUARD_READ to R.string.config_guard_read,
        Keys.GUARD_WRITE to R.string.config_guard_write,
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { SheetTitle(stringResource(R.string.batch_title), stringResource(R.string.selected_count, selectedCount)) }
        rows.forEach { (key, label) ->
            item(key = key) { BatchToggleRow(stringResource(label), onOn = { onSet(key, true) }, onOff = { onSet(key, false) }) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.batch_group_template), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilledTonalButton(
                        onClick = onApplyTemplate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) { Text(stringResource(R.string.batch_apply_template)) }
                    FilledTonalButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) { Text(stringResource(R.string.batch_reset)) }
                }
            }
        }
        item { Box(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun BatchToggleRow(label: String, onOn: () -> Unit, onOff: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilledTonalButton(
                onClick = onOn,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) { Text(stringResource(R.string.batch_on)) }
            FilledTonalButton(onClick = onOff, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.batch_off))
            }
        }
    }
}