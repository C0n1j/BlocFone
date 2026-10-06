package es.c0n1j.blocfone.quicksettings

import es.c0n1j.blocfone.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TilePresentationTest {
    @Test
    fun `enabled blocking maps to active presentation`() {
        val presentation = tilePresentation(isBlockingEnabled = true, supportsSubtitle = true)

        assertEquals(TileVisualState.ACTIVE, presentation.state)
        assertEquals(R.string.app_name, presentation.labelRes)
        assertEquals(R.string.tile_blocking_active, presentation.subtitleRes)
    }

    @Test
    fun `disabled blocking maps to inactive presentation`() {
        val presentation = tilePresentation(isBlockingEnabled = false, supportsSubtitle = true)

        assertEquals(TileVisualState.INACTIVE, presentation.state)
        assertEquals(R.string.app_name, presentation.labelRes)
        assertEquals(R.string.tile_blocking_paused, presentation.subtitleRes)
    }

    @Test
    fun `subtitle is omitted when the platform does not support it`() {
        val presentation = tilePresentation(isBlockingEnabled = true, supportsSubtitle = false)

        assertNull(presentation.subtitleRes)
    }
}
