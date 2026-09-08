package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getByTeam(teamId: Int): Flow<List<Task>>
    fun searchByName(teamId: Int, query: String): Flow<List<Task>>
    fun getById(id: Int): Flow<Task?>
    suspend fun add(task: Task): Int
    suspend fun update(task: Task)
    suspend fun delete(task: Task)
}
