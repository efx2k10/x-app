package com.efx.twitterwrapper.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Verifies that a downloaded APK is signed by the same certificate as the
 * currently installed application. This is the only check that actually
 * protects against a malicious APK being published in the release.
 */
object ApkSignatureVerifier {

    fun isSignedByInstalledApp(
        context: Context,
        apkFile: File,
        expectedVersionCode: Int,
    ): Boolean {
        if (!apkFile.isFile) return false

        val packageManager = context.packageManager
        val flags = signingFlags()

        val archiveInfo = try {
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        } catch (t: Throwable) {
            null
        } ?: return false

        if (archiveInfo.packageName != context.packageName) return false
        if (archiveInfo.longVersionCode < expectedVersionCode) return false

        val archiveCertificates = certificateDigests(archiveInfo)
        val installedInfo: PackageInfo = try {
            packageManager.getPackageInfo(context.packageName, flags)
        } catch (t: PackageManager.NameNotFoundException) {
            return false
        }
        val installedCertificates = certificateDigests(installedInfo)

        return archiveCertificates.isNotEmpty() &&
            installedCertificates.isNotEmpty() &&
            archiveCertificates == installedCertificates
    }

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    @Suppress("DEPRECATION")
    private fun certificateDigests(info: PackageInfo): Set<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners ?: return emptySet()
        } else {
            info.signatures ?: return emptySet()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.mapTo(LinkedHashSet()) { signature ->
            digest.reset()
            digest.digest(signature.toByteArray()).joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xFF)
            }
        }
    }
}
