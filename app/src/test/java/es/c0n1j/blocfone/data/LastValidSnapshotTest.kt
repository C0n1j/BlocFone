package es.c0n1j.blocfone.data

import es.c0n1j.blocfone.domain.ScreeningRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastValidSnapshotTest {
    @Test
    fun `snapshot is absent until a valid value is stored`() {
        val snapshot = LastValidSnapshot<String>()

        assertNull(snapshot.current())
    }

    @Test
    fun `snapshot retains the most recent valid value`() {
        val snapshot = LastValidSnapshot<String>()
        snapshot.update("first")
        snapshot.update("latest")

        assertEquals("latest", snapshot.current())
    }

    @Test
    fun `unified settings snapshot is replaced without mixing revisions`() {
        val snapshot = LastValidSnapshot<SettingsSnapshot>()
        val first = SettingsSnapshot(
            rules = ScreeningRules(isBlockingEnabled = true),
            contactExceptions = ContactExceptions(manualNumbers = setOf("first")),
        )
        val latest = SettingsSnapshot(
            rules = ScreeningRules(isBlockingEnabled = false),
            contactExceptions = ContactExceptions(manualNumbers = setOf("latest")),
        )

        snapshot.update(first)
        snapshot.update(latest)

        assertEquals(latest, snapshot.current())
    }

    @Test
    fun `retry controller advances only on explicit retry`() {
        val controller = ReadRetryController()

        assertEquals(0L, controller.attempts.value)
        controller.retry()
        assertEquals(1L, controller.attempts.value)
    }
}
