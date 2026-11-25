package com.efx.twitterwrapper

import android.app.Application
import android.webkit.WebView
import com.efx.twitterwrapper.core.settings.SettingsRepository

class App : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)

        // Only for debug builds: inspect the wrapper with chrome://inspect.
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }
}
