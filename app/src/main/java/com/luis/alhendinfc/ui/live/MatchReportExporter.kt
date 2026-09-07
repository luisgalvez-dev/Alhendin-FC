package com.luis.alhendinfc.ui.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.luis.alhendinfc.domain.model.EventLabels
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.model.Team
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta el acta del partido en PDF, pensado para compartir o analizar con IA.
 */
class MatchReportExporter(private val context: Context) {

    data class Result(val pdfUri: Uri?)

    fun exportAll(
        match: Match,
        team: Team?,
        events: List<MatchEvent>,
        players: List<Player>,
        teamGoals: Int,
        rivalGoals: Int,
        period: Int,
        elapsedSeconds: Int,
        secondsOnField: Map<Int, Int>,
        eventLabel: (MatchEvent) -> String
    ): Result {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val safeRival = match.rival.ifBlank { "rival" }
            .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            .take(24)
        val base = "partido_${safeRival}_$stamp"

        val pdf = exportPdf(
            fileName = "$base.pdf",
            match = match,
            team = team,
            events = events,
            players = players,
            teamGoals = teamGoals,
            rivalGoals = rivalGoals,
            period = period,
            elapsedSeconds = elapsedSeconds,
            secondsOnField = secondsOnField,
            eventLabel = eventLabel
        )
        return Result(pdfUri = pdf)
    }

