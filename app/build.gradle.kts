plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun semverToVersionCode(name: String): Int {
    val match = Regex("""^[vV]?(\d{1,4})\.(\d{1,2})\.(\d{1,2})$""").find(name.trim())
        ?: throw GradleException("versionName must look like MAJOR.MINOR.PATCH, got '$name'")
    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].toInt()
    if (major > 2000 || minor > 99 || patch > 99) {
        throw GradleException("versionName '$name' is outside the supported range (major<=2000, minor<=99, patch<=99)")
    }
    return major * 10_000 + minor * 100 + patch
}

val githubOwner = providers.gradleProperty("github.owner").orElse("CHANGE_ME").get()
val githubRepo = providers.gradleProperty("github.repo").orElse("CHANGE_ME").get()

val appVersionName = providers.gradleProperty("versionName")
    .orElse("1.0.0")
    .get()
    .trim()
    .removePrefix("v")
    .removePrefix("V")

// Single source of truth: the version code is always derived from the name.
// An explicit -PversionCode is accepted only when it matches, so a typo cannot
// produce an APK that the update checker considers newer/older by accident.
val derivedVersionCode = semverToVersionCode(appVersionName)
val appVersionCode = providers.gradleProperty("versionCode").getOrNull()?.trim()?.let { raw ->
    val explicit = raw.toIntOrNull()
        ?: throw GradleException("versionCode must be an integer, got '$raw'")
    if (explicit != derivedVersionCode) {
        throw GradleException(
            "versionCode $explicit does not match versionName '$appVersionName' " +
                "(expected $derivedVersionCode)",
        )
    }
    explicit
} ?: derivedVersionCode

val keystorePath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val keystoreFile = keystorePath?.let { rootProject.file(it) }
val releaseSigningAvailable = keystoreFile?.isFile == true &&
    !System.getenv("KEYSTORE_PASSWORD").isNullOrEmpty() &&
    !System.getenv("KEY_ALIAS").isNullOrEmpty()

android {
    namespace = "com.efx.twitterwrapper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.efx.twitterwrapper"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "GITHUB_OWNER", "\"$githubOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = keystoreFile!!
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
        )
    }
}

dependencies {
    implementation(project(":core:settings"))
    implementation(project(":core:update-checker"))
    implementation(project(":core:webview"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.core.ktx)
}
