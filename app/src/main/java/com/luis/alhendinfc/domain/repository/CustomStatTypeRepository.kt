package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.CustomStatType
import kotlinx.coroutines.flow.Flow

interface CustomStatTypeRepository {
    fun getByTeam(teamId: Int): Flow<List<CustomStatType>>
    fun getActiveByTeam(teamId: Int): Flow<List<CustomStatType>>
    suspend fun add(type: CustomStatType): Int
    suspend fun update(type: CustomStatType)
    /** Soft-delete si hay eventos; hard-delete si no. */
    suspend fun deleteOrDeactivate(type: CustomStatType)
    /** Inserta tipos de prueba si el equipo no tiene ninguno. */
    suspend fun ensureSampleCustomStats(teamId: Int): Boolean
}
