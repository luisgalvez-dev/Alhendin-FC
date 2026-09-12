package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RivalLinkRulesTest {

    @Test
    fun openableUrl_requiresHttpOrHttpsAndHost() {
        assertTrue(RivalLinkRules.isOpenableUrl("https://example.com/dev/rfaf"))
        assertTrue(RivalLinkRules.isOpenableUrl("http://example.com"))
        assertTrue(RivalLinkRules.isOpenableUrl("  https://play.rfaf.es/team/1  "))
        assertFalse(RivalLinkRules.isOpenableUrl("ftp://example.com"))
        assertFalse(RivalLinkRules.isOpenableUrl("javascript:alert(1)"))
        assertFalse(RivalLinkRules.isOpenableUrl("file:///tmp/x"))
        assertFalse(RivalLinkRules.isOpenableUrl("intent://scan"))
        assertFalse(RivalLinkRules.isOpenableUrl("example.com"))
        assertFalse(RivalLinkRules.isOpenableUrl("https://"))
        assertFalse(RivalLinkRules.isOpenableUrl("   "))
        assertTrue(RivalLinkRules.isKnownType(RivalLinkType.RFAF_TV))
        assertTrue(RivalLinkRules.isKnownType(RivalLinkType.YOUTUBE))
        assertFalse(RivalLinkRules.isKnownType("PLAY_RFAF"))
        assertFalse(RivalLinkRules.isKnownType("SCRAPE"))
        assertEquals("https://example.com/a", RivalLinkRules.normalizeUrl("  https://example.com/a  "))
        assertEquals("https://example.com/a", RivalLinkRules.requireNormalizedUrl(" https://example.com/a "))
    }
}
