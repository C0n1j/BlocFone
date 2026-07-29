package es.c0n1j.blocfone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactExceptionCodecTest {
    @Test
    fun `reference codec round trips url unsafe display names`() {
        val reference = ContactRef(
            lookupKey = "lookup|key",
            contactId = 42L,
            displayName = "Ana / Beto?",
        )

        assertEquals(reference, ContactExceptionCodec.decodeRef(ContactExceptionCodec.encodeRef(reference)))
    }

    @Test
    fun `reference codec ignores unknown versions and malformed records`() {
        assertNull(ContactExceptionCodec.decodeRef("v2|bG9va3Vw|NDI|QW5h"))
        assertNull(ContactExceptionCodec.decodeRef("v1|not-base64|NDI|QW5h"))
        assertNull(ContactExceptionCodec.decodeRef("v1|bG9va3Vw|LTE|QW5h"))
        assertNull(ContactExceptionCodec.decodeRef("v1|bG9va3Vw|NDI"))
    }

    @Test
    fun `contact exceptions retain selected references without contributions`() {
        val reference = ContactRef("lookup", 7L, "Sin teléfonos")
        val exceptions = ContactExceptions(contactRefs = setOf(reference))

        assertEquals(setOf(reference), exceptions.contacts)
    }

    @Test
    fun `contact exceptions deduplicate shared reference contributions`() {
        val reference = ContactRef("lookup", 7L, "Ana")
        val exceptions = ContactExceptions(
            contactRefs = setOf(reference),
            contactContributions = setOf(ContactContribution(reference, setOf("+541155551234"))),
        )

        assertEquals(setOf(reference), exceptions.contacts)
    }

    @Test
    fun `legacy national contribution decodes to Spanish E164`() {
        val reference = ContactRef("lookup", 7L, "Ana")
        val encoded = ContactExceptionCodec.encodeContribution(
            ContactContribution(reference, setOf("612 345 678")),
        ).single()

        assertEquals("lookup" to "+34612345678", ContactExceptionCodec.decodeContribution(encoded))
    }
}
