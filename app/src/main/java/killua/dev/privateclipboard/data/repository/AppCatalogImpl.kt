package killua.dev.privateclipboard.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.di.ApplicationScope
import killua.dev.privateclipboard.domain.AppCatalog
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.domain.model.AppEntry
import killua.dev.privateclipboard.domain.model.ClipConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCatalogImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
    private val configs: ClipConfigRepository,
) : AppCatalog {

    private val packageManager: PackageManager = context.packageManager
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val installed: SharedFlow<List<AppEntry>> =
        merge(refreshRequests, packageChanges())
            .onStart { emit(Unit) }
            .mapLatest { loadInstalled() }
            .flowOn(Dispatchers.IO)
            .shareIn(scope, SharingStarted.WhileSubscribed(KEEP_ALIVE_MS), replay = 1)

    override fun observeApps(): Flow<List<AppEntry>> =
        combine(installed, configs.observeAll()) { apps, configMap ->
            apps.map { app -> app.copy(config = configMap[app.packageName] ?: ClipConfig.Default) }
        }

    override suspend fun refresh() {
        refreshRequests.emit(Unit)
    }

    private fun loadInstalled(): List<AppEntry> =
        queryApplications()
            .filter { it.packageName != context.packageName && it.uid % PER_USER_RANGE >= FIRST_APPLICATION_UID }
            .map { app ->
                AppEntry(
                    packageName = app.packageName,
                    label = app.loadLabel(packageManager).toString(),
                    isSystemApp = app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                )
            }

    @Suppress("DEPRECATION")
    private fun queryApplications(): List<ApplicationInfo> {
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            packageManager.getInstalledApplications(flags)
        }
    }

    private fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme(PACKAGE_SCHEME)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
        const val FIRST_APPLICATION_UID = 10_000
        const val KEEP_ALIVE_MS = 30_000L
        const val PACKAGE_SCHEME = "package"
    }
}
