package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opponent_player",
    indices = [
        Index(value = ["opponentClubId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class OpponentPlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val opponentClubId: Int,
    val name: String,
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
