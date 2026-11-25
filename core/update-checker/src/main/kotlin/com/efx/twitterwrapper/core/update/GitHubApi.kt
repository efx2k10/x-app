package com.efx.twitterwrapper.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    /** GitHub returns `null` when the release has no description. */
    @SerialName("body") val body: String? = null,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size") val size: Long = 0L,
)

class GitHubApi(
    private val client: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {

    suspend fun fetchLatest(owner: String, repo: String): GitHubRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (io: IOException) {
            throw UpdateException(UpdateState.Error.Kind.NETWORK, io.message, io)
        }

        response.use { resp ->
            when {
                resp.isSuccessful -> {
                    val body = resp.body?.string()
                        ?: throw UpdateException(UpdateState.Error.Kind.NETWORK, "Empty response body")
                    try {
                        json.decodeFromString(GitHubRelease.serializer(), body)
                    } catch (t: Throwable) {
                        throw UpdateException(UpdateState.Error.Kind.PARSE, t.message, t)
                    }
                }

                resp.code == 404 -> throw UpdateException(UpdateState.Error.Kind.NO_RELEASE, "HTTP 404")
                resp.code == 403 || resp.code == 429 ->
                    throw UpdateException(UpdateState.Error.Kind.RATE_LIMIT, "HTTP ${resp.code}")

                else -> throw UpdateException(UpdateState.Error.Kind.HTTP, "HTTP ${resp.code}")
            }
        }
    }

    private companion object {
        const val USER_AGENT = "efx-update-checker"
    }
}
