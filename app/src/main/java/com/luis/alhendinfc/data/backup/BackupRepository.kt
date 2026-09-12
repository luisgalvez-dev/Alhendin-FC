package com.luis.alhendinfc.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.DiskFileStore
import com.luis.alhendinfc.data.local.AlhendinDatabase
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
        val tasks: Int = 0,
        val trainings: Int = 0,
        val trainingTasks: Int = 0,
        val attachments: Int = 0,
        val rivalAnalyses: Int = 0,
        val rivalLinks: Int = 0,
        val opponentPlayers: Int = 0,
        val boards: Int = 0,
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
                "Tareas: $tasks\n" +
                "Entrenamientos: $trainings\n" +
                "Tareas de sesión: $trainingTasks\n" +
                "Archivos: $attachments\n" +
                "Análisis de rival: $rivalAnalyses\n" +
                "Enlaces de rival: $rivalLinks\n" +
                "Plantilla rival: $opponentPlayers\n" +
                "Pizarras: $boards\n" +
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
                                name.startsWith("attachments/") ||
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
                jsonFile.readText(Charsets.UTF_8)
            )

            writeSafetyBackup()

            incomingTag = "restore_${System.currentTimeMillis()}"
            val mediaMap = copyIncomingMedia(extractDir, incomingTag)
            val attachmentRoot = File(context.filesDir, AndroidAttachmentStore.DIR)
            val restoredAttachments = remapRestoredAttachments(
                extractDir,
                payload.attachments,
                DiskFileStore(attachmentRoot)
            )
            val resolved = payload.withResolvedUris { value ->
                if (value.isNullOrBlank()) null else mediaMap[value] ?: value
            }.copy(attachments = restoredAttachments)

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
                tasks = resolved.counts.tasks,
                trainings = resolved.counts.trainings,
                trainingTasks = resolved.counts.trainingTasks,
                attachments = resolved.counts.attachments,
                rivalAnalyses = resolved.counts.rivalAnalyses,
                rivalLinks = resolved.counts.rivalLinks,
                opponentPlayers = resolved.counts.opponentPlayers,
                boards = resolved.counts.boards,
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
        val tasks = db.taskDao().getAllOnce()
        val trainings = db.trainingDao().getAllOnce()
        val trainingTasks = db.trainingTaskDao().getAllOnce()
        val attachmentFiles = mutableMapOf<String, File>()
        val attachments = db.attachmentDao().getAllOnce().map { att ->
            if (att.deletedAt == null) {
                val srcPath = att.localPath
                val src = srcPath?.let { File(it) }
                if (src != null && src.isFile) {
                    val relative = "attachments/${att.syncId}"
                    attachmentFiles[relative] = src
                    att.copy(localPath = relative)
                } else att
            } else att
        }
        val rivalAnalyses = db.rivalAnalysisDao().getAllOnce()
        val rivalLinks = db.rivalLinkDao().getAllOnce()
        val opponentPlayers = db.opponentPlayerDao().getAllOnce()
        val boards = db.boardDao().getAllOnce()
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
            .put("tasks", tasks.size)
            .put("trainings", trainings.size)
            .put("trainingTasks", trainingTasks.size)
            .put("attachments", attachments.size)
            .put("rivalAnalyses", rivalAnalyses.size)
            .put("rivalLinks", rivalLinks.size)
            .put("opponentPlayers", opponentPlayers.size)
            .put("boards", boards.size)

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
            .put("tasks", tasksToJson(tasks))
            .put("trainings", trainingsToJson(trainings))
            .put("trainingTasks", trainingTasksToJson(trainingTasks))
            .put("attachments", attachmentsToJson(attachments))
            .put("rivalAnalyses", rivalAnalysesToJson(rivalAnalyses))
            .put("rivalLinks", rivalLinksToJson(rivalLinks))
            .put("opponentPlayers", opponentPlayersToJson(opponentPlayers))
            .put("boards", boardsToJson(boards))

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
            attachmentFiles.forEach { (relative, file) ->
                zip.putNextEntry(ZipEntry(relative))
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
            tasks = tasks.size,
            trainings = trainings.size,
            trainingTasks = trainingTasks.size,
            attachments = attachments.size,
            rivalAnalyses = rivalAnalyses.size,
            rivalLinks = rivalLinks.size,
            opponentPlayers = opponentPlayers.size,
            boards = boards.size,
            mediaFiles = mediaCount + attachmentFiles.size
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

        /** Tablas deportivas del ZIP. `sync_outbox` y `transfer_job` no viajan. */
        val ALL_TABLES = listOf(
            "team",
            "player",
            "match_table",
            "match_player",
            "match_event",
            "custom_stat_type",
            "opponent_club",
            "season_fixture",
            "task",
            "training",
            "training_task",
            "attachment",
            "rival_analysis",
            "rival_link",
            "opponent_player",
            "board"
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
                .put("syncId", t.syncId)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
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
                .put("syncId", p.syncId)
                .put("createdAt", p.createdAt)
                .put("updatedAt", p.updatedAt)
                .putOptLong("deletedAt", p.deletedAt)
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
                .put("syncId", m.syncId)
                .put("createdAt", m.createdAt)
                .put("updatedAt", m.updatedAt)
                .putOptLong("deletedAt", m.deletedAt)
                .putOptLong("dateEpochDay", m.dateEpochDay)
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
                .put("syncId", mp.syncId)
                .put("createdAt", mp.createdAt)
                .put("updatedAt", mp.updatedAt)
                .putOptLong("deletedAt", mp.deletedAt)
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
                .put("syncId", e.syncId)
                .put("updatedAt", e.updatedAt)
                .putOptLong("deletedAt", e.deletedAt)
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
                .put("syncId", t.syncId)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
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
                .put("syncId", c.syncId)
                .put("createdAt", c.createdAt)
                .put("updatedAt", c.updatedAt)
                .putOptLong("deletedAt", c.deletedAt)
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
                .put("syncId", f.syncId)
                .put("createdAt", f.createdAt)
                .put("updatedAt", f.updatedAt)
                .putOptLong("deletedAt", f.deletedAt)
                .putOptLong("dateEpochDay", f.dateEpochDay)
        )
    }
}

