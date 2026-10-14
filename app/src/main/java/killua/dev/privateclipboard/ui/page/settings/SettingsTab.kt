package killua.dev.privateclipboard.ui.page.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.gate.Gate
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.templates.scaffolds.plus
import killua.dev.arsenal.material.templates.settings.ArsenalSettingsScreen
import killua.dev.arsenal.material.templates.settings.SettingsAccent
import killua.dev.arsenal.material.templates.settings.SettingsEntry
import killua.dev.arsenal.material.templates.settings.SettingsItemModel
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.arsenal.spi.annotation.SpiImpl
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.data.AppSettings
import killua.dev.privateclipboard.data.AskTimeout
import killua.dev.privateclipboard.data.ThemeMode
import killua.dev.privateclipboard.ui.components.ClipSurfaces
import killua.dev.privateclipboard.ui.components.TabScaffold
import killua.dev.privateclipboard.ui.components.labelRes
import javax.inject.Inject

internal const val SETTINGS_SCREEN = "settings"

@HiltViewModel
class SettingsViewModel @Inject constructor(val store: SettingsStore) : ViewModel()

@Composable
fun SettingsTab() {
    val viewModel: SettingsViewModel = hiltViewModel()
    TabScaffold(title = stringResource(R.string.nav_settings)) { padding ->
        ArsenalSettingsScreen(
            screen = SETTINGS_SCREEN,
            store = viewModel.store,
            contentPadding = padding + PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            itemContainerColor = ClipSurfaces.row,
        )
    }
}

@SpiImpl
class StatusBannerSettingEntry @Inject constructor() : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val order = -1
    override fun model() = SettingsItemModel.Custom(styleKey = STATUS_BANNER_STYLE, showContainer = false)
}

@SpiImpl
class AskTimeoutSettingEntry @Inject constructor(@ApplicationContext private val context: Context) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_group_guard)
    override val order = 0
    override fun model() = SettingsItemModel.Panel(
        definition = AppSettings.askTimeout,
        title = context.getString(R.string.settings_ask_timeout),
        description = context.getString(R.string.settings_ask_timeout_description),
        options = AskTimeout.entries,
        optionLabel = { context.getString(R.string.seconds_value, it.seconds) },
        icon = Icons.Rounded.Timer,
        accent = SettingsAccent.Tertiary,
    )
}

@SpiImpl
class TemplatesSettingEntry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val navigator: Navigator,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_group_guard)
    override val order = 1
    override fun model() = SettingsItemModel.Navigation(
        title = context.getString(R.string.template_manage),
        description = context.getString(R.string.settings_templates_description),
        icon = Icons.Rounded.Dashboard,
        accent = SettingsAccent.Secondary,
        onClick = { navigator.navigate(GeneratedRoutes.TEMPLATES_SCREEN) },
    )
}

@SpiImpl
class ThemeSettingEntry @Inject constructor(@ApplicationContext private val context: Context) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_group_appearance)
    override val order = 10
    override fun model() = SettingsItemModel.Panel(
        definition = AppSettings.themeMode,
        title = context.getString(R.string.settings_theme),
        options = ThemeMode.entries,
        optionLabel = { context.getString(it.labelRes()) },
        icon = Icons.Rounded.Palette,
        accent = SettingsAccent.Primary,
    )
}

@SpiImpl
class DynamicColorSettingEntry @Inject constructor(@ApplicationContext private val context: Context) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_group_appearance)
    override val order = 11
    override fun model() = SettingsItemModel.Switch(
        definition = AppSettings.dynamicColor,
        title = context.getString(R.string.settings_dynamic_color),
        description = context.getString(R.string.settings_dynamic_color_description),
        icon = Icons.Rounded.Wallpaper,
        accent = SettingsAccent.Tertiary,
        gate = Gate { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
    )
}
