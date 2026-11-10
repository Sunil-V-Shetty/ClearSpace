package com.sunilshetty.clearspace.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.sunilshetty.clearspace.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.Preferences.DATA_STORE_NAME,
)

private val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val darkModeEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[DARK_MODE_ENABLED] ?: false
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }
}
