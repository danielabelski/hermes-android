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

    @Test
    fun `page origin canonicalizes an expanded ipv6 literal the way a browser does`() {
        // A browser reports location.origin after WHATWG host parsing, which compresses IPv6.
        // Emitting the expanded spelling would make the guard literal never match, silently
        // suppressing every runtime script on such a configured server.
        assertThat(UrlOrigins.pageOrigin("http://[0:0:0:0:0:0:0:1]:80"))
            .isEqualTo("http://[::1]")
        assertThat(UrlOrigins.pageOrigin("http://[2001:0db8:0000:0000:0000:0000:1428:57ab]:9000"))
            .isEqualTo("http://[2001:db8::1428:57ab]:9000")
        assertThat(UrlOrigins.pageOrigin("http://[fe80:0:0:0:0:0:0:1]"))
            .isEqualTo("http://[fe80::1]")
        assertThat(UrlOrigins.pageOrigin("http://[0:0:0:0:0:0:0:0]"))
            .isEqualTo("http://[::]")
        // Longest zero-run wins; a shorter run stays expanded.
        assertThat(UrlOrigins.pageOrigin("http://[1:0:0:2:0:0:0:3]:8787"))
            .isEqualTo("http://[1:0:0:2::3]:8787")
        // An embedded dotted-quad is re-serialized as hextets.
        assertThat(UrlOrigins.pageOrigin("http://[::ffff:127.0.0.1]:8787"))
            .isEqualTo("http://[::ffff:7f00:1]:8787")
        // The embedded quad uses the SAME radix-aware part parsing as a bare IPv4 host, so
        // octal 010 == 8 (…:7f00:8), not decimal 10 (…:7f00:a).
        assertThat(UrlOrigins.pageOrigin("http://[::ffff:127.0.0.010]:18770"))
            .isEqualTo("http://[::ffff:7f00:8]:18770")
        assertThat(UrlOrigins.pageOrigin("http://[::ffff:1.2.3.04]:80"))
            .isEqualTo("http://[::ffff:102:304]")
    }

    @Test
    fun `page origin canonicalizes numeric ipv4 hosts the way a browser does`() {
        assertThat(UrlOrigins.pageOrigin("http://2130706433")).isEqualTo("http://127.0.0.1")
        assertThat(UrlOrigins.pageOrigin("http://0x7f000001")).isEqualTo("http://127.0.0.1")
        // A leading zero means octal: 010 == 8.
        assertThat(UrlOrigins.pageOrigin("http://010.0.0.1")).isEqualTo("http://8.0.0.1")
        // A bare `0x` is an empty hex payload, which is zero.
        assertThat(UrlOrigins.pageOrigin("http://0x")).isEqualTo("http://0.0.0.0")
        assertThat(UrlOrigins.pageOrigin("http://0x.0x.0x.0x")).isEqualTo("http://0.0.0.0")
        // A numeric host drops a single trailing dot.
        assertThat(UrlOrigins.pageOrigin("http://2130706433.")).isEqualTo("http://127.0.0.1")
        assertThat(UrlOrigins.pageOrigin("http://127.0.0.1.")).isEqualTo("http://127.0.0.1")
        // Already-canonical dotted-decimal is untouched.
        assertThat(UrlOrigins.pageOrigin("http://192.168.1.10:8787"))
            .isEqualTo("http://192.168.1.10:8787")
    }

    @Test
    fun `page origin keeps a trailing dot on a dns name`() {
        // A browser drops a trailing dot from a NUMERIC host but keeps it on a name.
        assertThat(UrlOrigins.pageOrigin("http://hermes.example.com."))
            .isEqualTo("http://hermes.example.com.")
    }

    @Test
    fun `page origin recovers a host java URI rejects for a trailing-dot numeric address`() {
        // java.net.URI returns a null host (and port -1) for these; the raw-authority fallback
        // recovers both, so the runtime shims are not silently disabled on such a server URL.
        assertThat(UrlOrigins.pageOrigin("http://127.0.0.1.:8787"))
            .isEqualTo("http://127.0.0.1:8787")
        assertThat(UrlOrigins.pageOrigin("http://2130706433.:8080"))
            .isEqualTo("http://127.0.0.1:8080")
        assertThat(UrlOrigins.pageOrigin("http://127.0.0.1.:80"))
            .isEqualTo("http://127.0.0.1")
    }

    @Test
    fun `page origin strips userinfo in the raw-authority fallback like a browser`() {
        // A browser drops credentials from location.origin. On the URI-rejected fallback path the
        // host is still recovered without the userinfo.
        assertThat(UrlOrigins.pageOrigin("http://user:pass@127.0.0.1.:8787"))
            .isEqualTo("http://127.0.0.1:8787")
    }

    @Test
    fun `page origin fails closed on an out-of-range numeric host`() {
        // These are numeric candidates the browser rejects outright. Returning the raw spelling
        // would emit a literal that can never match; null makes the caller skip injection.
        assertThat(UrlOrigins.pageOrigin("http://999.1.1.1")).isNull()
        assertThat(UrlOrigins.pageOrigin("http://256.1.1.1")).isNull()
        assertThat(UrlOrigins.pageOrigin("http://4294967296")).isNull()
        assertThat(UrlOrigins.pageOrigin("http://1.2.3.4.5")).isNull()
    }

    @Test
    fun `page origin returns null for a host it cannot canonicalize`() {
        // Better to skip injection than to emit a literal that can never match.
        assertThat(UrlOrigins.pageOrigin("http://[not-an-ip]:8787")).isNull()
        assertThat(UrlOrigins.pageOrigin("http://[::1::2]:8787")).isNull()
        // A URI-ADMITTED host that canonicalization must still reject. This one matters for
        // mutation coverage: java.net.URI accepts it, so it reaches canonicalBrowserHost and the
        // assertion fails if canonicalization is reduced to `return host`.
        assertThat(UrlOrigins.pageOrigin("http://[1:2:3:4:5:6:7:8:9]")).isNull()
    }
}
