package killua.dev.privateclipboard.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import killua.dev.privateclipboard.data.local.EventDao
import killua.dev.privateclipboard.data.local.UsageDao
import killua.dev.privateclipboard.data.local.UsageDatabase
import killua.dev.privateclipboard.data.repository.AppCatalogImpl
import killua.dev.privateclipboard.data.repository.ClipConfigRepositoryImpl
import killua.dev.privateclipboard.data.repository.TemplateRepositoryImpl
import killua.dev.privateclipboard.data.repository.UsageRepositoryImpl
import killua.dev.privateclipboard.domain.AppCatalog
import killua.dev.privateclipboard.domain.ClipConfigRepository
import killua.dev.privateclipboard.domain.TemplateRepository
import killua.dev.privateclipboard.domain.UsageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

    @Provides
    @Singleton
    fun usageDatabase(@ApplicationContext context: Context): UsageDatabase =
        Room.databaseBuilder(context, UsageDatabase::class.java, UsageDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun usageDao(database: UsageDatabase): UsageDao = database.usage()

    @Provides
    fun eventDao(database: UsageDatabase): EventDao = database.events()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {
    @Binds
    abstract fun appCatalog(impl: AppCatalogImpl): AppCatalog

    @Binds
    abstract fun clipConfigRepository(impl: ClipConfigRepositoryImpl): ClipConfigRepository

    @Binds
    abstract fun templateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds
    abstract fun usageRepository(impl: UsageRepositoryImpl): UsageRepository
}
