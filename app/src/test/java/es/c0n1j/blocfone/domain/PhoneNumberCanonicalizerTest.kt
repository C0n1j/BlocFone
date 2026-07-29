package es.c0n1j.blocfone.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberCanonicalizerTest {
    @Test
    fun `spanish national mobile and international forms share E164 identity`() {
        assertEquals("+34612345678", PhoneNumberCanonicalizer.canonicalize("612 345 678"))
        assertEquals("+34612345678", PhoneNumberCanonicalizer.canonicalize("+34 612-345-678"))
        assertEquals("+34712345678", PhoneNumberCanonicalizer.canonicalize("712345678"))
    }

    @Test
    fun `spanish fixed number canonicalizes with default region`() {
        assertEquals("+34912345678", PhoneNumberCanonicalizer.canonicalize("912 345 678"))
    }

    @Test
    fun `already E164 input remains canonical`() {
        assertEquals("+34612345678", PhoneNumberCanonicalizer.canonicalize("+34612345678"))
    }

    @Test
    fun `new invalid input is rejected while stored fallback stays exact`() {
        assertNull(PhoneNumberCanonicalizer.canonicalize("12345"))
        assertEquals("12345", PhoneNumberCanonicalizer.identity(" 12345 "))
        assertEquals("abc123", PhoneNumberCanonicalizer.identity("abc123"))
    }

    @Test
    fun `legacy sets canonicalize and deduplicate without suffix matching`() {
        assertEquals(
            setOf("+34612345678", "12345", "abc123"),
            PhoneNumberCanonicalizer.canonicalizeStored(
                setOf("612 345 678", "+34612345678", "12345", "abc123"),
            ),
        )
        assertEquals(
            setOf("+34612345679", "12345"),
            PhoneNumberCanonicalizer.removeIdentity(
                setOf("612345678", "+34612345678", "+34612345679", "12345"),
                "+34 612 345 678",
            ),
        )
    }
}
