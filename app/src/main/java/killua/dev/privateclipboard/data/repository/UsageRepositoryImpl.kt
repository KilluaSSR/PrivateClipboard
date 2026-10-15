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

    override fun observeTotals(excluded: Collection<String>): Flow<UsageCounts> =
        usage.observeTotals(excluded)
            .map { UsageCounts(read = it.read ?: 0, write = it.write ?: 0) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun observe(packageName: String): Flow<UsageCounts> =
        usage.observe(packageName)
            .map { entity ->
                entity?.run { UsageCounts(read, write, denied) } ?: UsageCounts.Empty
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
                read = if (category.isRead) 1 else 0,
                write = if (category.isRead) 0 else 1,
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
