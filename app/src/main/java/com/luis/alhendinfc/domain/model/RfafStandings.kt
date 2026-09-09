package com.luis.alhendinfc.domain.model

/**
 * URL de la clasificación RFAF de la temporada actual.
 * Cambiar [URL] aquí cuando cambie la competición o el grupo.
 */
object RfafStandings {
    const val URL =
        "https://www.rfaf.es/pnfg/NPcd/NFG_VisClasificacion?cod_primaria=1000120&codjornada=1&codcompeticion=48465911&codgrupo=48908427&codjornada=1"

    fun isOpenableUrl(url: String = URL): Boolean = RivalLinkRules.isOpenableUrl(url)
}
