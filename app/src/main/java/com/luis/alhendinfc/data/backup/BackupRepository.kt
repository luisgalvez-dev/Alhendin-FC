package com.luis.alhendinfc.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.data.preferences.HomePreferencesRepository
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exporta / importa **toda** la BD Room (equipos, jugadores, partidos, convocatorias,
 * eventos/estadísticas, tipos personalizados, clubs, calendario) + layout del Home.
 * Las estadísticas de temporada se reconstruyen desde partidos FINISHED + match_event + convocatorias.
 */
class BackupRepository(
    private val context: Context,
    private val homePrefs: HomePreferencesRepository
) {

    data class BackupSummary(
        val teams: Int,
        val players: Int,
        val matches: Int,
        val matchPlayers: Int,
        val events: Int,
        val customStatTypes: Int,
        val opponentClubs: Int,
        val fixtures: Int,
        val mediaFiles: Int
    ) {
        fun asMessage(prefix: String): String =
            "$prefix\n\n" +
                "Equipos: $teams\n" +
                "Jugadores: $players\n" +
                "Partidos: $matches\n" +
                "Convocatorias: $matchPlayers\n" +
                "Eventos/estadísticas: $events\n" +
                "Tipos personalizados: $customStatTypes\n" +
                "Clubs rivales: $opponentClubs\n" +
                "Jornadas: $fixtures\n" +
                "Archivos media: $mediaFiles"
    }

    suspend fun exportToUri(destUri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val db = AlhendinDatabase.getInstance(context)
        // Asegura que lo escrito en WAL esté en el fichero (por si también copiamos la BD).
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

        val mediaDir = File(context.cacheDir, "backup_export_media").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        val uriMap = mutableMapOf<String, String>()

        fun packMedia(uriString: String?): String? {
            if (uriString.isNullOrBlank()) return null
            uriMap[uriString]?.let { return it }
            return try {
                val uri = Uri.parse(uriString)
                val name = "m${abs(uriString.hashCode())}_${System.nanoTime() % 100000}.bin"
                val outFile = File(mediaDir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                } ?: return uriString
                val relative = "media/$name"
                uriMap[uriString] = relative
                relative
            } catch (_: Exception) {
                uriString
            }
        }

        val teams = db.teamDao().getAllOnce().map { t ->
            t.copy(shieldUri = packMedia(t.shieldUri))
        }
        val players = db.playerDao().getAllOnce().map { p ->
            p.copy(photoUri = packMedia(p.photoUri))
        }
        val matches = db.matchDao().getAllMatchesOnce().map { m ->
            m.copy(rivalShieldUri = packMedia(m.rivalShieldUri))
        }
        val matchPlayers = db.matchDao().getAllMatchPlayersOnce()
        val events = db.matchEventDao().getAllOnce()
        val customStats = db.customStatTypeDao().getAllOnce()
        val clubs = db.opponentClubDao().getAllOnce().map { c ->
            c.copy(shieldUri = packMedia(c.shieldUri))
        }
        val fixtures = db.seasonFixtureDao().getAllOnce()
        val homeLayout = homePrefs.currentEncodedLayout()

        val counts = JSONObject()
            .put("teams", teams.size)
            .put("players", players.size)
            .put("matches", matches.size)
            .put("matchPlayers", matchPlayers.size)
            .put("events", events.size)
            .put("customStatTypes", customStats.size)
            .put("opponentClubs", clubs.size)
            .put("fixtures", fixtures.size)

        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("covers", "full_room_database")
            .put("tables", JSONArray(ALL_TABLES))
            .put("counts", counts)
            .put("homeLayout", homeLayout)
            .put("teams", teamsToJson(teams))
            .put("players", playersToJson(players))
            .put("matches", matchesToJson(matches))
            .put("matchPlayers", matchPlayersToJson(matchPlayers))
            .put("events", eventsToJson(events))
            .put("customStatTypes", customStatsToJson(customStats))
            .put("opponentClubs", clubsToJson(clubs))
            .put("fixtures", fixturesToJson(fixtures))

        val dbFile = context.getDatabasePath("alhendin_db")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        context.contentResolver.openOutputStream(destUri)?.use { os ->
            ZipOutputStream(BufferedOutputStream(os)).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // Copia binaria completa de la BD (todas las tablas, byte a byte).
                if (dbFile.exists()) {
                    zip.putNextEntry(ZipEntry("database/alhendin_db"))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                if (walFile.exists() && walFile.length() > 0) {
                    zip.putNextEntry(ZipEntry("database/alhendin_db-wal"))
                    walFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                if (shmFile.exists() && shmFile.length() > 0) {
                    zip.putNextEntry(ZipEntry("database/alhendin_db-shm"))
                    shmFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                mediaDir.listFiles()?.forEach { file ->
                    zip.putNextEntry(ZipEntry("media/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("No se pudo escribir el archivo de backup")

        val mediaCount = mediaDir.listFiles()?.size ?: 0
        mediaDir.deleteRecursively()

        BackupSummary(
            teams = teams.size,
            players = players.size,
            matches = matches.size,
            matchPlayers = matchPlayers.size,
            events = events.size,
            customStatTypes = customStats.size,
            opponentClubs = clubs.size,
            fixtures = fixtures.size,
            mediaFiles = mediaCount
        )
    }

    suspend fun importFromUri(sourceUri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "backup_import").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory &&
                        (name == "backup.json" ||
                            name.startsWith("media/") ||
                            name.startsWith("database/"))
                    ) {
                        val out = File(extractDir, name)
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("No se pudo leer el backup")

        val jsonFile = File(extractDir, "backup.json")
        if (!jsonFile.exists()) error("El ZIP no contiene backup.json")
        val root = JSONObject(jsonFile.readText(Charsets.UTF_8))

        val mediaOut = File(context.filesDir, "backup_media").also {
            it.mkdirs()
        }
        val mediaMap = mutableMapOf<String, String>()
        File(extractDir, "media").listFiles()?.forEach { file ->
            val dest = File(mediaOut, file.name)
            file.copyTo(dest, overwrite = true)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dest
            ).toString()
            mediaMap["media/${file.name}"] = contentUri
        }

        fun resolveUri(value: String?): String? {
            if (value.isNullOrBlank()) return null
            return mediaMap[value] ?: value
        }

        val teams = parseTeams(root.arrayOrEmpty("teams")).map {
            it.copy(shieldUri = resolveUri(it.shieldUri))
        }
        val players = parsePlayers(root.arrayOrEmpty("players")).map {
            it.copy(photoUri = resolveUri(it.photoUri))
        }
        val matches = parseMatches(root.arrayOrEmpty("matches")).map {
            it.copy(rivalShieldUri = resolveUri(it.rivalShieldUri))
        }
        val matchPlayers = parseMatchPlayers(root.arrayOrEmpty("matchPlayers"))
        val events = parseEvents(root.arrayOrEmpty("events"))
        val customStats = parseCustomStats(root.arrayOrEmpty("customStatTypes"))
        val clubs = parseClubs(root.arrayOrEmpty("opponentClubs")).map {
            it.copy(shieldUri = resolveUri(it.shieldUri))
        }
        val fixtures = parseFixtures(root.arrayOrEmpty("fixtures"))
        val homeLayout = if (root.has("homeLayout") && !root.isNull("homeLayout")) {
            root.getString("homeLayout")
        } else {
            null
        }

        AlhendinDatabase.resetInstance()
        val db = AlhendinDatabase.getInstance(context)
        db.clearAllTables()
        if (teams.isNotEmpty()) db.teamDao().insertAll(teams)
        if (players.isNotEmpty()) db.playerDao().insertAll(players)
        if (matches.isNotEmpty()) db.matchDao().insertMatches(matches)
        if (matchPlayers.isNotEmpty()) db.matchDao().insertMatchPlayers(matchPlayers)
        if (events.isNotEmpty()) db.matchEventDao().insertAll(events)
        if (customStats.isNotEmpty()) db.customStatTypeDao().replaceAll(customStats)
        if (clubs.isNotEmpty()) db.opponentClubDao().replaceAll(clubs)
        if (fixtures.isNotEmpty()) db.seasonFixtureDao().replaceAll(fixtures)
        fixSqliteSequences(db)
        homePrefs.restoreEncodedLayout(homeLayout)

        extractDir.deleteRecursively()

        BackupSummary(
            teams = teams.size,
            players = players.size,
            matches = matches.size,
            matchPlayers = matchPlayers.size,
            events = events.size,
            customStatTypes = customStats.size,
            opponentClubs = clubs.size,
            fixtures = fixtures.size,
            mediaFiles = mediaMap.size
        )
    }

    private fun fixSqliteSequences(db: AlhendinDatabase) {
        val sqlDb = db.openHelper.writableDatabase
        ALL_TABLES.forEach { table ->
            sqlDb.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
            sqlDb.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) " +
                    "SELECT ?, IFNULL(MAX(id), 0) FROM $table",
                arrayOf(table)
            )
        }
    }

    companion object {
        const val SCHEMA_VERSION = 14

        /** Todas las tablas Room de la app (estadísticas = matches + events + match_player). */
        val ALL_TABLES = listOf(
            "team",
            "player",
            "match_table",
            "match_player",
            "match_event",
            "custom_stat_type",
            "opponent_club",
            "season_fixture"
        )

        fun getInstance(context: Context): BackupRepository =
            BackupRepository(
                context.applicationContext,
                HomePreferencesRepository.getInstance(context)
            )
    }
}

private fun JSONObject.arrayOrEmpty(key: String): JSONArray =
    if (has(key) && !isNull(key)) getJSONArray(key) else JSONArray()

private fun teamsToJson(list: List<TeamEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("name", t.name)
                .put("category", t.category)
                .put("season", t.season)
                .put("shieldUri", t.shieldUri)
                .put("isSelected", t.isSelected)
        )
    }
}

private fun playersToJson(list: List<PlayerEntity>) = JSONArray().also { arr ->
    list.forEach { p ->
        arr.put(
            JSONObject()
                .put("id", p.id)
                .put("teamId", p.teamId)
                .put("name", p.name)
                .put("alias", p.alias)
                .put("position", p.position)
                .put("jerseyNumber", p.jerseyNumber)
                .put("photoUri", p.photoUri)
                .put("height", p.height)
                .put("weight", p.weight)
                .put("laterality", p.laterality)
                .put("isActive", p.isActive)
                .put("observations", p.observations)
        )
    }
}

private fun matchesToJson(list: List<MatchEntity>) = JSONArray().also { arr ->
    list.forEach { m ->
        arr.put(
            JSONObject()
                .put("id", m.id)
                .put("teamId", m.teamId)
                .put("rival", m.rival)
                .put("stadium", m.stadium)
                .put("date", m.date)
                .put("time", m.time)
                .put("matchday", m.matchday)
                .put("isHome", m.isHome)
                .put("durationPerPart", m.durationPerPart)
                .put("numParts", m.numParts)
                .put("formation", m.formation)
                .put("notes", m.notes)
                .put("status", m.status)
                .put("homeScore", m.homeScore)
                .put("awayScore", m.awayScore)
                .put("opponentClubId", m.opponentClubId)
                .put("rivalShieldUri", m.rivalShieldUri)
                .put("livePeriod", m.livePeriod)
                .put("liveElapsedSeconds", m.liveElapsedSeconds)
                .put("liveClockRunning", m.liveClockRunning)
                .put("liveClockAnchorWallMs", m.liveClockAnchorWallMs)
                .put("fieldSecondsJson", m.fieldSecondsJson)
                .put("fieldPositionsJson", m.fieldPositionsJson)
        )
    }
}

private fun matchPlayersToJson(list: List<MatchPlayerEntity>) = JSONArray().also { arr ->
    list.forEach { mp ->
        arr.put(
            JSONObject()
                .put("id", mp.id)
                .put("matchId", mp.matchId)
                .put("playerId", mp.playerId)
                .put("callupStatus", mp.callupStatus)
                .put("isOnField", mp.isOnField)
        )
    }
}

private fun eventsToJson(list: List<MatchEventEntity>) = JSONArray().also { arr ->
    list.forEach { e ->
        arr.put(
            JSONObject()
                .put("id", e.id)
                .put("matchId", e.matchId)
                .put("typeCode", e.typeCode)
                .put("playerId", e.playerId)
                .put("relatedPlayerId", e.relatedPlayerId)
                .put("minute", e.minute)
                .put("period", e.period)
                .put("value", e.value)
                .put("createdAt", e.createdAt)
        )
    }
}

private fun customStatsToJson(list: List<CustomStatTypeEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("teamId", t.teamId)
                .put("code", t.code)
                .put("label", t.label)
                .put("shortLabel", t.shortLabel)
                .put("appliesTo", t.appliesTo)
                .put("sortOrder", t.sortOrder)
                .put("isActive", t.isActive)
                .put("createdAt", t.createdAt)
        )
    }
}

private fun clubsToJson(list: List<OpponentClubEntity>) = JSONArray().also { arr ->
    list.forEach { c ->
        arr.put(
            JSONObject()
                .put("id", c.id)
                .put("teamId", c.teamId)
                .put("name", c.name)
                .put("shortName", c.shortName)
                .put("stadium", c.stadium)
                .put("shieldUri", c.shieldUri)
                .put("kitColors", c.kitColors)
                .put("sortOrder", c.sortOrder)
        )
    }
}

private fun fixturesToJson(list: List<SeasonFixtureEntity>) = JSONArray().also { arr ->
    list.forEach { f ->
        arr.put(
            JSONObject()
                .put("id", f.id)
                .put("teamId", f.teamId)
                .put("matchday", f.matchday)
                .put("opponentClubId", f.opponentClubId)
                .put("isHome", f.isHome)
                .put("date", f.date)
                .put("time", f.time)
                .put("stadiumOverride", f.stadiumOverride)
        )
    }
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
