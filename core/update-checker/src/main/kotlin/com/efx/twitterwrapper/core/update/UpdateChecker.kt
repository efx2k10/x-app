package com.efx.twitterwrapper.core.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual (user triggered) update checker for GitHub Releases.
 *
 * The module knows nothing about the UI or the host application: it exposes
 * [state] and localizes nothing. All network work runs on the caller supplied
 * [scope] (usually `viewModelScope`), so it is cancelled together with the UI.
 */
class UpdateChecker(
    context: Context,
    private val owner: String,
    private val repo: String,
    private val currentVersionCode: Int,
    private val scope: CoroutineScope,
) {

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val api = GitHubApi(client)
    private val downloader = ApkDownloader(client)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    fun check() {
        if (job?.isActive == true) return

        job = scope.launch {
            _state.value = UpdateState.Checking
            try {
                requireConfigured()

                val release = api.fetchLatest(owner, repo)
                val parsed = SemverParser.parse(release.tagName)
                    ?: throw UpdateException(UpdateState.Error.Kind.BAD_TAG, release.tagName)

                if (parsed.versionCode <= currentVersionCode) {
                    _state.value = UpdateState.UpToDate
                    return@launch
                }

                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: throw UpdateException(UpdateState.Error.Kind.NO_APK)
                val checksumsAsset = release.assets.firstOrNull { it.name.equals("checksums.txt", ignoreCase = true) }
                    ?: throw UpdateException(UpdateState.Error.Kind.NO_CHECKSUMS, "checksums.txt is missing in the release")

                _state.value = UpdateState.Available(
                    UpdateInfo(
                        versionName = parsed.versionName,
                        versionCode = parsed.versionCode,
                        changelog = release.body.orEmpty(),
                        apkUrl = apkAsset.downloadUrl,
                        apkSize = apkAsset.size,
                        apkFileName = apkAsset.name,
                        checksumsUrl = checksumsAsset.downloadUrl,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                _state.value = t.toErrorState()
            }
        }
    }

    fun downloadAndVerify(info: UpdateInfo) {
        if (job?.isActive == true) return

        job = scope.launch {
            val directory = File(appContext.filesDir, "updates").apply { mkdirs() }
            val fileName = File(info.apkFileName).name
                .takeIf { it.endsWith(".apk", ignoreCase = true) }
                ?: "app-${info.versionName}.apk"
            val target = File(directory, fileName)

            try {
                pruneOldFiles(directory, keep = fileName)
                _state.value = UpdateState.Downloading(0f, 0L, info.apkSize)

                downloader.download(info.apkUrl, target).collect { progress ->
                    val fraction = if (progress.bytesTotal > 0) {
                        (progress.bytesWritten.toFloat() / progress.bytesTotal).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    _state.value = UpdateState.Downloading(
                        progress = fraction,
                        bytesWritten = progress.bytesWritten,
                        bytesTotal = progress.bytesTotal,
                    )
                }

                _state.value = UpdateState.Verifying

                // Fail closed: no checksums -> no install.
                val checksumsFile = Sha256Verifier.fetchChecksums(client, info.checksumsUrl)
                val expected = Checksums.findHash(checksumsFile, target.name)
                    ?: throw UpdateException(UpdateState.Error.Kind.NO_CHECKSUMS, target.name)
                val actual = Sha256Verifier.computeFileHash(target)
                if (!Checksums.constantTimeEquals(expected, actual)) {
                    target.delete()
                    throw UpdateException(UpdateState.Error.Kind.CHECKSUM_MISMATCH)
                }

                if (!ApkSignatureVerifier.isSignedByInstalledApp(appContext, target, info.versionCode)) {
                    target.delete()
                    throw UpdateException(UpdateState.Error.Kind.SIGNATURE_MISMATCH)
                }

                _state.value = UpdateState.ReadyToInstall(target.absolutePath)
            } catch (cancellation: CancellationException) {
                target.delete()
                throw cancellation
            } catch (t: Throwable) {
                target.delete()
                _state.value = t.toErrorState()
            }
        }
    }

    fun reset() {
        if (job?.isActive == true) return
        _state.value = UpdateState.Idle
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = UpdateState.Idle
    }

    private fun requireConfigured() {
        val notConfigured = owner.isBlank() || repo.isBlank() ||
            owner.equals("CHANGE_ME", ignoreCase = true) ||
            repo.equals("CHANGE_ME", ignoreCase = true)
        if (notConfigured) {
            throw UpdateException(UpdateState.Error.Kind.NOT_CONFIGURED, "$owner/$repo")
        }
    }

    private fun pruneOldFiles(directory: File, keep: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name != keep) file.delete()
        }
    }

    private fun Throwable.toErrorState(): UpdateState.Error = when (this) {
        is UpdateException -> UpdateState.Error(kind, detail)
        else -> UpdateState.Error(UpdateState.Error.Kind.UNKNOWN, message)
    }
}
