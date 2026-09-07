package com.luis.alhendinfc.ui.matches

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.Team
import java.io.File
import java.io.FileOutputStream

class ConvocatoriaPdfExporter(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595   // A4 portrait at 72 dpi
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 36f
        private const val GREEN = 0xFF2E7D32.toInt()
        private const val DARK = 0xFF212121.toInt()
        private const val GREY = 0xFF757575.toInt()
        private const val LIGHT_GREY = 0xFFEEEEEE.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val AMBER = 0xFFF9A825.toInt()
    }

    fun export(
        match: Match,
        team: Team?,
        players: List<Player>,
        callupMap: Map<Int, CallupStatus>
    ): Uri? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = MARGIN

        // ─── Header ──────────────────────────────────────────────────────────
        y = drawHeader(canvas, team, match, y)
        y += 8f

        // Green separator line
        val separatorPaint = Paint().apply { color = GREEN; strokeWidth = 2f; isAntiAlias = true }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, separatorPaint)
        y += 12f

        // ─── Match details ────────────────────────────────────────────────────
        y = drawMatchDetails(canvas, match, y)
        y += 12f

        // Green separator
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, separatorPaint)
        y += 12f

        // ─── Players section ──────────────────────────────────────────────────
        val titulares = players.filter { callupMap[it.id] == CallupStatus.TITULAR }
        val suplentes = players.filter { callupMap[it.id] == CallupStatus.SUPLENTE }

        if (titulares.isNotEmpty()) {
            y = drawPlayersSection(canvas, "TITULARES (${titulares.size})", titulares, GREEN, y)
            y += 8f
        }
        if (suplentes.isNotEmpty()) {
            y = drawPlayersSection(canvas, "SUPLENTES (${suplentes.size})", suplentes, AMBER, y)
        }

        // ─── Footer ───────────────────────────────────────────────────────────
        drawFooter(canvas)

        document.finishPage(page)

        val safeRival = match.rival.ifBlank { "partido" }
            .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        val fileName = "convocatoria_${safeRival}_${match.date.replace("/", "-").ifBlank { "sin_fecha" }}.pdf"
        val file = File(context.cacheDir, fileName)

        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun drawHeader(canvas: Canvas, team: Team?, match: Match, startY: Float): Float {
        var y = startY
        val contentWidth = PAGE_WIDTH - MARGIN * 2

        // Team shield (left side)
        val logoSize = 60f
        team?.shieldUri?.let { uriStr ->
            try {
                val bitmap = loadBitmap(uriStr)
                if (bitmap != null) {
                    val src = Rect(0, 0, bitmap.width, bitmap.height)
                    val dst = RectF(MARGIN, y, MARGIN + logoSize, y + logoSize)
                    canvas.drawBitmap(bitmap, src, dst, null)
                }
            } catch (_: Exception) { }
        }

        // Team name and title
        val textLeft = if (team?.shieldUri != null) MARGIN + logoSize + 10f else MARGIN

        val titlePaint = Paint().apply {
            color = DARK; textSize = 22f; isFakeBoldText = true; isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = GREY; textSize = 11f; isAntiAlias = true
        }
        val convocatoriaPaint = Paint().apply {
            color = GREEN; textSize = 18f; isFakeBoldText = true; isAntiAlias = true
        }

        canvas.drawText(team?.name ?: "Alhendin", textLeft, y + 20f, titlePaint)
        canvas.drawText(
            listOf(team?.category, team?.season).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
            textLeft, y + 34f, subtitlePaint
        )

        // "CONVOCATORIA" right-aligned
        val convLabel = "CONVOCATORIA"
        val convWidth = convocatoriaPaint.measureText(convLabel)
        canvas.drawText(convLabel, PAGE_WIDTH - MARGIN - convWidth, y + 20f, convocatoriaPaint)

        // Date and matchday right-aligned
        val dateLine = listOf(
            match.date.ifBlank { null },
            match.time.ifBlank { null },
            if (match.matchday > 0) "J${match.matchday}" else null
        ).filterNotNull().joinToString("  ·  ")
        if (dateLine.isNotBlank()) {
            val dateWidth = subtitlePaint.measureText(dateLine)
            canvas.drawText(dateLine, PAGE_WIDTH - MARGIN - dateWidth, y + 34f, subtitlePaint)
        }

        return y + logoSize + 6f
    }

    private fun drawMatchDetails(canvas: Canvas, match: Match, startY: Float): Float {
        var y = startY
        val labelPaint = Paint().apply { color = GREY; textSize = 10f; isAntiAlias = true }
        val valuePaint = Paint().apply { color = DARK; textSize = 12f; isFakeBoldText = true; isAntiAlias = true }

        val rival = match.rival.ifBlank { "–" }
        val location = if (match.isHome) "Local" else "Visitante"
        val stadium = match.stadium.ifBlank { "–" }
        val duration = "${match.numParts} × ${match.durationPerPart}'"
        val formation = match.formation.ifBlank { "–" }

        // Two columns of match details
        val col1X = MARGIN
        val col2X = PAGE_WIDTH / 2f + 10f

        data class Field(val label: String, val value: String, val col: Float)

        val fields = listOf(
            Field("RIVAL", rival, col1X),
            Field("CONDICIÓN", location, col2X),
            Field("ESTADIO", stadium, col1X),
            Field("DURACIÓN", duration, col2X),
            Field("ALINEACIÓN", formation, col1X)
        )

        val colHeight = 28f
        var col1Y = y
        var col2Y = y

        for (field in fields) {
            val fieldY = if (field.col == col1X) { col1Y.also { col1Y += colHeight } }
                         else { col2Y.also { col2Y += colHeight } }
            canvas.drawText(field.label, field.col, fieldY + 11f, labelPaint)
            canvas.drawText(field.value, field.col, fieldY + 23f, valuePaint)
        }

        if (match.notes.isNotBlank()) {
            val maxY = maxOf(col1Y, col2Y) + 4f
            canvas.drawText("NOTAS", MARGIN, maxY + 11f, labelPaint)
            canvas.drawText(match.notes, MARGIN, maxY + 23f, valuePaint)
            return maxY + 28f
        }

        return maxOf(col1Y, col2Y) + 4f
    }

    private fun drawPlayersSection(
        canvas: Canvas,
        title: String,
        players: List<Player>,
        accentColor: Int,
        startY: Float
    ): Float {
        var y = startY

        // Section title
        val titlePaint = Paint().apply {
            color = accentColor; textSize = 12f; isFakeBoldText = true; isAntiAlias = true
        }
        canvas.drawText(title, MARGIN, y + 13f, titlePaint)
        y += 20f

        // Table header
        val headerBgPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL }
        val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f)
        canvas.drawRect(headerRect, headerBgPaint)

        val headerTextPaint = Paint().apply { color = WHITE; textSize = 9f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("Nº", MARGIN + 4f, y + 13f, headerTextPaint)
        canvas.drawText("NOMBRE", MARGIN + 26f, y + 13f, headerTextPaint)
        canvas.drawText("POSICIÓN", PAGE_WIDTH / 2f, y + 13f, headerTextPaint)
        canvas.drawText("ESTADO", PAGE_WIDTH - MARGIN - 60f, y + 13f, headerTextPaint)
        y += 18f

        val rowPaint = Paint().apply { color = DARK; textSize = 10f; isAntiAlias = true }
        val rowBoldPaint = Paint().apply { color = DARK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val dimPaint = Paint().apply { color = GREY; textSize = 9f; isAntiAlias = true }
        val borderPaint = Paint().apply { color = LIGHT_GREY; style = Paint.Style.STROKE; strokeWidth = 0.5f }

        for ((idx, player) in players.withIndex()) {
            val rowBg = if (idx % 2 == 0) WHITE else 0xFFFAFAFA.toInt()
            val bgPaint = Paint().apply { color = rowBg; style = Paint.Style.FILL }
            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f)
            canvas.drawRect(rowRect, bgPaint)
            canvas.drawRect(rowRect, borderPaint)

            canvas.drawText(
                if (player.jerseyNumber > 0) player.jerseyNumber.toString() else "-",
                MARGIN + 4f, y + 14f, rowBoldPaint
            )
            canvas.drawText(player.name, MARGIN + 26f, y + 14f, rowPaint)
            canvas.drawText(player.position.label, PAGE_WIDTH / 2f, y + 14f, dimPaint)

            val statusLabel = if (!player.isActive) "Inactivo" else "Convocado"
            canvas.drawText(statusLabel, PAGE_WIDTH - MARGIN - 60f, y + 14f, dimPaint)

            y += 20f
        }

        return y
    }

    private fun drawFooter(canvas: Canvas) {
        val footerPaint = Paint().apply { color = GREY; textSize = 8f; isAntiAlias = true }
        val text = "Generado con Alhendin App"
        val textWidth = footerPaint.measureText(text)
        canvas.drawText(text, (PAGE_WIDTH - textWidth) / 2f, PAGE_HEIGHT - 16f, footerPaint)
    }

    private fun loadBitmap(uriStr: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            val maxSide = 256
            while (w / 2 >= maxSide || h / 2 >= maxSide) {
                w /= 2
                h /= 2
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } catch (_: Exception) { null }
    }
}
