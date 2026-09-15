package com.hermeswebui.android.core.security

import java.net.URI
import java.util.Locale

enum class NavigationDecision {
    ALLOW_IN_WEBVIEW,
    OPEN_IN_EXTERNAL_BROWSER,
    BLOCK
}

class UrlPolicy(private val allowedHosts: Set<String>) {
    private val normalizedAllowedHosts = allowedHosts
        .asSequence()
        .map { it.trim().lowercase(Locale.US) }
        .filter { it.isNotEmpty() }
        .toSet()

    fun isAllowed(url: String): Boolean {
        val uri = url.toUriOrNull() ?: return false
        if (!uri.isHttpOrHttpsScheme()) return false
        return uri.hasAllowedHost()
    }

    fun navigationDecision(url: String): NavigationDecision {
        val uri = url.toUriOrNull() ?: return NavigationDecision.BLOCK
        if (!uri.isHttpOrHttpsScheme()) return NavigationDecision.BLOCK
        return if (uri.hasAllowedHost()) {
            NavigationDecision.ALLOW_IN_WEBVIEW
        } else {
            NavigationDecision.OPEN_IN_EXTERNAL_BROWSER
        }
    }

    private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()

    private fun URI.hasAllowedHost(): Boolean {
        val host = normalizedHost() ?: return false
        return host in normalizedAllowedHosts || normalizedAllowedHosts.any { host.endsWith(".$it") }
    }

    private fun URI.isHttpOrHttpsScheme(): Boolean {
        return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
    }

    private fun URI.normalizedHost(): String? = host?.lowercase(Locale.US)
}

object UrlOrigins {
    /**
     * Sentinel returned by [ipv4FromNumericHost] when a host IS a numeric-IPv4 candidate but is
     * out of range (e.g. `http://999.1.1.1`). A browser rejects such a host outright, so the
     * caller must fail closed rather than fall through and emit the raw spelling. Compared by
     * identity (`===`), never by value.
     */
    private val INVALID_NUMERIC_HOST = String()

    fun hostFrom(url: String): String? {
        return url.toUriOrNull()?.normalizedHost()?.takeIf { it.isNotBlank() }
    }

    fun hasSameOrigin(url: String?, baseUrl: String, ignoreScheme: Boolean = false): Boolean {
        if (url.isNullOrBlank() || baseUrl.isBlank()) return false
        val target = url.toUriOrNull() ?: return false
        val base = baseUrl.toUriOrNull() ?: return false
        val targetScheme = target.scheme?.lowercase(Locale.US) ?: return false
        val baseScheme = base.scheme?.lowercase(Locale.US) ?: return false
        val targetHost = target.normalizedHost() ?: return false
        val baseHost = base.normalizedHost() ?: return false

        val schemesMatch = ignoreScheme || targetScheme == baseScheme
        val targetPort = target.effectivePort()
        val basePort = base.effectivePort()

        // When ignoring scheme, we also allow the standard web ports (80/443) to be equivalent.
        val portsMatch = targetPort == basePort || (ignoreScheme && isStandardWebPort(targetPort) && isStandardWebPort(basePort))

        return schemesMatch &&
            targetHost == baseHost &&
            portsMatch
    }

    fun hasSameOrUpgradedWebOrigin(url: String?, baseUrl: String): Boolean {
        if (url.isNullOrBlank() || baseUrl.isBlank()) return false
        val target = url.toUriOrNull() ?: return false
        val base = baseUrl.toUriOrNull() ?: return false
        val targetScheme = target.scheme?.lowercase(Locale.US) ?: return false
        val baseScheme = base.scheme?.lowercase(Locale.US) ?: return false
        if (targetScheme !in setOf("http", "https") || baseScheme !in setOf("http", "https")) {
            return false
        }

        val schemesMatchOrUpgrade = targetScheme == baseScheme ||
            (baseScheme == "http" && targetScheme == "https")
        if (!schemesMatchOrUpgrade) return false

        val targetHost = target.normalizedHost() ?: return false
        val baseHost = base.normalizedHost() ?: return false
        val targetPort = target.effectivePort()
        val basePort = base.effectivePort()
        val portsMatch = targetPort == basePort ||
            (targetScheme != baseScheme && isStandardWebPort(targetPort) && isStandardWebPort(basePort))

        return targetHost == baseHost && portsMatch
    }

    private fun isStandardWebPort(port: Int): Boolean = port == 80 || port == 443

    fun documentStartOriginRule(url: String): String? {
        val uri = url.toUriOrNull() ?: return null
        val scheme = uri.scheme
            ?.lowercase(Locale.US)
            ?.takeIf { it == "http" || it == "https" }
            ?: return null
        val host = uri.normalizedHost()?.takeIf { it.isNotBlank() } ?: return null
        val hostRule = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        val portRule = if (uri.port != -1) ":${uri.port}" else ""
        return "$scheme://$hostRule$portRule"
    }

