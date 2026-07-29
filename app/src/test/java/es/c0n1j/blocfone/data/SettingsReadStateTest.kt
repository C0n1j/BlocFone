package es.c0n1j.blocfone.data

import es.c0n1j.blocfone.domain.ScreeningRules
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsReadStateTest {
    @Test
    fun `first read error has no misleading fallback`() {
        val state = SettingsReadState.Error(lastValid = null, cause = IOException("unavailable"))

        assertNull(state.lastValid)
        assertEquals("unavailable", state.cause.message)
    }

    @Test
    fun `read error retains the complete last valid settings view`() {
        val snapshot = SettingsSnapshot(
            rules = ScreeningRules(isBlockingEnabled = false),
            contactExceptions = ContactExceptions(manualNumbers = setOf("+541155551234")),
        )
        val state = SettingsReadState.Error(snapshot, IOException("temporary"))

        assertEquals(snapshot, state.lastValid)
        assertEquals(setOf("+541155551234"), state.lastValid?.contactExceptions?.manualNumbers)
    }
}
