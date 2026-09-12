package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.AttachmentEntity
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupCounts(
    val teams: Int,
    val players: Int,
    val matches: Int,
    val matchPlayers: Int,
    val events: Int,
    val customStatTypes: Int,
    val opponentClubs: Int,
    val fixtures: Int,
    val tasks: Int = 0,
    val trainings: Int = 0,
    val trainingTasks: Int = 0,
    val attachments: Int = 0,
    val rivalAnalyses: Int = 0,
    val rivalLinks: Int = 0,
    val opponentPlayers: Int = 0,
    val boards: Int = 0
)

data class ValidatedBackup(
    val schemaVersion: Int,
    val teams: List<TeamEntity>,
    val players: List<PlayerEntity>,
    val matches: List<MatchEntity>,
    val matchPlayers: List<MatchPlayerEntity>,
    val events: List<MatchEventEntity>,
    val customStatTypes: List<CustomStatTypeEntity>,
    val opponentClubs: List<OpponentClubEntity>,
    val fixtures: List<SeasonFixtureEntity>,
    val tasks: List<TaskEntity> = emptyList(),
    val trainings: List<TrainingEntity> = emptyList(),
    val trainingTasks: List<TrainingTaskEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val rivalAnalyses: List<RivalAnalysisEntity> = emptyList(),
    val rivalLinks: List<RivalLinkEntity> = emptyList(),
    val opponentPlayers: List<OpponentPlayerEntity> = emptyList(),
    val boards: List<BoardEntity> = emptyList(),
    val homeLayout: String?,
    val counts: BackupCounts
) {
    fun withResolvedUris(resolve: (String?) -> String?) = copy(
        teams = teams.map { it.copy(shieldUri = resolve(it.shieldUri)) },
        players = players.map { it.copy(photoUri = resolve(it.photoUri)) },
        matches = matches.map { it.copy(rivalShieldUri = resolve(it.rivalShieldUri)) },
        opponentClubs = opponentClubs.map { it.copy(shieldUri = resolve(it.shieldUri)) }
    )
}

object BackupValidator {

    private val REQUIRED_ARRAYS = listOf(
        "teams",
        "players",
        "matches",
        "matchPlayers",
        "events",
        "customStatTypes",
        "opponentClubs",
        "fixtures"
    )

    fun validateJson(json: String): ValidatedBackup {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("backup.json no es un JSON válido", e)
        }

        if (!root.has("schemaVersion") || root.isNull("schemaVersion")) {
            throw IllegalArgumentException("El backup no indica schemaVersion")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion !in 14..21) {
            throw IllegalArgumentException(
                "schemaVersion incompatible: $schemaVersion (se aceptan 14, 15, 16, 17, 18, 19, 20 o 21)"
            )
        }

        REQUIRED_ARRAYS.forEach { key ->
            if (!root.has(key) || root.isNull(key)) {
                throw IllegalArgumentException("El backup no contiene el array obligatorio '$key'")
            }
            try {
                root.getJSONArray(key)
            } catch (e: JSONException) {
                throw IllegalArgumentException("El campo '$key' no es un array JSON", e)
            }
        }

        if (schemaVersion >= 16) {
            requireJsonArray(root, "tasks")
        }
        if (schemaVersion >= 17) {
            requireJsonArray(root, "trainings")
            requireJsonArray(root, "trainingTasks")
            requireJsonArray(root, "attachments")
        }
        if (schemaVersion >= 18) {
            requireJsonArray(root, "rivalAnalyses")
            requireJsonArray(root, "rivalLinks")
        }
        if (schemaVersion >= 19) {
            requireJsonArray(root, "opponentPlayers")
        }
        if (schemaVersion >= 20) {
            requireJsonArray(root, "boards")
        }

