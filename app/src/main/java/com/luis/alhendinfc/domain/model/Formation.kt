package com.luis.alhendinfc.domain.model

data class FormationSlot(
    val x: Float,
    val y: Float
)

enum class Formation(val label: String, val slots: List<FormationSlot>) {
    F_4_3_3(
        "4-3-3",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.28f, 0.18f),
            FormationSlot(0.26f, 0.38f),
            FormationSlot(0.26f, 0.62f),
            FormationSlot(0.28f, 0.82f),
            FormationSlot(0.50f, 0.28f),
            FormationSlot(0.48f, 0.50f),
            FormationSlot(0.50f, 0.72f),
            FormationSlot(0.78f, 0.18f),
            FormationSlot(0.82f, 0.50f),
            FormationSlot(0.78f, 0.82f)
        )
    ),
    F_4_4_2(
        "4-4-2",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.28f, 0.18f),
            FormationSlot(0.26f, 0.38f),
            FormationSlot(0.26f, 0.62f),
            FormationSlot(0.28f, 0.82f),
            FormationSlot(0.55f, 0.18f),
            FormationSlot(0.50f, 0.38f),
            FormationSlot(0.50f, 0.62f),
            FormationSlot(0.55f, 0.82f),
            FormationSlot(0.80f, 0.38f),
            FormationSlot(0.80f, 0.62f)
        )
    ),
    F_4_2_3_1(
        "4-2-3-1",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.28f, 0.18f),
            FormationSlot(0.26f, 0.38f),
            FormationSlot(0.26f, 0.62f),
            FormationSlot(0.28f, 0.82f),
            FormationSlot(0.45f, 0.38f),
            FormationSlot(0.45f, 0.62f),
            FormationSlot(0.68f, 0.20f),
            FormationSlot(0.62f, 0.50f),
            FormationSlot(0.68f, 0.80f),
            FormationSlot(0.84f, 0.50f)
        )
    ),
    F_3_5_2(
        "3-5-2",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.26f, 0.28f),
            FormationSlot(0.24f, 0.50f),
            FormationSlot(0.26f, 0.72f),
            FormationSlot(0.48f, 0.12f),
            FormationSlot(0.50f, 0.38f),
            FormationSlot(0.48f, 0.50f),
            FormationSlot(0.50f, 0.62f),
            FormationSlot(0.48f, 0.88f),
            FormationSlot(0.80f, 0.38f),
            FormationSlot(0.80f, 0.62f)
        )
    ),
    F_3_4_3(
        "3-4-3",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.26f, 0.28f),
            FormationSlot(0.24f, 0.50f),
            FormationSlot(0.26f, 0.72f),
            FormationSlot(0.50f, 0.18f),
            FormationSlot(0.48f, 0.38f),
            FormationSlot(0.48f, 0.62f),
            FormationSlot(0.50f, 0.82f),
            FormationSlot(0.78f, 0.20f),
            FormationSlot(0.84f, 0.50f),
            FormationSlot(0.78f, 0.80f)
        )
    ),
    F_5_3_2(
        "5-3-2",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.32f, 0.12f),
            FormationSlot(0.24f, 0.32f),
            FormationSlot(0.22f, 0.50f),
            FormationSlot(0.24f, 0.68f),
            FormationSlot(0.32f, 0.88f),
            FormationSlot(0.52f, 0.30f),
            FormationSlot(0.50f, 0.50f),
            FormationSlot(0.52f, 0.70f),
            FormationSlot(0.80f, 0.38f),
            FormationSlot(0.80f, 0.62f)
        )
    ),
    F_4_1_4_1(
        "4-1-4-1",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.28f, 0.18f),
            FormationSlot(0.26f, 0.38f),
            FormationSlot(0.26f, 0.62f),
            FormationSlot(0.28f, 0.82f),
            FormationSlot(0.44f, 0.50f),
            FormationSlot(0.62f, 0.18f),
            FormationSlot(0.60f, 0.38f),
            FormationSlot(0.60f, 0.62f),
            FormationSlot(0.62f, 0.82f),
            FormationSlot(0.84f, 0.50f)
        )
    ),
    F_4_5_1(
        "4-5-1",
        listOf(
            FormationSlot(0.10f, 0.50f),
            FormationSlot(0.28f, 0.18f),
            FormationSlot(0.26f, 0.38f),
            FormationSlot(0.26f, 0.62f),
            FormationSlot(0.28f, 0.82f),
            FormationSlot(0.55f, 0.12f),
            FormationSlot(0.50f, 0.35f),
            FormationSlot(0.48f, 0.50f),
            FormationSlot(0.50f, 0.65f),
            FormationSlot(0.55f, 0.88f),
            FormationSlot(0.82f, 0.50f)
        )
    );

    companion object {
        fun fromLabel(label: String): Formation =
            entries.firstOrNull { it.label == label } ?: F_4_3_3
    }
}

fun assignPlayersToFormation(
    players: List<Player>,
    formation: Formation
): List<Pair<FormationSlot, Player?>> {
    val slots = formation.slots
    val assignedPlayer = mutableSetOf<Int>()
    val assignedSlot = mutableSetOf<Int>()
    val result = MutableList<Player?>(slots.size) { null }

    val pairs = players.flatMap { player ->
        slots.mapIndexed { index, slot ->
            val dx = player.position.fieldX - slot.x
            val dy = player.position.fieldY - slot.y
            Triple(player, index, dx * dx + dy * dy)
        }
    }.sortedBy { it.third }

    for ((player, slotIndex, _) in pairs) {
        if (player.id in assignedPlayer || slotIndex in assignedSlot) continue
        result[slotIndex] = player
        assignedPlayer.add(player.id)
        assignedSlot.add(slotIndex)
    }

    return slots.mapIndexed { index, slot -> slot to result[index] }
}
