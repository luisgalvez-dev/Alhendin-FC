package com.luis.alhendinfc.data.sync

/**
 * Qué viaja en el dataset deportivo compartido y qué se queda en el dispositivo.
 *
 * Compartido (futuro Firestore): Team (sin isSelected), Player, OpponentClub,
 * SeasonFixture, Match durable, MatchPlayer durable, MatchEvent, CustomStatType,
 * y más adelante Task/Training/Board/Attachment/RivalAnalysis.
 *
 * Personal / local:
 * - DataStore `home_prefs` del dispositivo (`home_layout`). No se lee ni escribe
 *   `users/{uid}/preferences/homeLayout` en cloud.
 * - [com.luis.alhendinfc.data.local.TeamEntity.isSelected]
 * - estado LIVE de partido (ver [MatchLiveLocalState])
 */
object SyncScope {
    const val HOME_PREFS_ARE_PERSONAL = true
    const val TEAM_IS_SELECTED_IS_LOCAL = true
}
