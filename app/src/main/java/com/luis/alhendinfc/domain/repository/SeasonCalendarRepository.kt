package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.OpponentClubDao
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.domain.model.FixtureRow
import com.luis.alhendinfc.domain.model.OpponentClub
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

    suspend fun addClub(club: OpponentClub): Int {
        val now = EntitySync.now()
        val existing = clubDao.getByTeamAndNameIncludingDeleted(club.teamId, club.name.trim())
        if (existing != null && existing.deletedAt != null) {
            clubDao.update(EntityWrites.clubForRevive(existing, club.toEntity(), now))
            return existing.id
        }
        return clubDao.insert(EntityWrites.clubForInsert(club.toEntity(), now)).toInt()
    }

    suspend fun updateClub(club: OpponentClub) {
        val existing = clubDao.getById(club.id) ?: return
        clubDao.update(EntityWrites.clubForUpdate(existing, club.toEntity(), EntitySync.now()))
    }

    suspend fun deleteClub(club: OpponentClub) {
        clubDao.markDeleted(club.id, EntitySync.now())
    }

    suspend fun upsertFixture(fixture: SeasonFixture): Int {
        val now = EntitySync.now()
        if (fixture.id <= 0) {
            val tombstone = fixtureDao.getByMatchdayIncludingDeleted(fixture.teamId, fixture.matchday)
            if (tombstone != null && tombstone.deletedAt != null) {
                fixtureDao.update(EntityWrites.fixtureForRevive(tombstone, fixture.toEntity(), now))
                return tombstone.id
            }
            return fixtureDao.insert(EntityWrites.fixtureForInsert(fixture.toEntity(), now)).toInt()
        }
        val existing = fixtureDao.getByIdOnce(fixture.id) ?: return 0
        fixtureDao.update(EntityWrites.fixtureForUpdate(existing, fixture.toEntity(), now))
        return existing.id
    }

    suspend fun deleteFixture(fixture: SeasonFixture) {
        fixtureDao.markDeleted(fixture.id, EntitySync.now())
    }

    private fun OpponentClubEntity.toDomain() = OpponentClub(
        id = id,
        teamId = teamId,
        name = name,
        shortName = shortName,
        stadium = stadium,
        shieldUri = shieldUri,
        kitColors = kitColors,
        sortOrder = sortOrder
    )

    private fun OpponentClub.toEntity() = OpponentClubEntity(
        id = id,
        teamId = teamId,
        name = name,
        shortName = shortName,
        stadium = stadium,
        shieldUri = shieldUri,
        kitColors = kitColors,
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
