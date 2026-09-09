package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_task",
    indices = [
        Index(value = ["trainingId"]),
        Index(value = ["taskId"]),
        Index(value = ["trainingId", "taskId"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class TrainingTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val trainingId: Int,
    val taskId: Int,
    val sortOrder: Int = 0,
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
