package com.luis.alhendinfc.domain.model

/** A quién se aplica un tipo de estadística personalizada en el partido. */
enum class CustomStatAppliesTo(val label: String) {
    ALL("Todos"),
    GOALKEEPER("Solo porteros"),
    OUTFIELD("Solo de campo");

    fun matches(position: PlayerPosition): Boolean = when (this) {
        ALL -> true
        GOALKEEPER -> position == PlayerPosition.PORTERO
        OUTFIELD -> position != PlayerPosition.PORTERO
    }

    companion object {
        fun fromName(name: String): CustomStatAppliesTo =
            entries.firstOrNull { it.name == name } ?: ALL
    }
}
