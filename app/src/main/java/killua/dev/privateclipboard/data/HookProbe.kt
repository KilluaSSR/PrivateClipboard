package killua.dev.privateclipboard.data

import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.privateclipboard.shared.ClipShared
import javax.inject.Inject
import javax.inject.Singleton

/** [Outdated]: system_server still runs a hook loaded from an older APK; takes effect after reboot. */
enum class HookStatus { Active, Outdated, Inactive }

@Singleton
class HookProbe @Inject constructor(@ApplicationContext private val context: Context) {

    fun status(): HookStatus = runCatching {
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
        when {
            clip?.description?.label != ClipShared.PROBE_LABEL -> HookStatus.Inactive
            clip.getItemAt(0).text?.toString() == ClipShared.PROBE_VERSION.toString() -> HookStatus.Active
            else -> HookStatus.Outdated
        }
    }.getOrDefault(HookStatus.Inactive)
}
