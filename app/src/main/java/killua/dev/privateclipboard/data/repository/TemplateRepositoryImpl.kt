package killua.dev.privateclipboard.data.repository

import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.privateclipboard.data.AppSettings
import killua.dev.privateclipboard.domain.TemplateRepository
import killua.dev.privateclipboard.domain.model.ClipConfig
import killua.dev.privateclipboard.domain.model.ClipTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val store: SettingsStore,
) : TemplateRepository {

    override fun observeAll(): Flow<List<ClipTemplate>> = store.flow(AppSettings.templates)

    override fun observe(id: String): Flow<ClipTemplate?> =
        observeAll().map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    override suspend fun create(name: String, config: ClipConfig): ClipTemplate {
        val template = ClipTemplate(id = UUID.randomUUID().toString(), name = name, config = config)
        store.update(AppSettings.templates) { it + template }
        return template
    }

    override suspend fun save(template: ClipTemplate) {
        store.update(AppSettings.templates) { list ->
            if (list.any { it.id == template.id }) list.map { if (it.id == template.id) template else it }
            else list + template
        }
    }

    override suspend fun delete(ids: Collection<String>) {
        val doomed = ids.toSet()
        store.update(AppSettings.templates) { list -> list.filterNot { it.id in doomed } }
    }
}