private fun tasksToJson(list: List<TaskEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("teamId", t.teamId)
                .put("name", t.name)
                .put("objective", t.objective)
                .putOptInt("playerCount", t.playerCount)
                .putOptInt("durationMinutes", t.durationMinutes)
                .put("description", t.description)
                .put("boardSyncId", t.boardSyncId)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun trainingsToJson(list: List<TrainingEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("teamId", t.teamId)
                .put("date", t.date)
                .put("dateEpochDay", t.dateEpochDay)
                .put("opponentClubId", t.opponentClubId)
                .put("notes", t.notes)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun trainingTasksToJson(list: List<TrainingTaskEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("trainingId", t.trainingId)
                .put("taskId", t.taskId)
                .put("sortOrder", t.sortOrder)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun attachmentsToJson(list: List<AttachmentEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("parentType", t.parentType)
                .put("parentSyncId", t.parentSyncId)
                .put("mimeType", t.mimeType)
                .put("name", t.name)
                .put("localPath", t.localPath ?: JSONObject.NULL)
                .put("remotePath", t.remotePath)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun rivalAnalysesToJson(list: List<RivalAnalysisEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("opponentClubId", t.opponentClubId)
                .put("usualSystem", t.usualSystem)
                .put("variants", t.variants)
                .put("buildUp", t.buildUp)
                .put("progression", t.progression)
                .put("finalThird", t.finalThird)
                .put("highPress", t.highPress)
                .put("midBlock", t.midBlock)
                .put("lowBlock", t.lowBlock)
                .put("transAttackToDefense", t.transAttackToDefense)
                .put("transDefenseToAttack", t.transDefenseToAttack)
                .put("cornersOffensive", t.cornersOffensive)
                .put("cornersDefensive", t.cornersDefensive)
                .put("setPieces", t.setPieces)
                .put("strengths", t.strengths)
                .put("weaknesses", t.weaknesses)
                .put("keyPlayers", t.keyPlayers)
                .put("generalNotes", t.generalNotes)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun rivalLinksToJson(list: List<RivalLinkEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("opponentClubId", t.opponentClubId)
                .put("type", t.type)
                .put("label", t.label)
                .put("url", t.url)
                .put("sortOrder", t.sortOrder)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun opponentPlayersToJson(list: List<OpponentPlayerEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("opponentClubId", t.opponentClubId)
                .put("name", t.name)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun boardsToJson(list: List<BoardEntity>) = JSONArray().also { arr ->
    list.forEach { t ->
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("syncId", t.syncId)
                .put("teamId", t.teamId)
                .put("name", t.name)
                .put("sceneVersion", t.sceneVersion)
                .put("sceneJson", t.sceneJson)
                .put("createdAt", t.createdAt)
                .put("updatedAt", t.updatedAt)
                .putOptLong("deletedAt", t.deletedAt)
        )
    }
}

private fun JSONObject.putOptInt(key: String, value: Int?): JSONObject {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
    return this
}

private fun JSONObject.putOptLong(key: String, value: Long?): JSONObject {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
    return this
}
