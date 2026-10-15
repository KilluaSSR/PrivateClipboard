package killua.dev.privateclipboard.data

import killua.dev.arsenal.data.settings.boolSetting
import killua.dev.arsenal.data.settings.enumSetting
import killua.dev.arsenal.data.settings.jsonSetting
import killua.dev.privateclipboard.domain.model.ClipTemplate

enum class ThemeMode { System, Light, Dark }

enum class AskTimeout(val seconds: Int) { Five(5), Ten(10), Fifteen(15) }

object AppSettings {
    val themeMode = enumSetting("theme_mode", ThemeMode.System)
    val dynamicColor = boolSetting("dynamic_color", default = true)

    val askTimeout = enumSetting("ask_timeout", AskTimeout.Ten)
    val excludeImeStats = boolSetting("exclude_ime_stats", default = true)

    val templates = jsonSetting<List<ClipTemplate>>("templates", default = emptyList())
}
