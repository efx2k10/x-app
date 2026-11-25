================================================================================
  TWITTER WEB WRAPPER — ANDROID ПРИЛОЖЕНИЕ С СИСТЕМОЙ ОБНОВЛЕНИЙ С GITHUB
  Полное техническое описание и реализация
  Версия документа: 1.0
  Дата: 2026-11-25
================================================================================


================================================================================
1. ОБЩЕЕ ОПИСАНИЕ
================================================================================

Приложение — это Android-обёртка (WebView) вокруг сайта x.com / twitter.com,
позволяющая пользователю:

  * Открывать сайт X/Twitter в полноэкранном WebView без хрома браузера.
  * Переключать мобильную / десктопную версию сайта.
  * Инжектить собственные CSS-стили и JavaScript-скрипты.
  * Входить в аккаунт, загружать файлы, использовать геолокацию.
  * Открывать внешние ссылки в системном браузере.
  * Скачивать файлы через системный DownloadManager.
  * Вызывать меню специфическим жестом (двойной тап двумя пальцами).
  * Проверять и устанавливать обновления из GitHub Releases вручную.

Модуль проверки обновлений вынесен в отдельный переиспользуемый модуль
(:core:update-checker) и не содержит зависимостей от Twitter/WebView, что
позволяет подключать его в другие приложения проекта.


================================================================================
2. ТРЕБОВАНИЯ
================================================================================

Платформа:          Android 7.0+ (minSdk 24, targetSdk 34)
Язык:               Kotlin
UI:                 Jetpack Compose (Material 3)
Архитектура:        MVVM + Clean-ish (data/domain/ui), без фанатизма
Хранение настроек:  DataStore Preferences
Сеть:               OkHttp + kotlinx.serialization (без Retrofit — меньше зависимостей)
Распространение:    Только GitHub Releases (сайдлоад APK)
Локализация:        Русский, английский
Система обновлений: Ручная проверка, скачивание APK, проверка SHA-256,
                    установка через Intent + FileProvider
Подпись:            Один keystore, генерируется в CI через GitHub Secrets


================================================================================
3. АРХИТЕКТУРА
================================================================================

Слои:

  ┌─────────────────────────────────────────────────────────────────┐
  │  :app (UI)                                                       │
  │  MainActivity, MenuOverlay, SettingsScreen, UpdateDialog,        │
  │  OnboardingOverlay                                               │
  └────────────────────────┬────────────────────────────────────────┘
                           │
  ┌────────────────────────▼────────────────────────────────────────┐
  │  :core:webview                                                   │
  │  TwitterWebView, WebViewSetup, CssJsInjector, DownloadHandler,   │
  │  FileChooserHandler, GeoPermissionHandler, TwoFingerDoubleTap    │
  └────────────────────────┬────────────────────────────────────────┘
                           │
  ┌────────────────────────▼────────────────────────────────────────┐
  │  :core:update-checker                                            │
  │  UpdateChecker, GitHubApi, SemverParser, ApkDownloader,          │
  │  ApkInstaller, Sha256Verifier                                    │
  └────────────────────────┬────────────────────────────────────────┘
                           │
  ┌────────────────────────▼────────────────────────────────────────┐
  │  :core:settings                                                  │
  │  SettingsRepository (DataStore)                                  │
  └─────────────────────────────────────────────────────────────────┘


Поток данных:
  SettingsRepository (StateFlow<Settings>) → UI + WebViewSetup
  UpdateChecker (StateFlow<UpdateState>)   → UpdateDialog
  WebView → callbacks → UI (menu, error, download)


================================================================================
4. СТРУКТУРА ПРОЕКТА
================================================================================

twitter-wrapper/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── release.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/example/twitterwrapper/
│       │   ├── MainActivity.kt
│       │   ├── App.kt
│       │   ├── ui/
│       │   │   ├── MainScreen.kt
│       │   │   ├── MenuOverlay.kt
│       │   │   ├── SettingsScreen.kt
│       │   │   ├── UpdateDialog.kt
│       │   │   ├── OnboardingOverlay.kt
│       │   │   └── ErrorOverlay.kt
│       │   └── vm/
│       │       └── MainViewModel.kt
│       └── res/
│           ├── values/strings.xml
│           ├── values-ru/strings.xml
│           ├── xml/file_paths.xml
│           └── mipmap-*/ic_launcher.*
├── core/
│   ├── settings/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/example/core/settings/
│   │       ├── Settings.kt
│   │       └── SettingsRepository.kt
│   ├── update-checker/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/com/example/core/update/
│   │       ├── UpdateChecker.kt
│   │       ├── UpdateState.kt
│   │       ├── UpdateInfo.kt
│   │       ├── GitHubApi.kt
│   │       ├── SemverParser.kt
│   │       ├── ApkDownloader.kt
│   │       ├── ApkInstaller.kt
│   │       └── Sha256Verifier.kt
│   └── webview/
│       ├── build.gradle.kts
│       └── src/main/kotlin/com/example/core/webview/
│           ├── TwitterWebView.kt
│           ├── WebViewSetup.kt
│           ├── CssJsInjector.kt
│           ├── DownloadHandler.kt
│           ├── FileChooserHandler.kt
│           ├── GeoPermissionHandler.kt
│           └── TwoFingerDoubleTap.kt
└── README.md


