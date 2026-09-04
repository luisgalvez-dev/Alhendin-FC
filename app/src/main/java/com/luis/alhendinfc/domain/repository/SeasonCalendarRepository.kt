package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.OpponentClubDao
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.SampleSeasonCalendar
import com.luis.alhendinfc.domain.model.SeasonFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SeasonCalendarRepository(
    private val clubDao: OpponentClubDao,
    private val fixtureDao: SeasonFixtureDao
) {

    fun getClubs(teamId: Int): Flow<List<OpponentClub>> =
        clubDao.getByTeam(teamId).map { list -> list.map { it.toDomain() } }

    fun getFixtureRows(teamId: Int): Flow<List<FixtureRow>> =
        combine(
            fixtureDao.getByTeam(teamId),
            clubDao.getByTeam(teamId)
        ) { fixtures, clubs ->
            val byId = clubs.associateBy { it.id }
            fixtures.map { f ->
                FixtureRow(fixture = f.toDomain(), club = byId[f.opponentClubId]?.toDomain())
            }
        }

    suspend fun getFixtureForMatchday(teamId: Int, matchday: Int): FixtureRow? {
        val f = fixtureDao.getByMatchday(teamId, matchday) ?: return null
        val club = clubDao.getById(f.opponentClubId)?.toDomain()
        return FixtureRow(f.toDomain(), club)
    }

    suspend fun addClub(club: OpponentClub): Int =
        clubDao.insert(club.toEntity()).toInt()

    suspend fun updateClub(club: OpponentClub) =
        clubDao.update(club.toEntity())

    suspend fun deleteClub(club: OpponentClub) =
        clubDao.delete(club.toEntity())

    suspend fun upsertFixture(fixture: SeasonFixture): Int =
        fixtureDao.upsert(fixture.toEntity()).toInt()

    suspend fun deleteFixture(fixture: SeasonFixture) =
        fixtureDao.delete(fixture.toEntity())

    suspend fun ensureSampleCalendar(teamId: Int): Boolean {
        if (teamId <= 0) return false
        if (clubDao.countByTeam(teamId) > 0) return false
        val seeds = SampleSeasonCalendar.createClubs(teamId)
        val ids = seeds.map { clubDao.insert(it.toEntity()).toInt() }
        val validIds = ids.filter { it > 0 }
        if (validIds.isEmpty()) return false
        fixtureDao.insertAll(
            SampleSeasonCalendar.createFixtures(teamId, validIds).map { it.toEntity() }
        )
        return true
    }

    private fun OpponentClubEntity.toDomain() = OpponentClub(
        id = id,
        teamId = teamId,
        name = name,
        shortName = shortName,
        stadium = stadium,
        shieldUri = shieldUri,
        sortOrder = sortOrder
    )

    private fun OpponentClub.toEntity() = OpponentClubEntity(
        id = id,
        teamId = teamId,
        name = name,
        shortName = shortName,
        stadium = stadium,
        shieldUri = shieldUri,
        sortOrder = sortOrder
    )

    private fun SeasonFixtureEntity.toDomain() = SeasonFixture(
        id = id,
        teamId = teamId,
        matchday = matchday,
        opponentClubId = opponentClubId,
        isHome = isHome,
        date = date,
        time = time,
        stadiumOverride = stadiumOverride
    )

    private fun SeasonFixture.toEntity() = SeasonFixtureEntity(
        id = id,
        teamId = teamId,
        matchday = matchday,
        opponentClubId = opponentClubId,
        isHome = isHome,
        date = date,
        time = time,
        stadiumOverride = stadiumOverride
    )
}
