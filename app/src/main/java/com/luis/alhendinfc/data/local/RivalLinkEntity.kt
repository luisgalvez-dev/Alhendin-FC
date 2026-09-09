package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rival_link",
    indices = [
        Index(value = ["opponentClubId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class RivalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val opponentClubId: Int,
    val type: String,
    val label: String = "",
    val url: String = "",
    val sortOrder: Int = 0,
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
