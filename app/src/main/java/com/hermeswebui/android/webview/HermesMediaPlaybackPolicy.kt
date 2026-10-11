package com.hermeswebui.android.webview

import android.webkit.WebView
import com.hermeswebui.android.core.security.WebTrustPolicy

object HermesMediaPlaybackPolicy {
    fun beforeAllowedMainFrameNavigation(view: WebView, target: String, trustPolicy: WebTrustPolicy) {
        if (!trustPolicy.allowsGestureFreeMediaPlayback(target)) {
            view.settings.mediaPlaybackRequiresUserGesture = true
        }
    }

    fun onDocumentReady(view: WebView, trustPolicy: WebTrustPolicy) {
        view.settings.mediaPlaybackRequiresUserGesture =
            !trustPolicy.allowsGestureFreeMediaPlayback(view.url)
    }
}
