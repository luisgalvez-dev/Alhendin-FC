package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "dateEpochDay"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class TrainingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val teamId: Int,
    val date: String,
    val dateEpochDay: Long,
    val opponentClubId: Int? = null,
    val notes: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
