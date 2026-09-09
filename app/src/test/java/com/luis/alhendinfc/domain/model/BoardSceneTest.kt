package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardSceneTest {

    @Test
    fun roundTrip_preservesNormalizedCoordsObjectIdAndTypes() {
        val original = BoardScene(
            version = BoardScene.CURRENT_VERSION,
            background = BoardBackgroundType.FIELD,
            objects = listOf(
                BoardObject(
                    objectId = "obj-stable-1",
                    type = BoardObjectType.BLUE_PLAYER,
                    x = 0.25f,
                    y = 0.75f,
                    number = "4"
                ),
                BoardObject(
                    objectId = "obj-arrow-1",
                    type = BoardObjectType.ARROW,
                    x = 0.4f,
                    y = 0.4f,
                    points = listOf(BoardNormPoint(0.1f, 0.2f), BoardNormPoint(0.8f, 0.9f))
                )
            )
        )
        val restored = BoardScene.fromJson(original.toJson())
        assertEquals(original.version, restored.version)
        assertEquals(original.background, restored.background)
        assertEquals(2, restored.objects.size)
        assertEquals("obj-stable-1", restored.objects[0].objectId)
        assertEquals(BoardObjectType.BLUE_PLAYER, restored.objects[0].type)
        assertEquals(0.25f, restored.objects[0].x, 0.0001f)
        assertEquals(0.75f, restored.objects[0].y, 0.0001f)
        assertEquals("4", restored.objects[0].number)
        assertEquals(2, restored.objects[1].points.size)
        assertEquals(0.8f, restored.objects[1].points[1].x, 0.0001f)
    }

    @Test
    fun blankJson_isEmptyScene() {
        val scene = BoardScene.fromJson("  ")
        assertEquals(BoardScene.CURRENT_VERSION, scene.version)
        assertTrue(scene.objects.isEmpty())
        assertNull(scene.mediaAttachmentSyncId)
    }

    @Test
    fun copyName_prefixesOnce() {
        assertEquals("Copia de Salida", BoardRules.copyName("Salida"))
        assertEquals("Copia de Salida", BoardRules.copyName("Copia de Salida"))
        assertEquals("El nombre es obligatorio", BoardRules.validate("  "))
        assertNull(BoardRules.validate("Presión"))
    }
}
