package com.efx.twitterwrapper.core.update

/**
 * Result of the update flow. UI-free: the app maps [Error.kind] to localized text,
 * so the checker never contains user-facing strings.
 */
sealed interface UpdateState {

    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    data class Downloading(
        val progress: Float,
        val bytesWritten: Long = 0L,
        val bytesTotal: Long = -1L,
    ) : UpdateState

    data object Verifying : UpdateState

    data class ReadyToInstall(val apkPath: String) : UpdateState

    data class Error(
        val kind: Kind,
        val detail: String? = null,
    ) : UpdateState {
        enum class Kind {
            NOT_CONFIGURED,
            NETWORK,
            HTTP,
            RATE_LIMIT,
            NO_RELEASE,
            BAD_TAG,
            NO_APK,
            NO_CHECKSUMS,
            CHECKSUM_MISMATCH,
            SIGNATURE_MISMATCH,
            IO,
            PARSE,
            UNKNOWN,
        }
    }
}
