package killua.dev.privateclipboard.domain.model

import killua.dev.privateclipboard.shared.ClipShared
import killua.dev.privateclipboard.shared.ClipShared.Category
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClipConfig(
    @SerialName("guardManualRead") val guardRead: Boolean = false,
    @SerialName("guardManualWrite") val guardWrite: Boolean = false,
    val blockRead: Boolean = false,
    val blockWrite: Boolean = false,
) {
    val isConfigured: Boolean get() = this != Default

    fun guards(category: Category): Boolean = if (category.isRead) guardRead else guardWrite

    fun isBlocked(category: Category): Boolean = if (category.isRead) blockRead else blockWrite

    fun valueOf(key: String): Boolean = when (key) {
        ClipShared.Keys.GUARD_READ -> guardRead
        ClipShared.Keys.GUARD_WRITE -> guardWrite
        ClipShared.Keys.BLOCK_READ -> blockRead
        ClipShared.Keys.BLOCK_WRITE -> blockWrite
        else -> false
    }

    fun with(key: String, value: Boolean): ClipConfig = when (key) {
        ClipShared.Keys.GUARD_READ -> copy(guardRead = value)
        ClipShared.Keys.GUARD_WRITE -> copy(guardWrite = value)
        ClipShared.Keys.BLOCK_READ -> copy(blockRead = value)
        ClipShared.Keys.BLOCK_WRITE -> copy(blockWrite = value)
        else -> this
    }

    companion object {
        val Default = ClipConfig()

        val GuardAll = ClipConfig(guardRead = true, guardWrite = true)

        fun of(values: (String) -> Boolean): ClipConfig =
            ClipShared.Keys.all.fold(Default) { acc, key -> acc.with(key, values(key)) }
    }
}

@Serializable
data class ClipTemplate(
    val id: String,
    val name: String,
    val config: ClipConfig,
)

enum class Outcome(val storage: String) {
    Allowed(ClipShared.OUTCOME_ALLOWED),
    Blocked(ClipShared.OUTCOME_BLOCKED),
    AskAllowed(ClipShared.OUTCOME_ASK_ALLOW),
    AskDenied(ClipShared.OUTCOME_ASK_DENY);

    val isDenied: Boolean get() = this == Blocked || this == AskDenied

    companion object {
        fun fromStorage(value: String?): Outcome? = entries.firstOrNull { it.storage == value }
    }
}

data class UsageCounts(
    val read: Long = 0,
    val write: Long = 0,
    val denied: Long = 0,
) {
    fun of(category: Category): Long = if (category.isRead) read else write

    companion object {
        val Empty = UsageCounts()
    }
}

data class AccessEvent(
    val id: Long,
    val category: Category,
    val outcome: Outcome,
    val content: String,
    val occurredAt: Long,
)

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val config: ClipConfig = ClipConfig.Default,
)

enum class AppScope { UserOnly, All }

data class AppFilter(
    val scope: AppScope = AppScope.UserOnly,
    val query: String = "",
)
