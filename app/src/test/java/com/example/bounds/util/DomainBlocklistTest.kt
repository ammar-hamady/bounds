package com.example.bounds.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBlocklistTest {

    @Test
    fun `normalizes schemes case trailing dot and wildcard prefix`() {
        assertEquals("example.com", DomainBlocklist.canonicalize(" HTTPS://Example.COM./path "))
        assertEquals("example.com", DomainBlocklist.canonicalize("*.example.com"))
    }

    @Test
    fun `deduplicates valid domains and drops invalid entries`() {
        assertEquals(
            listOf("example.com", "news.example"),
            DomainBlocklist.canonicalizeAll(
                listOf("example.com", "EXAMPLE.COM.", "10.0.0.1", "news.example")
            )
        )
    }

    @Test
    fun `rejects urls with ports paths and ip literals`() {
        assertNull(DomainBlocklist.canonicalize("example.com:443"))
        assertNull(DomainBlocklist.canonicalize("example.com/path"))
        assertNull(DomainBlocklist.canonicalize("192.168.1.1"))
    }

    @Test
    fun `matches exact domain and subdomains but not suffix lookalikes`() {
        assertTrue(DomainBlocklist.matchesDomain("example.com", "example.com"))
        assertTrue(DomainBlocklist.matchesDomain("cdn.Example.com.", "example.com"))
        assertFalse(DomainBlocklist.matchesDomain("notexample.com", "example.com"))
        assertFalse(DomainBlocklist.matchesDomain("example.co", "example.com"))
    }
}