    fun exportPdf(
        fileName: String,
        match: Match,
        team: Team?,
        events: List<MatchEvent>,
        players: List<Player>,
        teamGoals: Int,
        rivalGoals: Int,
        @Suppress("UNUSED_PARAMETER") period: Int,
        @Suppress("UNUSED_PARAMETER") elapsedSeconds: Int,
        secondsOnField: Map<Int, Int>,
        eventLabel: (MatchEvent) -> String
    ): Uri? {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            drawFooter(canvas, pageNumber)
            document.finishPage(page)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - 40f) newPage()
        }

        val titlePaint = Paint().apply {
            color = DARK; textSize = 20f; isFakeBoldText = true; isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = GREEN; textSize = 13f; isFakeBoldText = true; isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = DARK; textSize = 11f; isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = GREY; textSize = 10f; isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = GREEN; strokeWidth = 1.5f; isAntiAlias = true
        }

        val teamName = team?.name ?: "Equipo"
        val rival = match.rival.ifBlank { "Rival" }
        canvas.drawText("ACTA DEL PARTIDO", MARGIN, y + 18f, titlePaint)
        y += 28f
        canvas.drawText(
            "$teamName  $teamGoals - $rivalGoals  $rival",
            MARGIN, y + 14f, bodyPaint
        )
        y += 22f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 14f

        val meta = listOf(
            "Fecha: ${match.date.ifBlank { "—" }}  Hora: ${match.time.ifBlank { "—" }}",
            "Jornada: ${if (match.matchday > 0) match.matchday else "—"}  " +
                "Estadio: ${match.stadium.ifBlank { "—" }}",
            "Formación: ${match.formation.ifBlank { "—" }}  " +
                "Condición: ${if (match.isHome) "Local" else "Visitante"}"
        )
        meta.forEach {
            canvas.drawText(it, MARGIN, y + 12f, mutedPaint)
            y += 16f
        }
        y += 8f

        val labelOf: (MatchEvent) -> String = { e ->
            val raw = eventLabel(e)
            if (raw.startsWith("SAMPLE_") || raw == e.typeCode) EventLabels.resolve(e) else raw
        }

        // Resumen por tipo
        ensureSpace(80f)
        canvas.drawText("RESUMEN DE EVENTOS", MARGIN, y + 12f, sectionPaint)
        y += 20f
        val counts = events.groupingBy { labelOf(it) }.eachCount().toList()
            .sortedByDescending { it.second }
        if (counts.isEmpty()) {
            canvas.drawText("Sin eventos registrados.", MARGIN, y + 12f, bodyPaint)
            y += 20f
        } else {
            counts.forEach { (label, count) ->
                ensureSpace(16f)
                canvas.drawText("• $label: $count", MARGIN, y + 12f, bodyPaint)
                y += 16f
            }
        }
        y += 10f

        // Timeline agrupada por parte
        ensureSpace(40f)
        canvas.drawText("CRONOLOGÍA", MARGIN, y + 12f, sectionPaint)
        y += 18f

        fun playerName(id: Int?): String {
            if (id == null) return rival
            val p = players.firstOrNull { it.id == id } ?: return "?"
            return p.alias.ifBlank { p.name }
        }

        val headerBg = Paint().apply { color = GREEN; style = Paint.Style.FILL }
        val headerText = Paint().apply {
            color = WHITE; textSize = 9f; isFakeBoldText = true; isAntiAlias = true
        }
        val periodHeaderPaint = Paint().apply {
            color = DARK; textSize = 11f; isFakeBoldText = true; isAntiAlias = true
        }
        val partDuration = match.durationPerPart.coerceAtLeast(1)

        if (events.isEmpty()) {
            canvas.drawText("Sin eventos registrados.", MARGIN, y + 12f, bodyPaint)
            y += 20f
        } else {
            events.groupBy { it.period }.toSortedMap().forEach { (part, partEvents) ->
                ensureSpace(44f)
                canvas.drawText("${part}ª PARTE", MARGIN, y + 12f, periodHeaderPaint)
                y += 16f
                canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f), headerBg)
                canvas.drawText("MIN", MARGIN + 4f, y + 13f, headerText)
                canvas.drawText("EVENTO", MARGIN + 40f, y + 13f, headerText)
                canvas.drawText("DETALLE", MARGIN + 200f, y + 13f, headerText)
                y += 18f

                partEvents.forEachIndexed { idx, e ->
                    ensureSpace(20f)
                    val rowBg = if (idx % 2 == 0) WHITE else 0xFFFAFAFA.toInt()
                    canvas.drawRect(
                        RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f),
                        Paint().apply { color = rowBg; style = Paint.Style.FILL }
                    )
                    val detail = when (e.type) {
                        StatisticType.SUBSTITUTION ->
                            "${playerName(e.playerId)} → ${playerName(e.relatedPlayerId)}"
                        StatisticType.RIVAL_GOAL -> rival
                        else -> if (e.playerId == null) rival else playerName(e.playerId)
                    }
                    val minute = EventLabels.displayMinute(e, partDuration)
                    canvas.drawText("$minute'", MARGIN + 4f, y + 13f, bodyPaint)
                    canvas.drawText(labelOf(e).take(28), MARGIN + 40f, y + 13f, bodyPaint)
                    canvas.drawText(detail.take(36), MARGIN + 200f, y + 13f, mutedPaint)
                    y += 18f
                }
                y += 8f
            }
        }
        y += 4f

        // Minutos en campo
        ensureSpace(40f)
        canvas.drawText("MINUTOS EN CAMPO", MARGIN, y + 12f, sectionPaint)
        y += 20f
        val withTime = secondsOnField.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, secs) ->
                val p = players.firstOrNull { it.id == id } ?: return@mapNotNull null
                p to secs
            }
        if (withTime.isEmpty()) {
            canvas.drawText("Sin tiempos registrados.", MARGIN, y + 12f, bodyPaint)
            y += 20f
        } else {
            withTime.forEach { (p, secs) ->
                ensureSpace(16f)
                val name = p.alias.ifBlank { p.name }
                val time = "%d'%02d".format(secs / 60, secs % 60)
                canvas.drawText(
                    "#${p.jerseyNumber} $name — $time",
                    MARGIN, y + 12f, bodyPaint
                )
                y += 16f
            }
        }

        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        val file = File(context.cacheDir, fileName)
        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            document.close()
            null
        }
    }

    fun exportCsv(
        fileName: String,
        match: Match,
        team: Team?,
        events: List<MatchEvent>,
        players: List<Player>,
        teamGoals: Int,
        rivalGoals: Int,
        period: Int,
        elapsedSeconds: Int,
        secondsOnField: Map<Int, Int>,
        eventLabel: (MatchEvent) -> String
    ): Uri? {
        val teamName = team?.name ?: "Equipo"
        val rival = match.rival.ifBlank { "Rival" }
        fun playerName(id: Int?): String {
            if (id == null) return rival
            val p = players.firstOrNull { it.id == id } ?: return "?"
            return p.alias.ifBlank { p.name }
        }
        fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""

        val sb = StringBuilder()
        sb.appendLine("# ACTA PARTIDO — Alhendin App")
        sb.appendLine("equipo,${esc(teamName)}")
        sb.appendLine("rival,${esc(rival)}")
        sb.appendLine("resultado,$teamGoals-$rivalGoals")
        sb.appendLine("fecha,${esc(match.date)}")
        sb.appendLine("hora,${esc(match.time)}")
        sb.appendLine("jornada,${match.matchday}")
        sb.appendLine("estadio,${esc(match.stadium)}")
        sb.appendLine("formacion,${esc(match.formation)}")
        sb.appendLine("local,${match.isHome}")
        sb.appendLine("parte,$period/${match.numParts}")
        sb.appendLine("cronometro_segundos,$elapsedSeconds")
        sb.appendLine("estado,${match.status.name}")
        sb.appendLine()
        sb.appendLine("minuto,periodo,codigo,evento,jugador,jugador_relacionado,detalle")
        events.forEach { e ->
            val detail = when (e.type) {
                StatisticType.SUBSTITUTION ->
                    "${playerName(e.playerId)} → ${playerName(e.relatedPlayerId)}"
                StatisticType.RIVAL_GOAL -> "Gol rival"
                else -> if (e.playerId == null) "Rival" else playerName(e.playerId)
            }
            sb.appendLine(
                listOf(
                    e.minute.toString(),
                    e.period.toString(),
                    esc(e.typeCode),
                    esc(eventLabel(e)),
                    esc(playerName(e.playerId)),
                    esc(e.relatedPlayerId?.let { playerName(it) } ?: ""),
                    esc(detail)
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("jugador_id,dorsal,nombre,segundos_en_campo,minutos_formato")
        secondsOnField.entries.sortedByDescending { it.value }.forEach { (id, secs) ->
            val p = players.firstOrNull { it.id == id } ?: return@forEach
            sb.appendLine(
                listOf(
                    id.toString(),
                    p.jerseyNumber.toString(),
                    esc(p.alias.ifBlank { p.name }),
                    secs.toString(),
                    esc("%d'%02d".format(secs / 60, secs % 60))
                ).joinToString(",")
            )
        }

        val file = File(context.cacheDir, fileName)
        return try {
            file.writeText(sb.toString(), Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawFooter(canvas: Canvas, page: Int) {
        val footerPaint = Paint().apply { color = GREY; textSize = 8f; isAntiAlias = true }
        val text = "Alhendin App · Acta para análisis / IA · pág. $page"
        val w = footerPaint.measureText(text)
        canvas.drawText(text, (PAGE_WIDTH - w) / 2f, PAGE_HEIGHT - 16f, footerPaint)
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 36f
        private const val GREEN = 0xFF2E7D32.toInt()
        private const val DARK = 0xFF212121.toInt()
        private const val GREY = 0xFF757575.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
    }
}
