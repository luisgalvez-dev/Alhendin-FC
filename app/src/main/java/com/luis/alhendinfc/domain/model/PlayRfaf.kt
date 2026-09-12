package com.luis.alhendinfc.domain.model

/**
 * Acceso web global a PlayRFAF / FootballClub.
 * No hay URL por rival: la búsqueda de partidos se hace dentro de esa pantalla.
 */
object PlayRfaf {
    const val URL = "https://www.footballclub.pro/main-fc"

    fun isOpenableUrl(url: String = URL): Boolean = RivalLinkRules.isOpenableUrl(url)

    fun searchHint(rivalName: String): String {
        val name = rivalName.trim().ifBlank { "este rival" }
        return "Buscar partidos grabados de $name"
    }
}
