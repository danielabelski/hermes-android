package com.hermeswebui.android

import com.google.common.truth.Truth.assertThat
import com.hermeswebui.android.domain.ServerUrlValidator
import org.junit.Test

class ServerUrlValidatorTest {
    private val validator = ServerUrlValidator()

    @Test
    fun `accepts valid https host`() {
        assertThat(validator.isValid("https://hermes.example.com")).isTrue()
    }

    @Test
    fun `accepts valid http host`() {
        assertThat(validator.isValid("http://hermes.example.com")).isTrue()
    }

    @Test
    fun `rejects missing scheme`() {
        assertThat(validator.isValid("hermes.example.com")).isFalse()
    }

    @Test
    fun `rejects unsupported scheme`() {
        assertThat(validator.isValid("ftp://hermes.example.com")).isFalse()
    }

    @Test
    fun `accepts host with port and path`() {
        assertThat(validator.isValid("https://hermes.example.com:8443/dashboard?tab=chat")).isTrue()
        assertThat(validator.isValid("http://192.168.1.50:8080/")).isTrue()
    }

    @Test
    fun `rejects blank input`() {
        assertThat(validator.isValid("")).isFalse()
        assertThat(validator.isValid("   ")).isFalse()
    }

    @Test
    fun `rejects scheme without host`() {
        assertThat(validator.isValid("https://")).isFalse()
    }
}
