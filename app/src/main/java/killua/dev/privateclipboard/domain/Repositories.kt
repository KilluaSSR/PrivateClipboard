package killua.dev.privateclipboard.domain

import killua.dev.privateclipboard.domain.model.AccessEvent
import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import killua.dev.privateclipboard.domain.model.Outcome
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import kotlinx.coroutines.flow.Flow

interface AppCatalog {
    fun observeApps(): Flow<List<AppEntry>>

    suspend fun refresh()
}

interface ClipConfigRepository {
    fun observeAll(): Flow<Map<String, ClipConfig>>

    fun observe(packageName: String): Flow<ClipConfig>

    suspend fun update(packageName: String, transform: (ClipConfig) -> ClipConfig)

    suspend fun apply(packageNames: Collection<String>, config: ClipConfig)

    suspend fun setKey(packageNames: Collection<String>, key: String, value: Boolean)
}

interface TemplateRepository {
    fun observeAll(): Flow<List<ClipTemplate>>

    fun observe(id: String): Flow<ClipTemplate?>

    suspend fun create(name: String, config: ClipConfig): ClipTemplate

    suspend fun save(template: ClipTemplate)

    suspend fun delete(ids: Collection<String>)
}

interface UsageRepository {
    fun observeTotals(excluded: Collection<String>): Flow<UsageCounts>

    fun observe(packageName: String): Flow<UsageCounts>

    fun observeEvents(packageName: String, category: Category, limit: Int): Flow<List<AccessEvent>>

    suspend fun record(packageName: String, category: Category, outcome: Outcome, content: String, at: Long)
}
