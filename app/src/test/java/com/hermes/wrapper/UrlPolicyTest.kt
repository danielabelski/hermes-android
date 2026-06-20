package com.hermes.wrapper

import com.google.common.truth.Truth.assertThat
import com.hermes.wrapper.core.security.NavigationDecision
import com.hermes.wrapper.core.security.UrlPolicy
import org.junit.Test

class UrlPolicyTest {
    private val policy = UrlPolicy(setOf("hermes.example.com"))

    @Test
    fun `allows allowlisted host over https`() {
        assertThat(policy.isAllowed("https://hermes.example.com")).isTrue()
    }

    @Test
    fun `blocks allowlisted host over http`() {
        assertThat(policy.navigationDecision("http://hermes.example.com")).isEqualTo(NavigationDecision.BLOCK)
    }

    @Test
    fun `opens non allowlisted https hosts externally`() {
        assertThat(policy.navigationDecision("https://example.org/docs")).isEqualTo(NavigationDecision.OPEN_IN_EXTERNAL_BROWSER)
    }

    @Test
    fun `allows allowlisted subdomains`() {
        assertThat(policy.navigationDecision("https://api.hermes.example.com")).isEqualTo(NavigationDecision.ALLOW_IN_WEBVIEW)
    }

    @Test
    fun `blocks non-web schemes`() {
        assertThat(policy.navigationDecision("ftp://hermes.example.com")).isEqualTo(NavigationDecision.BLOCK)
    }
}
