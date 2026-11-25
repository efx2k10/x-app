package com.efx.twitterwrapper.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.efx.twitterwrapper.BuildConfig
import com.efx.twitterwrapper.MainActivity
import com.efx.twitterwrapper.R
import com.efx.twitterwrapper.core.settings.Settings
import com.efx.twitterwrapper.core.settings.SiteMode
import com.efx.twitterwrapper.core.update.ApkInstaller
import com.efx.twitterwrapper.core.update.UpdateState
import com.efx.twitterwrapper.core.webview.CssJsInjector
import com.efx.twitterwrapper.core.webview.DownloadHandler
import com.efx.twitterwrapper.core.webview.TwitterWebView
import com.efx.twitterwrapper.core.webview.TwitterWebViewClient
import com.efx.twitterwrapper.core.webview.WebViewCallbacks
import com.efx.twitterwrapper.core.webview.WebViewSetup
import com.efx.twitterwrapper.vm.MainViewModel
import com.efx.twitterwrapper.web.AppChromeClient
import java.io.File

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()

    val loaded = settings
    if (loaded == null) {
        SplashScreen()
        return
    }

    MainContent(
        vm = vm,
        activity = LocalContext.current as MainActivity,
        settings = loaded,
        updateState = updateState,
    )
}

@Composable
private fun MainContent(
    vm: MainViewModel,
    activity: MainActivity,
    settings: Settings,
    updateState: UpdateState,
) {
    val context = LocalContext.current
    val holder = remember { WebViewHolder() }

    var generation by rememberSaveable { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<WebError?>(null) }

    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showUpdate by rememberSaveable { mutableStateOf(false) }
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }

    var lastAppliedMode by remember { mutableStateOf(settings.siteMode) }
    var lastCss by remember { mutableStateOf(settings.userCss) }
    var lastJs by remember { mutableStateOf(settings.userJs) }

    val callbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {
                isLoading = true
                progress = 0
                error = null
            }

            override fun onPageCommit(url: String?) {
                holder.injector?.applyOnPage(url)
            }

            override fun onPageFinished(url: String?) {
                isLoading = false
                progress = 100
            }

            override fun onProgressChanged(progressValue: Int) {
                progress = progressValue
                if (progressValue >= 100) isLoading = false
            }

            override fun onCanGoBackChanged(canGoBackNow: Boolean) {
                canGoBack = canGoBackNow
            }

            override fun onNetworkError(description: String?) {
                error = WebError.Generic(description)
            }

            override fun onHttpError(statusCode: Int) {
                error = WebError.Http(statusCode)
            }

            override fun onSslError() {
                error = WebError.Ssl
            }

            override fun onRendererGone() {
                holder.webView = null
                holder.injector = null
                generation += 1
                Toast.makeText(context, R.string.renderer_restarted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val downloadCallbacks = remember(context) {
        object : DownloadHandler.Callbacks {
            override fun onDownloadStarted(fileName: String) {
                Toast.makeText(
                    context,
                    context.getString(R.string.download_started, fileName),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun onUnsupported(url: String) {
                Toast.makeText(context, R.string.download_unsupported, Toast.LENGTH_SHORT).show()
            }

            override fun onError(message: String?) {
                Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // DownloadManager needs WRITE_EXTERNAL_STORAGE only on Android 7-9.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(settings.siteMode) {
        if (settings.siteMode != lastAppliedMode) {
            lastAppliedMode = settings.siteMode
            holder.webView?.let { webView ->
                WebViewSetup.applySiteMode(webView, settings.siteMode == SiteMode.DESKTOP)
                webView.reload()
            }
        }
    }

    LaunchedEffect(settings.userCss, settings.userJs) {
        if (settings.userCss != lastCss || settings.userJs != lastJs) {
            lastCss = settings.userCss
            lastJs = settings.userJs
            holder.injector?.setInjection(settings.userCss, settings.userJs)
            holder.webView?.reload()
        }
    }

    val isFullscreen = activity.isFullscreen.value
    BackHandler(enabled = canGoBack && !isFullscreen) {
        holder.webView?.goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            key(generation) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        createWebView(
                            context = viewContext,
                            activity = activity,
                            settings = settings,
                            callbacks = callbacks,
                            downloadCallbacks = downloadCallbacks,
                            holder = holder,
                            onProgress = callbacks::onProgressChanged,
                            onMenuGesture = { showMenu = true },
                        )
                    },
                    update = { container ->
                        (container.getChildAt(0) as? TwitterWebView)?.onMenuGesture = { showMenu = true }
                    },
                    onRelease = { container ->
                        val webView = container.getChildAt(0) as? TwitterWebView ?: return@AndroidView
                        if (holder.webView === webView) {
                            holder.webView = null
                            holder.injector = null
                        }
                        container.removeView(webView)
                        webView.destroy()
                    },
                )
            }

            if (isLoading && error == null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }

        error?.let { currentError ->
            ErrorOverlay(currentError) {
                error = null
                holder.webView?.reload()
            }
        }

        if (showMenu) {
            MenuOverlay(
                versionName = BuildConfig.VERSION_NAME,
                onDismiss = { showMenu = false },
                onSettings = {
                    showMenu = false
                    showSettings = true
                },
                onReload = {
                    showMenu = false
                    holder.webView?.reload()
                },
                onCheckUpdates = {
                    showMenu = false
                    showUpdate = true
                    vm.checkForUpdates()
                },
            )
        }

        if (showSettings) {
            SettingsScreen(
                settings = settings,
                onDismiss = { showSettings = false },
                onSave = { css, js ->
                    vm.saveInjection(css, js)
                    showSettings = false
                },
                onReset = {
                    vm.resetInjection()
                    showSettings = false
                },
                onSiteModeChange = vm::setSiteMode,
            )
        }

        if (showUpdate) {
            UpdateDialog(
                state = updateState,
                onDismiss = {
                    vm.cancelUpdate()
                    showUpdate = false
                },
                onRetry = vm::checkForUpdates,
                onDownload = vm::downloadUpdate,
                onInstall = { apkPath ->
                    val apk = File(apkPath)
                    when {
                        !apk.isFile -> Toast.makeText(
                            context,
                            R.string.update_apk_missing,
                            Toast.LENGTH_SHORT,
                        ).show()

                        !ApkInstaller.canInstall(context) -> ApkInstaller.openPermissionSettings(context)

                        else -> runCatching { ApkInstaller.install(context, apk) }
                            .onFailure {
                                Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                },
            )
        }

        val showOnboarding = !settings.onboardingShown && !onboardingDismissed
        if (showOnboarding) {
            OnboardingOverlay(
                onDismiss = {
                    onboardingDismissed = true
                    vm.setOnboardingShown(true)
                },
                onOpenMenu = {
                    onboardingDismissed = true
                    vm.setOnboardingShown(true)
                    showMenu = true
                },
            )
        }
    }
}

private fun createWebView(
    context: Context,
    activity: MainActivity,
    settings: Settings,
    callbacks: WebViewCallbacks,
    downloadCallbacks: DownloadHandler.Callbacks,
    holder: WebViewHolder,
    onProgress: (Int) -> Unit,
    onMenuGesture: () -> Unit,
): FrameLayout {
    val webView = TwitterWebView(context)
    WebViewSetup.configure(webView, settings.siteMode == SiteMode.DESKTOP)
    webView.webViewClient = TwitterWebViewClient(context, callbacks)
    webView.webChromeClient = AppChromeClient(activity, onProgress)
    webView.onMenuGesture = onMenuGesture
    DownloadHandler.attach(webView, downloadCallbacks)

    val injector = CssJsInjector(webView)
    injector.setInjection(settings.userCss, settings.userJs)

    holder.webView = webView
    holder.injector = injector

    // A FrameLayout as the direct AndroidView child is required: Compose
    // measures the direct child in a way that leaves Blink's viewport-unit
    // height at 0 (100vh/100dvh == 0), which collapses dialogs sized with dvh
    // (the X login form is one). The wrapper gives the WebView a normal size.
    val container = FrameLayout(context)
    container.addView(
        webView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    webView.doOnLayout { webView.loadUrl(WebViewSetup.HOME_URL) }
    return container
}

private class WebViewHolder {
    var webView: TwitterWebView? = null
    var injector: CssJsInjector? = null
}
