package com.starledger.app.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val language: String = "system",
    val coolingDays: Int = 7,
    val showDailyAmount: Boolean = false,
    val lastAccountId: Long = 0,
    val lastCategoryId: Long = 0,
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
        val COOLING_DAYS = intPreferencesKey("cooling_days")
        val SHOW_DAILY_AMOUNT = booleanPreferencesKey("show_daily_amount")
        val LAST_ACCOUNT_ID = longPreferencesKey("last_account_id")
        val LAST_CATEGORY_ID = longPreferencesKey("last_category_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "system",
            coolingDays = prefs[Keys.COOLING_DAYS] ?: 7,
            showDailyAmount = prefs[Keys.SHOW_DAILY_AMOUNT] ?: false,
            lastAccountId = prefs[Keys.LAST_ACCOUNT_ID] ?: 0,
            lastCategoryId = prefs[Keys.LAST_CATEGORY_ID] ?: 0,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    /** 同步读语言偏好（用 SharedPreferences，避免启动时阻塞） */
    fun getLanguageSync(): String =
        context.getSharedPreferences("language", Context.MODE_PRIVATE)
            .getString("lang", "system") ?: "system"

    /** 同步写语言偏好 */
    fun setLanguageSync(language: String) {
        context.getSharedPreferences("language", Context.MODE_PRIVATE)
            .edit().putString("lang", language).apply()
    }

    suspend fun setCoolingDays(days: Int) {
        context.dataStore.edit { it[Keys.COOLING_DAYS] = days }
    }

    suspend fun setShowDailyAmount(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_DAILY_AMOUNT] = show }
    }

    suspend fun setLastAccountId(id: Long) {
        context.dataStore.edit { it[Keys.LAST_ACCOUNT_ID] = id }
    }

    suspend fun setLastCategoryId(id: Long) {
        context.dataStore.edit { it[Keys.LAST_CATEGORY_ID] = id }
    }
}
