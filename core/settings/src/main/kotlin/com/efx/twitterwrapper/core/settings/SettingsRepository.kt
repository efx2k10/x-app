package com.efx.twitterwrapper.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val userCss = stringPreferencesKey("user_css")
        val userJs = stringPreferencesKey("user_js")
        val siteMode = stringPreferencesKey("site_mode")
        val onboardingShown = booleanPreferencesKey("onboarding_shown")
    }

    val settings: Flow<Settings> = appContext.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            Settings(
                userCss = prefs[Keys.userCss].orEmpty(),
                userJs = prefs[Keys.userJs].orEmpty(),
                siteMode = prefs[Keys.siteMode]
                    ?.let { stored -> SiteMode.entries.firstOrNull { it.name == stored } }
                    ?: SiteMode.MOBILE,
                onboardingShown = prefs[Keys.onboardingShown] ?: false,
            )
        }

    suspend fun setInjection(css: String, js: String) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[Keys.userCss] = css
            prefs[Keys.userJs] = js
        }
    }

    suspend fun resetInjection() = setInjection("", "")

    suspend fun setSiteMode(mode: SiteMode) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[Keys.siteMode] = mode.name
        }
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[Keys.onboardingShown] = shown
        }
    }
}
