package com.luis.alhendinfc.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachment",
    indices = [
        Index(value = ["parentType", "parentSyncId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    override val syncId: String = "",
    val parentType: String,
    val parentSyncId: String,
    val mimeType: String,
    val name: String,
    val localPath: String? = null,
    val remotePath: String? = null,
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val deletedAt: Long? = null
) : Syncable