================================================================================
5. GRADLE: НАСТРОЙКА
================================================================================

---- settings.gradle.kts ----

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "twitter-wrapper"
include(":app")
include(":core:settings")
include(":core:update-checker")
include(":core:webview")


---- build.gradle.kts (root) ----

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}


---- gradle/libs.versions.toml ----

[versions]
composeBom     = "2024.09.03"
activityCompose= "1.9.2"
lifecycle      = "2.8.6"
datastore      = "1.1.1"
okhttp         = "4.12.0"
serialization  = "1.7.3"
coroutines     = "1.9.0"

[libraries]
androidx-compose-bom            = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui             = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling     = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-prev= { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3      = { module = "androidx.compose.material3:material3" }
androidx-compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
androidx-activity-compose       = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-vm           = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime      = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-datastore              = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
okhttp                          = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
kotlinx-serialization-json      = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android      = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }


---- app/build.gradle.kts ----

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.twitterwrapper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.twitterwrapper"
        minSdk = 24
        targetSdk = 34
        versionCode = 1        // ВНИМАНИЕ: вычисляется из versionName в CI
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Значения приходят из GitHub Secrets через переменные окружения
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false       // R8 выключен — по требованию
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:settings"))
    implementation(project(":core:update-checker"))
    implementation(project(":core:webview"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
}


---- core/update-checker/build.gradle.kts ----

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.core.update"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.core:core-ktx:1.13.1")
}


---- core/webview/build.gradle.kts ----

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.core.webview"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.webkit:webkit:1.11.0")
}


---- core/settings/build.gradle.kts ----

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.core.settings"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
}


================================================================================
6. ANDROID MANIFEST
================================================================================

---- app/src/main/AndroidManifest.xml ----

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <!-- Для загрузки файлов через <input type="file"> -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
        android:minSdkVersion="33" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- Для геолокации -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".App"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.TwitterWrapper"
        android:usesCleartextTraffic="false">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|uiMode"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider для установки APK-обновления -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>


---- app/src/main/res/xml/file_paths.xml ----

<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="updates" path="updates/" />
</paths>


================================================================================
7. МОДУЛЬ :core:settings
================================================================================

---- Settings.kt ----

package com.example.core.settings

data class Settings(
    val userCss: String = "",
    val userJs: String = "",
    val siteMode: SiteMode = SiteMode.MOBILE,
    val onboardingShown: Boolean = false,
)

enum class SiteMode { MOBILE, DESKTOP }


---- SettingsRepository.kt ----

package com.example.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val userCss         = stringPreferencesKey("user_css")
        val userJs          = stringPreferencesKey("user_js")
        val siteMode        = stringPreferencesKey("site_mode")
        val onboardingShown = booleanPreferencesKey("onboarding_shown")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            userCss = p[Keys.userCss].orEmpty(),
            userJs = p[Keys.userJs].orEmpty(),
            siteMode = p[Keys.siteMode]
                ?.let { runCatching { SiteMode.valueOf(it) }.getOrNull() }
                ?: SiteMode.MOBILE,
            onboardingShown = p[Keys.onboardingShown] ?: false,
        )
    }

    suspend fun setCss(css: String) = context.dataStore.edit { it[Keys.userCss] = css }
    suspend fun setJs(js: String)   = context.dataStore.edit { it[Keys.userJs] = js }
    suspend fun setSiteMode(mode: SiteMode) =
        context.dataStore.edit { it[Keys.siteMode] = mode.name }
    suspend fun setOnboardingShown(v: Boolean) =
        context.dataStore.edit { it[Keys.onboardingShown] = v }
}


================================================================================
8. МОДУЛЬ :core:update-checker
================================================================================

---- UpdateInfo.kt ----

package com.example.core.update

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    val checksumsUrl: String?,
)


---- UpdateState.kt ----

