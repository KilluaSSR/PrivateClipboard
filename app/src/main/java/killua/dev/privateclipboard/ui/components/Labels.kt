package killua.dev.privateclipboard.ui.components

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.data.ThemeMode
import killua.dev.privateclipboard.domain.model.Outcome
import killua.dev.privateclipboard.shared.ClipShared.Category

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.AutoRead -> R.string.category_auto_read
    Category.AutoWrite -> R.string.category_auto_write
    Category.ManualRead -> R.string.category_manual_read
    Category.ManualWrite -> R.string.category_manual_write
}

@StringRes
fun Outcome.labelRes(): Int = when (this) {
    Outcome.Allowed -> R.string.outcome_allowed
    Outcome.Blocked -> R.string.outcome_blocked
    Outcome.AskAllowed -> R.string.outcome_ask_allowed
    Outcome.AskDenied -> R.string.outcome_ask_denied
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

fun Context.formatDateTime(millis: Long): String = DateUtils.formatDateTime(
    this,
    millis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
)