        val requireSync = schemaVersion >= 15
        val teams = parseTeams(root.getJSONArray("teams"), requireSync)
        val players = parsePlayers(root.getJSONArray("players"), requireSync)
        val matches = parseMatches(root.getJSONArray("matches"), requireSync)
        val matchPlayers = parseMatchPlayers(root.getJSONArray("matchPlayers"), requireSync)
        val events = parseEvents(root.getJSONArray("events"), requireSync)
        val customStats = parseCustomStats(root.getJSONArray("customStatTypes"), requireSync)
        val clubs = parseClubs(root.getJSONArray("opponentClubs"), requireSync)
        val fixtures = parseFixtures(root.getJSONArray("fixtures"), requireSync)
        val tasks = if (schemaVersion >= 16) {
            parseTasks(root.getJSONArray("tasks"), requireSync = true)
        } else {
            emptyList()
        }
        val trainings = if (schemaVersion >= 17) {
            parseTrainings(root.getJSONArray("trainings"), requireSync = true)
        } else {
            emptyList()
        }
        val trainingTasks = if (schemaVersion >= 17) {
            parseTrainingTasks(root.getJSONArray("trainingTasks"), requireSync = true)
        } else {
            emptyList()
        }
        val attachments = if (schemaVersion >= 17) {
            parseAttachments(root.getJSONArray("attachments"), requireSync = true)
        } else {
            emptyList()
        }
        val rivalAnalyses = if (schemaVersion >= 18) {
            parseRivalAnalyses(root.getJSONArray("rivalAnalyses"), requireSync = true)
        } else {
            emptyList()
        }
        val rivalLinks = if (schemaVersion >= 18) {
            parseRivalLinks(root.getJSONArray("rivalLinks"), requireSync = true)
        } else {
            emptyList()
        }
        val opponentPlayers = if (schemaVersion >= 19) {
            parseOpponentPlayers(root.getJSONArray("opponentPlayers"), requireSync = true)
        } else {
            emptyList()
        }
        val boards = if (schemaVersion >= 20) {
            parseBoards(root.getJSONArray("boards"), requireSync = true)
        } else {
            emptyList()
        }

        if (requireSync) {
            assertUniqueSyncIds("teams", teams.map { it.syncId })
            assertUniqueSyncIds("players", players.map { it.syncId })
            assertUniqueSyncIds("matches", matches.map { it.syncId })
            assertUniqueSyncIds("matchPlayers", matchPlayers.map { it.syncId })
            assertUniqueSyncIds("events", events.map { it.syncId })
            assertUniqueSyncIds("customStatTypes", customStats.map { it.syncId })
            assertUniqueSyncIds("opponentClubs", clubs.map { it.syncId })
            assertUniqueSyncIds("fixtures", fixtures.map { it.syncId })
            if (schemaVersion >= 16) {
                assertUniqueSyncIds("tasks", tasks.map { it.syncId })
            }
            if (schemaVersion >= 17) {
                assertUniqueSyncIds("trainings", trainings.map { it.syncId })
                assertUniqueSyncIds("trainingTasks", trainingTasks.map { it.syncId })
                assertUniqueSyncIds("attachments", attachments.map { it.syncId })
            }
            if (schemaVersion >= 18) {
                assertUniqueSyncIds("rivalAnalyses", rivalAnalyses.map { it.syncId })
                assertUniqueSyncIds("rivalLinks", rivalLinks.map { it.syncId })
            }
            if (schemaVersion >= 19) {
                assertUniqueSyncIds("opponentPlayers", opponentPlayers.map { it.syncId })
            }
            if (schemaVersion >= 20) {
                assertUniqueSyncIds("boards", boards.map { it.syncId })
            }
        }

        val parsed = BackupCounts(
            teams = teams.size,
            players = players.size,
            matches = matches.size,
            matchPlayers = matchPlayers.size,
            events = events.size,
            customStatTypes = customStats.size,
            opponentClubs = clubs.size,
            fixtures = fixtures.size,
            tasks = tasks.size,
            trainings = trainings.size,
            trainingTasks = trainingTasks.size,
            attachments = attachments.size,
            rivalAnalyses = rivalAnalyses.size,
            rivalLinks = rivalLinks.size,
            opponentPlayers = opponentPlayers.size,
            boards = boards.size
        )

