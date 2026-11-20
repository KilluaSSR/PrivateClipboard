package killua.dev.privateclipboard.di

import android.content.Context
import com.highcapable.yukihookapi.YukiHookAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import killua.dev.arsenal.logging.LogSource
import killua.dev.arsenal.logging.LogcatConfig
import killua.dev.arsenal.logging.LogcatLogSource
import javax.inject.Singleton

/** YLog 的输出走 logcat（Tag 为 YukiHookAPI.TAG），以独立来源贡献给日志查看器。 */
@Module
@InstallIn(SingletonComponent::class)
object LogSourcesModule {

    @Provides
    @Singleton
    @IntoSet
    fun yukiLogSource(@ApplicationContext context: Context): LogSource = LogcatLogSource(
        context,
        LogcatConfig(tag = YukiHookAPI.TAG, allProcesses = true, id = "yuki"),
    )
}