    /**
     * The origin exactly as a page reports it in `window.location.origin`: scheme + host, with
     * the port omitted when it is the scheme's default (80/http, 443/https).
     *
     * This is deliberately NOT [documentStartOriginRule] — that builds a WebViewCompat allow-rule,
     * which keeps an explicitly-specified default port (`http://host:80`) that the browser drops.
     * Comparing against the rule would silently fail the guard for such a server URL. Canonicalizing
     * here, natively, is what lets the injected guard compare a literal string literal instead of
     * calling the page-controlled `URL` constructor.
     *
     * Returns null for any host we cannot serialize the way a browser would. That is deliberate:
     * the caller skips script injection entirely rather than emitting a literal that can never
     * match, which would silently disable every runtime shim.
     */
    fun pageOrigin(url: String): String? {
        val uri = url.toUriOrNull() ?: return null
        val scheme = uri.scheme
            ?.lowercase(Locale.US)
            ?.takeIf { it == "http" || it == "https" }
            ?: return null
        // java.net.URI is RFC 2396-strict and returns a null host for spellings a browser accepts
        // (a trailing-dot IPv4 like `127.0.0.1.`, or `0x.0x.0x.0x`). Fall back to reading the raw
        // authority so those still canonicalize instead of silently disabling every runtime shim.
        // When URI rejects the host it also reports port -1, so the fallback recovers both.
        val host: String
        var port = uri.port
        val normalized = uri.normalizedHost()
        if (normalized != null && normalized.isNotBlank()) {
            host = normalized
        } else {
            val raw = rawAuthorityHostPort(url) ?: return null
            host = raw.first
            raw.second?.let { port = parsePort(it) ?: return null }
        }
        // A browser rejects an out-of-range port; java.net.URI does NOT, so validate here too.
        // Fail closed rather than synthesize a valid literal from an invalid URL.
        if (port != -1 && port !in 0..65535) return null
        val canonicalHost = canonicalBrowserHost(host) ?: return null
        val defaultPort = if (scheme == "https") 443 else 80
        val portPart = if (port != -1 && port != defaultPort) ":$port" else ""
        return "$scheme://$canonicalHost$portPart"
    }

    /** Parse a port the way a browser does: ASCII digits only, in 0..65535. Anything else is null. */
    private fun parsePort(text: String): Int? {
        if (text.isEmpty() || text.any { it !in '0'..'9' }) return null
        return text.toIntOrNull()?.takeIf { it in 0..65535 }
    }