package com.example.core.update

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Verifying : UpdateState
    data class ReadyToInstall(val apkPath: String) : UpdateState
    data class Error(val message: String, val cause: Throwable? = null) : UpdateState
}


---- SemverParser.kt ----

package com.example.core.update

/**
 * versionName -> versionCode, чтобы Android мог сравнивать версии.
 * Формат тега: "v1.2.3" или "1.2.3".
 * versionCode = major * 10000 + minor * 100 + patch
 */
object SemverParser {

    private val regex = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

    fun parse(tag: String): Parsed? {
        val m = regex.find(tag.trim()) ?: return null
        val (maj, min, pat) = m.destructured
        val a = maj.toIntOrNull() ?: return null
        val b = min.toIntOrNull() ?: return null
        val c = pat.toIntOrNull() ?: return null
        if (a > 2000 || b > 99 || c > 99) return null
        return Parsed(versionName = "$a.$b.$c", versionCode = a * 10000 + b * 100 + c)
    }

    data class Parsed(val versionName: String, val versionCode: Int)
}


---- GitHubApi.kt ----

package com.example.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")     val tagName: String,
    @SerialName("body")         val body: String = "",
    @SerialName("assets")       val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    @SerialName("name")                 val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size")                 val size: Long,
)

class GitHubApi(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchLatest(owner: String, repo: String): GitHubRelease =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "update-checker")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("GitHub API HTTP ${resp.code}")
                }
                val body = resp.body?.string() ?: error("Empty body")
                json.decodeFromString(GitHubRelease.serializer(), body)
            }
        }
}


---- ApkDownloader.kt ----

