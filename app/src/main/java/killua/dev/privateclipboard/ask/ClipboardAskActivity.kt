package killua.dev.privateclipboard.ask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.privateclipboard.data.AppSettings
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.shared.ClipShared
import killua.dev.privateclipboard.ui.theme.PrivateClipboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskRequest(
    val id: Long,
    val packageName: String,
    val category: ClipShared.Category,
    val preview: String,
    val timeoutMs: Long,
    val receivedAt: Long,
)

@AndroidEntryPoint
class ClipboardAskActivity : ComponentActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var configs: ClipConfigRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var request by mutableStateOf<AskRequest?>(null)
    private var answered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = parse(intent) ?: return finish()
        request = initial
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = decide(allow = false)
        })
        setContent {
            val themeMode by settingsStore.state(AppSettings.themeMode).collectAsStateWithLifecycle()
            val dynamicColor by settingsStore.state(AppSettings.dynamicColor).collectAsStateWithLifecycle()
            PrivateClipboardTheme(themeMode = themeMode, dynamicColor = dynamicColor, transparentBackground = true) {
                request?.let { current ->
                    AskDialog(
                        request = current,
                        onAllow = { decide(allow = true) },
                        onDeny = { decide(allow = false) },
                        onAlwaysDeny = { decide(allow = false, remember = true) },
                        onTimeout = { decide(allow = false) },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val next = parse(intent) ?: return
        answered = false
        request = next
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) decide(allow = false)
    }

    private fun decide(allow: Boolean, remember: Boolean = false) {
        val current = request ?: return finish()
        if (answered) return
        answered = true
        sendBroadcast(
            Intent(ClipShared.ACTION_DECISION).apply {
                setPackage(ClipShared.SYSTEM_PACKAGE)
                putExtra(ClipShared.EXTRA_TOKEN, ClipShared.TOKEN)
                putExtra(ClipShared.EXTRA_REQUEST_ID, current.id)
                putExtra(ClipShared.EXTRA_ALLOW, allow)
            }
        )
        if (remember) {
            appScope.launch {
                configs.update(current.packageName) {
                    if (current.category.isRead) it.copy(blockRead = true) else it.copy(blockWrite = true)
                }
            }
        }
        finish()
    }

    private fun parse(intent: Intent?): AskRequest? {
        intent ?: return null
        val id = intent.getLongExtra(ClipShared.EXTRA_REQUEST_ID, 0L)
        val pkg = intent.getStringExtra(ClipShared.EXTRA_PACKAGE) ?: return null
        val category = ClipShared.Category.fromStorage(intent.getStringExtra(ClipShared.EXTRA_CATEGORY)) ?: return null
        return AskRequest(
            id = id,
            packageName = pkg,
            category = category,
            preview = intent.getStringExtra(ClipShared.EXTRA_PREVIEW).orEmpty(),
            timeoutMs = intent.getLongExtra(ClipShared.EXTRA_TIMEOUT_MS, ClipShared.DEFAULT_ASK_TIMEOUT_SEC * 1000L),
            receivedAt = System.currentTimeMillis(),
        )
    }
}
