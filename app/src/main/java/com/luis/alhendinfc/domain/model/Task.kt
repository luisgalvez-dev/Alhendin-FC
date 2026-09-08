package com.luis.alhendinfc.domain.model

data class Task(
    val id: Int = 0,
    val teamId: Int,
    val name: String,
    val objective: String = "",
    val playerCount: Int? = null,
    val durationMinutes: Int? = null,
    val description: String = "",
    val boardSyncId: String? = null,
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

object TaskRules {
    fun validate(
        name: String,
        playerCount: Int?,
        durationMinutes: Int?
    ): String? {
        if (name.isBlank()) return "El nombre es obligatorio"
        if (playerCount != null && playerCount <= 0) return "El número de jugadores debe ser mayor que 0"
        if (durationMinutes != null && durationMinutes <= 0) return "La duración debe ser mayor que 0 minutos"
        return null
    }

    fun matchesName(name: String, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return name.contains(needle, ignoreCase = true)
    }
}
