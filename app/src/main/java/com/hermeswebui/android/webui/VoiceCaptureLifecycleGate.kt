package com.hermeswebui.android.webui

/**
 * Android permission gate and proposed `hermes-app-foreground` host signal.
 *
 * WebUI owns capture, wake, STT, TTS and every profile/session/generation check. Android only
 * denies new WebView microphone requests while the activity is not resumed. A trusted pending
 * RECORD_AUDIO result may be granted while started, just before onResume. Android reports
 * foreground state but does not stop already-granted tracks or playback. Current WebUI does not
 * consume this signal; stopping existing capture/playback and explicit re-arm require a consumer.
 *
 * Generic over the WebView permission request so the lifecycle rules are JVM-testable.
 */
class VoiceCaptureLifecycleGate<R : Any>(
    private val grantAudioCapture: (R) -> Unit,
    private val denyAudioCapture: (R) -> Unit,
    private val launchRecordAudioPrompt: () -> Unit,
    private val signalAppForeground: (active: Boolean) -> Unit
) {
    private var started = false
    private var resumed = false
    private var pendingPromptRequest: R? = null

    val isRecordAudioPromptPending: Boolean
        get() = pendingPromptRequest != null

    fun onStart() {
        started = true
    }

    fun onResume() {
        resumed = true
        signalAppForeground(true)
    }

    fun onPause() {
        resumed = false
        // The RECORD_AUDIO prompt itself pauses this activity. No capture can exist before
        // that permission is granted, so keep the arm the user just tapped alive across it.
        if (pendingPromptRequest == null) {
            signalAppForeground(false)
        }
    }

    fun onStop() {
        started = false
        resumed = false
        pendingPromptRequest?.let {
            pendingPromptRequest = null
            denyAudioCapture(it)
        }
        signalAppForeground(false)
    }

    /** Re-syncs a freshly loaded WebUI document, which starts without host state. */
    fun onPageReady() {
        signalAppForeground(resumed)
    }

    fun onCaptureRequested(request: R, trustedAudioRequest: Boolean, hasRecordAudioPermission: Boolean) {
        if (!trustedAudioRequest || !resumed) {
            denyAudioCapture(request)
            return
        }
        if (hasRecordAudioPermission) {
            grantAudioCapture(request)
            return
        }
        pendingPromptRequest?.let(denyAudioCapture)
        pendingPromptRequest = request
        launchRecordAudioPrompt()
    }

    /** Returns true when the user denied RECORD_AUDIO for a pending WebView request. */
    fun onRecordAudioPromptResult(granted: Boolean, stillTrusted: (R) -> Boolean): Boolean {
        val request = pendingPromptRequest ?: return false
        pendingPromptRequest = null
        // A result delivered after onStop belongs to a foreground the user already left.
        if (granted && started && stillTrusted(request)) {
            grantAudioCapture(request)
        } else {
            denyAudioCapture(request)
        }
        return !granted
    }

    fun onCaptureRequestCanceled(request: R) {
        if (pendingPromptRequest == request) {
            pendingPromptRequest = null
        }
    }
}
