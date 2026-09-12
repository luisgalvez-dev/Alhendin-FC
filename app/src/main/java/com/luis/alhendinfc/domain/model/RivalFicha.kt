package com.luis.alhendinfc.domain.model

/**
 * Presentación de la ficha de rival. No persiste datos ni toca Room.
 */
object RivalFicha {

    const val TABLET_MIN_WIDTH_DP = 600

    fun shieldInitials(name: String, shortName: String = ""): String =
        CalendarDayVisual.initialsOf(shortName.ifBlank { name })

    fun stadiumLine(stadium: String): String? =
        stadium.trim().takeIf { it.isNotEmpty() }

    fun kitLine(kitColors: String): String? =
        kitColors.trim().takeIf { it.isNotEmpty() }

    fun playerCountLabel(count: Int): String =
        if (count == 1) "1 jugador" else "$count jugadores"

    fun emptyPlayers(queryBlank: Boolean): String =
        if (queryBlank) "No hay jugadores añadidos" else "Ningún jugador coincide con la búsqueda."

    fun emptyAnalysis(): String = "No hay análisis del rival"

    fun emptyFiles(): String = "No hay archivos"

    fun emptyReports(): String = "No hay informes de partido"

    fun emptyLinks(): String = "No hay enlaces configurados"

    fun linkTitle(link: RivalLink): String =
        link.label.trim().ifBlank { RivalLinkType.labelOf(link.type) }

    fun linkHost(url: String): String? {
        val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return null
        return uri.host?.removePrefix("www.")?.removePrefix("WWW.")?.takeIf { it.isNotBlank() }
    }

    fun linkSubtitle(link: RivalLink): String {
        val type = RivalLinkType.labelOf(link.type)
        val host = linkHost(link.url)
        return if (host.isNullOrBlank()) type else "$type · $host"
    }

    enum class LinkGlyph { PLAY, STAR, INFO }

    fun linkGlyph(type: String): LinkGlyph = when (type) {
        RivalLinkType.RFAF -> LinkGlyph.STAR
        RivalLinkType.RFAF_TV, RivalLinkType.YOUTUBE -> LinkGlyph.PLAY
        else -> LinkGlyph.INFO
    }

    fun analysisFields(analysis: RivalAnalysis?): List<Pair<String, String>> {
        val a = analysis ?: return emptyList()
        return listOf(
            "Sistema habitual" to a.usualSystem,
            "Variantes" to a.variants,
            "Salida de balón" to a.buildUp,
            "Progresión" to a.progression,
            "Último tercio" to a.finalThird,
            "Presión alta" to a.highPress,
            "Bloque medio" to a.midBlock,
            "Bloque bajo" to a.lowBlock,
            "Ataque → defensa" to a.transAttackToDefense,
            "Defensa → ataque" to a.transDefenseToAttack,
            "Córners ofensivos" to a.cornersOffensive,
            "Córners defensivos" to a.cornersDefensive,
            "Balón parado" to a.setPieces,
            "Fortalezas" to a.strengths,
            "Debilidades" to a.weaknesses,
            "Jugadores clave" to a.keyPlayers,
            "Notas generales" to a.generalNotes
        )
    }

    fun analysisHasContent(analysis: RivalAnalysis?): Boolean =
        analysisFields(analysis).any { it.second.isNotBlank() }

    fun analysisFilledCount(analysis: RivalAnalysis?): Int =
        analysisFields(analysis).count { it.second.isNotBlank() }

    fun analysisPreview(analysis: RivalAnalysis?, maxChars: Int = 140): String? {
        val text = analysisFields(analysis)
            .firstOrNull { it.second.isNotBlank() }
            ?.let { "${it.first}: ${it.second.trim()}" }
            ?: return null
        val compact = text.replace(Regex("\\s+"), " ")
        return if (compact.length <= maxChars) compact else compact.take(maxChars - 1).trimEnd() + "…"
    }

    fun formatDay(millis: Long): String? {
        if (millis <= 0L) return null
        val date = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return CalendarDate.format(date.toEpochDay())
    }

    fun analysisUpdatedLabel(updatedAt: Long): String? =
        formatDay(updatedAt)?.let { "Actualizado el $it" }

    fun fileDisplayName(attachment: Attachment): String =
        attachment.name.trim().ifBlank { attachment.typeLabel }

    fun fileDateLabel(attachment: Attachment): String? =
        formatDay(attachment.updatedAt.takeIf { it > 0L } ?: attachment.createdAt)

    fun sortAttachments(attachments: List<Attachment>): List<Attachment> =
        attachments.sortedByDescending { att ->
            att.updatedAt.takeIf { it > 0L } ?: att.createdAt
        }

    fun sortReports(reports: List<MatchReportRef>): List<MatchReportRef> =
        reports.sortedWith(
            compareByDescending<MatchReportRef> {
                CalendarDate.toEpochDay(it.match.date) ?: Long.MIN_VALUE
            }.thenByDescending { it.attachment.updatedAt }
        )

    data class KitSwatch(val argb: Long)

    fun kitSwatches(kitColors: String): List<KitSwatch> {
        val text = kitColors.lowercase()
        val hex = HEX_COLOR.findAll(kitColors).mapNotNull { match ->
            match.groupValues[1].toLongOrNull(16)?.let { KitSwatch(0xFF000000L or it) }
        }.toList()
        val named = NAMED_COLORS
            .filterKeys { key -> text.contains(key) }
            .values
            .map { KitSwatch(it) }
        return (hex + named).distinctBy { it.argb }.take(5)
    }

    private val HEX_COLOR = Regex("#([0-9a-fA-F]{6})")

    private val NAMED_COLORS = mapOf(
        "blanco" to 0xFFF5F5F5L,
        "white" to 0xFFF5F5F5L,
        "negro" to 0xFF212121L,
        "black" to 0xFF212121L,
        "rojo" to 0xFFC62828L,
        "red" to 0xFFC62828L,
        "azul" to 0xFF1565C0L,
        "blue" to 0xFF1565C0L,
        "verde" to 0xFF2E7D32L,
        "green" to 0xFF2E7D32L,
        "amarillo" to 0xFFF9A825L,
        "yellow" to 0xFFF9A825L,
        "naranja" to 0xFFEF6C00L,
        "orange" to 0xFFEF6C00L,
        "morado" to 0xFF6A1B9AL,
        "purple" to 0xFF6A1B9AL,
        "granate" to 0xFF7B1E3AL,
        "gris" to 0xFF757575L,
        "gray" to 0xFF757575L,
        "grey" to 0xFF757575L,
        "rosa" to 0xFFD81B60L,
        "pink" to 0xFFD81B60L
    )
}
