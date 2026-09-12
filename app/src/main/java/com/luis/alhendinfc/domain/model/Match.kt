package com.luis.alhendinfc.domain.model

data class Match(
    val id: Int = 0,
    val teamId: Int,
    val rival: String = "",
    val stadium: String = "",
    val date: String = "",
    val time: String = "",
    val matchday: Int = 1,
    val isHome: Boolean = true,
    val durationPerPart: Int = 45,
    val numParts: Int = 2,
    val formation: String = "",
    val notes: String = "",
    val status: MatchStatus = MatchStatus.OPEN,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val opponentClubId: Int? = null,
    /** Identidad portable del [OpponentClub]. Preferida frente a [opponentClubId]. */
    val opponentClubSyncId: String? = null,
    val rivalShieldUri: String? = null,
    val livePeriod: Int = 1,
    val liveElapsedSeconds: Int = 0,
    val liveClockRunning: Boolean = false,
    val liveClockAnchorWallMs: Long = 0L,
    val fieldSecondsJson: String = "",
    val fieldPositionsJson: String = "",
    val syncId: String = ""
) {
    fun decodeFieldSeconds(): Map<Int, Int> = parseFieldSeconds(fieldSecondsJson)

    /** playerId → (x, y) relativos 0..1 */
    fun decodeFieldPositions(): Map<Int, Pair<Float, Float>> {
        if (fieldPositionsJson.isBlank()) return emptyMap()
        return fieldPositionsJson.split(',')
            .mapNotNull { part ->
                val bits = part.split(':')
                if (bits.size != 3) return@mapNotNull null
                val id = bits[0].toIntOrNull() ?: return@mapNotNull null
                val x = bits[1].toFloatOrNull() ?: return@mapNotNull null
                val y = bits[2].toFloatOrNull() ?: return@mapNotNull null
                id to (x to y)
            }
            .toMap()
    }

    companion object {
        fun parseFieldSeconds(json: String): Map<Int, Int> {
            if (json.isBlank()) return emptyMap()
            return json.split(',')
                .mapNotNull { part ->
                    val bits = part.trim().split(':')
                    if (bits.size != 2) return@mapNotNull null
                    val id = bits[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val secs = bits[1].trim().toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
                    id to secs
                }
                .toMap()
        }

        fun encodeFieldSeconds(map: Map<Int, Int>): String =
            map.entries.joinToString(",") { "${it.key}:${it.value}" }

        fun encodeFieldPositions(map: Map<Int, Pair<Float, Float>>): String =
            map.entries.joinToString(",") { "${it.key}:${it.value.first}:${it.value.second}" }
    }
}
