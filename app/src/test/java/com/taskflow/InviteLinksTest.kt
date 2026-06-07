package com.taskflow

import com.taskflow.core.sharing.InviteLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteLinksTest {
    @Test
    fun buildsHttpsInviteUrl() {
        assertEquals(
            "https://gen-lang-client-0780081219.web.app/invite/token-123",
            InviteLinks.urlForToken("token-123")
        )
    }

    @Test
    fun parsesHttpsAndLegacyTokens() {
        assertEquals("abc", InviteLinks.tokenFromUrl("https://gen-lang-client-0780081219.web.app/invite/abc"))
        assertEquals("abc", InviteLinks.tokenFromUrl("taskflow://invite/abc"))
    }

    @Test
    fun rejectsUnknownInviteUrl() {
        assertNull(InviteLinks.tokenFromUrl("https://example.com/invite/abc"))
    }
}
