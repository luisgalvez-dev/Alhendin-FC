package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TeamEntity
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
    val fixtures: Int
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

    fun validateJson(json: String, expectedSchemaVersion: Int): ValidatedBackup {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("backup.json no es un JSON válido", e)
        }

        if (!root.has("schemaVersion") || root.isNull("schemaVersion")) {
            throw IllegalArgumentException("El backup no indica schemaVersion")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != expectedSchemaVersion) {
            throw IllegalArgumentException(
                "schemaVersion incompatible: $schemaVersion (se espera $expectedSchemaVersion)"
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

        val teams = parseTeams(root.getJSONArray("teams"))
        val players = parsePlayers(root.getJSONArray("players"))
        val matches = parseMatches(root.getJSONArray("matches"))
        val matchPlayers = parseMatchPlayers(root.getJSONArray("matchPlayers"))
        val events = parseEvents(root.getJSONArray("events"))
        val customStats = parseCustomStats(root.getJSONArray("customStatTypes"))
        val clubs = parseClubs(root.getJSONArray("opponentClubs"))
        val fixtures = parseFixtures(root.getJSONArray("fixtures"))

        val parsed = BackupCounts(
            teams = teams.size,
            players = players.size,
            matches = matches.size,
            matchPlayers = matchPlayers.size,
            events = events.size,
            customStatTypes = customStats.size,
            opponentClubs = clubs.size,
            fixtures = fixtures.size
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

        return ValidatedBackup(
            schemaVersion = schemaVersion,
            teams = teams,
            players = players,
            matches = matches,
            matchPlayers = matchPlayers,
            events = events,
            customStatTypes = customStats,
            opponentClubs = clubs,
            fixtures = fixtures,
            homeLayout = homeLayout,
            counts = parsed
        )
    }

    private fun parseDeclaredCounts(o: JSONObject) = BackupCounts(
        teams = o.optInt("teams"),
        players = o.optInt("players"),
        matches = o.optInt("matches"),
        matchPlayers = o.optInt("matchPlayers"),
        events = o.optInt("events"),
        customStatTypes = o.optInt("customStatTypes"),
        opponentClubs = o.optInt("opponentClubs"),
        fixtures = o.optInt("fixtures")
    )
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key)
    return value.takeIf { it.isNotBlank() && it != "null" }
}

private fun parseTeams(arr: JSONArray) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            TeamEntity(
                id = o.getInt("id"),
                name = o.optString("name"),
                category = o.optString("category"),
                season = o.optString("season"),
                shieldUri = o.optNullableString("shieldUri"),
                isSelected = o.optBoolean("isSelected", false)
            )
        )
    }
}

private fun parsePlayers(arr: JSONArray) = buildList {
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
                observations = o.optString("observations")
            )
        )
    }
}

private fun parseMatches(arr: JSONArray) = buildList {
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
                rivalShieldUri = o.optNullableString("rivalShieldUri"),
                livePeriod = o.optInt("livePeriod", 1),
                liveElapsedSeconds = o.optInt("liveElapsedSeconds", 0),
                liveClockRunning = o.optBoolean("liveClockRunning", false),
                liveClockAnchorWallMs = o.optLong("liveClockAnchorWallMs", 0L),
                fieldSecondsJson = o.optString("fieldSecondsJson", ""),
                fieldPositionsJson = o.optString("fieldPositionsJson", "")
            )
        )
    }
}

private fun parseMatchPlayers(arr: JSONArray) = buildList {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
            MatchPlayerEntity(
                id = o.getInt("id"),
                matchId = o.getInt("matchId"),
                playerId = o.getInt("playerId"),
                callupStatus = o.optString("callupStatus", "NONE"),
                isOnField = o.optBoolean("isOnField", false)
            )
        )
    }
}

private fun parseEvents(arr: JSONArray) = buildList {
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
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        )
    }
}

private fun parseCustomStats(arr: JSONArray) = buildList {
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
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        )
    }
}

private fun parseClubs(arr: JSONArray) = buildList {
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
                sortOrder = o.optInt("sortOrder")
            )
        )
    }
}

private fun parseFixtures(arr: JSONArray) = buildList {
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
                stadiumOverride = o.optString("stadiumOverride")
            )
        )
    }
}
