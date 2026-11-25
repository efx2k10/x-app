package com.efx.twitterwrapper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efx.twitterwrapper.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuOverlay(
    versionName: String,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onReload: () -> Unit,
    onCheckUpdates: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            MenuRow(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.menu_settings),
                onClick = onSettings,
            )
            MenuRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.menu_reload),
                onClick = onReload,
            )
            MenuRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.menu_check_updates),
                onClick = onCheckUpdates,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(R.string.menu_version, versionName),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(20.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}
