package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_stat_type",
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["teamId", "code"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class CustomStatTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val code: String,
    val label: String,
    val shortLabel: String,
    val appliesTo: String = "ALL",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
