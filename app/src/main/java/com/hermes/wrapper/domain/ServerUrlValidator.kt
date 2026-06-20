package com.hermes.wrapper.domain

import java.net.URI

class ServerUrlValidator {
    fun isValid(url: String): Boolean {
        val parsed = runCatching { URI(url) }.getOrNull() ?: return false
        val host = parsed.host ?: return false
        val isHttpsScheme = parsed.scheme.equals("https", ignoreCase = true)
        return isHttpsScheme && host.isNotBlank()
    }
}