    /**
     * Read (host, port?) from the raw authority, lowercased, for URLs `java.net.URI` parses but
     * whose host it rejects. This fallback exists ONLY to recover the numeric/ASCII hosts a
     * browser accepts but java.net.URI does not (trailing-dot IPv4, `0x.0x.0x.0x`); it does NOT
     * implement WHATWG percent-decoding or IDNA. So it fails closed (returns null) on any host
     * carrying a `%` escape or a non-ASCII character, rather than emit a literal that would never
     * match the browser's decoded/punycode origin. Userinfo (credentials) is stripped to match a
     * browser's `location.origin`.
     */
    private fun rawAuthorityHostPort(url: String): Pair<String, String?>? {
        val afterScheme = url.substringAfter("://", "").ifEmpty { return null }
        var authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isEmpty()) return null
        // A browser drops userinfo from the origin (`user:pass@host` → `host`).
        if (authority.contains('@')) authority = authority.substringAfterLast('@')
        if (authority.isEmpty()) return null
        val host: String
        var port: String? = null
        if (authority.startsWith("[")) {
            val end = authority.indexOf(']')
            if (end < 0) return null
            host = authority.substring(0, end + 1)
            val rest = authority.substring(end + 1)
            if (rest.startsWith(":")) port = rest.substring(1).ifEmpty { null }
        } else {
            val colon = authority.indexOf(':')
            if (colon >= 0) {
                host = authority.substring(0, colon)
                port = authority.substring(colon + 1).ifEmpty { null }
            } else {
                host = authority
            }
        }
        val lowered = host.lowercase(Locale.US)
        if (lowered.isEmpty()) return null
        // We do not decode/IDNA in this fallback, so refuse anything that would need it.
        if (lowered.any { it.code > 0x7F } || lowered.contains('%')) return null
        return lowered to port
    }

    /**
     * Serialize a host the way a browser does when it builds `location.origin`.
     *
     * Browsers apply WHATWG host parsing, which `java.net.URI` does not:
     *  - a bare number or hex literal is an IPv4 address (`2130706433` → `127.0.0.1`,
     *    `0x7f000001` → `127.0.0.1`);
     *  - IPv6 literals are compressed to their shortest form (`[0:0:0:0:0:0:0:1]` → `[::1]`).
     *
     * Emitting the un-canonicalized spelling would make the guard's literal comparison fail
     * forever on such a configured server, silently suppressing every runtime script. Anything we
     * cannot canonicalize confidently returns null so the caller can skip injection instead.
     */
    private fun canonicalBrowserHost(host: String): String? {
        if (host.startsWith("[") && host.endsWith("]")) {
            val compressed = compressIpv6(host.substring(1, host.length - 1)) ?: return null
            return "[$compressed]"
        }
        if (host.contains(":")) {
            val compressed = compressIpv6(host) ?: return null
            return "[$compressed]"
        }
        ipv4FromNumericHost(host)?.let { return if (it === INVALID_NUMERIC_HOST) null else it }
        // Ordinary DNS names pass through verbatim — including a trailing dot, which a browser
        // KEEPS in location.origin for a name (`http://example.com.`) even though it drops one
        // from a numeric address (`http://2130706433.` → `http://127.0.0.1`).
        return host
    }

    /**
     * WHATWG IPv4 parsing: the host is numeric when every dot-separated part parses as a number
     * (decimal, `0`-prefixed octal, or `0x`-prefixed hex). Fewer than four parts means the last
     * part supplies the remaining bytes, so `2130706433` → `127.0.0.1`.
     *
     * Returns null when the host is not a numeric candidate at all (an ordinary DNS name, which
     * the caller passes through unchanged). Returns [INVALID_NUMERIC_HOST] when it IS numeric but
     * out of range — the browser rejects those, so the caller must fail closed rather than emit
     * the raw spelling.
     */
    private fun ipv4FromNumericHost(host: String): String? {
        // A single trailing dot is dropped before parsing: `2130706433.` and `127.0.0.1.` are the
        // same hosts as their undotted forms (verified against Chromium).
        val trimmed = if (host.endsWith(".")) host.dropLast(1) else host
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(".")
        if (parts.size > 4) {
            // An all-numeric host with more than four parts is an INVALID address, not a DNS
            // name — the browser rejects `http://1.2.3.4.5` outright, so fail closed.
            return if (parts.all { it.isNotEmpty() && parseIpv4Part(it) != null }) {
                INVALID_NUMERIC_HOST
            } else {
                null
            }
        }
        if (parts.any { it.isEmpty() }) return null
        // Numeric-candidate test first: if ANY part fails to parse as a number this is a DNS name,
        // not a malformed address, so the caller should pass it through untouched.
        val numbers = parts.map { parseIpv4Part(it) ?: return null }
        // From here the host IS numeric, so any range failure is a browser-rejected host.
        val lastMax = 1L shl (8 * (4 - numbers.size + 1))
        if (numbers.last() >= lastMax) return INVALID_NUMERIC_HOST
        if (numbers.dropLast(1).any { it > 255 }) return INVALID_NUMERIC_HOST
        var value = numbers.last()
        numbers.dropLast(1).forEachIndexed { index, part ->
            value += part shl (8 * (3 - index))
        }
        return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
    }

    /**
     * Parse one IPv4 part. `0x`/`0X` prefix is hex, a leading `0` is octal, otherwise decimal.
     * A bare `0x` (empty hex payload) is zero, matching Chromium: `http://0x` → `http://0.0.0.0`.
     */
    private fun parseIpv4Part(part: String): Long? {
        val (radix, digits) = when {
            part.length >= 2 && (part.startsWith("0x") || part.startsWith("0X")) -> 16 to part.substring(2)
            part.startsWith("0") -> 8 to part.substring(1)
            else -> 10 to part
        }
        // An empty payload means the part was exactly "0", "0x" or "0X" — all of which are zero.
        if (digits.isEmpty()) return 0L
        if (digits.any { Character.digit(it, radix) < 0 }) return null
        return digits.toLongOrNull(radix)?.takeIf { it >= 0 }
    }

    /**
     * Parse and re-serialize an IPv6 literal to its shortest browser form (RFC 5952), or null if
     * it is not a valid IPv6 literal.
     *
     * Implemented with pure string handling rather than [InetAddress] on purpose: this runs on the
     * main thread during script injection, and we must never risk a name-resolution call here.
     */
    private fun compressIpv6(literal: String): String? {
        val groups = parseIpv6Groups(literal) ?: return null

        // RFC 5952: compress the LONGEST run of two-or-more zero groups; leftmost run wins a tie.
        var bestStart = -1
        var bestLen = 0
        var runStart = -1
        var runLen = 0
        for (i in 0..8) {
            val isZero = i < 8 && groups[i] == 0
            if (isZero) {
                if (runStart < 0) runStart = i
                runLen++
            } else {
                if (runLen > bestLen && runLen >= 2) {
                    bestStart = runStart
                    bestLen = runLen
                }
                runStart = -1
                runLen = 0
            }
        }

        val out = StringBuilder()
        var i = 0
        while (i < 8) {
            if (i == bestStart) {
                out.append("::")
                i += bestLen
                continue
            }
            if (out.isNotEmpty() && !out.endsWith(":")) out.append(':')
            out.append(Integer.toHexString(groups[i]))
            i++
        }
        return out.toString().ifEmpty { "::" }
    }

    /**
     * Parse an IPv6 literal into its 8 16-bit groups, honoring `::` compression and an optional
     * trailing dotted-quad (`::ffff:127.0.0.1`).
     */
    private fun parseIpv6Groups(literal: String): IntArray? {
        if (literal.isEmpty() || literal.contains('%')) return null
        // At most one `::`, and a lone `:` may not dangle at either end.
        if (literal.indexOf("::") != literal.lastIndexOf("::")) return null
        if (literal.startsWith(":") && !literal.startsWith("::")) return null
        if (literal.endsWith(":") && !literal.endsWith("::")) return null

        val doubleColon = literal.indexOf("::")
        val leftText = if (doubleColon >= 0) literal.substring(0, doubleColon) else literal
        val rightText = if (doubleColon >= 0) literal.substring(doubleColon + 2) else ""

        val left = if (leftText.isEmpty()) mutableListOf() else leftText.split(":").toMutableList()
        val right = if (rightText.isEmpty()) mutableListOf() else rightText.split(":").toMutableList()

        // A dotted-quad may only appear as the very last token, and expands to two groups.
        val tailSide = if (right.isNotEmpty()) right else left
        var ipv4: IntArray? = null
        if (tailSide.isNotEmpty() && tailSide.last().contains('.')) {
            ipv4 = ipv4ToGroups(tailSide.removeAt(tailSide.size - 1)) ?: return null
        }
        // A '.' anywhere else is invalid.
        if (left.any { it.contains('.') } || right.any { it.contains('.') }) return null

        val leftGroups = left.map { parseIpv6Group(it) ?: return null }
        val rightGroups = right.map { parseIpv6Group(it) ?: return null }
        val extra = ipv4?.size ?: 0
        val total = leftGroups.size + rightGroups.size + extra

        val result = IntArray(8)
        if (doubleColon < 0) {
            if (total != 8) return null
            leftGroups.forEachIndexed { i, v -> result[i] = v }
            ipv4?.forEachIndexed { i, v -> result[leftGroups.size + i] = v }
            return result
        }
        // `::` must stand for at least one elided zero group.
        if (total > 7) return null
        leftGroups.forEachIndexed { i, v -> result[i] = v }
        val tailStart = 8 - rightGroups.size - extra
        rightGroups.forEachIndexed { i, v -> result[tailStart + i] = v }
        ipv4?.forEachIndexed { i, v -> result[8 - extra + i] = v }
        return result
    }

    /** Convert a dotted-quad into the two 16-bit groups it occupies inside an IPv6 literal. */
    private fun ipv4ToGroups(text: String): IntArray? {
        val quad = text.split(".")
        if (quad.size != 4) return null
        // Chromium applies the same radix-aware part parsing here as for a bare IPv4 host, so
        // `[::ffff:127.0.0.010]` is `…:7f00:8` (octal 010 == 8), not `…:7f00:a`. Each of the four
        // components must still fit in one byte.
        val bytes = quad.map { part ->
            val value = parseIpv4Part(part) ?: return null
            if (value > 255) return null
            value.toInt()
        }
        return intArrayOf((bytes[0] shl 8) or bytes[1], (bytes[2] shl 8) or bytes[3])
    }

    private fun parseIpv6Group(text: String): Int? {
        if (text.isEmpty() || text.length > 4) return null
        if (text.any { Character.digit(it, 16) < 0 }) return null
        return text.toIntOrNull(16)
    }

    fun normalizeOriginUrl(url: String): String {
        val trimmed = url.trim()
        val parsed = trimmed.toUriOrNull() ?: return trimmed
        val scheme = parsed.scheme ?: return trimmed
        val host = parsed.host ?: return trimmed
        return runCatching {
            URI(scheme, null, host, parsed.port, null, null, null)
                .toString()
                .trimEnd('/')
        }.getOrDefault(trimmed)
    }

    fun normalizedPath(url: String): String {
        return url.toUriOrNull()?.path.orEmpty().trimEnd('/')
    }

    private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()

    private fun URI.effectivePort(): Int {
        if (port != -1) return port
        return when (scheme?.lowercase(Locale.US)) {
            "https" -> 443
            "http" -> 80
            else -> -1
        }
    }

    private fun URI.normalizedHost(): String? = host?.lowercase(Locale.US)
}