package com.example.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class ApkDownloader(private val client: OkHttpClient = OkHttpClient()) {

    /**
     * Качает APK и параллельно отдаёт прогресс 0f..1f.
     * Итоговый файл: cacheDir/updates/<fileName>
     */
    fun download(url: String, destDir: File, fileName: String): Flow<Float> = flow {
        val req = Request.Builder().url(url).build()
        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        if (!resp.isSuccessful) error("HTTP ${resp.code}")
        val body = resp.body ?: error("Empty body")
        val total = body.contentLength().takeIf { it > 0 } ?: -1L

        destDir.mkdirs()
        val file = File(destDir, fileName)

        withContext(Dispatchers.IO) {
            body.byteStream().use { input ->
                FileOutputStream(file).use { out ->
                    val buf = ByteArray(8 * 1024)
                    var read: Int
                    var written = 0L
                    var lastEmit = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        written += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 100 && total > 0) {
                            lastEmit = now
                            emit((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        emit(1f)
    }.flowOn(Dispatchers.IO)
}


---- Sha256Verifier.kt ----

package com.example.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

object Sha256Verifier {

    /** Скачивает checksums.txt и находит строку с нашим именем файла. */
    suspend fun fetchExpectedHash(
        client: OkHttpClient,
        checksumsUrl: String,
        apkFileName: String,
    ): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(checksumsUrl).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            body.lineSequence().forEach { line ->
                // Формат "hash  filename" или "hash *filename"
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size == 2) {
                    val hash = parts[0].lowercase()
                    val name = parts[1].removePrefix("*").trim()
                    if (name == apkFileName && hash.length == 64) return@withContext hash
                }
            }
            null
        }
    }

    suspend fun computeFileHash(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8 * 1024)
            var n: Int
            while (input.read(buf).also { n = it } != -1) md.update(buf, 0, n)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
}


---- ApkInstaller.kt ----

package com.example.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    /**
     * Возвращает true, если разрешение "установка из неизвестных источников"
     * уже выдано, либо не требуется (Android < 8).
     */
    fun canInstall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    /** Открывает системные настройки для выдачи разрешения. */
    fun openPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Запускает установку APK через FileProvider. */
    fun install(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}


---- UpdateChecker.kt ----

package com.example.core.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

/**
 * Переиспользуемый чекер обновлений с GitHub Releases.
 * Не зависит от UI. Возвращает StateFlow<UpdateState>.
 *
 * Параметры (owner, repo) можно задать в конструкторе — модуль подходит
 * для любого приложения проекта.
 */
class UpdateChecker(
    private val context: Context,
    private val owner: String,
    private val repo: String,
    private val currentVersionCode: Int,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val client = OkHttpClient()
    private val api = GitHubApi(client)
    private val downloader = ApkDownloader(client)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var lastInfo: UpdateInfo? = null

    fun check() {
        if (_state.value == UpdateState.Checking) return
        scope.launch {
            _state.value = UpdateState.Checking
            try {
                val release = api.fetchLatest(owner, repo)
                val parsed = SemverParser.parse(release.tagName)
                    ?: error("Некорректный тег релиза: ${release.tagName}")

                if (parsed.versionCode <= currentVersionCode) {
                    _state.value = UpdateState.UpToDate
                    return@launch
                }

                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: error("В релизе нет APK")
                val checksumsAsset = release.assets.firstOrNull {
                    it.name.equals("checksums.txt", ignoreCase = true)
                }

                val info = UpdateInfo(
                    versionName = parsed.versionName,
                    versionCode = parsed.versionCode,
                    changelog = release.body,
                    apkUrl = apkAsset.downloadUrl,
                    apkSize = apkAsset.size,
                    checksumsUrl = checksumsAsset?.downloadUrl,
                )
                lastInfo = info
                _state.value = UpdateState.Available(info)
            } catch (t: Throwable) {
                _state.value = UpdateState.Error(
                    message = t.message ?: "Ошибка проверки обновлений",
                    cause = t,
                )
            }
        }
    }

    fun downloadAndVerify(info: UpdateInfo) {
        scope.launch {
            try {
                val fileName = "app-${info.versionName}.apk"
                val destDir = File(context.cacheDir, "updates")
                downloader.download(info.apkUrl, destDir, fileName).collect { p ->
                    _state.value = UpdateState.Downloading(p)
                }

                _state.value = UpdateState.Verifying

                if (info.checksumsUrl != null) {
                    val expected = Sha256Verifier.fetchExpectedHash(
                        client, info.checksumsUrl, fileName
                    )
                    if (expected != null) {
                        val actual = Sha256Verifier.computeFileHash(
                            File(destDir, fileName)
                        )
                        if (expected != actual) {
                            File(destDir, fileName).delete()
                            error("SHA-256 не совпал. Файл повреждён или подменён.")
                        }
                    }
                }

                _state.value = UpdateState.ReadyToInstall(
                    File(destDir, fileName).absolutePath
                )
            } catch (t: Throwable) {
                _state.value = UpdateState.Error(
                    message = t.message ?: "Ошибка загрузки",
                    cause = t,
                )
            }
        }
    }

    fun reset() { _state.value = UpdateState.Idle }
}


================================================================================
9. МОДУЛЬ :core:webview
================================================================================

---- CssJsInjector.kt ----

package com.example.core.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat

object CssJsInjector {

    fun inject(webView: WebView, css: String, js: String) {
        if (css.isNotBlank()) {
            val escaped = css
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            webView.evaluateJavascript(
                """
                (function() {
                    var s = document.createElement('style');
                    s.setAttribute('data-user', '1');
                    s.textContent = `$escaped`;
                    (document.head || document.documentElement).appendChild(s);
                })();
                """.trimIndent(), null
            )
        }
        if (js.isNotBlank()) {
            webView.evaluateJavascript(js, null)
        }
    }

    /**
     * Инжектит скрипт на каждом документе до загрузки страницы,
     * чтобы правки применялись к SPA без гонки таймингов.
     * API доступно с androidx.webkit 1.9+ (WebViewFeature.DOCUMENT_START_SCRIPT).
     */
    fun registerDocumentStartScript(
        webView: WebView,
        id: String,
        script: String,
        allowedOriginRules: Set<String> = setOf("https://x.com", "https://twitter.com"),
    ) {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(
                androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT
            )
        ) {
            WebViewCompat.addDocumentStartJavaScript(webView, script, allowedOriginRules)
        }
    }
}


---- DownloadHandler.kt ----

package com.example.core.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.webkit.WebView

object DownloadHandler {

    fun attach(webView: WebView, context: Context) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            // blob: URL нельзя скачать через DownloadManager — нужен JS-бридж.
            if (url.startsWith("blob:")) return@setDownloadListener

            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName
                )
            }
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
                .enqueue(request)
        }
    }
}


---- FileChooserHandler.kt ----

package com.example.core.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Обработка <input type="file">. Использование:
 *
 *   val handler = FileChooserHandler(activity, webView)
 *   handler.register()   // в onCreate/onStart
 *   webView.webChromeClient = handler.chromeClient(baseChromeClient)
 */
class FileChooserHandler(
    private val activity: AppCompatActivity,
    private val webView: WebView,
) {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        val data: Array<Uri>? = when {
            result.resultCode != Activity.RESULT_OK -> null
            result.data?.clipData != null -> {
                val clip = result.data!!.clipData!!
                Array(clip.itemCount) { clip.getItemAt(it).uri }
            }
            result.data?.data != null -> arrayOf(result.data!!.data!!)
            else -> null
        }
        callback.onReceiveValue(data)
    }

    fun chromeClient(base: WebChromeClient): WebChromeClient =
        object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?,
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                // Разрешение на чтение обязательно на Android 14+
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                launcher.launch(Intent.createChooser(intent, null))
                return true
            }
        }
}


