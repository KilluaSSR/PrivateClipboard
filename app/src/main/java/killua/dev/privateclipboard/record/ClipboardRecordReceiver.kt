package killua.dev.privateclipboard.record

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.privateclipboard.domain.UsageRepository
import killua.dev.privateclipboard.domain.model.Outcome
import killua.dev.privateclipboard.shared.ClipShared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardRecordReceiver : BroadcastReceiver() {

    @Inject
    lateinit var usage: UsageRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ClipShared.ACTION_RECORD) return
        if (intent.getStringExtra(ClipShared.EXTRA_TOKEN) != ClipShared.TOKEN) return
        val pkg = intent.getStringExtra(ClipShared.EXTRA_PACKAGE) ?: return
        val category = ClipShared.Category.fromStorage(intent.getStringExtra(ClipShared.EXTRA_CATEGORY)) ?: return
        val outcome = Outcome.fromStorage(intent.getStringExtra(ClipShared.EXTRA_OUTCOME)) ?: return
        val content = intent.getStringExtra(ClipShared.EXTRA_PREVIEW).orEmpty()
        val at = System.currentTimeMillis()

        val pending = goAsync()
        scope.launch(Dispatchers.IO) {
            try {
                usage.record(pkg, category, outcome, content, at)
            } finally {
                pending.finish()
            }
        }
    }
}
