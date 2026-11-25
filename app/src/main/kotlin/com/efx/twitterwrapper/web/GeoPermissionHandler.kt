package com.efx.twitterwrapper.web

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class GeoPermissionHandler(private val activity: ComponentActivity) {

    private var pending: Pair<String, GeolocationPermissions.Callback>? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        pending?.let { (origin, callback) -> callback.invoke(origin, granted, false) }
        pending = null
    }

    fun request(origin: String, callback: GeolocationPermissions.Callback) {
        if (hasLocationPermission()) {
            callback.invoke(origin, true, false)
            return
        }

        pending?.let { (pendingOrigin, pendingCallback) ->
            pendingCallback.invoke(pendingOrigin, false, false)
        }
        pending = origin to callback

        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
