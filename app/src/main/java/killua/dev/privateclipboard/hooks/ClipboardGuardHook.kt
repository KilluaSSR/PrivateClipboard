package killua.dev.privateclipboard.hooks

import android.content.ClipData
import android.os.Binder
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.param.PackageParam
import killua.dev.privateclipboard.shared.ClipShared
import java.util.concurrent.ConcurrentHashMap

internal object ClipboardGuardHook {

    private const val CLIPBOARD_IMPL = "com.android.server.clipboard.ClipboardService\$ClipboardImpl"
    private const val CLIPBOARD_SERVICE = "com.android.server.clipboard.ClipboardService"

    private const val FIRST_APP_UID = 10_000
    private const val PER_USER_RANGE = 100_000

    private const val DEVICE_ID_DEFAULT = 0
    private const val PREVIEW_LIMIT = 600

    private val uidPackages = ConcurrentHashMap<Int, Array<String>>()
    private val approved = ThreadLocal<Boolean>()

    fun apply(param: PackageParam) = with(param) {
        val target = CLIPBOARD_IMPL.toClassOrNull() ?: CLIPBOARD_SERVICE.toClassOrNull()
        if (target == null) {
            YLog.warn("ClipboardGuardHook: clipboard service class not found")
            return@with
        }
        target.hook {
            runCatching {
                injectMember {
                    method { name = "getPrimaryClip" }.all()
                    beforeHook { gate(param, this, read = true, clip = null) }
                    afterHook { approved.remove() }
                }
            }.onFailure { YLog.error("hook getPrimaryClip failed", it) }

            runCatching {
                injectMember {
                    method { name = "setPrimaryClip" }.all()
                    beforeHook { gate(param, this, read = false, clip = firstClip(args)) }
                    afterHook { approved.remove() }
                }
            }.onFailure { YLog.error("hook setPrimaryClip failed", it) }
        }

        CLIPBOARD_SERVICE.toClassOrNull()?.hook {
            runCatching {
                injectMember {
                    method { name = "clipboardAccessAllowed" }.all()
                    beforeHook { if (approved.get() == true) result = true }
                }
            }.onFailure { YLog.error("hook clipboardAccessAllowed failed", it) }
        }
        YLog.info("ClipboardGuardHook installed on ${target.name}")
    }

    private fun gate(
        param: PackageParam,
        hook: HookParam,
        read: Boolean,
        clip: ClipData?,
    ) = with(param) {
        val uid = Binder.getCallingUid()
        if (uid % PER_USER_RANGE < FIRST_APP_UID) return@with
        val pkg = packageForUid(hook, uid) ?: return@with
        if (pkg == ClipShared.SELF_PACKAGE) {
            if (read) hook.result = ClipData.newPlainText(ClipShared.PROBE_LABEL, ClipShared.PROBE_VERSION.toString())
            return@with
        }

        val p = prefs(pkg)
        val category = ClipShared.Category.of(read)
        val subject = if (read) currentClip(hook.instance, uid) else clip
        val content = previewOf(subject)

        val blocked = if (read) {
            p.getBoolean(ClipShared.Keys.BLOCK_READ, false)
        } else {
            p.getBoolean(ClipShared.Keys.BLOCK_WRITE, false)
        }
        if (blocked) {
            hook.result = null
            GuardCoordinator.record(pkg, category, ClipShared.OUTCOME_BLOCKED, content)
            return@with
        }

        val guarded = p.getBoolean(ClipShared.Keys.guardKey(category), false)
        if (!guarded || (read && subject == null)) {
            GuardCoordinator.record(pkg, category, ClipShared.OUTCOME_ALLOWED, content)
            return@with
        }

        val timeoutSec = runCatching {
            prefs(ClipShared.GLOBAL_PREFS).getInt(ClipShared.GLOBAL_ASK_TIMEOUT_SEC, ClipShared.DEFAULT_ASK_TIMEOUT_SEC)
        }.getOrDefault(ClipShared.DEFAULT_ASK_TIMEOUT_SEC)

        val allow = GuardCoordinator.requestDecision(pkg, category, content, timeoutSec * 1000L)
        if (allow) {
            approved.set(true)
            GuardCoordinator.record(pkg, category, ClipShared.OUTCOME_ASK_ALLOW, content)
        } else {
            hook.result = null
            GuardCoordinator.record(pkg, category, ClipShared.OUTCOME_ASK_DENY, content)
        }
    }

    private fun firstClip(args: Array<Any?>): ClipData? = args.firstOrNull { it is ClipData } as? ClipData

    private fun previewOf(clip: ClipData?): String {
        val item = runCatching { clip?.getItemAt(0) }.getOrNull() ?: return ""
        val text = runCatching { item.text?.toString() }.getOrNull()
            ?: runCatching { item.htmlText }.getOrNull()
            ?: runCatching { item.uri?.toString() }.getOrNull()
            ?: runCatching { item.intent?.toUri(0) }.getOrNull()
            ?: ""
        return if (text.length > PREVIEW_LIMIT) text.take(PREVIEW_LIMIT) + "…" else text
    }

    private fun currentClip(instance: Any?, uid: Int): ClipData? = runCatching {
        instance ?: return null
        val service = if (instance.javaClass.name == CLIPBOARD_SERVICE) instance else {
            instance.javaClass.getDeclaredField("this\$0").apply { isAccessible = true }.get(instance)
        } ?: return null
        val serviceClass = service.javaClass
        val getter = serviceClass.declaredMethods.firstOrNull { it.name == "getClipboardLocked" }
            ?.apply { isAccessible = true } ?: return null
        val lock = runCatching {
            serviceClass.getDeclaredField("mLock").apply { isAccessible = true }.get(service)
        }.getOrNull() ?: service
        val userId = uid / PER_USER_RANGE
        val clipboard = synchronized(lock) {
            when (getter.parameterCount) {
                1 -> getter.invoke(service, userId)
                2 -> getter.invoke(service, userId, DEVICE_ID_DEFAULT)
                else -> null
            }
        } ?: return null
        clipboard.javaClass.getDeclaredField("primaryClip").apply { isAccessible = true }.get(clipboard) as? ClipData
    }.onFailure { YLog.warn("currentClip failed: ${it.message}") }.getOrNull()

    private fun packageForUid(hook: HookParam, uid: Int): String? {
        val owned = uidPackages[uid] ?: packagesForUid(uid).also {
            if (it.isNotEmpty()) uidPackages[uid] = it
        }
        if (owned.isEmpty()) return null
        val claimed = hook.args.firstOrNull { it is String && it in owned } as? String
        return claimed ?: owned.first()
    }

    private fun packagesForUid(uid: Int): Array<String> = runCatching<Array<String>> {
        val appGlobals = Class.forName("android.app.AppGlobals")
        val pm = appGlobals.getDeclaredMethod("getPackageManager").invoke(null)
        val resolved = pm.javaClass.methods.first {
            it.name == "getPackagesForUid" && it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
        }.invoke(pm, uid) as? Array<*>
        resolved?.mapNotNull { it as? String }?.toTypedArray() ?: emptyArray<String>()
    }.getOrElse {
        YLog.warn("Cannot resolve packages for uid=$uid: ${it.message}")
        emptyArray<String>()
    }
}
