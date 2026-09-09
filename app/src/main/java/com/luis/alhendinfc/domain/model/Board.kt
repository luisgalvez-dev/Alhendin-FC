package com.luis.alhendinfc.domain.model

data class Board(
    val id: Int = 0,
    val syncId: String = "",
    val teamId: Int,
    val name: String = "",
    val sceneVersion: Int = BoardScene.CURRENT_VERSION,
    val sceneJson: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

object BoardRules {
    fun validate(name: String): String? {
        if (name.trim().isEmpty()) return "El nombre es obligatorio"
        return null
    }

    fun copyName(original: String): String {
        val trimmed = original.trim()
        return if (trimmed.startsWith("Copia de ")) trimmed else "Copia de $trimmed"
    }
}
