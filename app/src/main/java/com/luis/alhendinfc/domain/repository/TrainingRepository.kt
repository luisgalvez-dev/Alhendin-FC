package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun getActiveByParent(parentType: String, parentSyncId: String): Flow<List<Attachment>>
    fun getActiveByType(parentType: String): Flow<List<Attachment>>
    suspend fun add(
        parentType: String,
        parentSyncId: String,
        mimeType: String,
        name: String,
        localPath: String,
        syncId: String = ""
    ): Int
    suspend fun delete(attachment: Attachment)
    suspend fun deleteByParent(parentType: String, parentSyncId: String)
    suspend fun setTaskImage(taskSyncId: String, mimeType: String, name: String, localPath: String)
    suspend fun clearTaskImages(taskSyncId: String)
    suspend fun setSlotImage(
        parentType: String,
        parentSyncId: String,
        mimeType: String,
        name: String,
        localPath: String,
        syncId: String = ""
    ): Int
    suspend fun clearSlot(parentType: String, parentSyncId: String)
}

interface TrainingRepository {
    fun getByTeam(teamId: Int): kotlinx.coroutines.flow.Flow<List<com.luis.alhendinfc.domain.model.Training>>
    fun getById(id: Int): kotlinx.coroutines.flow.Flow<com.luis.alhendinfc.domain.model.Training?>
    fun getTasks(trainingId: Int): kotlinx.coroutines.flow.Flow<List<com.luis.alhendinfc.domain.model.TrainingTaskItem>>
    suspend fun canCreate(teamId: Int, epochDay: Long): Boolean
    suspend fun add(training: com.luis.alhendinfc.domain.model.Training): Int
    suspend fun update(training: com.luis.alhendinfc.domain.model.Training)
    suspend fun delete(training: com.luis.alhendinfc.domain.model.Training)
    suspend fun addTask(trainingId: Int, taskId: Int)
    suspend fun removeTask(trainingId: Int, taskId: Int)
    suspend fun moveTask(trainingId: Int, taskId: Int, up: Boolean)
}
