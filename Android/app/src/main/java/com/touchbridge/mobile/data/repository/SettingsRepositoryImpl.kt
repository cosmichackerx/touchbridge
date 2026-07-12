package com.touchbridge.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.touchbridge.mobile.domain.model.TouchSettings
import com.touchbridge.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val NATURAL_SCROLL = booleanPreferencesKey("natural_scroll")
        val HAPTICS = booleanPreferencesKey("haptics")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
    }

    override val settings: Flow<TouchSettings> = dataStore.data.map { prefs ->
        TouchSettings(
            sensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            naturalScroll = prefs[Keys.NATURAL_SCROLL] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            keepAwake = prefs[Keys.KEEP_AWAKE] ?: true
        )
    }

    override suspend fun updateSettings(settings: TouchSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.SENSITIVITY] = settings.sensitivity
            prefs[Keys.NATURAL_SCROLL] = settings.naturalScroll
            prefs[Keys.HAPTICS] = settings.hapticsEnabled
            prefs[Keys.KEEP_AWAKE] = settings.keepAwake
        }
    }
}
