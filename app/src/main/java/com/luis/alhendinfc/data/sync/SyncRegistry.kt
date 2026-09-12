package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.cloud.CloudDoc
import com.luis.alhendinfc.cloud.CloudMappers
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.BoardEntity
import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.OpponentPlayerEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.RivalAnalysisEntity
import com.luis.alhendinfc.data.local.RivalLinkEntity
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TaskEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.data.local.TrainingEntity
import com.luis.alhendinfc.data.local.TrainingTaskEntity
import com.luis.alhendinfc.domain.model.MatchStatus

enum class RemoteApplyResult { APPLIED, DEFERRED, IGNORED }

class SyncAdapter(
    val type: String,
    val listLocal: suspend () -> List<CloudDoc>,
    val readLocal: suspend (String) -> CloudDoc?,
    val applyRemote: suspend (CloudDoc) -> RemoteApplyResult,
    val shouldPush: suspend (CloudDoc) -> Boolean = { true }
)

class SyncRegistry(private val db: AlhendinDatabase) {

    val adapters: Map<String, SyncAdapter> = linkedMapOf(
        SyncEntityType.TEAM to teamAdapter(),
        SyncEntityType.PLAYER to playerAdapter(),
        SyncEntityType.OPPONENT_CLUB to opponentClubAdapter(),
        SyncEntityType.CUSTOM_STAT to customStatAdapter(),
        SyncEntityType.BOARD to boardAdapter(),
        SyncEntityType.TASK to taskAdapter(),
        SyncEntityType.MATCH to matchAdapter(),
        SyncEntityType.SEASON_FIXTURE to fixtureAdapter(),
        SyncEntityType.TRAINING to trainingAdapter(),
        SyncEntityType.MATCH_PLAYER to matchPlayerAdapter(),
        SyncEntityType.MATCH_EVENT to matchEventAdapter(),
        SyncEntityType.TRAINING_TASK to trainingTaskAdapter(),
        SyncEntityType.RIVAL_ANALYSIS to rivalAnalysisAdapter(),
        SyncEntityType.RIVAL_LINK to rivalLinkAdapter(),
        SyncEntityType.OPPONENT_PLAYER to opponentPlayerAdapter()
    )

    fun adapter(type: String): SyncAdapter? = adapters[type]

    suspend fun hasLocalSportsData(): Boolean {
        return db.teamDao().getAllOnce().isNotEmpty() ||
            db.playerDao().getAllOnce().isNotEmpty() ||
            db.opponentClubDao().getAllOnce().isNotEmpty() ||
            db.matchDao().getAllMatchesOnce().isNotEmpty() ||
            db.taskDao().getAllOnce().isNotEmpty() ||
            db.trainingDao().getAllOnce().isNotEmpty() ||
            db.boardDao().getAllOnce().isNotEmpty() ||
            db.seasonFixtureDao().getAllOnce().isNotEmpty() ||
            db.customStatTypeDao().getAllOnce().isNotEmpty()
    }