        if (root.has("counts") && !root.isNull("counts")) {
            val declared = parseDeclaredCounts(root.getJSONObject("counts"))
            if (declared != parsed) {
                throw IllegalArgumentException(
                    "Los conteos del backup no coinciden con los arrays (declarado=$declared, real=$parsed)"
                )
            }
        }

        val homeLayout = if (root.has("homeLayout") && !root.isNull("homeLayout")) {
            root.getString("homeLayout")
        } else {
            null
        }

        val payload = ValidatedBackup(
            schemaVersion = schemaVersion,
            teams = teams,
            players = players,
            matches = matches,
            matchPlayers = matchPlayers,
            events = events,
            customStatTypes = customStats,
            opponentClubs = clubs,
            fixtures = fixtures,
            tasks = tasks,
            trainings = trainings,
            trainingTasks = trainingTasks,
            attachments = attachments,
            rivalAnalyses = rivalAnalyses,
            rivalLinks = rivalLinks,
            opponentPlayers = opponentPlayers,
            boards = boards,
            homeLayout = homeLayout,
            counts = parsed
        )
        return when (schemaVersion) {
            14 -> BackupUpgrade.toV21(BackupUpgrade.toV20(BackupUpgrade.toV19(BackupUpgrade.toV18(BackupUpgrade.toV17(BackupUpgrade.toV16(BackupUpgrade.toV15(payload)))))))
            15 -> BackupUpgrade.toV21(BackupUpgrade.toV20(BackupUpgrade.toV19(BackupUpgrade.toV18(BackupUpgrade.toV17(BackupUpgrade.toV16(payload))))))
            16 -> BackupUpgrade.toV21(BackupUpgrade.toV20(BackupUpgrade.toV19(BackupUpgrade.toV18(BackupUpgrade.toV17(payload)))))
            17 -> BackupUpgrade.toV21(BackupUpgrade.toV20(BackupUpgrade.toV19(BackupUpgrade.toV18(payload))))
            18 -> BackupUpgrade.toV21(BackupUpgrade.toV20(BackupUpgrade.toV19(payload)))
            19 -> BackupUpgrade.toV21(BackupUpgrade.toV20(payload))
            20 -> BackupUpgrade.toV21(payload)
            else -> payload
        }
    }

    private fun parseDeclaredCounts(o: JSONObject) = BackupCounts(
        teams = o.optInt("teams"),
        players = o.optInt("players"),
        matches = o.optInt("matches"),
        matchPlayers = o.optInt("matchPlayers"),
        events = o.optInt("events"),
        customStatTypes = o.optInt("customStatTypes"),
        opponentClubs = o.optInt("opponentClubs"),
        fixtures = o.optInt("fixtures"),
        tasks = o.optInt("tasks"),
        trainings = o.optInt("trainings"),
        trainingTasks = o.optInt("trainingTasks"),
        attachments = o.optInt("attachments"),
        rivalAnalyses = o.optInt("rivalAnalyses"),
        rivalLinks = o.optInt("rivalLinks"),
        opponentPlayers = o.optInt("opponentPlayers"),
        boards = o.optInt("boards")
    )
}

internal fun requireJsonArray(root: JSONObject, key: String) {
    if (!root.has(key) || root.isNull(key)) {
        throw IllegalArgumentException("El backup no contiene el array obligatorio '$key'")
    }
    try {
        root.getJSONArray(key)
    } catch (e: JSONException) {
        throw IllegalArgumentException("El campo '$key' no es un array JSON", e)
    }
}

