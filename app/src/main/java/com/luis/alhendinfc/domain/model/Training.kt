package com.luis.alhendinfc.domain.model

data class Training(
    val id: Int = 0,
    val teamId: Int,
    val date: String,
    val dateEpochDay: Long = 0L,
    val opponentClubId: Int? = null,
    val notes: String = "",
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

data class TrainingTask(
    val id: Int = 0,
    val trainingId: Int,
    val taskId: Int,
    val sortOrder: Int = 0,
    val syncId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

data class TrainingTaskItem(
    val relation: TrainingTask,
    val task: Task
)

object TrainingRules {
    fun occupiedByMatchOrFixture(
        epochDay: Long,
        matchEpochDays: Set<Long>,
        fixtureEpochDays: Set<Long>
    ): Boolean = epochDay in matchEpochDays || epochDay in fixtureEpochDays

    fun canCreateTraining(
        epochDay: Long,
        hasActiveTraining: Boolean,
        matchEpochDays: Set<Long>,
        fixtureEpochDays: Set<Long>
    ): Boolean {
        if (hasActiveTraining) return false
        if (occupiedByMatchOrFixture(epochDay, matchEpochDays, fixtureEpochDays)) return false
        return true
    }
}
