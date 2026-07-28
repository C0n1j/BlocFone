package es.c0n1j.blocfone.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberInputValidatorTest {
    @Test
    fun `accepts digits and supported punctuation`() {
        assertTrue(NumberInputValidator.accepts("+54 (11) 5555-1234"))
    }

    @Test
    fun `rejects alphabetic input before normalization`() {
        assertFalse(NumberInputValidator.accepts("0800-FLORES"))
        assertFalse(NumberInputValidator.accepts("abc123"))
    }

    @Test
    fun `rejects unsupported punctuation and non-space whitespace`() {
        assertFalse(NumberInputValidator.accepts("11.5555.1234"))
        assertFalse(NumberInputValidator.accepts("11/5555/1234"))
        assertFalse(NumberInputValidator.accepts("11\t5555\n1234"))
        assertFalse(NumberInputValidator.accepts("\t1155551234\n"))
    }

    @Test
    fun `rejects blank input`() {
        assertFalse(NumberInputValidator.accepts("   "))
    }

    @Test
    fun `requires at least three digits after normalization`() {
        assertFalse(NumberInputValidator.hasMinimumDigits("+12"))
        assertTrue(NumberInputValidator.hasMinimumDigits("+123"))
    }
}
