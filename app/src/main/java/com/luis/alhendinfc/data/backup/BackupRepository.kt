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
import java.io.OutputStream
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
        context.contentResolver.openOutputStream(destUri)?.use { os ->
            writeBackupToStream(os)
        } ?: error("No se pudo escribir el archivo de backup")
    }

    suspend fun importFromUri(sourceUri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "backup_import").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        var incomingTag: String? = null
        try {
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

            val payload = BackupValidator.validateJson(
                jsonFile.readText(Charsets.UTF_8),
                SCHEMA_VERSION
            )

            writeSafetyBackup()

            incomingTag = "restore_${System.currentTimeMillis()}"
            val mediaMap = copyIncomingMedia(extractDir, incomingTag)
            val resolved = payload.withResolvedUris { value ->
                if (value.isNullOrBlank()) null else mediaMap[value] ?: value
            }

            try {
                val db = AlhendinDatabase.getInstance(context)
                commitValidatedBackup(RoomBackupMutator(db), resolved)
                homePrefs.restoreEncodedLayout(resolved.homeLayout)
            } catch (t: Throwable) {
                incomingTag?.let { deleteTaggedMedia(it) }
                throw t
            }

            BackupSummary(
                teams = resolved.counts.teams,
                players = resolved.counts.players,
                matches = resolved.counts.matches,
                matchPlayers = resolved.counts.matchPlayers,
                events = resolved.counts.events,
                customStatTypes = resolved.counts.customStatTypes,
                opponentClubs = resolved.counts.opponentClubs,
                fixtures = resolved.counts.fixtures,
                mediaFiles = mediaMap.size
            )
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private suspend fun writeSafetyBackup() {
        val dir = File(context.filesDir, SAFETY_DIR).apply { mkdirs() }
        val file = File(dir, "pre_restore_${System.currentTimeMillis()}.zip")
        FileOutputStream(file).use { writeBackupToStream(it) }
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("pre_restore_") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(SAFETY_KEEP)
            ?.forEach { it.delete() }
    }

    private suspend fun writeBackupToStream(os: OutputStream): BackupSummary {
        val db = AlhendinDatabase.getInstance(context)
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

        val dbFile = context.getDatabasePath(AlhendinDatabase.NAME)
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        ZipOutputStream(BufferedOutputStream(os)).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

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

        val mediaCount = mediaDir.listFiles()?.size ?: 0
        mediaDir.deleteRecursively()

        return BackupSummary(
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

    private fun copyIncomingMedia(extractDir: File, tag: String): Map<String, String> {
        val mediaOut = File(context.filesDir, "backup_media").also { it.mkdirs() }
        val mediaMap = mutableMapOf<String, String>()
        File(extractDir, "media").listFiles()?.forEach { file ->
            val dest = File(mediaOut, "${tag}_${file.name}")
            file.copyTo(dest, overwrite = false)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dest
            ).toString()
            mediaMap["media/${file.name}"] = contentUri
        }
        return mediaMap
    }

    private fun deleteTaggedMedia(tag: String) {
        val mediaOut = File(context.filesDir, "backup_media")
        mediaOut.listFiles()?.filter { it.name.startsWith("${tag}_") }?.forEach { it.delete() }
    }

    companion object {
        const val SCHEMA_VERSION = AlhendinDatabase.VERSION
        private const val SAFETY_DIR = "safety_backups"
        private const val SAFETY_KEEP = 5

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