    private fun teamAdapter() = SyncAdapter(
        type = SyncEntityType.TEAM,
        listLocal = { db.teamDao().getAllOnce().filter { it.syncId.isNotBlank() }.map { CloudMappers.team(it) } },
        readLocal = readLocal@{ id -> db.teamDao().getBySyncIdIncludingDeleted(id)?.let { CloudMappers.team(it) } },
        applyRemote = applyRemote@{ doc ->
            val dao = db.teamDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.TEAM, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val remoteShield = CloudMappers.mergeLocalUri(existing?.shieldUri, doc.strOrNull("shieldUri"))
                    if (existing == null) {
                        dao.insertTeam(
                            TeamEntity(
                                name = doc.str("name"),
                                category = doc.str("category"),
                                season = doc.str("season"),
                                shieldUri = remoteShield,
                                isSelected = false,
                                syncId = doc.id,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.updateTeam(
                            existing.copy(
                                name = doc.str("name"),
                                category = doc.str("category"),
                                season = doc.str("season"),
                                shieldUri = remoteShield,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun playerAdapter() = SyncAdapter(
        type = SyncEntityType.PLAYER,
        listLocal = {
            db.playerDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                CloudMappers.player(row, team.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.playerDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            CloudMappers.player(row, team.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val teamSyncId = doc.str("teamSyncId")
            val team = db.teamDao().getBySyncIdIncludingDeleted(teamSyncId) ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.playerDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.PLAYER, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val photo = existing?.photoUri
                    if (existing == null) {
                        dao.insert(
                            PlayerEntity(
                                teamId = team.id,
                                name = doc.str("name"),
                                alias = doc.str("alias"),
                                position = doc.str("position").ifBlank { "MEDIOCENTRO_DEFENSIVO" },
                                jerseyNumber = doc.int("jerseyNumber"),
                                photoUri = photo,
                                height = doc.int("height"),
                                weight = doc.int("weight"),
                                laterality = doc.str("laterality").ifBlank { "DERECHA" },
                                isActive = doc.bool("isActive", true),
                                observations = doc.str("observations"),
                                syncId = doc.id,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                name = doc.str("name"),
                                alias = doc.str("alias"),
                                position = doc.str("position").ifBlank { existing.position },
                                jerseyNumber = doc.int("jerseyNumber"),
                                photoUri = photo,
                                height = doc.int("height"),
                                weight = doc.int("weight"),
                                laterality = doc.str("laterality").ifBlank { existing.laterality },
                                isActive = doc.bool("isActive", existing.isActive),
                                observations = doc.str("observations"),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun opponentClubAdapter() = SyncAdapter(
        type = SyncEntityType.OPPONENT_CLUB,
        listLocal = {
            db.opponentClubDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                CloudMappers.opponentClub(row, team.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.opponentClubDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            CloudMappers.opponentClub(row, team.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.opponentClubDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.OPPONENT_CLUB, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val shield = CloudMappers.mergeLocalUri(existing?.shieldUri, doc.strOrNull("shieldUri"))
                    if (existing == null) {
                        dao.insert(
                            OpponentClubEntity(
                                teamId = team.id,
                                name = doc.str("name"),
                                shortName = doc.str("shortName"),
                                stadium = doc.str("stadium"),
                                shieldUri = shield,
                                kitColors = doc.str("kitColors"),
                                sortOrder = doc.int("sortOrder"),
                                syncId = doc.id,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                name = doc.str("name"),
                                shortName = doc.str("shortName"),
                                stadium = doc.str("stadium"),
                                shieldUri = shield,
                                kitColors = doc.str("kitColors"),
                                sortOrder = doc.int("sortOrder"),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun customStatAdapter() = SyncAdapter(
        type = SyncEntityType.CUSTOM_STAT,
        listLocal = {
            db.customStatTypeDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                CloudMappers.customStat(row, team.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.customStatTypeDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            CloudMappers.customStat(row, team.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.customStatTypeDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.CUSTOM_STAT, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            CustomStatTypeEntity(
                                teamId = team.id,
                                code = doc.str("code"),
                                label = doc.str("label"),
                                shortLabel = doc.str("shortLabel"),
                                appliesTo = doc.str("appliesTo").ifBlank { "ALL" },
                                sortOrder = doc.int("sortOrder"),
                                isActive = doc.bool("isActive", true),
                                createdAt = doc.createdAt,
                                syncId = doc.id,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                code = doc.str("code"),
                                label = doc.str("label"),
                                shortLabel = doc.str("shortLabel"),
                                appliesTo = doc.str("appliesTo").ifBlank { existing.appliesTo },
                                sortOrder = doc.int("sortOrder"),
                                isActive = doc.bool("isActive", existing.isActive),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun boardAdapter() = SyncAdapter(
        type = SyncEntityType.BOARD,
        listLocal = {
            db.boardDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                CloudMappers.board(row, team.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.boardDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            CloudMappers.board(row, team.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.boardDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.BOARD, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val scene = BoardCloudSanitizer.portableSceneJson(doc.str("sceneJson"))
                    if (existing == null) {
                        dao.insert(
                            BoardEntity(
                                syncId = doc.id,
                                teamId = team.id,
                                name = doc.str("name"),
                                sceneVersion = doc.int("sceneVersion", 1),
                                sceneJson = scene,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                name = doc.str("name"),
                                sceneVersion = doc.int("sceneVersion", existing.sceneVersion),
                                sceneJson = scene,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun taskAdapter() = SyncAdapter(
        type = SyncEntityType.TASK,
        listLocal = {
            db.taskDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                CloudMappers.task(row, team.syncId, row.boardSyncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.taskDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            CloudMappers.task(row, team.syncId, row.boardSyncId)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.taskDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.TASK, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val boardSyncId = doc.strOrNull("boardSyncId")
                    if (!boardSyncId.isNullOrBlank()) {
                        db.boardDao().getBySyncIdIncludingDeleted(boardSyncId)
                            ?: return@applyRemote RemoteApplyResult.DEFERRED
                    }
                    if (existing == null) {
                        dao.insert(
                            TaskEntity(
                                syncId = doc.id,
                                teamId = team.id,
                                name = doc.str("name"),
                                objective = doc.str("objective"),
                                playerCount = doc.intOrNull("playerCount"),
                                durationMinutes = doc.intOrNull("durationMinutes"),
                                description = doc.str("description"),
                                boardSyncId = boardSyncId,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                name = doc.str("name"),
                                objective = doc.str("objective"),
                                playerCount = doc.intOrNull("playerCount"),
                                durationMinutes = doc.intOrNull("durationMinutes"),
                                description = doc.str("description"),
                                boardSyncId = boardSyncId,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun matchAdapter() = SyncAdapter(
        type = SyncEntityType.MATCH,
        listLocal = {
            db.matchDao().getAllMatchesOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                val clubSync = row.opponentClubId?.let {
                    db.opponentClubDao().getByIdIncludingDeleted(it)?.syncId
                }
                CloudMappers.match(row, team.syncId, clubSync)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.matchDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            val clubSync = row.opponentClubId?.let {
                db.opponentClubDao().getByIdIncludingDeleted(it)?.syncId
            }
            CloudMappers.match(row, team.syncId, clubSync)
        },
        shouldPush = { doc -> LiveMatchGuard.shouldPushMatch(doc.str("status")) },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val clubSync = doc.strOrNull("opponentClubSyncId")
            val club = if (clubSync.isNullOrBlank()) {
                null
            } else {
                db.opponentClubDao().getBySyncIdIncludingDeleted(clubSync)
                    ?: return@applyRemote RemoteApplyResult.DEFERRED
            }
            val dao = db.matchDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            if (existing != null && LiveMatchGuard.deferRemoteMatch(existing.status)) {
                return@applyRemote RemoteApplyResult.DEFERRED
            }
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.MATCH, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val status = doc.str("status").ifBlank { MatchStatus.OPEN.name }
                    val fieldSeconds = if (status == MatchStatus.FINISHED.name) {
                        doc.str("fieldSecondsJson")
                    } else {
                        existing?.fieldSecondsJson.orEmpty()
                    }
                    val shield = CloudMappers.mergeLocalUri(existing?.rivalShieldUri, doc.strOrNull("rivalShieldUri"))
                    if (existing == null) {
                        dao.insertMatch(
                            MatchEntity(
                                teamId = team.id,
                                rival = doc.str("rival"),
                                stadium = doc.str("stadium"),
                                date = doc.str("date"),
                                time = doc.str("time"),
                                matchday = doc.int("matchday", 1),
                                isHome = doc.bool("isHome", true),
                                durationPerPart = doc.int("durationPerPart", 45),
                                numParts = doc.int("numParts", 2),
                                formation = doc.str("formation"),
                                notes = doc.str("notes"),
                                status = status,
                                homeScore = doc.intOrNull("homeScore"),
                                awayScore = doc.intOrNull("awayScore"),
                                opponentClubId = club?.id,
                                rivalShieldUri = shield,
                                fieldSecondsJson = fieldSeconds,
                                fieldPositionsJson = "",
                                syncId = doc.id,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt,
                                dateEpochDay = doc.longOrNull("dateEpochDay")
                            )
                        )
                    } else {
                        dao.updateMatch(
                            existing.copy(
                                teamId = team.id,
                                rival = doc.str("rival"),
                                stadium = doc.str("stadium"),
                                date = doc.str("date"),
                                time = doc.str("time"),
                                matchday = doc.int("matchday", existing.matchday),
                                isHome = doc.bool("isHome", existing.isHome),
                                durationPerPart = doc.int("durationPerPart", existing.durationPerPart),
                                numParts = doc.int("numParts", existing.numParts),
                                formation = doc.str("formation"),
                                notes = doc.str("notes"),
                                status = status,
                                homeScore = doc.intOrNull("homeScore"),
                                awayScore = doc.intOrNull("awayScore"),
                                opponentClubId = club?.id,
                                rivalShieldUri = shield,
                                fieldSecondsJson = fieldSeconds,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt,
                                dateEpochDay = doc.longOrNull("dateEpochDay") ?: existing.dateEpochDay
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun fixtureAdapter() = SyncAdapter(
        type = SyncEntityType.SEASON_FIXTURE,
        listLocal = {
            db.seasonFixtureDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@mapNotNull null
                CloudMappers.seasonFixture(row, team.syncId, club.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.seasonFixtureDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@readLocal null
            CloudMappers.seasonFixture(row, team.syncId, club.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val club = db.opponentClubDao().getBySyncIdIncludingDeleted(doc.str("opponentClubSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.seasonFixtureDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.SEASON_FIXTURE, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            SeasonFixtureEntity(
                                teamId = team.id,
                                matchday = doc.int("matchday"),
                                opponentClubId = club.id,
                                isHome = doc.bool("isHome", true),
                                date = doc.str("date"),
                                time = doc.str("time"),
                                stadiumOverride = doc.str("stadiumOverride"),
                                syncId = doc.id,
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt,
                                dateEpochDay = doc.longOrNull("dateEpochDay")
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                matchday = doc.int("matchday", existing.matchday),
                                opponentClubId = club.id,
                                isHome = doc.bool("isHome", existing.isHome),
                                date = doc.str("date"),
                                time = doc.str("time"),
                                stadiumOverride = doc.str("stadiumOverride"),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt,
                                dateEpochDay = doc.longOrNull("dateEpochDay") ?: existing.dateEpochDay
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun trainingAdapter() = SyncAdapter(
        type = SyncEntityType.TRAINING,
        listLocal = {
            db.trainingDao().getAllOnce().mapNotNull { row ->
                val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@mapNotNull null
                val clubSync = row.opponentClubId?.let {
                    db.opponentClubDao().getByIdIncludingDeleted(it)?.syncId
                }
                CloudMappers.training(row, team.syncId, clubSync)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.trainingDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val team = db.teamDao().getByIdIncludingDeleted(row.teamId) ?: return@readLocal null
            val clubSync = row.opponentClubId?.let {
                db.opponentClubDao().getByIdIncludingDeleted(it)?.syncId
            }
            CloudMappers.training(row, team.syncId, clubSync)
        },
        applyRemote = applyRemote@{ doc ->
            val team = db.teamDao().getBySyncIdIncludingDeleted(doc.str("teamSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val clubSync = doc.strOrNull("opponentClubSyncId")
            val club = if (clubSync.isNullOrBlank()) {
                null
            } else {
                db.opponentClubDao().getBySyncIdIncludingDeleted(clubSync)
                    ?: return@applyRemote RemoteApplyResult.DEFERRED
            }
            val dao = db.trainingDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.TRAINING, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            TrainingEntity(
                                syncId = doc.id,
                                teamId = team.id,
                                date = doc.str("date"),
                                dateEpochDay = doc.long("dateEpochDay"),
                                opponentClubId = club?.id,
                                notes = doc.str("notes"),
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                teamId = team.id,
                                date = doc.str("date"),
                                dateEpochDay = doc.long("dateEpochDay", existing.dateEpochDay),
                                opponentClubId = club?.id,
                                notes = doc.str("notes"),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun matchPlayerAdapter() = SyncAdapter(
        type = SyncEntityType.MATCH_PLAYER,
        listLocal = {
            db.matchDao().getAllMatchPlayersOnce().mapNotNull { row ->
                val match = db.matchDao().getByIdOnce(row.matchId)
                    ?: db.matchDao().getAllMatchesOnce().firstOrNull { it.id == row.matchId }
                    ?: return@mapNotNull null
                val player = db.playerDao().getByIdIncludingDeleted(row.playerId) ?: return@mapNotNull null
                CloudMappers.matchPlayer(row, match.syncId, player.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.matchDao().getMatchPlayerBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val match = db.matchDao().getAllMatchesOnce().firstOrNull { it.id == row.matchId } ?: return@readLocal null
            val player = db.playerDao().getByIdIncludingDeleted(row.playerId) ?: return@readLocal null
            CloudMappers.matchPlayer(row, match.syncId, player.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val match = db.matchDao().getBySyncIdIncludingDeleted(doc.str("matchSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val player = db.playerDao().getBySyncIdIncludingDeleted(doc.str("playerSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.matchDao()
            val existing = dao.getMatchPlayerBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.MATCH_PLAYER, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val onField = existing?.isOnField ?: false
                    if (existing == null) {
                        dao.insertMatchPlayers(
                            listOf(
                                MatchPlayerEntity(
                                    matchId = match.id,
                                    playerId = player.id,
                                    callupStatus = doc.str("callupStatus").ifBlank { "NONE" },
                                    isOnField = onField,
                                    syncId = doc.id,
                                    createdAt = doc.createdAt,
                                    updatedAt = doc.updatedAt,
                                    deletedAt = doc.deletedAt
                                )
                            )
                        )
                    } else {
                        dao.upsertMatchPlayerPreservingIdentity(
                            matchId = match.id,
                            playerId = player.id,
                            callupStatus = doc.str("callupStatus").ifBlank { existing.callupStatus },
                            isOnField = existing.isOnField,
                            syncId = existing.syncId,
                            createdAt = existing.createdAt,
                            updatedAt = doc.updatedAt,
                            deletedAt = doc.deletedAt
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun matchEventAdapter() = SyncAdapter(
        type = SyncEntityType.MATCH_EVENT,
        listLocal = {
            db.matchEventDao().getAllOnce().mapNotNull { row ->
                val match = db.matchDao().getAllMatchesOnce().firstOrNull { it.id == row.matchId }
                    ?: return@mapNotNull null
                val playerSync = row.playerId?.let { db.playerDao().getByIdIncludingDeleted(it)?.syncId }
                val relatedSync = row.relatedPlayerId?.let { db.playerDao().getByIdIncludingDeleted(it)?.syncId }
                CloudMappers.matchEvent(row, match.syncId, playerSync, relatedSync)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.matchEventDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val match = db.matchDao().getAllMatchesOnce().firstOrNull { it.id == row.matchId } ?: return@readLocal null
            val playerSync = row.playerId?.let { db.playerDao().getByIdIncludingDeleted(it)?.syncId }
            val relatedSync = row.relatedPlayerId?.let { db.playerDao().getByIdIncludingDeleted(it)?.syncId }
            CloudMappers.matchEvent(row, match.syncId, playerSync, relatedSync)
        },
        shouldPush = { doc ->
            val match = db.matchDao().getBySyncIdIncludingDeleted(doc.str("matchSyncId"))
            LiveMatchGuard.shouldPushMatchEvent(match?.status)
        },
        applyRemote = applyRemote@{ doc ->
            val match = db.matchDao().getBySyncIdIncludingDeleted(doc.str("matchSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            if (LiveMatchGuard.deferRemoteMatch(match.status)) {
                return@applyRemote RemoteApplyResult.DEFERRED
            }
            val playerSync = doc.strOrNull("playerSyncId")
            val player = if (playerSync.isNullOrBlank()) {
                null
            } else {
                db.playerDao().getBySyncIdIncludingDeleted(playerSync)
                    ?: return@applyRemote RemoteApplyResult.DEFERRED
            }
            val relatedSync = doc.strOrNull("relatedPlayerSyncId")
            val related = if (relatedSync.isNullOrBlank()) {
                null
            } else {
                db.playerDao().getBySyncIdIncludingDeleted(relatedSync)
                    ?: return@applyRemote RemoteApplyResult.DEFERRED
            }
            val dao = db.matchEventDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.MATCH_EVENT, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            MatchEventEntity(
                                matchId = match.id,
                                typeCode = doc.str("typeCode"),
                                playerId = player?.id,
                                relatedPlayerId = related?.id,
                                minute = doc.int("minute"),
                                period = doc.int("period", 1),
                                value = doc.int("value", 1),
                                createdAt = doc.createdAt,
                                syncId = doc.id,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        db.matchEventDao().update(
                            existing.copy(
                                matchId = match.id,
                                typeCode = doc.str("typeCode"),
                                playerId = player?.id,
                                relatedPlayerId = related?.id,
                                minute = doc.int("minute"),
                                period = doc.int("period", existing.period),
                                value = doc.int("value", existing.value),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun trainingTaskAdapter() = SyncAdapter(
        type = SyncEntityType.TRAINING_TASK,
        listLocal = {
            db.trainingTaskDao().getAllOnce().mapNotNull { row ->
                val training = db.trainingDao().getByIdIncludingDeleted(row.trainingId) ?: return@mapNotNull null
                val task = db.taskDao().getByIdIncludingDeleted(row.taskId) ?: return@mapNotNull null
                CloudMappers.trainingTask(row, training.syncId, task.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.trainingTaskDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val training = db.trainingDao().getByIdIncludingDeleted(row.trainingId) ?: return@readLocal null
            val task = db.taskDao().getByIdIncludingDeleted(row.taskId) ?: return@readLocal null
            CloudMappers.trainingTask(row, training.syncId, task.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val training = db.trainingDao().getBySyncIdIncludingDeleted(doc.str("trainingSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val task = db.taskDao().getBySyncIdIncludingDeleted(doc.str("taskSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.trainingTaskDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.TRAINING_TASK, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            TrainingTaskEntity(
                                syncId = doc.id,
                                trainingId = training.id,
                                taskId = task.id,
                                sortOrder = doc.int("sortOrder"),
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                trainingId = training.id,
                                taskId = task.id,
                                sortOrder = doc.int("sortOrder", existing.sortOrder),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun rivalAnalysisAdapter() = SyncAdapter(
        type = SyncEntityType.RIVAL_ANALYSIS,
        listLocal = {
            db.rivalAnalysisDao().getAllOnce().mapNotNull { row ->
                val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@mapNotNull null
                CloudMappers.rivalAnalysis(row, club.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.rivalAnalysisDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@readLocal null
            CloudMappers.rivalAnalysis(row, club.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val club = db.opponentClubDao().getBySyncIdIncludingDeleted(doc.str("opponentClubSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.rivalAnalysisDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.RIVAL_ANALYSIS, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    val entity = RivalAnalysisEntity(
                        id = existing?.id ?: 0,
                        syncId = existing?.syncId ?: doc.id,
                        opponentClubId = club.id,
                        usualSystem = doc.str("usualSystem"),
                        variants = doc.str("variants"),
                        buildUp = doc.str("buildUp"),
                        progression = doc.str("progression"),
                        finalThird = doc.str("finalThird"),
                        highPress = doc.str("highPress"),
                        midBlock = doc.str("midBlock"),
                        lowBlock = doc.str("lowBlock"),
                        transAttackToDefense = doc.str("transAttackToDefense"),
                        transDefenseToAttack = doc.str("transDefenseToAttack"),
                        cornersOffensive = doc.str("cornersOffensive"),
                        cornersDefensive = doc.str("cornersDefensive"),
                        setPieces = doc.str("setPieces"),
                        strengths = doc.str("strengths"),
                        weaknesses = doc.str("weaknesses"),
                        keyPlayers = doc.str("keyPlayers"),
                        generalNotes = doc.str("generalNotes"),
                        createdAt = existing?.createdAt ?: doc.createdAt,
                        updatedAt = doc.updatedAt,
                        deletedAt = doc.deletedAt
                    )
                    if (existing == null) dao.insert(entity.copy(id = 0, syncId = doc.id, createdAt = doc.createdAt))
                    else dao.update(entity)
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun rivalLinkAdapter() = SyncAdapter(
        type = SyncEntityType.RIVAL_LINK,
        listLocal = {
            db.rivalLinkDao().getAllOnce().mapNotNull { row ->
                val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@mapNotNull null
                CloudMappers.rivalLink(row, club.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.rivalLinkDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@readLocal null
            CloudMappers.rivalLink(row, club.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val club = db.opponentClubDao().getBySyncIdIncludingDeleted(doc.str("opponentClubSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.rivalLinkDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.RIVAL_LINK, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            RivalLinkEntity(
                                syncId = doc.id,
                                opponentClubId = club.id,
                                type = doc.str("type"),
                                label = doc.str("label"),
                                url = doc.str("url"),
                                sortOrder = doc.int("sortOrder"),
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                opponentClubId = club.id,
                                type = doc.str("type"),
                                label = doc.str("label"),
                                url = doc.str("url"),
                                sortOrder = doc.int("sortOrder", existing.sortOrder),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun opponentPlayerAdapter() = SyncAdapter(
        type = SyncEntityType.OPPONENT_PLAYER,
        listLocal = {
            db.opponentPlayerDao().getAllOnce().mapNotNull { row ->
                val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@mapNotNull null
                CloudMappers.opponentPlayer(row, club.syncId)
            }
        },
        readLocal = readLocal@{ id ->
            val row = db.opponentPlayerDao().getBySyncIdIncludingDeleted(id) ?: return@readLocal null
            val club = db.opponentClubDao().getByIdIncludingDeleted(row.opponentClubId) ?: return@readLocal null
            CloudMappers.opponentPlayer(row, club.syncId)
        },
        applyRemote = applyRemote@{ doc ->
            val club = db.opponentClubDao().getBySyncIdIncludingDeleted(doc.str("opponentClubSyncId"))
                ?: return@applyRemote RemoteApplyResult.DEFERRED
            val dao = db.opponentPlayerDao()
            val existing = dao.getBySyncIdIncludingDeleted(doc.id)
            when (lww(existing?.updatedAt ?: 0L, existing?.deletedAt, doc)) {
                LwwDecision.NOOP -> RemoteApplyResult.IGNORED
                LwwDecision.KEEP_LOCAL_AND_PUSH -> {
                    existing?.syncId?.let { SyncHooks.enqueue(SyncEntityType.OPPONENT_PLAYER, it) }
                    RemoteApplyResult.IGNORED
                }
                LwwDecision.APPLY_REMOTE -> {
                    if (existing == null) {
                        dao.insert(
                            OpponentPlayerEntity(
                                syncId = doc.id,
                                opponentClubId = club.id,
                                name = doc.str("name"),
                                createdAt = doc.createdAt,
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    } else {
                        dao.update(
                            existing.copy(
                                opponentClubId = club.id,
                                name = doc.str("name"),
                                updatedAt = doc.updatedAt,
                                deletedAt = doc.deletedAt
                            )
                        )
                    }
                    RemoteApplyResult.APPLIED
                }
            }
        }
    )

    private fun lww(localUpdatedAt: Long, localDeletedAt: Long?, doc: CloudDoc): LwwDecision {
        if (localUpdatedAt == 0L && localDeletedAt == null) {
            return LwwDecision.APPLY_REMOTE
        }
        return LastWriteWins.decide(localUpdatedAt, localDeletedAt, doc.updatedAt, doc.deletedAt)
    }
}
