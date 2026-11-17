package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeModePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "talikhata_prefs")

data class AppPreferences(
    val themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    val language: AppLanguage = AppLanguage.BN,
    val activeBusinessId: String = "",
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lastBackupEpoch: Long = 0L,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVE_BIZ_ID = stringPreferencesKey("active_business_id")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_EPOCH = longPreferencesKey("last_backup_epoch")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = enumValues<ThemeModePreference>().firstOrNull {
                it.name == prefs[Keys.THEME_MODE]
            } ?: ThemeModePreference.SYSTEM,
            language = enumValues<AppLanguage>().firstOrNull {
                it.name == prefs[Keys.LANGUAGE]
            } ?: AppLanguage.BN,
            activeBusinessId = prefs[Keys.ACTIVE_BIZ_ID] ?: "",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: true,
            lastBackupEpoch = prefs[Keys.LAST_BACKUP_EPOCH] ?: 0L,
        )
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang.name }
    }

    suspend fun setActiveBusinessId(businessId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_BIZ_ID] = businessId }
    }

    suspend fun setPin(rawPin: String) {
        val hash = if (rawPin.isBlank()) "" else hashPin(rawPin)
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setLastBackupEpoch(epoch: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_EPOCH] = epoch }
    }

    fun verifyPin(rawPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        return hashPin(rawPin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val salted = "TaliKhata_JEOBAYER_SALT_$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
