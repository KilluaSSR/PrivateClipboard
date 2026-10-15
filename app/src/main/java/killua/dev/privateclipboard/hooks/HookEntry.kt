package killua.dev.privateclipboard.hooks

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import killua.dev.privateclipboard.shared.ClipShared

@InjectYukiHookWithXposed(modulePackageName = ClipShared.SELF_PACKAGE)
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        loadSystem {
            runCatching {
                ClipboardGuardHook.apply(this)
            }.onFailure { YLog.error("ClipboardGuardHook failed", it) }
        }
    }
}