---- GeoPermissionHandler.kt ----

package com.example.core.webview

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class GeoPermissionHandler(private val activity: AppCompatActivity) {

    private var pending: Pair<String, GeolocationPermissions.Callback>? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        pending?.let { (origin, cb) -> cb.invoke(origin, granted, false) }
        pending = null
    }

    fun chromeClient(base: WebChromeClient): WebChromeClient =
        object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?,
            ) {
                if (origin == null || callback == null) return
                val fine = ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (fine) {
                    callback.invoke(origin, true, false)
                } else {
                    pending = origin to callback
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
            }
        }
}


---- WebViewSetup.kt ----

package com.example.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.core.settings.SiteMode

object WebViewSetup {

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val HOME_URL = "https://x.com/"

    private val INTERNAL_HOSTS = setOf(
        "x.com", "www.x.com",
        "twitter.com", "www.twitter.com",
        "t.co",
        "abs.twimg.com", "pbs.twimg.com",
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        siteMode: SiteMode,
        userCss: String,
        userJs: String,
        onError: (String) -> Unit,
        onPageLoaded: () -> Unit,
    ): WebView {
        val webView = WebView(context)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(android.graphics.Color.BLACK)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString = when (siteMode) {
                SiteMode.MOBILE -> null   // дефолтный UA WebView
                SiteMode.DESKTOP -> DESKTOP_UA
            }
        }
        if (siteMode == SiteMode.MOBILE) {
            // null в userAgentString не сбрасывает UA — нужно явно
            webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url
                val scheme = url.scheme?.lowercase()
                val host = url.host?.lowercase()?.removePrefix("www.")

                return when {
                    scheme == "http" || scheme == "https" -> {
                        if (host in INTERNAL_HOSTS) false   // пусть WebView грузит
                        else {
                            openExternal(context, url)
                            true
                        }
                    }
                    scheme == "intent" -> {
                        openIntentUrl(context, url)
                        true
                    }
                    scheme == "mailto" || scheme == "tel" -> {
                        openExternal(context, url)
                        true
                    }
                    else -> false
                }
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                CssJsInjector.inject(view, userCss, userJs)
                onPageLoaded()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (request.isForMainFrame) {
                    onError(error.description?.toString() ?: "Ошибка загрузки")
                }
            }
        }

        webView.loadUrl(HOME_URL)
        return webView
    }

    private fun openExternal(context: Context, url: Uri) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun openIntentUrl(context: Context, url: Uri) {
        runCatching {
            val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                // fallback — открыть браузером
                val fallback = intent.getStringExtra("browser_fallback_url")
                if (!fallback.isNullOrBlank()) openExternal(context, Uri.parse(fallback))
            }
        }
    }
}


---- TwoFingerDoubleTap.kt ----

package com.example.core.webview

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration

@Composable
fun TwoFingerDoubleTapOverlay(
    onGestureDetected: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val viewConfig = LocalViewConfiguration.current
    val haptics = LocalHapticFeedback.current

    val singleTapMaxDuration = 250L
    val doubleTapWindow = viewConfig.doubleTapTimeoutMillis
    val touchSlop = viewConfig.touchSlop
    val doubleTapSlop = touchSlop * 2f

    Box(Modifier.fillMaxSize()) {
        content()

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var lastTapEnd = 0L
                    var lastTapPos = Offset.Zero
                    var hasFirst = false

                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        val downTime = down.uptimeMillis
                        val downPos = down.position

                        var maxPointers = 1
                        var maxMove = 0f
                        var abnormal = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.count { it.pressed }
                            if (pressed > maxPointers) maxPointers = pressed
                            if (pressed > 2) abnormal = true
                            event.changes.forEach { c ->
                                if (c.pressed) {
                                    val d = (c.position - downPos).getDistance()
                                    if (d > maxMove) maxMove = d
                                }
                            }
                            if (pressed == 0) break
                        }

                        val upTime = SystemClock.uptimeMillis()
                        val duration = upTime - downTime

                        val isTwoFingerTap = !abnormal &&
                            maxPointers == 2 &&
                            duration <= singleTapMaxDuration &&
                            maxMove <= touchSlop

                        if (!isTwoFingerTap) {
                            hasFirst = false
                        } else {
                            val since = upTime - lastTapEnd
                            val dist = (downPos - lastTapPos).getDistance()
                            if (hasFirst && since in 0..doubleTapWindow && dist <= doubleTapSlop) {
                                hasFirst = false
                                onGestureDetected()
                                haptics.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                            } else {
                                hasFirst = true
                                lastTapEnd = upTime
                                lastTapPos = downPos
                            }
                        }
                    }
                }
        )
    }
}


