package com.efx.twitterwrapper.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.efx.twitterwrapper.R
import com.efx.twitterwrapper.core.update.ApkInstaller
import com.efx.twitterwrapper.core.update.UpdateState

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (String) -> Unit,
) {
    val context = LocalContext.current
    var canInstall by remember { mutableStateOf(ApkInstaller.canInstall(context)) }

    // The user may leave for the system settings screen and come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canInstall = ApkInstaller.canInstall(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val busy = state is UpdateState.Downloading || state is UpdateState.Verifying

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                when (state) {
                    UpdateState.Idle -> Text(stringResource(R.string.update_idle))

                    UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.update_checking))
                    }

                    UpdateState.UpToDate -> Text(stringResource(R.string.update_up_to_date))

                    is UpdateState.Available -> {
                        Text(stringResource(R.string.update_available, state.info.versionName))
                        if (state.info.changelog.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.info.changelog.take(4000),
                                modifier = Modifier
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.info.apkSize > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.update_size,
                                    Formatter.formatShortFileSize(context, state.info.apkSize),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.update_downloading, (state.progress * 100).toInt()))
                    }

                    UpdateState.Verifying -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.update_verifying))
                    }

                    is UpdateState.ReadyToInstall -> {
                        Text(stringResource(R.string.update_ready))
                        if (!canInstall) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.update_permission_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    is UpdateState.Error -> Text(
                        errorMessage(state.kind, state.detail),
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> TextButton(onClick = onDownload) {
                    Text(stringResource(R.string.update_download))
                }

                is UpdateState.ReadyToInstall -> TextButton(onClick = { onInstall(state.apkPath) }) {
                    Text(stringResource(R.string.update_install))
                }

                is UpdateState.Error, UpdateState.Idle, UpdateState.UpToDate -> TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.update_retry))
                }

                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        if (busy) R.string.update_cancel else R.string.update_close,
                    ),
                )
            }
        },
    )
}

@Composable
private fun errorMessage(kind: UpdateState.Error.Kind, detail: String?): String = when (kind) {
    UpdateState.Error.Kind.NOT_CONFIGURED -> stringResource(R.string.update_error_not_configured)
    UpdateState.Error.Kind.NETWORK -> stringResource(R.string.update_error_network)
    UpdateState.Error.Kind.HTTP -> stringResource(R.string.update_error_http, detail ?: "?")
    UpdateState.Error.Kind.RATE_LIMIT -> stringResource(R.string.update_error_rate_limit)
    UpdateState.Error.Kind.NO_RELEASE -> stringResource(R.string.update_error_no_release)
    UpdateState.Error.Kind.BAD_TAG -> stringResource(R.string.update_error_bad_tag)
    UpdateState.Error.Kind.NO_APK -> stringResource(R.string.update_error_no_apk)
    UpdateState.Error.Kind.NO_CHECKSUMS -> stringResource(R.string.update_error_no_checksums)
    UpdateState.Error.Kind.CHECKSUM_MISMATCH -> stringResource(R.string.update_error_checksum)
    UpdateState.Error.Kind.SIGNATURE_MISMATCH -> stringResource(R.string.update_error_signature)
    UpdateState.Error.Kind.IO -> stringResource(R.string.update_error_io)
    UpdateState.Error.Kind.PARSE -> stringResource(R.string.update_error_parse)
    UpdateState.Error.Kind.UNKNOWN -> stringResource(R.string.update_error_unknown)
}
