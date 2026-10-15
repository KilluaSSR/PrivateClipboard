package killua.dev.privateclipboard.shared

object ClipShared {

    enum class Category(val storage: String) {
        Read("read"),
        Write("write");

        val isRead: Boolean get() = this == Read

        companion object {
            fun of(read: Boolean): Category = if (read) Read else Write

            fun fromStorage(value: String?): Category? = when (value) {
                "read", "manual_read", "auto_read" -> Read
                "write", "manual_write", "auto_write" -> Write
                else -> null
            }
        }
    }

    object Keys {
        const val GUARD_READ = "guard_manual_read"
        const val GUARD_WRITE = "guard_manual_write"
        const val BLOCK_READ = "block_read"
        const val BLOCK_WRITE = "block_write"

        fun guardKey(category: Category): String = if (category.isRead) GUARD_READ else GUARD_WRITE

        val all: List<String> = listOf(GUARD_READ, GUARD_WRITE, BLOCK_READ, BLOCK_WRITE)
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
