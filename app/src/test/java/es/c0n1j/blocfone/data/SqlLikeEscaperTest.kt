package es.c0n1j.blocfone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlLikeEscaperTest {
    @Test
    fun `escapes literal SQL LIKE metacharacters in search queries`() {
        assertEquals("Ana\\%\\_\\\\", escapeLikeLiteral("Ana%_\\"))
    }
}
