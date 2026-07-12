package com.touchbridge.mobile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.touchbridge.mobile.data.net.OkHttpWebSocketClient
import com.touchbridge.mobile.data.net.UdpDiscoveryClient
import com.touchbridge.mobile.data.repository.SettingsRepositoryImpl
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import com.touchbridge.mobile.domain.repository.DiscoveryRepository
import com.touchbridge.mobile.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("touchbridge_settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindDiscovery(impl: UdpDiscoveryClient): DiscoveryRepository

    @Binds @Singleton
    abstract fun bindConnection(impl: OkHttpWebSocketClient): ConnectionRepository

    @Binds @Singleton
    abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
