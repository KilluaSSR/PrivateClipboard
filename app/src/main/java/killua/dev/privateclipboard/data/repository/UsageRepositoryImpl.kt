package killua.dev.privateclipboard.data.repository

import androidx.room.withTransaction
import killua.dev.privateclipboard.data.local.EventDao
import killua.dev.privateclipboard.data.local.EventEntity
import killua.dev.privateclipboard.data.local.UsageDao
import killua.dev.privateclipboard.data.local.UsageDatabase
import killua.dev.privateclipboard.domain.UsageRepository
import killua.dev.privateclipboard.domain.model.AccessEvent
import killua.dev.privateclipboard.domain.model.Outcome
import killua.dev.privateclipboard.domain.model.UsageCounts
import killua.dev.privateclipboard.shared.ClipShared.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val database: UsageDatabase,
    private val usage: UsageDao,
    private val events: EventDao,
) : UsageRepository {

    override fun observeTotals(): Flow<UsageCounts> =
        usage.observeTotals()
            .map {
                UsageCounts(
                    autoRead = it.autoRead ?: 0,
                    autoWrite = it.autoWrite ?: 0,
                    manualRead = it.manualRead ?: 0,
                    manualWrite = it.manualWrite ?: 0,
                )
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun observe(packageName: String): Flow<UsageCounts> =
        usage.observe(packageName)
            .map { entity ->
                entity?.run { UsageCounts(autoRead, autoWrite, manualRead, manualWrite, denied) } ?: UsageCounts.Empty
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun observeEvents(packageName: String, category: Category, limit: Int): Flow<List<AccessEvent>> =
        events.observe(packageName, category.storage, limit)
            .map { list ->
                list.mapNotNull { entity ->
                    val outcome = Outcome.fromStorage(entity.outcome) ?: return@mapNotNull null
                    AccessEvent(entity.id, category, outcome, entity.content, entity.occurredAt)
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override suspend fun record(packageName: String, category: Category, outcome: Outcome, content: String, at: Long) {
        database.withTransaction {
            usage.increment(
                packageName = packageName,
                autoRead = if (category == Category.AutoRead) 1 else 0,
                autoWrite = if (category == Category.AutoWrite) 1 else 0,
                manualRead = if (category == Category.ManualRead) 1 else 0,
                manualWrite = if (category == Category.ManualWrite) 1 else 0,
                denied = if (outcome.isDenied) 1 else 0,
            )
            events.insert(
                EventEntity(
                    packageName = packageName,
                    category = category.storage,
                    outcome = outcome.storage,
                    content = content,
                    occurredAt = at,
                )
            )
            events.trim(EVENT_LOG_LIMIT)
        }
    }

    private companion object {
        const val EVENT_LOG_LIMIT = 2_000
    }
}
