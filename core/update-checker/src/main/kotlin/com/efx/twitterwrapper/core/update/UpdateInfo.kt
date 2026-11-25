package com.efx.twitterwrapper.core.update

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    /** Asset name from the release, e.g. `app-1.0.1.apk`. Also the local file name. */
    val apkFileName: String,
    val checksumsUrl: String,
)
