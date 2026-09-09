package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RivalLinkRulesTest {

    @Test
    fun openableUrl_requiresHttpOrHttpsAndHost() {
        assertTrue(RivalLinkRules.isOpenableUrl("https://example.com/dev/rfaf"))
        assertTrue(RivalLinkRules.isOpenableUrl("http://example.com"))
        assertFalse(RivalLinkRules.isOpenableUrl("ftp://example.com"))
        assertFalse(RivalLinkRules.isOpenableUrl("javascript:alert(1)"))
        assertFalse(RivalLinkRules.isOpenableUrl("example.com"))
        assertFalse(RivalLinkRules.isOpenableUrl("https://"))
        assertFalse(RivalLinkRules.isOpenableUrl("   "))
        assertTrue(RivalLinkRules.isKnownType(RivalLinkType.RFAF_TV))
        assertFalse(RivalLinkRules.isKnownType("SCRAPE"))
    }
}
