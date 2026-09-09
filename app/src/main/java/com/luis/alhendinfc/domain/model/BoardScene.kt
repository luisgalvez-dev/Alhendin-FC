package com.luis.alhendinfc.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Escena versionada de una pizarra. Coordenadas normalizadas 0..1
 * respecto al área del campo, para reconstruir igual en cualquier tamaño.
 * Cada elemento tiene [objectId] estable (preparado para animaciones futuras).
 */
data class BoardScene(
    val version: Int = CURRENT_VERSION,
    val background: String = BoardBackgroundType.FIELD,
    val mediaAttachmentSyncId: String? = null,
    val objects: List<BoardObject> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun empty(): BoardScene = BoardScene()

        fun fromJson(json: String): BoardScene {
            if (json.isBlank()) return empty()
            val root = JSONObject(json)
            val objectsArr = root.optJSONArray("objects") ?: JSONArray()
            val objects = buildList {
                for (i in 0 until objectsArr.length()) {
                    add(BoardObject.fromJson(objectsArr.getJSONObject(i)))
                }
            }
            return BoardScene(
                version = root.optInt("version", CURRENT_VERSION),
                background = root.optString("background", BoardBackgroundType.FIELD)
                    .ifBlank { BoardBackgroundType.FIELD },
                mediaAttachmentSyncId = root.optString("mediaAttachmentSyncId")
                    .takeIf { it.isNotBlank() && it != "null" },
                objects = objects
            )
        }
    }

    fun toJson(): String {
        val arr = JSONArray()
        objects.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("version", version)
            .put("background", background)
            .put("mediaAttachmentSyncId", mediaAttachmentSyncId ?: JSONObject.NULL)
            .put("objects", arr)
            .toString()
    }
}

object BoardBackgroundType {
    const val FIELD = "FIELD"
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
}

object BoardObjectType {
    const val BLUE_PLAYER = "bluePlayer"
    const val RED_PLAYER = "redPlayer"
    const val BALL = "ball"
    const val CONE = "cone"
    const val GOAL = "goal"
    const val MINI_GOAL = "miniGoal"
    const val TEXT = "text"
    const val PATH = "path"
    const val ARROW = "arrow"
    const val OVAL = "oval"
    const val ERASER = "eraser"

    val PLACEABLE = setOf(BLUE_PLAYER, RED_PLAYER, BALL, CONE, GOAL, MINI_GOAL, TEXT)
    val STROKE = setOf(PATH, ARROW, OVAL, ERASER)
}

data class BoardNormPoint(val x: Float, val y: Float)

data class BoardObject(
    val objectId: String,
    val type: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val number: String? = null,
    val text: String? = null,
    val colorArgb: Long? = null,
    val width: Float? = null,
    val points: List<BoardNormPoint> = emptyList()
) {
    fun toJson(): JSONObject {
        val pts = JSONArray()
        points.forEach { p ->
            pts.put(JSONObject().put("x", p.x.toDouble()).put("y", p.y.toDouble()))
        }
        return JSONObject()
            .put("objectId", objectId)
            .put("type", type)
            .put("x", x.toDouble())
            .put("y", y.toDouble())
            .put("number", number ?: JSONObject.NULL)
            .put("text", text ?: JSONObject.NULL)
            .put("colorArgb", colorArgb ?: JSONObject.NULL)
            .put("width", width?.toDouble() ?: JSONObject.NULL)
            .put("points", pts)
    }

    companion object {
        fun fromJson(o: JSONObject): BoardObject {
            val ptsArr = o.optJSONArray("points") ?: JSONArray()
            val points = buildList {
                for (i in 0 until ptsArr.length()) {
                    val p = ptsArr.getJSONObject(i)
                    add(BoardNormPoint(p.optDouble("x").toFloat(), p.optDouble("y").toFloat()))
                }
            }
            return BoardObject(
                objectId = o.optString("objectId"),
                type = o.optString("type"),
                x = o.optDouble("x", 0.5).toFloat(),
                y = o.optDouble("y", 0.5).toFloat(),
                number = o.optString("number").takeIf { it.isNotBlank() && it != "null" },
                text = o.optString("text").takeIf { it.isNotBlank() && it != "null" },
                colorArgb = if (o.has("colorArgb") && !o.isNull("colorArgb")) o.optLong("colorArgb") else null,
                width = if (o.has("width") && !o.isNull("width")) o.optDouble("width").toFloat() else null,
                points = points
            )
        }
    }
}
