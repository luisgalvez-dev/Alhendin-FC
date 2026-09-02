package com.luis.alhendinfc.domain.model

enum class PlayerPosition(val label: String, val fieldX: Float, val fieldY: Float) {
    PORTERO("Portero", 0.08f, 0.5f),
    LATERAL_DERECHO("Lateral Derecho", 0.25f, 0.18f),
    LATERAL_IZQUIERDO("Lateral Izquierdo", 0.25f, 0.82f),
    CENTRAL_DERECHO("Central Derecho", 0.25f, 0.38f),
    CENTRAL_IZQUIERDO("Central Izquierdo", 0.25f, 0.62f),
    MEDIOCENTRO_DEFENSIVO("Medioc. Defensivo", 0.45f, 0.5f),
    MEDIOCENTRO_OFENSIVO("Medioc. Ofensivo", 0.6f, 0.5f),
    INTERIOR_DERECHO("Interior Derecho", 0.52f, 0.28f),
    INTERIOR_IZQUIERDO("Interior Izquierdo", 0.52f, 0.72f),
    EXTREMO_DERECHO("Extremo Derecho", 0.68f, 0.12f),
    EXTREMO_IZQUIERDO("Extremo Izquierdo", 0.68f, 0.88f),
    DELANTERO("Delantero", 0.82f, 0.5f)
}