================================================================================
10. МОДУЛЬ :app — UI
================================================================================

---- App.kt ----

package com.example.twitterwrapper

import android.app.Application
import com.example.core.settings.SettingsRepository

class App : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}


---- MainViewModel.kt ----

package com.example.twitterwrapper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.settings.Settings
import com.example.core.settings.SettingsRepository
import com.example.core.update.UpdateChecker
import com.example.core.update.UpdateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = (app as App).settingsRepository

    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Settings()
    )

    private val updateChecker = UpdateChecker(
        context = app,
        owner = "YOUR_GITHUB_USERNAME",
        repo = "YOUR_REPO_NAME",
        currentVersionCode = BuildConfig.VERSION_CODE,
    )

    val updateState: StateFlow<UpdateState> = updateChecker.state

    fun checkUpdates() = updateChecker.check()
    fun downloadUpdate() {
        val state = updateState.value
        if (state is UpdateState.Available) updateChecker.downloadAndVerify(state.info)
    }
    fun resetUpdateState() = updateChecker.reset()

    fun saveCss(css: String) = viewModelScope.launch { settingsRepo.setCss(css) }
    fun saveJs(js: String) = viewModelScope.launch { settingsRepo.setJs(js) }
    fun setOnboardingShown(v: Boolean) =
        viewModelScope.launch { settingsRepo.setOnboardingShown(v) }
}


---- MainActivity.kt ----

package com.example.twitterwrapper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.twitterwrapper.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    val vm: MainViewModel = viewModel()
                    MainScreen(vm)
                }
            }
        }
    }
}


---- MainScreen.kt ----

package com.example.twitterwrapper.ui

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.settings.Settings
import com.example.core.update.UpdateState
import com.example.core.webview.*
import com.example.twitterwrapper.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    val settings by vm.settings.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.onboardingShown) {
        if (!settings.onboardingShown) showOnboarding = true
    }

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

    TwoFingerDoubleTapOverlay(onGestureDetected = { showMenu = true }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebViewSetup.create(
                    context = context,
                    siteMode = settings.siteMode,
                    userCss = settings.userCss,
                    userJs = settings.userJs,
                    onError = { msg -> errorMessage = msg },
                    onPageLoaded = { errorMessage = null },
                ).also { wv ->
                    DownloadHandler.attach(wv, context)
                    val fileChooser = FileChooserHandler(activity, wv)
                    val geo = GeoPermissionHandler(activity)
                    wv.webChromeClient = fileChooser.chromeClient(
                        geo.chromeClient(WebChromeClient())
                    )
                    webView = wv
                }
            },
        )
    }

    if (errorMessage != null) {
        ErrorOverlay(
            message = errorMessage!!,
            onRetry = { webView?.reload(); errorMessage = null },
        )
    }

    if (showMenu) {
        MenuOverlay(
            onDismiss = { showMenu = false },
            onSettings = { showMenu = false; showSettings = true },
            onCheckUpdates = {
                showMenu = false
                showUpdate = true
                vm.checkUpdates()
            },
            onReload = { showMenu = false; webView?.reload() },
        )
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onDismiss = { showSettings = false },
            onSaveCss = vm::saveCss,
            onSaveJs = vm::saveJs,
            onReloadWebView = { webView?.reload() },
        )
    }

    if (showUpdate) {
        UpdateDialog(
            state = updateState,
            onDismiss = { showUpdate = false; vm.resetUpdateState() },
            onRetry = vm::checkUpdates,
            onDownload = vm::downloadUpdate,
        )
    }

    if (showOnboarding) {
        OnboardingOverlay(
            onDismiss = {
                showOnboarding = false
                vm.setOnboardingShown(true)
            }
        )
    }
}


---- MenuOverlay.kt ----

package com.example.twitterwrapper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuOverlay(
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReload: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            MenuItem("Настройки", Icons.Default.Settings, onSettings)
            MenuItem("Проверить обновления", Icons.Default.SystemUpdate, onCheckUpdates)
            MenuItem("Обновить страницу", Icons.Default.Refresh, onReload)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(text, modifier = Modifier.weight(1f))
    }
}


---- ErrorOverlay.kt ----

package com.example.twitterwrapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Нет подключения",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}


