package com.luis.alhendinfc.domain.model

/**
 * Módulos / atajos personalizables de la pantalla de inicio.
 */
enum class HomeModule(
    val id: String,
    val title: String,
    val description: String,
    val kind: HomeModuleKind
) {
    TEAM(
        id = "team",
        title = "EQUIPO",
        description = "Plantilla y ficha del club",
        kind = HomeModuleKind.PRIMARY
    ),
    MATCHES(
        id = "matches",
        title = "PARTIDOS",
        description = "Convocatoria y partidos",
        kind = HomeModuleKind.PRIMARY
    ),
    CALENDAR(
        id = "calendar",
        title = "CALENDARIO",
        description = "Jornadas de la temporada",
        kind = HomeModuleKind.PRIMARY
    ),
    TASKS(
        id = "tasks",
        title = "TAREAS",
        description = "Ejercicios de entrenamiento reutilizables",
        kind = HomeModuleKind.PRIMARY
    ),
    PIZARRA(
        id = "pizarra",
        title = "PIZARRA",
        description = "Análisis táctico sobre imagen o vídeo",
        kind = HomeModuleKind.PRIMARY
    ),
    STATISTICS(
        id = "statistics",
        title = "ESTADÍSTICAS",
        description = "Goles, asistencias, tarjetas…",
        kind = HomeModuleKind.SECONDARY
    ),
    SETTINGS(
        id = "settings",
        title = "AJUSTES",
        description = "Tipos de evento y personalización",
        kind = HomeModuleKind.SECONDARY
    ),
    NEXT_MATCH(
        id = "next_match",
        title = "PRÓXIMO PARTIDO",
        description = "Siguiente jornada del calendario",
        kind = HomeModuleKind.INFO
    ),
    LIVE_MATCH(
        id = "live_match",
        title = "EN VIVO",
        description = "Acceso directo si hay partido en curso",
        kind = HomeModuleKind.HIGHLIGHT
    );

    companion object {
        val DEFAULT_ORDER: List<HomeModule> = entries.toList()

        fun fromId(id: String): HomeModule? = entries.find { it.id == id }
    }
}

enum class HomeModuleKind {
    PRIMARY,
    SECONDARY,
    INFO,
    HIGHLIGHT
}

data class HomeModulePreference(
    val module: HomeModule,
    val enabled: Boolean
)

data class HomeLayoutConfig(
    val modules: List<HomeModulePreference>
) {
    val visibleOrdered: List<HomeModule>
        get() = modules.filter { it.enabled }.map { it.module }

    companion object {
        fun defaults(): HomeLayoutConfig =
            HomeLayoutConfig(
                HomeModule.DEFAULT_ORDER.map { HomeModulePreference(it, enabled = true) }
            )
    }
}
