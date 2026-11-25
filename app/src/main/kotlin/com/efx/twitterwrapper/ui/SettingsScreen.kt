package com.efx.twitterwrapper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.efx.twitterwrapper.R
import com.efx.twitterwrapper.core.settings.Settings
import com.efx.twitterwrapper.core.settings.SiteMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onDismiss: () -> Unit,
    onSave: (css: String, js: String) -> Unit,
    onReset: () -> Unit,
    onSiteModeChange: (SiteMode) -> Unit,
) {
    var css by remember(settings.userCss) { mutableStateOf(settings.userCss) }
    var js by remember(settings.userJs) { mutableStateOf(settings.userJs) }

    // This screen is drawn over the WebView, so it owns the back button while open.
    BackHandler(onBack = onDismiss)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_site_version),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.siteMode == SiteMode.MOBILE,
                    onClick = { onSiteModeChange(SiteMode.MOBILE) },
                    label = { Text(stringResource(R.string.settings_mode_mobile)) },
                )
                FilterChip(
                    selected = settings.siteMode == SiteMode.DESKTOP,
                    onClick = { onSiteModeChange(SiteMode.DESKTOP) },
                    label = { Text(stringResource(R.string.settings_mode_desktop)) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = stringResource(R.string.settings_css),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = css,
                onValueChange = { css = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                placeholder = { Text(stringResource(R.string.settings_css_hint)) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            Text(
                text = stringResource(R.string.settings_js),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = js,
                onValueChange = { js = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                placeholder = { Text(stringResource(R.string.settings_js_hint)) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            Text(
                text = stringResource(R.string.settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(css, js) }) {
                    Text(stringResource(R.string.settings_save))
                }
                OutlinedButton(onClick = onReset) {
                    Text(stringResource(R.string.settings_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        }
    }
}
