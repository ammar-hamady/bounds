package com.example.bounds.util

import java.net.IDN
import java.net.URI
import java.util.Locale

/**
 * Canonicalization and matching rules for the zone website blocklist.
 *
 * Policies are domain-only: URL paths, ports, IP literals, and wildcard
 * semantics are not stored. A blocked domain also matches every subdomain,
 * but never a lookalike suffix such as "notexample.com".
 */
object DomainBlocklist {

    data class Validation(val domain: String? = null, val error: String? = null) {
        val isValid: Boolean get() = domain != null
    }

    fun validate(raw: String): Validation {
        var value = raw.trim()
        if (value.isEmpty()) return Validation(error = "Enter a domain such as example.com.")

        if (value.startsWith("*.")) value = value.removePrefix("*.")

        if (SCHEME_REGEX.containsMatchIn(value)) {
            val uri = runCatching { URI(value) }.getOrNull()
            val host = uri?.host
            if (host.isNullOrBlank() || uri.port != -1 || uri.userInfo != null) {
                return Validation(error = "Use a domain name without a port or account name.")
            }
            value = host
        } else if (value.any { it == '/' || it == '?' || it == '#' }) {
            return Validation(error = "Use a domain only; URL paths are not supported.")
        }

        value = value.trim().trimEnd('.').lowercase(Locale.ROOT)
        if (value.isEmpty()) return Validation(error = "Enter a domain such as example.com.")
        if (value.contains(':') || IPV4_REGEX.matches(value)) {
            return Validation(error = "IP addresses are not supported; enter a domain name.")
        }

        val ascii = runCatching {
            IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        }.getOrElse {
            return Validation(error = "That domain contains an invalid label.")
        }
        val labels = ascii.split('.')
        if (labels.size < 2 || ascii.length > 253 || labels.all { it.all(Char::isDigit) }) {
            return Validation(error = "Enter a full domain such as example.com.")
        }
        if (labels.any { it.isEmpty() || it.length > 63 || !LABEL_REGEX.matches(it) }) {
            return Validation(error = "Domain labels may contain letters, numbers, and hyphens.")
        }
        if (labels.any { it.startsWith('-') || it.endsWith('-') }) {
            return Validation(error = "A domain label cannot start or end with a hyphen.")
        }
        return Validation(domain = ascii)
    }

    fun canonicalize(raw: String): String? = validate(raw).domain

    fun canonicalizeAll(domains: Iterable<String>): List<String> =
        domains.mapNotNull(::canonicalize).distinct()

    fun matchesDomain(queryName: String, blockedDomain: String): Boolean {
        val query = canonicalize(queryName) ?: return false
        val blocked = canonicalize(blockedDomain) ?: return false
        return query == blocked || query.endsWith(".$blocked")
    }

    fun matchesAny(queryName: String, blockedDomains: Iterable<String>): Boolean =
        blockedDomains.any { matchesDomain(queryName, it) }

    private val SCHEME_REGEX = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE)
    private val IPV4_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val LABEL_REGEX = Regex("""[a-z0-9-]+""")
}