internal fun assertUniqueSyncIds(label: String, ids: List<String>) {
    ids.forEachIndexed { index, id ->
        if (id.isBlank()) {
            throw IllegalArgumentException("syncId vacío o ausente en $label[$index]")
        }
    }
    if (ids.size != ids.toSet().size) {
        throw IllegalArgumentException("syncId duplicado en $label")
    }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key)
    return value.takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.readSyncId(requireSync: Boolean): String {
    if (!requireSync) return optString("syncId")
    if (!has("syncId") || isNull("syncId")) {
        throw IllegalArgumentException("syncId ausente")
    }
    return getString("syncId")
}

private fun parseTeams(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            TeamEntity(
                id = o.getInt("id"),
                name = o.optString("name"),
                category = o.optString("category"),
                season = o.optString("season"),
                shieldUri = o.optNullableString("shieldUri"),
                isSelected = o.optBoolean("isSelected", false),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parsePlayers(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            PlayerEntity(
                id = o.getInt("id"),
                teamId = o.getInt("teamId"),
                name = o.optString("name"),
                alias = o.optString("alias"),
                position = o.optString("position", "MEDIOCENTRO_DEFENSIVO"),
                jerseyNumber = o.optInt("jerseyNumber"),
                photoUri = o.optNullableString("photoUri"),
                height = o.optInt("height"),
                weight = o.optInt("weight"),
                laterality = o.optString("laterality", "DERECHA"),
                isActive = o.optBoolean("isActive", true),
                observations = o.optString("observations"),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseMatches(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            MatchEntity(
                id = o.getInt("id"),
                teamId = o.getInt("teamId"),
                rival = o.optString("rival"),
                stadium = o.optString("stadium"),
                date = o.optString("date"),
                time = o.optString("time"),
                matchday = o.optInt("matchday", 1),
                isHome = o.optBoolean("isHome", true),
                durationPerPart = o.optInt("durationPerPart", 45),
                numParts = o.optInt("numParts", 2),
                formation = o.optString("formation"),
                notes = o.optString("notes"),
                status = o.optString("status", "OPEN"),
                homeScore = o.optNullableInt("homeScore"),
                awayScore = o.optNullableInt("awayScore"),
                opponentClubId = o.optNullableInt("opponentClubId"),
                opponentClubSyncId = o.optNullableString("opponentClubSyncId"),
                rivalShieldUri = o.optNullableString("rivalShieldUri"),
                livePeriod = o.optInt("livePeriod", 1),
                liveElapsedSeconds = o.optInt("liveElapsedSeconds", 0),
                liveClockRunning = o.optBoolean("liveClockRunning", false),
                liveClockAnchorWallMs = o.optLong("liveClockAnchorWallMs", 0L),
                fieldSecondsJson = o.optString("fieldSecondsJson", ""),
                fieldPositionsJson = o.optString("fieldPositionsJson", ""),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt"),
                dateEpochDay = o.optNullableLong("dateEpochDay")
            )
        )
    }
}

private fun parseMatchPlayers(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            MatchPlayerEntity(
                id = o.getInt("id"),
                matchId = o.getInt("matchId"),
                playerId = o.getInt("playerId"),
                callupStatus = o.optString("callupStatus", "NONE"),
                isOnField = o.optBoolean("isOnField", false),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseEvents(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            MatchEventEntity(
                id = o.getInt("id"),
                matchId = o.getInt("matchId"),
                typeCode = o.optString("typeCode"),
                playerId = o.optNullableInt("playerId"),
                relatedPlayerId = o.optNullableInt("relatedPlayerId"),
                minute = o.optInt("minute"),
                period = o.optInt("period", 1),
                value = o.optInt("value", 1),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                syncId = o.readSyncId(requireSync),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseCustomStats(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            CustomStatTypeEntity(
                id = o.getInt("id"),
                teamId = o.getInt("teamId"),
                code = o.optString("code"),
                label = o.optString("label"),
                shortLabel = o.optString("shortLabel"),
                appliesTo = o.optString("appliesTo", "ALL"),
                sortOrder = o.optInt("sortOrder"),
                isActive = o.optBoolean("isActive", true),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                syncId = o.readSyncId(requireSync),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseClubs(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            OpponentClubEntity(
                id = o.getInt("id"),
                teamId = o.getInt("teamId"),
                name = o.optString("name"),
                shortName = o.optString("shortName"),
                stadium = o.optString("stadium"),
                shieldUri = o.optNullableString("shieldUri"),
                kitColors = o.optString("kitColors"),
                sortOrder = o.optInt("sortOrder"),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseFixtures(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            SeasonFixtureEntity(
                id = o.getInt("id"),
                teamId = o.getInt("teamId"),
                matchday = o.getInt("matchday"),
                opponentClubId = o.getInt("opponentClubId"),
                isHome = o.optBoolean("isHome", true),
                date = o.optString("date"),
                time = o.optString("time"),
                stadiumOverride = o.optString("stadiumOverride"),
                syncId = o.readSyncId(requireSync),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt"),
                dateEpochDay = o.optNullableLong("dateEpochDay")
            )
        )
    }
}

private fun parseTasks(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            TaskEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                teamId = o.getInt("teamId"),
                name = o.optString("name"),
                objective = o.optString("objective"),
                playerCount = o.optNullableInt("playerCount"),
                durationMinutes = o.optNullableInt("durationMinutes"),
                description = o.optString("description"),
                boardSyncId = o.optNullableString("boardSyncId"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseTrainings(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            TrainingEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                teamId = o.getInt("teamId"),
                date = o.optString("date"),
                dateEpochDay = o.optLong("dateEpochDay"),
                opponentClubId = o.optNullableInt("opponentClubId"),
                notes = o.optString("notes"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseTrainingTasks(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            TrainingTaskEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                trainingId = o.getInt("trainingId"),
                taskId = o.getInt("taskId"),
                sortOrder = o.optInt("sortOrder"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseAttachments(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            AttachmentEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                parentType = o.optString("parentType"),
                parentSyncId = o.optString("parentSyncId"),
                mimeType = o.optString("mimeType"),
                name = o.optString("name"),
                localPath = o.optNullableString("localPath"),
                remotePath = o.optNullableString("remotePath"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseRivalAnalyses(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            RivalAnalysisEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                opponentClubId = o.getInt("opponentClubId"),
                usualSystem = o.optString("usualSystem"),
                variants = o.optString("variants"),
                buildUp = o.optString("buildUp"),
                progression = o.optString("progression"),
                finalThird = o.optString("finalThird"),
                highPress = o.optString("highPress"),
                midBlock = o.optString("midBlock"),
                lowBlock = o.optString("lowBlock"),
                transAttackToDefense = o.optString("transAttackToDefense"),
                transDefenseToAttack = o.optString("transDefenseToAttack"),
                cornersOffensive = o.optString("cornersOffensive"),
                cornersDefensive = o.optString("cornersDefensive"),
                setPieces = o.optString("setPieces"),
                strengths = o.optString("strengths"),
                weaknesses = o.optString("weaknesses"),
                keyPlayers = o.optString("keyPlayers"),
                generalNotes = o.optString("generalNotes"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseRivalLinks(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            RivalLinkEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                opponentClubId = o.getInt("opponentClubId"),
                type = o.optString("type"),
                label = o.optString("label"),
                url = o.optString("url"),
                sortOrder = o.optInt("sortOrder"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseOpponentPlayers(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            OpponentPlayerEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                opponentClubId = o.getInt("opponentClubId"),
                name = o.optString("name"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}

private fun parseBoards(arr: JSONArray, requireSync: Boolean) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            BoardEntity(
                id = o.getInt("id"),
                syncId = o.readSyncId(requireSync),
                teamId = o.getInt("teamId"),
                name = o.optString("name"),
                sceneVersion = o.optInt("sceneVersion", 1),
                sceneJson = o.optString("sceneJson"),
                createdAt = o.optLong("createdAt", 0L),
                updatedAt = o.optLong("updatedAt", 0L),
                deletedAt = o.optNullableLong("deletedAt")
            )
        )
    }
}