---- SettingsScreen.kt ----

package com.example.twitterwrapper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.settings.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onDismiss: () -> Unit,
    onSaveCss: (String) -> Unit,
    onSaveJs: (String) -> Unit,
    onReloadWebView: () -> Unit,
) {
    var css by remember(settings.userCss) { mutableStateOf(settings.userCss) }
    var js by remember(settings.userJs) { mutableStateOf(settings.userJs) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("CSS-стили", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = css,
                onValueChange = { css = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text("Введите CSS для инъекции") },
            )
            Spacer(Modifier.height(16.dp))

            Text("JavaScript", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = js,
                onValueChange = { js = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text("Введите JS для инъекции") },
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onSaveCss(css)
                    onSaveJs(js)
                    onReloadWebView()
                    onDismiss()
                }) { Text("Сохранить") }
                OutlinedButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    }
}


---- UpdateDialog.kt ----

package com.example.twitterwrapper.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.update.ApkInstaller
import com.example.core.update.UpdateState
import java.io.File

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (state !is UpdateState.Downloading) onDismiss()
        },
        title = { Text("Обновление") },
        text = {
            when (state) {
                UpdateState.Idle -> Text("Нажмите «Проверить», чтобы начать.")
                UpdateState.Checking -> Row {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Проверка...")
                }
                UpdateState.UpToDate -> Text("У вас последняя версия.")
                is UpdateState.Available -> Column {
                    Text("Доступна версия ${state.info.versionName}")
                    Spacer(Modifier.height(8.dp))
                    if (state.info.changelog.isNotBlank()) {
                        Text(
                            state.info.changelog.take(500),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${(state.progress * 100).toInt()}%")
                }
                UpdateState.Verifying -> Text("Проверка SHA-256...")
                is UpdateState.ReadyToInstall -> Text(
                    "APK готов к установке. Нажмите «Установить»."
                )
                is UpdateState.Error -> Text("Ошибка: ${state.message}")
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available ->
                    TextButton(onClick = onDownload) { Text("Скачать") }
                is UpdateState.ReadyToInstall ->
                    TextButton(onClick = {
                        val file = File(state.apkPath)
                        if (ApkInstaller.canInstall(context)) {
                            ApkInstaller.install(context, file)
                        } else {
                            ApkInstaller.openPermissionSettings(context)
                        }
                    }) { Text("Установить") }
                is UpdateState.Error, UpdateState.UpToDate ->
                    TextButton(onClick = onRetry) { Text("Повторить") }
                else -> Unit
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) {
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        },
    )
}


---- OnboardingOverlay.kt ----

package com.example.twitterwrapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Card(Modifier.padding(24.dp)) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Как открыть меню",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Коснитесь экрана двумя пальцами дважды подряд " +
                    "(двойной тап двумя пальцами).",
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss) { Text("Понятно") }
            }
        }
    }
}


================================================================================
11. GITHUB ACTIONS — СБОРКА И РЕЛИЗ
================================================================================

---- .github/workflows/release.yml ----

name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Decode keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks

      - name: Compute version from tag
        id: ver
        run: |
          TAG="${GITHUB_REF#refs/tags/}"
          VER="${TAG#v}"
          IFS='.' read -r MAJ MIN PAT <<< "$VER"
          CODE=$((MAJ*10000 + MIN*100 + PAT))
          echo "name=$VER" >> $GITHUB_OUTPUT
          echo "code=$CODE" >> $GITHUB_OUTPUT

      - name: Build release APK
        env:
          KEYSTORE_PATH: keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          VERSION_NAME: ${{ steps.ver.outputs.name }}
          VERSION_CODE: ${{ steps.ver.outputs.code }}
        run: |
          # Перезаписываем versionCode/Name в build.gradle.kts
          sed -i "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" app/build.gradle.kts
          sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VERSION_NAME\"/" app/build.gradle.kts
          ./gradlew :app:assembleRelease

      - name: Rename & checksum
        run: |
          mkdir -p dist
          cp app/build/outputs/apk/release/app-release.apk \
             dist/app-${{ steps.ver.outputs.name }}.apk
          cd dist
          sha256sum app-${{ steps.ver.outputs.name }}.apk > checksums.txt
          cat checksums.txt

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ github.ref_name }}
          name: ${{ steps.ver.outputs.name }}
          body: |
            Автоматический релиз ${{ steps.ver.outputs.name }}.
            Установите APK из вложений.
          files: |
            dist/app-${{ steps.ver.outputs.name }}.apk
            dist/checksums.txt


