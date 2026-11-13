package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome

private data class QuickBookmark(val title: String, val url: String)

private val defaultBookmarks = listOf(
    QuickBookmark("Google", "https://www.google.com"),
    QuickBookmark("YouTube", "https://m.youtube.com"),
    QuickBookmark("Wikipedia", "https://tr.wikipedia.org"),
    QuickBookmark("GitHub", "https://github.com"),
    QuickBookmark("Outlook", "https://outlook.live.com"),
    QuickBookmark("TradingView", "https://www.tradingview.com")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWindowContent(
    initialUrl: String,
    loadNonce: Long,
    strings: AppStrings,
    onAddSiteToDesktop: (title: String, url: String) -> Unit
) {
    val chrome = LocalWinPadChrome.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val webViewProvider = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WebView.getCurrentWebViewPackage() else null
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var addressInput by remember { mutableStateOf(initialUrl) }
    var currentPageUrl by remember { mutableStateOf(initialUrl) }
    var currentPageTitle by remember { mutableStateOf("Google") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(100) }
    var showAddDialog by remember { mutableStateOf(false) }
    var shortcutSavedBanner by remember { mutableStateOf(false) }
    var lastHandledNonce by remember { mutableStateOf(-1L) }

    val navigateTo: (String) -> Unit = { raw ->
        val resolved = resolveBrowserUrl(raw)
        addressInput = resolved
        currentPageUrl = resolved
        focusManager.clearFocus()
        webViewRef?.loadUrl(resolved)
    }

    LaunchedEffect(initialUrl, loadNonce, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (lastHandledNonce != loadNonce) {
            lastHandledNonce = loadNonce
            val resolved = resolveBrowserUrl(initialUrl)
            addressInput = resolved
            currentPageUrl = resolved
            wv.loadUrl(resolved)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("built_in_browser")
    ) {
        // Browser Navigation + Address Bar
        Surface(
            color = chrome.cardSurface.copy(alpha = 0.85f),
            border = BorderStroke(0.6.dp, chrome.glassBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = strings.previousWindow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = strings.nextWindow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = strings.refresh,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { navigateTo("https://www.google.com") },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = strings.browser,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = {
                            Text(
                                text = strings.browserAddressHint,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = chrome.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateTo(addressInput) }
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("browser_address_input")
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAddDialog = true }
                            .testTag("add_site_to_desktop_button"),
                        color = chrome.accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.9.dp, chrome.accent.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddToHomeScreen,
                                contentDescription = strings.addSiteToDesktop,
                                tint = chrome.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.addToDesktop,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = chrome.accent,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(currentPageUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_open_external_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = strings.openInExternalBrowser,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Quick Bookmarks Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    defaultBookmarks.forEach { bm ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navigateTo(bm.url) },
                            color = chrome.windowSurface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.6.dp, chrome.glassBorder)
                        ) {
                            Text(
                                text = bm.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val providerVersion = webViewProvider?.versionName
              ?: webViewProvider?.packageName
              ?: strings.webViewProviderUnavailable
          Surface(
              modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(enabled = webViewProvider != null) {
                      webViewProvider?.let { provider ->
                          context.startActivity(
                              Intent(
                                  Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                  Uri.fromParts("package", provider.packageName, null)
                              )
                          )
                      }
                  }
                  .testTag("browser_webview_provider"),
              color = chrome.windowSurface.copy(alpha = 0.65f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(0.6.dp, chrome.glassBorder)
          ) {
              Text(
                  text = "${strings.webViewVersionLabel}: $providerVersion",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
              )
          }
          if (shortcutSavedBanner) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chrome.statusSuccess.copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, chrome.statusSuccess.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = chrome.statusSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = strings.addToDesktop,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chrome.statusSuccess
                                )
                            }
                        }
                    }
                }

                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        color = chrome.accent,
                        trackColor = chrome.glassBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )
                }
            }
        }

        // Embedded WebView Viewport
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url ?: return false
                                val scheme = target.scheme?.lowercase().orEmpty()
                                return if (scheme == "http" || scheme == "https") {
                                    false
                                } else {
                                    runCatching {
                                        ctx.startActivity(
                                            Intent(Intent.ACTION_VIEW, target)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                    true
                                }
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (!url.isNullOrBlank()) {
                                    addressInput = url
                                    currentPageUrl = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!url.isNullOrBlank()) {
                                    addressInput = url
                                    currentPageUrl = url
                                }
                                view?.title?.takeIf { it.isNotBlank() }?.let {
                                    currentPageTitle = it
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    currentPageTitle = title
                                }
                            }
                        }
                        loadUrl(resolveBrowserUrl(initialUrl))
                        webViewRef = this
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.destroy()
                }
            )
        }
    }

    if (showAddDialog) {
        val suggestedTitle = remember(currentPageTitle, currentPageUrl) {
            currentPageTitle.takeIf { it.isNotBlank() && !it.startsWith("http") }
                ?: runCatching { Uri.parse(currentPageUrl).host?.removePrefix("www.") }.getOrNull()
                ?: "Web App"
        }
        var titleInput by remember(currentPageUrl) { mutableStateOf(suggestedTitle.take(28)) }
        var urlInput by remember(currentPageUrl) { mutableStateOf(currentPageUrl) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    tint = chrome.accent
                )
            },
            title = { Text(strings.addSiteToDesktop, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        singleLine = true,
                        label = { Text(strings.siteTitleHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        label = { Text(strings.siteUrlHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.cancel)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSiteToDesktop(titleInput.ifBlank { suggestedTitle }, urlInput)
                        shortcutSavedBanner = true
                        showAddDialog = false
                    }
                ) {
                    Text(strings.addToDesktop)
                }
            }
        )
    }
}

private fun resolveBrowserUrl(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return "https://www.google.com"
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    return if (!trimmed.contains(" ") && trimmed.contains(".")) {
        "https://$trimmed"
    } else {
        "https://www.google.com/search?q=${Uri.encode(trimmed)}"
    }
}
