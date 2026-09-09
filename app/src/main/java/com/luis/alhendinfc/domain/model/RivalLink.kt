package com.luis.alhendinfc.domain.model

data class RivalLink(
    val id: Int = 0,
    val syncId: String = "",
    val opponentClubId: Int,
    val type: String = RivalLinkType.CUSTOM,
    val label: String = "",
    val url: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

object RivalLinkType {
    const val RFAF = "RFAF"
    const val RFAF_TV = "RFAF_TV"
    const val YOUTUBE = "YOUTUBE"
    const val CUSTOM = "CUSTOM"

    val ALL = listOf(RFAF, RFAF_TV, YOUTUBE, CUSTOM)

    fun labelOf(type: String): String = when (type) {
        RFAF -> "RFAF"
        RFAF_TV -> "RFAF TV"
        YOUTUBE -> "YouTube"
        else -> "Personalizado"
    }
}

object RivalLinkRules {
    fun isOpenableUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        return !uri.host.isNullOrBlank()
    }

    fun isKnownType(type: String): Boolean = type in RivalLinkType.ALL
}
