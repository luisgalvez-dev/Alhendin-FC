package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "player",
    indices = [Index(value = ["teamId"])]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val name: String,
    val alias: String = "",
    val position: String = "MEDIOCENTRO_DEFENSIVO",
    val jerseyNumber: Int = 0,
    val photoUri: String? = null,
    val height: Int = 0,
    val weight: Int = 0,
    val laterality: String = "DERECHA",
    val isActive: Boolean = true,
    val observations: String = ""
)
