package de.monsterfinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(val serverUrl: String, val plz: String, val radiusKm: Int)

class Prefs(private val context: Context) {
    private val serverKey = stringPreferencesKey("server_url")
    private val plzKey = stringPreferencesKey("plz")
    private val radiusKey = intPreferencesKey("radius")

    val settings: Flow<Settings> = context.dataStore.data.map {
        Settings(
            serverUrl = it[serverKey] ?: "http://raspberrypi.local:8000",
            plz = it[plzKey] ?: "",
            radiusKm = it[radiusKey] ?: 5,
        )
    }

    suspend fun setServer(url: String) = context.dataStore.edit { it[serverKey] = url.trim() }
    suspend fun setPlz(plz: String) = context.dataStore.edit { it[plzKey] = plz }
    suspend fun setRadius(km: Int) = context.dataStore.edit { it[radiusKey] = km }
}
