package com.efx.twitterwrapper.core.settings

/**
 * User-facing application settings.
 *
 * Other modules (WebView, UI) must never depend on DataStore directly:
 * they receive a [Settings] snapshot and push changes back through
 * [SettingsRepository].
 */
data class Settings(
    val userCss: String = "",
    val userJs: String = "",
    val siteMode: SiteMode = SiteMode.MOBILE,
    val onboardingShown: Boolean = false,
)

enum class SiteMode { MOBILE, DESKTOP }
