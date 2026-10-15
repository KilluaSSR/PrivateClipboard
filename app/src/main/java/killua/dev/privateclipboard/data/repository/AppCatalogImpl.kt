package killua.dev.privateclipboard.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.arsenal.utils.`package`.installedAppCatalog
import killua.dev.privateclipboard.domain.AppCatalog
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.ClipConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCatalogImpl @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
    private val configs: ClipConfigRepository,
) : AppCatalog {

    private val catalog = installedAppCatalog(context = context, scope = scope)

    override fun observeApps(): Flow<List<AppEntry>> =
        combine(catalog.apps, configs.observeAll()) { apps, configMap ->
            apps.map { app ->
                AppEntry(
                    packageName = app.packageName,
                    label = app.label,
                    isSystemApp = app.isSystemApp,
                    config = configMap[app.packageName] ?: ClipConfig.Default,
                )
            }
        }

    override suspend fun refresh() {
        catalog.refresh()
    }
}