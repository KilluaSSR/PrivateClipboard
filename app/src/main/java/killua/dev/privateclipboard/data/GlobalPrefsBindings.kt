package killua.dev.privateclipboard.data

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import killua.dev.arsenal.data.settings.SettingBinding
import killua.dev.arsenal.data.settings.bindSetting
import killua.dev.privateclipboard.shared.ClipShared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Module
@InstallIn(SingletonComponent::class)
object GlobalPrefsBindings {

    @Provides
    @IntoSet
    fun askTimeout(@ApplicationContext context: Context): SettingBinding<*> =
        bindSetting(AppSettings.askTimeout) { timeout ->
            withContext(Dispatchers.IO) {
                context.prefs(ClipShared.GLOBAL_PREFS).edit { putInt(ClipShared.GLOBAL_ASK_TIMEOUT_SEC, timeout.seconds) }
            }
        }
}
