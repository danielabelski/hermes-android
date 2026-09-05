package com.hermeswebui.android

import com.google.common.truth.Truth.assertThat
import com.hermeswebui.android.domain.TailscaleEndpointDetector
import org.junit.Test

class TailscaleEndpointDetectorTest {
    @Test
    fun `detects ts net hostname`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("https://node.tailabc.ts.net")).isTrue()
    }

    @Test
    fun `detects tailscale cgnat ipv4`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.101.102.103:8080")).isTrue()
    }

    @Test
    fun `detects tailscale ula ipv6`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("https://[fd7a:115c:a1e0::12]")).isTrue()
    }

    @Test
    fun `ignores non tailscale hosts`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("https://hermes.example.com")).isFalse()
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("https://192.168.1.12")).isFalse()
    }

    @Test
    fun `rejects cgnat addresses just outside the tailscale range`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.63.0.1")).isFalse()
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.128.0.1")).isFalse()
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://101.64.0.1")).isFalse()
    }

    @Test
    fun `detects cgnat range boundaries`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.64.0.1")).isTrue()
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.127.255.255")).isTrue()
    }

    @Test
    fun `rejects invalid ipv4 octets and malformed hosts`() {
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("http://100.999.1.1")).isFalse()
        assertThat(TailscaleEndpointDetector.isTailscaleUrl("https://not-a-url")).isFalse()
    }
}
