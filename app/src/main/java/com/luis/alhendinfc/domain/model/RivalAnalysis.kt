package com.luis.alhendinfc.domain.model

data class RivalAnalysis(
    val id: Int = 0,
    val syncId: String = "",
    val opponentClubId: Int,
    val usualSystem: String = "",
    val variants: String = "",
    val buildUp: String = "",
    val progression: String = "",
    val finalThird: String = "",
    val highPress: String = "",
    val midBlock: String = "",
    val lowBlock: String = "",
    val transAttackToDefense: String = "",
    val transDefenseToAttack: String = "",
    val cornersOffensive: String = "",
    val cornersDefensive: String = "",
    val setPieces: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    val keyPlayers: String = "",
    val generalNotes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
