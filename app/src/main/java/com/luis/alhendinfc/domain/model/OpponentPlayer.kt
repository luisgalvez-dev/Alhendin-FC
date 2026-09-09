package com.luis.alhendinfc.domain.model

data class OpponentPlayer(
    val id: Int = 0,
    val syncId: String = "",
    val opponentClubId: Int,
    val name: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

object OpponentPlayerRules {
    fun validate(name: String): String? {
        if (name.trim().isEmpty()) return "El nombre es obligatorio"
        return null
    }
}
