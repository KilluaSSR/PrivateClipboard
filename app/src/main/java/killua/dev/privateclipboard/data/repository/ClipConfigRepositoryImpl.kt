package killua.dev.privateclipboard.data.repository

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.shared.ClipShared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) : ClipConfigRepository {

    private val configs = MutableStateFlow<Map<String, ClipConfig>?>(null)
    private val writeMutex = Mutex()

    init {
        scope.launch(Dispatchers.IO) { configs.value = loadIndexed() }
    }

    override fun observeAll(): Flow<Map<String, ClipConfig>> = configs.filterNotNull()

    override fun observe(packageName: String): Flow<ClipConfig> =
        observeAll().map { it[packageName] ?: ClipConfig.Default }.distinctUntilChanged()

    override suspend fun update(packageName: String, transform: (ClipConfig) -> ClipConfig) = write {
        val current = it[packageName] ?: ClipConfig.Default
        mapOf(packageName to transform(current))
    }

    override suspend fun apply(packageNames: Collection<String>, config: ClipConfig) = write {
        packageNames.associateWith { config }
    }

    override suspend fun setKey(packageNames: Collection<String>, key: String, value: Boolean) = write { all ->
        packageNames.associateWith { (all[it] ?: ClipConfig.Default).with(key, value) }
    }

    private suspend fun write(changes: (Map<String, ClipConfig>) -> Map<String, ClipConfig>) {
        writeMutex.withLock {
            val current = observeAll().first()
            val changed = changes(current)
            if (changed.isEmpty()) return
            withContext(Dispatchers.IO) {
                changed.forEach { (pkg, config) -> persist(pkg, config) }
                val index = (current + changed).filterValues { it.isConfigured }.keys
                context.prefs(INDEX_PREFS).edit { putStringSet(INDEX_KEY, index) }
            }
            configs.value = current + changed
        }
    }

    private fun persist(pkg: String, config: ClipConfig) {
        context.prefs(pkg).edit {
            ClipShared.Keys.all.forEach { key -> putBoolean(key, config.valueOf(key)) }
        }
    }

    private fun loadIndexed(): Map<String, ClipConfig> {
        val index = runCatching { context.prefs(INDEX_PREFS).getStringSet(INDEX_KEY, emptySet()) }
            .getOrDefault(emptySet())
        return index.associateWith { pkg ->
            val p = context.prefs(pkg)
            ClipConfig.of { key -> runCatching { p.getBoolean(key, false) }.getOrDefault(false) }
        }
    }

    private companion object {
        const val INDEX_PREFS = "_index"
        const val INDEX_KEY = "packages"
    }
}
