package com.luis.alhendinfc.ui.pizarra

import com.luis.alhendinfc.domain.model.BoardNormPoint
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import kotlin.math.sqrt

/**
 * Hit-test y reglas de colocación “sello” del editor de pizarra.
 * Coordenadas normalizadas 0..1. Sin dependencias de Android UI.
 */
object BoardEditorLogic {

    fun isStampTool(tool: BoardEditorTool): Boolean = when (tool) {
        BoardEditorTool.BLUE_PLAYER,
        BoardEditorTool.RED_PLAYER,
        BoardEditorTool.BALL,
        BoardEditorTool.CONE,
        BoardEditorTool.GOAL,
        BoardEditorTool.MINI_GOAL,
        BoardEditorTool.TEXT -> true
        else -> false
    }

    fun toolAfterStamp(): BoardEditorTool = BoardEditorTool.SELECT

    /**
     * Un sello coloca como máximo un objeto por gesto. Si el press ya colocó,
     * un release o un drag del mismo gesto no debe volver a insertar.
     */
    fun shouldPlaceStamp(
        tool: BoardEditorTool,
        stampConsumedThisGesture: Boolean,
        pendingText: String?
    ): Boolean {
        if (!isStampTool(tool) || stampConsumedThisGesture) return false
        if (tool == BoardEditorTool.TEXT && pendingText.isNullOrBlank()) return false
        return true
    }

    fun stampObjectType(tool: BoardEditorTool): String? = when (tool) {
        BoardEditorTool.BLUE_PLAYER -> BoardObjectType.BLUE_PLAYER
        BoardEditorTool.RED_PLAYER -> BoardObjectType.RED_PLAYER
        BoardEditorTool.BALL -> BoardObjectType.BALL
        BoardEditorTool.CONE -> BoardObjectType.CONE
        BoardEditorTool.GOAL -> BoardObjectType.GOAL
        BoardEditorTool.MINI_GOAL -> BoardObjectType.MINI_GOAL
        BoardEditorTool.TEXT -> BoardObjectType.TEXT
        else -> null
    }

    /**
     * Objeto más cercano al toque, si cae dentro de su radio (o caja) de hit.
     * Los empates los gana el que está más arriba (último en la lista).
     */
    fun hitTest(objects: List<BoardObject>, point: BoardPoint): BoardObject? {
        var best: BoardObject? = null
        var bestDist = Float.MAX_VALUE
        for (obj in objects) {
            val dist = distanceTo(obj, point)
            val radius = hitRadius(obj)
            if (dist <= radius && dist <= bestDist) {
                best = obj
                bestDist = dist
            }
        }
        return best
    }

    internal fun distanceTo(obj: BoardObject, point: BoardPoint): Float {
        if (obj.type in BoardObjectType.STROKE && obj.points.size >= 2) {
            return minDistanceToPolyline(obj.points, point)
        }
        if (obj.type == BoardObjectType.GOAL || obj.type == BoardObjectType.MINI_GOAL) {
            return distanceToGoalBox(obj, point)
        }
        val dx = obj.x - point.x
        val dy = obj.y - point.y
        return sqrt(dx * dx + dy * dy)
    }

    internal fun hitRadius(obj: BoardObject): Float = when (obj.type) {
        BoardObjectType.BLUE_PLAYER, BoardObjectType.RED_PLAYER -> 0.08f
        BoardObjectType.BALL -> 0.04f
        BoardObjectType.CONE -> 0.07f
        BoardObjectType.GOAL -> 0.05f
        BoardObjectType.MINI_GOAL -> 0.03f
        BoardObjectType.TEXT -> 0.09f
        BoardObjectType.PATH, BoardObjectType.ARROW, BoardObjectType.ERASER -> 0.045f
        BoardObjectType.OVAL -> 0.05f
        else -> 0.07f
    }

    private fun distanceToGoalBox(obj: BoardObject, point: BoardPoint): Float {
        val halfW = if (obj.type == BoardObjectType.GOAL) 0.11f else 0.06f
        val halfH = if (obj.type == BoardObjectType.GOAL) 0.10f else 0.06f
        val dx = when {
            point.x < obj.x - halfW -> point.x - (obj.x - halfW)
            point.x > obj.x + halfW -> point.x - (obj.x + halfW)
            else -> 0f
        }
        val dy = when {
            point.y < obj.y - halfH -> point.y - (obj.y - halfH)
            point.y > obj.y + halfH -> point.y - (obj.y + halfH)
            else -> 0f
        }
        return sqrt(dx * dx + dy * dy)
    }

    private fun minDistanceToPolyline(points: List<BoardNormPoint>, point: BoardPoint): Float {
        var min = Float.MAX_VALUE
        for (i in 0 until points.lastIndex) {
            val d = distanceToSegment(points[i], points[i + 1], point)
            if (d < min) min = d
        }
        return min
    }

    private fun distanceToSegment(a: BoardNormPoint, b: BoardNormPoint, p: BoardPoint): Float {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val wx = p.x - a.x
        val wy = p.y - a.y
        val c1 = vx * wx + vy * wy
        if (c1 <= 0f) return hypot(p.x - a.x, p.y - a.y)
        val c2 = vx * vx + vy * vy
        if (c2 <= c1) return hypot(p.x - b.x, p.y - b.y)
        val t = c1 / c2
        return hypot(p.x - (a.x + t * vx), p.y - (a.y + t * vy))
    }

    private fun hypot(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)
}
