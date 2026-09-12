package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getPlayersByTeam(teamId: Int): Flow<List<Player>>
    fun getPlayerById(id: Int): Flow<Player?>
    suspend fun getOnce(id: Int): Player?
    suspend fun addPlayer(player: Player): Int
    suspend fun updatePlayer(player: Player)
    suspend fun deletePlayer(player: Player)
}
