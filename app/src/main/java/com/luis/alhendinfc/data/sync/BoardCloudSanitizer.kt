package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.domain.model.BoardBackgroundType
import com.luis.alhendinfc.domain.model.BoardScene
import org.json.JSONObject

object BoardCloudSanitizer {
    fun portableSceneJson(sceneJson: String): String {
        if (sceneJson.isBlank()) return BoardScene.empty().toJson()
        return runCatching {
            val scene = BoardScene.fromJson(sceneJson)
            val looksLocal = sceneJson.contains("content://") ||
                sceneJson.contains("/data/") ||
                sceneJson.contains("file://")
            if (!looksLocal) {
                scene.copy(
                    background = if (scene.background == BoardBackgroundType.IMAGE ||
                        scene.background == BoardBackgroundType.VIDEO
                    ) {
                        scene.background
                    } else {
                        scene.background
                    }
                ).toJson()
            } else {
                scene.copy(
                    background = BoardBackgroundType.FIELD,
                    mediaAttachmentSyncId = scene.mediaAttachmentSyncId
                ).toJson()
            }
        }.getOrElse { BoardScene.empty().toJson() }
    }

    fun containsLocalPath(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return value.contains("content://") ||
            value.contains("file://") ||
            value.contains("/data/")
    }
}
