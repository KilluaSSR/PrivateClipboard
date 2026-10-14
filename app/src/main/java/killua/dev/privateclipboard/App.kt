package killua.dev.privateclipboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import killua.dev.arsenal.data.settings.SettingsReactorRegistry
import killua.dev.arsenal.error.ArsenalErrorHandler
import killua.dev.arsenal.error.ErrorConfig
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var settingsReactorRegistry: SettingsReactorRegistry

    override fun onCreate() {
        super.onCreate()
        settingsReactorRegistry.ensureStarted()
        ArsenalErrorHandler.install(
            application = this,
            config = ErrorConfig(
                restartActivity = MainActivity::class.java,
                showStackTrace = BuildConfig.DEBUG
            ),
        )
    }
}
