package com.luis.alhendinfc.dev

/**
 * Marca visible de registros de prueba. Solo variante DEV.
 * No hay columna isDemo: el prefijo basta para identificarlos y borrarlos.
 */
object DevSeedMarkers {
    const val PREFIX = "[DEV] "
    const val DEMO_TEAM = "Equipo de prueba"

    fun labeled(name: String): String = PREFIX + name

    fun isDemo(value: String?): Boolean = value?.startsWith(PREFIX) == true

    fun isDemoTeam(name: String?): Boolean = name == labeled(DEMO_TEAM)
}

internal object DevSeedCalendar {
    fun nextFree(start: Long, occupied: MutableSet<Long>, step: Long): Long {
        require(step != 0L)
        var day = start
        var guard = 0
        while (day in occupied && guard < 60) {
            day += step
            guard++
        }
        occupied += day
        return day
    }
}
