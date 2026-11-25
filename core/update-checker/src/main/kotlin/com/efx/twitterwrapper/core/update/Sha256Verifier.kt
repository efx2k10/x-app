package com.efx.twitterwrapper.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

object Sha256Verifier {

    private const val USER_AGENT = "efx-update-checker"
    private const val HEX = "0123456789abcdef"

    suspend fun fetchChecksums(client: OkHttpClient, url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (io: IOException) {
            throw UpdateException(UpdateState.Error.Kind.NETWORK, io.message, io)
        }

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw UpdateException(UpdateState.Error.Kind.HTTP, "checksums HTTP ${resp.code}")
            }
            resp.body?.string() ?: throw UpdateException(UpdateState.Error.Kind.IO, "Empty checksums body")
        }
    }

    suspend fun computeFileHash(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xFF
            append(HEX[value ushr 4])
            append(HEX[value and 0x0F])
        }
    }
}
