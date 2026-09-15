package com.hermeswebui.android

import com.google.common.truth.Truth.assertThat
import com.hermeswebui.android.core.security.NavigationDecision
import com.hermeswebui.android.core.security.UrlOrigins
import com.hermeswebui.android.core.security.UrlPolicy
import org.junit.Test

class UrlPolicyTest {
    private val policy = UrlPolicy(setOf("hermes.example.com"))

    @Test
    fun `allows allowlisted host over https`() {
        assertThat(policy.isAllowed("https://hermes.example.com")).isTrue()
    }

    @Test
    fun `allows allowlisted host over http`() {
        assertThat(policy.navigationDecision("http://hermes.example.com")).isEqualTo(NavigationDecision.ALLOW_IN_WEBVIEW)
    }

    @Test
    fun `opens non allowlisted https hosts externally`() {
        assertThat(policy.navigationDecision("https://example.org/docs")).isEqualTo(NavigationDecision.OPEN_IN_EXTERNAL_BROWSER)
    }

    @Test
    fun `opens non allowlisted http hosts externally`() {
        assertThat(policy.navigationDecision("http://example.org/docs")).isEqualTo(NavigationDecision.OPEN_IN_EXTERNAL_BROWSER)
    }

    @Test
    fun `allows allowlisted subdomains`() {
        assertThat(policy.navigationDecision("https://api.hermes.example.com")).isEqualTo(NavigationDecision.ALLOW_IN_WEBVIEW)
    }

    @Test
    fun `normalizes allowlisted host casing`() {
        val mixedCasePolicy = UrlPolicy(setOf("Hermes.Example.Com"))

        assertThat(mixedCasePolicy.isAllowed("https://HERMES.example.com")).isTrue()
    }

    @Test
    fun `rejects deceptive suffix hosts`() {
        assertThat(policy.isAllowed("https://fakehermes.example.com")).isFalse()
    }

    @Test
    fun `blocks non-web schemes`() {
        assertThat(policy.navigationDecision("ftp://hermes.example.com")).isEqualTo(NavigationDecision.BLOCK)
    }

    @Test
    fun `matches same origin with default https port`() {
        assertThat(
            UrlOrigins.hasSameOrigin(
                "https://hermes.example.com:443/session",
                "https://hermes.example.com"
            )
        ).isTrue()
    }

    @Test
    fun `does not match different origin port`() {
        assertThat(
            UrlOrigins.hasSameOrigin(
                "https://hermes.example.com:8443/session",
                "https://hermes.example.com"
            )
        ).isFalse()
    }

    @Test
    fun `compatible web origin allows an https upgrade`() {
        assertThat(
            UrlOrigins.hasSameOrUpgradedWebOrigin(
                "https://hermes.example.com/session",
                "http://hermes.example.com"
            )
        ).isTrue()
    }

    @Test
    fun `compatible web origin rejects an http downgrade`() {
        assertThat(
            UrlOrigins.hasSameOrUpgradedWebOrigin(
                "http://hermes.example.com/session",
                "https://hermes.example.com"
            )
        ).isFalse()
    }

    @Test
    fun `does not match invalid origin values`() {
        assertThat(UrlOrigins.hasSameOrigin("hermes.example.com", "other.example.com")).isFalse()
    }

    @Test
    fun `builds document start origin rule with explicit port`() {
        assertThat(UrlOrigins.documentStartOriginRule("https://hermes.example.com:8457/path"))
            .isEqualTo("https://hermes.example.com:8457")
    }

    @Test
    fun `builds document start origin rule for http`() {
        assertThat(UrlOrigins.documentStartOriginRule("http://hermes.example.com:8457/path"))
            .isEqualTo("http://hermes.example.com:8457")
    }

    @Test
    fun `normalizes origin url by stripping path query and fragment`() {
        assertThat(UrlOrigins.normalizeOriginUrl(" https://hermes.example.com:8455/dashboard?x=1#status "))
            .isEqualTo("https://hermes.example.com:8455")
    }

    @Test
    fun `page origin keeps a non-default port`() {
        assertThat(UrlOrigins.pageOrigin("https://hermes.example.com:8443/path"))
            .isEqualTo("https://hermes.example.com:8443")
        assertThat(UrlOrigins.pageOrigin("http://hermes.example.com:8787/path"))
            .isEqualTo("http://hermes.example.com:8787")
    }

    @Test
    fun `page origin drops an explicitly-specified default port`() {
        // A browser reports window.location.origin WITHOUT the default port, so the guard literal
        // must drop it too. documentStartOriginRule deliberately keeps it (it builds an allow-rule),
        // which is exactly why the guard uses pageOrigin instead.
        assertThat(UrlOrigins.pageOrigin("http://hermes.example.com:80/path"))
            .isEqualTo("http://hermes.example.com")
        assertThat(UrlOrigins.pageOrigin("https://hermes.example.com:443/path"))
            .isEqualTo("https://hermes.example.com")
        assertThat(UrlOrigins.documentStartOriginRule("http://hermes.example.com:80/path"))
            .isEqualTo("http://hermes.example.com:80")
    }

    @Test
    fun `page origin omits an absent port and lowercases the host`() {
        assertThat(UrlOrigins.pageOrigin("https://Hermes.Example.COM/path"))
            .isEqualTo("https://hermes.example.com")
    }

    @Test
    fun `page origin rejects non-web schemes and malformed urls`() {
        assertThat(UrlOrigins.pageOrigin("file:///etc/passwd")).isNull()
        assertThat(UrlOrigins.pageOrigin("javascript:alert(1)")).isNull()
        assertThat(UrlOrigins.pageOrigin("not a url")).isNull()
    }

    @Test
    fun `page origin brackets an ipv6 host`() {
        assertThat(UrlOrigins.pageOrigin("http://[::1]:8787/path"))
            .isEqualTo("http://[::1]:8787")
    }
}