================================================================================
12. ПОРЯДОК ЗАПУСКА
================================================================================

1. Создать репозиторий на GitHub, назвать, например, twitter-wrapper.

2. Скопировать структуру и файлы из разделов 4–11 в репозиторий.
   Заменить YOUR_GITHUB_USERNAME / YOUR_REPO_NAME в MainViewModel.

3. Сгенерировать keystore локально:

     keytool -genkeypair -v \
       -keystore keystore.jks \
       -alias mykey \
       -keyalg RSA -keysize 2048 -validity 10000

4. Добавить в GitHub Secrets:
     KEYSTORE_BASE64      = base64 -w0 keystore.jks
     KEYSTORE_PASSWORD    = <пароль хранилища>
     KEY_ALIAS            = mykey
     KEY_PASSWORD         = <пароль ключа>

5. Сделать первый коммит, поставить тег v1.0.0, запушить:

     git tag v1.0.0
     git push origin v1.0.0

   CI соберёт APK, создаст Release с APK и checksums.txt.

6. Скачать APK на устройство, разрешить установку из неизвестных
   источников, установить.

7. Проверить сценарии:
     - Открытие x.com (мобильный режим).
     - Двойной тап двумя пальцами → меню.
     - Настройки → сохранить CSS → перезагрузка → стили применились.
     - Меню → Проверить обновления → с версией 1.0.0 при наличии v1.0.1
       должно показать «Доступна версия 1.0.1».
     - Скачивание → проверка SHA-256 → установка через системный
       диалог.
     - Вход в аккаунт (cookies сохраняются).
     - Загрузка файла через <input type="file">.
     - Внешняя ссылка открывается в браузере.
     - Ошибка сети → оверлей «Повторить».


================================================================================
13. ИЗВЕСТНЫЕ ОГРАНИЧЕНИЯ И РЕКОМЕНДАЦИИ
================================================================================

* X/Twitter — SPA. Пользовательские CSS-селекторы будут ломаться после
  каждого редизайна. Заложите кнопку «Сбросить правки» и версионирование
  профилей.

* Blob-URL (медиа) не скачиваются через DownloadManager. При необходимости
  добавьте JS-бридж: перехватывать fetch на blob, читать в base64, отдавать
  через @JavascriptInterface в Kotlin и сохранять.

* PackageInstaller (тихая установка с прогрессом) не реализован —
  использован Intent+FileProvider. Для кастомного UI установки смотрите
  Android PackageInstaller API.

* R8/ProGuard выключены по требованию. Это ухудшает размер и защиту, но
  снижает риск багов с рефлексией/Compose.

* На Android 10+ установка APK требует ручного подтверждения
  «Разрешить установку из этого источника». Один раз на версию —
  нормальный UX.

* Не используйте GitHub Token в APK: он утекает. Публичный API хватает
  (60 запросов/час на IP, при ручной проверке — с запасом).

* Двойной тап двумя пальцами не обнаружен на некоторых WebView, если
  оверлей перехватывает события — используйте fallback через
  webView.setOnTouchListener (см. раздел 8, TwoFingerDoubleTap).

* CI перезаписывает versionCode/Name в build.gradle.kts через sed.
  Это хрупко; при желании замените на передачу через -P параметры Gradle
  и чтение их в build.gradle.kts.

* Проверка обновлений ручная — никаких фоновых Worker'ов. Если понадобится,
  добавьте WorkManager с периодичностью 24 часа.

* Локализация: значения строк вынесены в values/strings.xml и
  values-ru/strings.xml. В коде приложения использованы русские строки
  напрямую для краткости — вынесите их в ресурсы перед публикацией.


================================================================================
14. КОНТРОЛЬНЫЙ СПИСОК РЕЛИЗА
================================================================================

[ ] versionName в build.gradle.kts соответствует тегу (v1.2.3 → 1.2.3)
[ ] GitHub Secrets настроены (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS,
    KEY_PASSWORD)
[ ] Тег создан и запушен (git tag vX.Y.Z && git push origin vX.Y.Z)
[ ] Release содержит APK и checksums.txt
[ ] APK подписан тем же ключом, что и предыдущие версии
[ ] versionCode увеличился относительно предыдущего релиза
[ ] Проверено обновление с предыдущей версии на новую на реальном
    устройстве
[ ] SHA-256 в checksums.txt совпадает с фактическим
[ ] Проверен онбординг на чистой установке
[ ] Проверено, что WebView грузит x.com после смены режима
    MOBILE ↔ DESKTOP (сброс cookies не требуется)


================================================================================
КОНЕЦ ДОКУМЕНТА
================================================================================