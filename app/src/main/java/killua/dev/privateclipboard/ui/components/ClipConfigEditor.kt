package killua.dev.privateclipboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Shield
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

private data class GuardRow(val key: String, val title: Int, val description: Int)

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
            guard = GuardRow(Keys.GUARD_READ, R.string.config_guard_read, R.string.config_guard_read_description),
            config = config,
            onChange = onChange,
        )
        DirectionGroup(
            title = stringResource(R.string.config_group_write),
            blockKey = Keys.BLOCK_WRITE,
            blockTitle = R.string.config_block_write,
            blockDescription = R.string.config_block_write_description,
            blockIcon = Icons.Rounded.EditNote,
            guard = GuardRow(Keys.GUARD_WRITE, R.string.config_guard_write, R.string.config_guard_write_description),
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
    guard: GuardRow,
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
        add { slot ->
            val checked = config.valueOf(guard.key)
            SettingsSwitch(
                title = stringResource(guard.title),
                description = stringResource(if (blocked) R.string.config_guard_overridden else guard.description),
                checked = checked,
                enabled = !blocked,
                onCheckedChange = { onChange(guard.key, it) },
                leadingIcon = Icons.Rounded.Shield,
                leadingAccent = if (checked && !blocked) SettingsAccent.Primary else SettingsAccent.Neutral,
                shape = slot.shape,
                containerColor = slot.color,
            )
        }
    }
    RowGroup(rows = rows, title = title)
}
