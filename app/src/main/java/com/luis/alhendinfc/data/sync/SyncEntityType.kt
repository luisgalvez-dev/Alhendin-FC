package com.luis.alhendinfc.data.sync

object SyncEntityType {
    const val TEAM = "teams"
    const val PLAYER = "players"
    const val OPPONENT_CLUB = "opponentClubs"
    const val OPPONENT_PLAYER = "opponentPlayers"
    const val RIVAL_ANALYSIS = "rivalAnalyses"
    const val RIVAL_LINK = "rivalLinks"
    const val SEASON_FIXTURE = "seasonFixtures"
    const val MATCH = "matches"
    const val MATCH_PLAYER = "matchPlayers"
    const val MATCH_EVENT = "matchEvents"
    const val CUSTOM_STAT = "customStatTypes"
    const val TASK = "tasks"
    const val TRAINING = "trainings"
    const val TRAINING_TASK = "trainingTasks"
    const val BOARD = "boards"
    const val ATTACHMENT = "attachments"

    val DOWNLOAD_ORDER = listOf(
        TEAM,
        PLAYER,
        OPPONENT_CLUB,
        CUSTOM_STAT,
        BOARD,
        TASK,
        MATCH,
        SEASON_FIXTURE,
        TRAINING,
        MATCH_PLAYER,
        MATCH_EVENT,
        TRAINING_TASK,
        RIVAL_ANALYSIS,
        RIVAL_LINK,
        OPPONENT_PLAYER,
        ATTACHMENT
    )
}
