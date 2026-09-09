package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rival_analysis",
    indices = [
        Index(value = ["opponentClubId"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class RivalAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
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
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
