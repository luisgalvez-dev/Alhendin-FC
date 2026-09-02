package com.luis.alhendinfc.domain.model

data class Player(
    val id: Int = 0,
    val teamId: Int,
    val name: String,
    val alias: String = "",
    val position: PlayerPosition = PlayerPosition.MEDIOCENTRO_DEFENSIVO,
    val jerseyNumber: Int = 0,
    val photoUri: String? = null,
    val height: Int = 0,
    val weight: Int = 0,
    val laterality: Laterality = Laterality.DERECHA,
    val isActive: Boolean = true,
    val observations: String = ""
)
