package killua.dev.privateclipboard.shared

object ClipShared {

    enum class Category(val storage: String) {
        AutoRead("auto_read"),
        AutoWrite("auto_write"),
        ManualRead("manual_read"),
        ManualWrite("manual_write");

        val isRead: Boolean get() = this == AutoRead || this == ManualRead
        val isWrite: Boolean get() = !isRead
        val isManual: Boolean get() = this == ManualRead || this == ManualWrite

        companion object {
            fun of(read: Boolean, manual: Boolean): Category = when {
                read && manual -> ManualRead
                read -> AutoRead
                manual -> ManualWrite
                else -> AutoWrite
            }

            fun fromStorage(value: String?): Category? = entries.firstOrNull { it.storage == value }
        }
    }

    object Keys {
        const val GUARD_AUTO_READ = "guard_auto_read"
        const val GUARD_AUTO_WRITE = "guard_auto_write"
        const val GUARD_MANUAL_READ = "guard_manual_read"
        const val GUARD_MANUAL_WRITE = "guard_manual_write"
        const val BLOCK_READ = "block_read"
        const val BLOCK_WRITE = "block_write"

        fun guardKey(category: Category): String = when (category) {
            Category.AutoRead -> GUARD_AUTO_READ
            Category.AutoWrite -> GUARD_AUTO_WRITE
            Category.ManualRead -> GUARD_MANUAL_READ
            Category.ManualWrite -> GUARD_MANUAL_WRITE
        }

        val all: List<String> = listOf(
            GUARD_AUTO_READ, GUARD_AUTO_WRITE, GUARD_MANUAL_READ, GUARD_MANUAL_WRITE,
            BLOCK_READ, BLOCK_WRITE,
        )
    }

    const val GLOBAL_PREFS = "_global"
    const val GLOBAL_ASK_TIMEOUT_SEC = "ask_timeout_sec"

    const val DEFAULT_ASK_TIMEOUT_SEC = 10

    const val ACTION_ASK = "killua.dev.privateclipboard.action.ASK"

    const val ACTION_DECISION = "killua.dev.privateclipboard.action.DECISION"

    const val ACTION_RECORD = "killua.dev.privateclipboard.action.RECORD"

    const val EXTRA_REQUEST_ID = "request_id"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_PREVIEW = "preview"
    const val EXTRA_ALLOW = "allow"
    const val EXTRA_OUTCOME = "outcome"
    const val EXTRA_TOKEN = "token"
    const val EXTRA_TIMEOUT_MS = "timeout_ms"

    const val TOKEN = "pcb-7e3a9f2c"

    const val SYSTEM_PACKAGE = "android"

    const val PROBE_LABEL = "killua.dev.privateclipboard.probe"
    const val PROBE_VERSION = 1

    const val OUTCOME_ALLOWED = "allowed"
    const val OUTCOME_BLOCKED = "blocked"
    const val OUTCOME_ASK_ALLOW = "ask_allow"
    const val OUTCOME_ASK_DENY = "ask_deny"
    const val SELF_PACKAGE = "killua.dev.privateclipboard"
}
