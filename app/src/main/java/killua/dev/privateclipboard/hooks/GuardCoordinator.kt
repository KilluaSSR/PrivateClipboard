package killua.dev.privateclipboard.hooks

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import com.highcapable.yukihookapi.hook.log.YLog
import killua.dev.privateclipboard.shared.ClipShared
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal object GuardCoordinator {

    private const val ASK_ACTIVITY = "killua.dev.privateclipboard.ask.ClipboardAskActivity"

    private const val DECISION_REUSE_MS = 4_000L

    private val random = SecureRandom()
    private val pending = ConcurrentHashMap<Long, ArrayBlockingQueue<Boolean>>()
    private val recent = ConcurrentHashMap<String, Pair<Boolean, Long>>()

    private val askLock = ReentrantLock(true)

    @Volatile
    private var receiverRegistered = false

    private fun systemContext(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val current = activityThread.getMethod("currentActivityThread").invoke(null)
        (activityThread.getMethod("getSystemContext").invoke(current) as Context).also { ensureReceiver(it) }
    }.onFailure { YLog.error("GuardCoordinator: no system context", it) }.getOrNull()

    private fun ensureReceiver(ctx: Context) {
        if (receiverRegistered) return
        synchronized(this) {
            if (receiverRegistered) return
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.getStringExtra(ClipShared.EXTRA_TOKEN) != ClipShared.TOKEN) return
                    val id = intent.getLongExtra(ClipShared.EXTRA_REQUEST_ID, 0L)
                    pending[id]?.offer(intent.getBooleanExtra(ClipShared.EXTRA_ALLOW, false))
                }
            }
            val filter = IntentFilter(ClipShared.ACTION_DECISION)
            asSystem {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    ctx.registerReceiver(receiver, filter)
                }
            }
            receiverRegistered = true
        }
    }

    private inline fun <T> asSystem(block: () -> T): T {
        val token = Binder.clearCallingIdentity()
        try {
            return block()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }
    fun record(pkg: String, category: ClipShared.Category, outcome: String, content: String) {
        val ctx = systemContext() ?: return
        runCatching {
            val intent = Intent(ClipShared.ACTION_RECORD).apply {
                setPackage(ClipShared.SELF_PACKAGE)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(ClipShared.EXTRA_TOKEN, ClipShared.TOKEN)
                putExtra(ClipShared.EXTRA_PACKAGE, pkg)
                putExtra(ClipShared.EXTRA_CATEGORY, category.storage)
                putExtra(ClipShared.EXTRA_OUTCOME, outcome)
                putExtra(ClipShared.EXTRA_PREVIEW, content)
            }
            asSystem { ctx.sendBroadcast(intent) }
        }.onFailure { YLog.error("GuardCoordinator: record failed", it) }
    }

    fun requestDecision(
        pkg: String,
        category: ClipShared.Category,
        preview: String,
        timeoutMs: Long,
    ): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return false
        val key = "$pkg/${category.storage}"
        reusable(key)?.let { return it }

        val ctx = systemContext() ?: return false
        if (!askLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) return false
        try {
            reusable(key)?.let { return it }
            val id = random.nextLong()
            val queue = ArrayBlockingQueue<Boolean>(1)
            pending[id] = queue
            try {
                val intent = Intent(ClipShared.ACTION_ASK).apply {
                    component = ComponentName(ClipShared.SELF_PACKAGE, ASK_ACTIVITY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(ClipShared.EXTRA_REQUEST_ID, id)
                    putExtra(ClipShared.EXTRA_PACKAGE, pkg)
                    putExtra(ClipShared.EXTRA_CATEGORY, category.storage)
                    putExtra(ClipShared.EXTRA_PREVIEW, preview)
                    putExtra(ClipShared.EXTRA_TIMEOUT_MS, timeoutMs)
                }
                asSystem { ctx.startActivity(intent) }
                val allow = queue.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: false
                recent[key] = allow to SystemClock.uptimeMillis()
                return allow
            } finally {
                pending.remove(id)
            }
        } catch (t: Throwable) {
            YLog.error("GuardCoordinator: ask failed", t)
            return false
        } finally {
            askLock.unlock()
        }
    }

    private fun reusable(key: String): Boolean? {
        val (allow, at) = recent[key] ?: return null
        return if (SystemClock.uptimeMillis() - at <= DECISION_REUSE_MS) allow else null
    }
}
