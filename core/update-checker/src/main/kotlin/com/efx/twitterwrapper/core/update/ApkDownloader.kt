package com.efx.twitterwrapper.core.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

data class DownloadProgress(
    val bytesWritten: Long,
    val bytesTotal: Long,
    val done: Boolean = false,
)

/**
 * Streams an APK to [destination] and reports progress.
 *
 * Uses `channelFlow` (not `flow` + `emit` from inside `withContext`) so progress
 * can be sent from the IO thread without violating the Flow context invariant.
 */
class ApkDownloader(private val client: OkHttpClient) {

    fun download(url: String, destination: File): Flow<DownloadProgress> = channelFlow {
        destination.parentFile?.mkdirs()
        val tempFile = File(destination.parentFile, destination.name + ".part")
        tempFile.delete()

        val call = client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "efx-update-checker")
                .build(),
        )

        coroutineContext[Job]?.invokeOnCompletion { call.cancel() }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw UpdateException(UpdateState.Error.Kind.HTTP, "APK HTTP ${response.code}")
                }
                val body = response.body
                    ?: throw UpdateException(UpdateState.Error.Kind.IO, "Empty APK body")
                val total = body.contentLength()

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var lastEmitAt = 0L

                        while (true) {
                            if (!isActive) throw CancellationException("Download cancelled")
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitAt >= 100) {
                                lastEmitAt = now
                                send(DownloadProgress(written, total))
                            }
                        }
                        send(DownloadProgress(written, total))
                    }
                }
            }

            if (!tempFile.renameTo(destination)) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }

            send(DownloadProgress(destination.length(), destination.length(), done = true))
        } catch (t: Throwable) {
            tempFile.delete()
            throw when {
                t is CancellationException -> t
                t is UpdateException -> t
                !isActive -> CancellationException("Download cancelled")
                t is IOException -> UpdateException(UpdateState.Error.Kind.NETWORK, t.message, t)
                else -> UpdateException(UpdateState.Error.Kind.IO, t.message, t)
            }
        }
    }.flowOn(Dispatchers.IO)
}
