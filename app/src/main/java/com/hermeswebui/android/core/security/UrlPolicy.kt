package com.hermeswebui.android.core.security

import java.net.URI
import java.util.Locale

enum class NavigationDecision {
    ALLOW_IN_WEBVIEW,
    OPEN_IN_EXTERNAL_BROWSER,
    BLOCK
}

class UrlPolicy(private val allowedHosts: Set<String>) {
    fun isAllowed(url: String): Boolean {
        val uri = url.toUriOrNull() ?: return false
        if (!uri.isHttpsScheme()) return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return host in allowedHosts || allowedHosts.any { host.endsWith(".$it") }
    }

    fun navigationDecision(url: String): NavigationDecision {
        val uri = url.toUriOrNull() ?: return NavigationDecision.BLOCK
        if (!uri.isHttpsScheme()) return NavigationDecision.BLOCK
        return if (isAllowed(url)) {
            NavigationDecision.ALLOW_IN_WEBVIEW
        } else {
            NavigationDecision.OPEN_IN_EXTERNAL_BROWSER
        }
    }

    private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()

    private fun URI.isHttpsScheme(): Boolean {
        return scheme.equals("https", ignoreCase = true)
    }
}
