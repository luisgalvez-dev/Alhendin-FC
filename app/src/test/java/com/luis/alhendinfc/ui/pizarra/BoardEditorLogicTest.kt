package com.luis.alhendinfc.ui.pizarra

import com.luis.alhendinfc.domain.model.BoardNormPoint
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEditorLogicTest {

    @Test
    fun stampTools_resetToSelectAndPlaceOncePerGesture() {
        val stamps = listOf(
            BoardEditorTool.BLUE_PLAYER,
            BoardEditorTool.RED_PLAYER,
            BoardEditorTool.BALL,
            BoardEditorTool.CONE,
            BoardEditorTool.GOAL,
            BoardEditorTool.MINI_GOAL,
            BoardEditorTool.TEXT
        )
        stamps.forEach { tool ->
            assertTrue(tool.name, BoardEditorLogic.isStampTool(tool))
            val pending = if (tool == BoardEditorTool.TEXT) "Pressing" else null
            assertTrue(tool.name, BoardEditorLogic.shouldPlaceStamp(tool, false, pending))
            assertFalse(tool.name, BoardEditorLogic.shouldPlaceStamp(tool, true, pending))
        }
        assertEquals(BoardEditorTool.SELECT, BoardEditorLogic.toolAfterStamp())
        assertFalse(BoardEditorLogic.isStampTool(BoardEditorTool.SELECT))
        assertFalse(BoardEditorLogic.isStampTool(BoardEditorTool.PEN))
        assertFalse(BoardEditorLogic.shouldPlaceStamp(BoardEditorTool.TEXT, false, "  "))
        assertFalse(BoardEditorLogic.shouldPlaceStamp(BoardEditorTool.TEXT, false, null))
        assertFalse(BoardEditorLogic.shouldPlaceStamp(BoardEditorTool.SELECT, false, null))
    }

    @Test
    fun hitTest_picksClosestPlayerNotExactCenter() {
        val blue = token("blue", BoardObjectType.BLUE_PLAYER, 0.40f, 0.50f)
        val red = token("red", BoardObjectType.RED_PLAYER, 0.70f, 0.50f)
        val objects = listOf(blue, red)

        assertEquals("blue", BoardEditorLogic.hitTest(objects, BoardPoint(0.40f, 0.50f))?.objectId)
        assertEquals("blue", BoardEditorLogic.hitTest(objects, BoardPoint(0.445f, 0.52f))?.objectId)
        assertEquals("red", BoardEditorLogic.hitTest(objects, BoardPoint(0.66f, 0.48f))?.objectId)
        assertNull(BoardEditorLogic.hitTest(objects, BoardPoint(0.10f, 0.10f)))
    }

    @Test
    fun hitTest_prefersNearestWhenTokensOverlapRadius() {
        val far = token("far", BoardObjectType.BLUE_PLAYER, 0.30f, 0.50f)
        val near = token("near", BoardObjectType.RED_PLAYER, 0.36f, 0.50f)
        val hit = BoardEditorLogic.hitTest(listOf(far, near), BoardPoint(0.37f, 0.50f))
        assertEquals("near", hit?.objectId)
    }

    @Test
    fun hitTest_playerWinsOverDistantStrokeStart() {
        val player = token("player", BoardObjectType.BLUE_PLAYER, 0.50f, 0.50f)
        val stroke = BoardObject(
            objectId = "line",
            type = BoardObjectType.PATH,
            x = 0.10f,
            y = 0.10f,
            points = listOf(BoardNormPoint(0.10f, 0.10f), BoardNormPoint(0.12f, 0.90f))
        )
        val hit = BoardEditorLogic.hitTest(listOf(stroke, player), BoardPoint(0.52f, 0.51f))
        assertEquals("player", hit?.objectId)
    }

    @Test
    fun hitTest_selectsStrokeByNearestSegment() {
        val stroke = BoardObject(
            objectId = "arrow",
            type = BoardObjectType.ARROW,
            x = 0.20f,
            y = 0.20f,
            points = listOf(BoardNormPoint(0.20f, 0.20f), BoardNormPoint(0.80f, 0.20f))
        )
        assertEquals("arrow", BoardEditorLogic.hitTest(listOf(stroke), BoardPoint(0.50f, 0.22f))?.objectId)
        assertNull(BoardEditorLogic.hitTest(listOf(stroke), BoardPoint(0.50f, 0.50f)))
    }

    @Test
    fun hitTest_goalBoxIsLargerThanMiniGoal() {
        val goal = token("goal", BoardObjectType.GOAL, 0.50f, 0.50f)
        val mini = token("mini", BoardObjectType.MINI_GOAL, 0.50f, 0.50f)
        assertEquals("goal", BoardEditorLogic.hitTest(listOf(goal), BoardPoint(0.58f, 0.50f))?.objectId)
        assertNull(BoardEditorLogic.hitTest(listOf(mini), BoardPoint(0.58f, 0.50f)))
        assertEquals("mini", BoardEditorLogic.hitTest(listOf(mini), BoardPoint(0.54f, 0.50f))?.objectId)
    }

    @Test
    fun hitTest_smallBallIsSelectableNearCenter() {
        val ball = token("ball", BoardObjectType.BALL, 0.50f, 0.50f)
        assertEquals("ball", BoardEditorLogic.hitTest(listOf(ball), BoardPoint(0.52f, 0.51f))?.objectId)
        assertNull(BoardEditorLogic.hitTest(listOf(ball), BoardPoint(0.58f, 0.50f)))
    }

    private fun token(id: String, type: String, x: Float, y: Float) = BoardObject(
        objectId = id,
        type = type,
        x = x,
        y = y
    )
}
