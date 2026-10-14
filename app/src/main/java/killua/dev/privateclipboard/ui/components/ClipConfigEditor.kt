package killua.dev.privateclipboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.templates.settings.SettingsAccent
import killua.dev.arsenal.material.templates.settings.SettingsSwitch
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.shared.ClipShared.Keys

private data class GuardRow(val key: String, val title: Int, val description: Int, val icon: ImageVector)

@Composable
fun ClipConfigEditor(
    config: ClipConfig,
    onChange: (key: String, value: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        DirectionGroup(
            title = stringResource(R.string.config_group_read),
            blockKey = Keys.BLOCK_READ,
            blockTitle = R.string.config_block_read,
            blockDescription = R.string.config_block_read_description,
            blockIcon = Icons.Rounded.ContentPaste,
            guards = listOf(
                GuardRow(Keys.GUARD_AUTO_READ, R.string.config_guard_auto_read, R.string.config_guard_auto_read_description, Icons.Rounded.SmartToy),
                GuardRow(Keys.GUARD_MANUAL_READ, R.string.config_guard_manual_read, R.string.config_guard_manual_read_description, Icons.Rounded.TouchApp),
            ),
            config = config,
            onChange = onChange,
        )
        DirectionGroup(
            title = stringResource(R.string.config_group_write),
            blockKey = Keys.BLOCK_WRITE,
            blockTitle = R.string.config_block_write,
            blockDescription = R.string.config_block_write_description,
            blockIcon = Icons.Rounded.EditNote,
            guards = listOf(
                GuardRow(Keys.GUARD_AUTO_WRITE, R.string.config_guard_auto_write, R.string.config_guard_auto_write_description, Icons.Rounded.SmartToy),
                GuardRow(Keys.GUARD_MANUAL_WRITE, R.string.config_guard_manual_write, R.string.config_guard_manual_write_description, Icons.Rounded.TouchApp),
            ),
            config = config,
            onChange = onChange,
        )
    }
}

@Composable
private fun DirectionGroup(
    title: String,
    blockKey: String,
    blockTitle: Int,
    blockDescription: Int,
    blockIcon: ImageVector,
    guards: List<GuardRow>,
    config: ClipConfig,
    onChange: (String, Boolean) -> Unit,
) {
    val blocked = config.valueOf(blockKey)
    val rows = buildList<@Composable (RowSlot) -> Unit> {
        add { slot ->
            SettingsSwitch(
                title = stringResource(blockTitle),
                description = stringResource(blockDescription),
                checked = blocked,
                onCheckedChange = { onChange(blockKey, it) },
                leadingIcon = if (blocked) Icons.Rounded.Block else blockIcon,
                leadingAccent = if (blocked) SettingsAccent.Error else SettingsAccent.Neutral,
                shape = slot.shape,
                containerColor = slot.color,
            )
        }
        guards.forEach { guard ->
            add { slot ->
                val checked = config.valueOf(guard.key)
                SettingsSwitch(
                    title = stringResource(guard.title),
                    description = stringResource(if (blocked) R.string.config_guard_overridden else guard.description),
                    checked = checked,
                    enabled = !blocked,
                    onCheckedChange = { onChange(guard.key, it) },
                    leadingIcon = guard.icon,
                    leadingAccent = if (checked && !blocked) SettingsAccent.Primary else SettingsAccent.Neutral,
                    shape = slot.shape,
                    containerColor = slot.color,
                )
            }
        }
    }
    RowGroup(rows = rows, title = title)
}
