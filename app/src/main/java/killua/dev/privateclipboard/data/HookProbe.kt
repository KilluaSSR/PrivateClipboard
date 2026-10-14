package killua.dev.privateclipboard.data

import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.privateclipboard.shared.ClipShared
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HookProbe @Inject constructor(@ApplicationContext private val context: Context) {

    fun isHookActive(): Boolean = runCatching {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.description?.label == ClipShared.PROBE_LABEL
    }.getOrDefault(false)
}
