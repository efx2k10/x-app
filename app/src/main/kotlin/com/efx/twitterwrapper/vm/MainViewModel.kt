package com.efx.twitterwrapper.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.efx.twitterwrapper.App
import com.efx.twitterwrapper.BuildConfig
import com.efx.twitterwrapper.core.settings.Settings
import com.efx.twitterwrapper.core.settings.SiteMode
import com.efx.twitterwrapper.core.update.UpdateChecker
import com.efx.twitterwrapper.core.update.UpdateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = (application as App).settingsRepository

    /**
     * `null` means "not loaded yet". The UI waits for the first DataStore emission
     * so the WebView is created with the correct site mode and the onboarding
     * overlay does not flash for returning users.
     */
    val settings: StateFlow<Settings?> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val updateChecker = UpdateChecker(
        context = application,
        owner = BuildConfig.GITHUB_OWNER,
        repo = BuildConfig.GITHUB_REPO,
        currentVersionCode = BuildConfig.VERSION_CODE,
        scope = viewModelScope,
    )

    val updateState: StateFlow<UpdateState> = updateChecker.state

    fun checkForUpdates() = updateChecker.check()

    fun downloadUpdate() {
        val available = updateState.value as? UpdateState.Available ?: return
        updateChecker.downloadAndVerify(available.info)
    }

    fun cancelUpdate() = updateChecker.cancel()

    fun saveInjection(css: String, js: String) {
        viewModelScope.launch { settingsRepository.setInjection(css, js) }
    }

    fun resetInjection() {
        viewModelScope.launch { settingsRepository.resetInjection() }
    }

    fun setSiteMode(mode: SiteMode) {
        viewModelScope.launch { settingsRepository.setSiteMode(mode) }
    }

    fun setOnboardingShown(shown: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingShown(shown) }
    }

    override fun onCleared() {
        updateChecker.cancel()
        super.onCleared()
    }
}
