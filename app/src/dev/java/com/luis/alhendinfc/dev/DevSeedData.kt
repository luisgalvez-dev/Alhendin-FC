package com.luis.alhendinfc.dev

import android.content.Context
import android.util.Log
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.BoardDao
import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.OpponentClubDao
import com.luis.alhendinfc.data.local.OpponentPlayerDao
import com.luis.alhendinfc.data.local.PlayerDao
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.RivalAnalysisDao
import com.luis.alhendinfc.data.local.RivalLinkDao
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.TaskDao
import com.luis.alhendinfc.data.local.TrainingDao
import com.luis.alhendinfc.domain.model.Board
import com.luis.alhendinfc.domain.model.BoardNormPoint
import com.luis.alhendinfc.domain.model.BoardObject
import com.luis.alhendinfc.domain.model.BoardObjectType
import com.luis.alhendinfc.domain.model.BoardScene
import com.luis.alhendinfc.domain.model.CalendarDate
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Laterality
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerPosition
import com.luis.alhendinfc.domain.model.SeasonFixture
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.model.Team
import com.luis.alhendinfc.domain.model.Training
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalLink
import com.luis.alhendinfc.domain.model.RivalLinkType
import com.luis.alhendinfc.domain.repository.BoardRepository
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.repository.RivalRepository
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import com.luis.alhendinfc.domain.repository.TeamRepositoryImpl
import com.luis.alhendinfc.domain.repository.TrainingRepositoryImpl
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Inserta datos de prueba identificables en AlhendinFC DEV.
 * No borra, no hace REPLACE, no pisa registros reales.
 * Si un demo ya existe (aunque esté tombstoneado), no lo recrea ni resincroniza
 * su contenido: las relaciones demo solo se completan en el alta inicial.
 */
object DevSeedData {

    private const val TAG = "DevSeed"
    private val mutex = Mutex()
    private val STARTER_NUMBERS = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    private val BENCH_NUMBERS = setOf(12, 13, 14, 15, 16, 17, 19)

    suspend fun runIfNeeded(context: Context) {
        mutex.withLock {
            runCatching { seed(context.applicationContext) }
                .onFailure { Log.w(TAG, "Seed DEV omitido por error", it) }
        }
    }

    private suspend fun seed(context: Context) {
        val db = AlhendinDatabase.getInstance(context)
        val teamRepo = TeamRepositoryImpl(db.teamDao())
        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val calendarRepo = SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao())
        val rivalRepo = RivalRepository(db.rivalAnalysisDao(), db.rivalLinkDao(), db.opponentPlayerDao())
        val boardRepo = BoardRepository(db.boardDao(), db.taskDao(), db.attachmentDao())
        val matchRepo = MatchRepositoryImpl(db.matchDao(), db.matchEventDao())
        val trainingRepo = TrainingRepositoryImpl(
            db.trainingDao(),
            db.trainingTaskDao(),
            db.taskDao(),
            db.matchDao(),
            db.seasonFixtureDao(),
            db.attachmentDao()
        )

        val teamId = ensureTeam(db, teamRepo) ?: return
        val teamName = db.teamDao().getByIdOnce(teamId)?.name ?: return
        val stores = DevSeedStores(
            taskDao = db.taskDao(),
            taskRepo = taskRepo,
            clubDao = db.opponentClubDao(),
            calendarRepo = calendarRepo,
            trainingDao = db.trainingDao(),
            matchDao = db.matchDao(),
            fixtureDao = db.seasonFixtureDao(),
            trainingRepo = trainingRepo,
            analysisDao = db.rivalAnalysisDao(),
            linkDao = db.rivalLinkDao(),
            playerDao = db.opponentPlayerDao(),
            rivalRepo = rivalRepo,
            boardDao = db.boardDao(),
            boardRepo = boardRepo,
            ownPlayerDao = db.playerDao(),
            playerRepo = PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
            matchRepo = matchRepo
        )
        val clubs = seedModuleExamples(stores, teamId, teamName)
        seedMatchesAndFixture(stores, teamId, clubs)
    }

    internal data class DevSeedStores(
        val taskDao: TaskDao,
        val taskRepo: TaskRepositoryImpl,
        val clubDao: OpponentClubDao,
        val calendarRepo: SeasonCalendarRepository,
        val trainingDao: TrainingDao,
        val matchDao: MatchDao,
        val fixtureDao: SeasonFixtureDao,
        val trainingRepo: TrainingRepositoryImpl,
        val analysisDao: RivalAnalysisDao,
        val linkDao: RivalLinkDao,
        val playerDao: OpponentPlayerDao,
        val rivalRepo: RivalRepository,
        val boardDao: BoardDao,
        val boardRepo: BoardRepository,
        val ownPlayerDao: PlayerDao,
        val playerRepo: PlayerRepositoryImpl,
        val matchRepo: MatchRepositoryImpl
    )

    /**
     * Crea plantilla [DEV] solo si:
     * - el equipo es `[DEV] Equipo de prueba`, o
     * - el equipo no tiene ningún jugador vivo (`deletedAt == null`);
     * y además no hay jugadores reales vivos (nombre sin prefijo `[DEV] `).
     * No mezcla ficticios con plantilla real. Tombstones bloquean recrear ese nombre.
     */
    internal fun canSeedOwnSquad(teamName: String, playersForTeam: List<PlayerEntity>): Boolean {
        val living = playersForTeam.filter { it.deletedAt == null }
        if (living.any { !DevSeedMarkers.isDemo(it.name) }) return false
        return DevSeedMarkers.isDemoTeam(teamName) || living.isEmpty()
    }

    internal suspend fun seedModuleExamples(
        stores: DevSeedStores,
        teamId: Int,
        teamName: String = DevSeedMarkers.labeled(DevSeedMarkers.DEMO_TEAM)
    ): List<OpponentClub> {
        seedOwnSquad(stores.ownPlayerDao, stores.playerRepo, teamId, teamName)
        seedTasks(stores.taskDao, stores.taskRepo, teamId)
        val clubs = seedDevClubs(stores.clubDao, stores.calendarRepo, teamId)
        seedTrainings(
            stores.trainingDao,
            stores.taskDao,
            stores.matchDao,
            stores.fixtureDao,
            stores.trainingRepo,
            teamId,
            clubs
        )
        seedRivalDossiers(stores.analysisDao, stores.linkDao, stores.rivalRepo, clubs)
        seedRivalSquads(stores.playerDao, stores.rivalRepo, clubs)
        seedBoards(stores.boardDao, stores.boardRepo, stores.taskDao, stores.taskRepo, teamId)
        return clubs
    }

    private suspend fun ensureTeam(
        db: AlhendinDatabase,
        teamRepo: TeamRepositoryImpl
    ): Int? {
        val active = db.teamDao().getAllOnce().filter { it.deletedAt == null }
        if (active.isNotEmpty()) {
            return active.firstOrNull { it.isSelected }?.id ?: active.first().id
        }
        val name = DevSeedMarkers.labeled(DevSeedMarkers.DEMO_TEAM)
        if (db.teamDao().getAllOnce().any { it.name == name }) return null
        return teamRepo.addTeam(
            Team(name = name, category = "Prueba DEV", season = "25/26")
        )
    }

    private suspend fun seedTasks(
        taskDao: TaskDao,
        taskRepo: TaskRepositoryImpl,
        teamId: Int
    ) {
        val specs = listOf(
            TaskSpec("Presión tras pérdida", "Recuperar en los primeros 6 segundos", 8, 12),
            TaskSpec("Salida de balón 3+2", "Construir desde atrás con superioridad", 10, 15),
            TaskSpec("Finalización", "Cara a portería con definición", 6, 10),
            TaskSpec("Posesión en espacio reducido", "Conservar y circular en poco espacio", 8, 12)
        )
        val existing = taskDao.getAllOnce().filter { it.teamId == teamId }
        specs.forEach { spec ->
            val name = DevSeedMarkers.labeled(spec.name)
            if (existing.any { it.name == name }) return@forEach
            taskRepo.add(
                Task(
                    teamId = teamId,
                    name = name,
                    objective = spec.objective,
                    playerCount = spec.players,
                    durationMinutes = spec.minutes,
                    description = "Dato de prueba DEV. Se puede eliminar."
                )
            )
        }
    }

    private suspend fun seedDevClubs(
        clubDao: OpponentClubDao,
        calendarRepo: SeasonCalendarRepository,
        teamId: Int
    ): List<OpponentClub> {
        val all = clubDao.getAllOnce().filter { it.teamId == teamId }
        listOf(
            ClubSpec("Rival A", "DEVA", "Campo DEV A", "Verde y blanco"),
            ClubSpec("Rival B", "DEVB", "Campo DEV B", "Azul"),
            ClubSpec("Rival C", "DEVC", "Campo DEV C", "Rojo")
        ).forEachIndexed { index, spec ->
            val labeled = DevSeedMarkers.labeled(spec.name)
            if (all.any { it.name == labeled }) return@forEachIndexed
            calendarRepo.addClub(
                OpponentClub(
                    teamId = teamId,
                    name = labeled,
                    shortName = spec.shortName,
                    stadium = spec.stadium,
                    kitColors = spec.kitColors,
                    sortOrder = index
                )
            )
        }
        return clubDao.getAllOnce()
            .filter { it.teamId == teamId && it.deletedAt == null && DevSeedMarkers.isDemo(it.name) }
            .map { it.toSeedClub() }
    }

    internal suspend fun seedMatchesAndFixture(
        stores: DevSeedStores,
        teamId: Int,
        clubs: List<OpponentClub>
    ) {
        val occupied = occupiedDays(stores.matchDao, stores.fixtureDao, stores.trainingDao, teamId)
        val today = LocalDate.now().toEpochDay()
        val matches = stores.matchDao.getAllMatchesOnce().filter { it.teamId == teamId }
        val fixtures = stores.fixtureDao.getAllOnce().filter { it.teamId == teamId }
        var nextMatchday = (
            matches.maxOfOrNull { it.matchday } ?: 0
        ).coerceAtLeast(fixtures.maxOfOrNull { it.matchday } ?: 0) + 1

        fun takeMatchday(): Int {
            while (fixtures.any { it.matchday == nextMatchday }) nextMatchday++
            val chosen = nextMatchday
            nextMatchday++
            return chosen
        }

        val pastClub = clubs.getOrNull(0)
        val futureClub = clubs.getOrNull(1)
        val fixtureClub = clubs.getOrNull(2) ?: clubs.lastOrNull()

        if (matches.none { DevSeedMarkers.isDemo(it.rival) && it.status == MatchStatus.FINISHED.name }) {
            val epoch = DevSeedCalendar.nextFree(today - 4, occupied, -1L)
            val id = stores.matchRepo.createMatch(
                Match(
                    teamId = teamId,
                    rival = pastClub?.name ?: DevSeedMarkers.labeled("Rival A"),
                    stadium = pastClub?.stadium ?: "Campo DEV",
                    date = CalendarDate.format(epoch),
                    time = "12:00",
                    matchday = takeMatchday(),
                    isHome = true,
                    opponentClubId = pastClub?.id,
                    status = MatchStatus.OPEN,
                    notes = DevSeedMarkers.labeled("Partido pasado de prueba. Listo para añadir informes.")
                )
            )
            seedDemoCallupIfEmpty(stores, teamId, id)
            stores.matchRepo.markMatchFinished(id, 2, 1, 2, 90 * 60, "", "")
        }

        if (matches.none { DevSeedMarkers.isDemo(it.rival) && it.status == MatchStatus.OPEN.name }) {
            val epoch = DevSeedCalendar.nextFree(today + 8, occupied, 1L)
            val id = stores.matchRepo.createMatch(
                Match(
                    teamId = teamId,
                    rival = futureClub?.name ?: DevSeedMarkers.labeled("Rival B"),
                    stadium = futureClub?.stadium ?: "Campo DEV",
                    date = CalendarDate.format(epoch),
                    time = "17:00",
                    matchday = takeMatchday(),
                    isHome = false,
                    opponentClubId = futureClub?.id,
                    status = MatchStatus.OPEN,
                    notes = DevSeedMarkers.labeled("Partido futuro de prueba. Listo para añadir informes.")
                )
            )
            seedDemoCallupIfEmpty(stores, teamId, id)
        }

        if (fixtureClub != null &&
            fixtures.none { DevSeedMarkers.isDemo(it.stadiumOverride) }
        ) {
            val epoch = DevSeedCalendar.nextFree(today + 20, occupied, 1L)
            stores.calendarRepo.upsertFixture(
                SeasonFixture(
                    teamId = teamId,
                    matchday = takeMatchday(),
                    opponentClubId = fixtureClub.id,
                    isHome = true,
                    date = CalendarDate.format(epoch),
                    time = "11:30",
                    stadiumOverride = DevSeedMarkers.labeled("Jornada futura")
                )
            )
        }

        stores.matchDao.getAllMatchesOnce()
            .filter { it.teamId == teamId && it.deletedAt == null && isDemoMatch(it.rival, it.notes) }
            .forEach { seedDemoCallupIfEmpty(stores, teamId, it.id) }
    }

    private suspend fun seedTrainings(
        trainingDao: TrainingDao,
        taskDao: TaskDao,
        matchDao: MatchDao,
        fixtureDao: SeasonFixtureDao,
        trainingRepo: TrainingRepositoryImpl,
        teamId: Int,
        clubs: List<OpponentClub>
    ) {
        val existing = trainingDao.getAllOnce().filter { it.teamId == teamId }
        val occupied = occupiedDays(matchDao, fixtureDao, trainingDao, teamId)
        val today = LocalDate.now().toEpochDay()
        val demoTasks = taskDao.getAllOnce()
            .filter { it.teamId == teamId && it.deletedAt == null && DevSeedMarkers.isDemo(it.name) }

        fun taskNamed(fragment: String) =
            demoTasks.firstOrNull { it.name.contains(fragment, ignoreCase = true) }

        val salida = taskNamed("Salida")
        val presion = taskNamed("Presión")
        val posesion = taskNamed("Posesión")
        val finalizacion = taskNamed("Finalización")

        data class TrainingSpec(
            val notes: String,
            val withRival: Boolean,
            val taskIds: List<Int>,
            val startOffset: Long,
            val step: Long
        )

        val specs = listOf(
            TrainingSpec(
                notes = DevSeedMarkers.labeled("Sesión con varias tareas. Notas de prueba."),
                withRival = false,
                taskIds = listOfNotNull(salida?.id, presion?.id),
                startOffset = -1,
                step = -1
            ),
            TrainingSpec(
                notes = DevSeedMarkers.labeled("Sesión con rival. Notas de prueba."),
                withRival = true,
                taskIds = listOfNotNull(posesion?.id, finalizacion?.id),
                startOffset = 2,
                step = 1
            ),
            TrainingSpec(
                notes = DevSeedMarkers.labeled("Sesión sin rival. Notas de prueba."),
                withRival = false,
                taskIds = listOfNotNull(finalizacion?.id),
                startOffset = 4,
                step = 1
            )
        )

        specs.forEach { spec ->
            if (existing.any { it.notes == spec.notes }) return@forEach
            if (spec.withRival && clubs.isEmpty()) return@forEach
            val epoch = DevSeedCalendar.nextFree(today + spec.startOffset, occupied, spec.step)
            if (!trainingRepo.canCreate(teamId, epoch)) return@forEach
            val trainingId = trainingRepo.add(
                Training(
                    teamId = teamId,
                    date = CalendarDate.format(epoch),
                    dateEpochDay = epoch,
                    opponentClubId = if (spec.withRival) clubs.first().id else null,
                    notes = spec.notes
                )
            )
            spec.taskIds.forEach { taskId -> trainingRepo.addTask(trainingId, taskId) }
        }
    }

    private suspend fun seedRivalDossiers(
        analysisDao: RivalAnalysisDao,
        linkDao: RivalLinkDao,
        rivalRepo: RivalRepository,
        clubs: List<OpponentClub>
    ) {
        val rivalA = clubs.firstOrNull { it.name.contains("Rival A") }
        val rivalB = clubs.firstOrNull { it.name.contains("Rival B") }
        if (rivalA != null && analysisDao.getByClubIncludingDeleted(rivalA.id) == null) {
            rivalRepo.saveAnalysis(
                RivalAnalysis(
                    opponentClubId = rivalA.id,
                    usualSystem = "1-4-3-3",
                    variants = "1-4-2-3-1",
                    buildUp = "Centrales abiertos y pivote bajando",
                    progression = "Interior recibiendo entre líneas",
                    finalThird = "Centros desde banda y llegada del 9",
                    highPress = "Salta extremo sobre lateral",
                    midBlock = "4-1-4-1 compacto",
                    lowBlock = "Líneas juntas en 16m",
                    transAttackToDefense = "Falta táctica del pivote si pierden cerca",
                    transDefenseToAttack = "Buscan rápido al extremo derecho",
                    cornersOffensive = "Primer palo + remate atrasado",
                    cornersDefensive = "Mixto: 2 en palos y resto zonal",
                    setPieces = "Falta lateral: 4 en el área",
                    strengths = "Juego aéreo y transiciones",
                    weaknesses = "Espacio a espalda de laterales",
                    keyPlayers = "Nº9 fuerte de espaldas",
                    generalNotes = "Dossier DEMO DEV. Se puede editar o eliminar."
                )
            )
        }
        if (rivalB != null && analysisDao.getByClubIncludingDeleted(rivalB.id) == null) {
            rivalRepo.saveAnalysis(
                RivalAnalysis(
                    opponentClubId = rivalB.id,
                    usualSystem = "1-4-4-2",
                    variants = "1-4-4-2 en rombo",
                    buildUp = "Salida corta con laterales altos",
                    highPress = "Presión 2 vs 2 sobre centrales",
                    midBlock = "4-4-2 plano",
                    transDefenseToAttack = "Pase directo al 9 de movimiento",
                    strengths = "Duelos y segundo balón",
                    weaknesses = "Costados si el interior no recupera",
                    keyPlayers = "Nº10 entre líneas — DEMO",
                    generalNotes = "Dossier DEMO DEV B."
                )
            )
        }
        clubs.take(2).forEach { club ->
            val existingLinks = linkDao.getAllOnce().filter { it.opponentClubId == club.id }
            // Relaciones demo solo en el alta: si ya hay enlaces (también tombstoneados), no añadir más.
            if (existingLinks.isNotEmpty()) return@forEach
            rivalRepo.addLink(
                RivalLink(
                    opponentClubId = club.id,
                    type = RivalLinkType.RFAF,
                    label = DevSeedMarkers.labeled("Ficha RFAF DEMO"),
                    url = "https://example.com/dev/rfaf"
                )
            )
            rivalRepo.addLink(
                RivalLink(
                    opponentClubId = club.id,
                    type = RivalLinkType.YOUTUBE,
                    label = DevSeedMarkers.labeled("Vídeo DEMO"),
                    url = "https://example.com/dev/youtube"
                )
            )
        }
    }

    private suspend fun seedRivalSquads(
        playerDao: OpponentPlayerDao,
        rivalRepo: RivalRepository,
        clubs: List<OpponentClub>
    ) {
        val specs = listOf(
            "Rival A" to listOf(
                "Antonio Pérez", "Mario López", "Carlos Ruiz", "Iván Martín", "Sergio García", "Pablo Romero"
            ),
            "Rival B" to listOf(
                "Javier Ortega", "Luis Navarro", "Diego Castro", "Hugo Molina", "Álvaro Rueda"
            )
        )
        specs.forEach { (fragment, names) ->
            val club = clubs.firstOrNull { it.name.contains(fragment) } ?: return@forEach
            val existing = playerDao.getAllOnce().filter { it.opponentClubId == club.id }
            if (existing.isNotEmpty()) return@forEach
            names.forEach { name ->
                rivalRepo.addPlayer(OpponentPlayer(opponentClubId = club.id, name = name))
            }
        }
    }

    private suspend fun seedOwnSquad(
        playerDao: PlayerDao,
        playerRepo: PlayerRepositoryImpl,
        teamId: Int,
        teamName: String
    ) {
        val existing = playerDao.getAllOnce().filter { it.teamId == teamId }
        if (!canSeedOwnSquad(teamName, existing)) return
        // Alta inicial: si ya hay filas (vivas o tombstone), no añadir nombres que falten.
        if (existing.isNotEmpty()) return
        ownSquadSpecs().forEach { spec ->
            val labeled = DevSeedMarkers.labeled(spec.name)
            if (existing.any { it.name == labeled }) return@forEach
            playerRepo.addPlayer(
                Player(
                    teamId = teamId,
                    name = labeled,
                    position = spec.position,
                    jerseyNumber = spec.number,
                    laterality = spec.laterality,
                    observations = "Dato de prueba DEV. Se puede editar o eliminar."
                )
            )
        }
    }

    private suspend fun seedDemoCallupIfEmpty(
        stores: DevSeedStores,
        teamId: Int,
        matchId: Int
    ) {
        val existingRows = stores.matchDao.getAllMatchPlayersOnce().filter { it.matchId == matchId }
        if (existingRows.isNotEmpty()) return
        val players = stores.ownPlayerDao.getAllByTeamOnce(teamId)
            .filter { it.isActive && DevSeedMarkers.isDemo(it.name) }
            .sortedBy { it.jerseyNumber }
        if (players.size < 11) return
        players.forEach { player ->
            val status = when (player.jerseyNumber) {
                in STARTER_NUMBERS -> CallupStatus.TITULAR
                in BENCH_NUMBERS -> CallupStatus.SUPLENTE
                else -> return@forEach
            }
            stores.matchRepo.setPlayerCallup(matchId, player.id, status)
        }
    }

    private fun isDemoMatch(rival: String, notes: String): Boolean =
        DevSeedMarkers.isDemo(rival) || DevSeedMarkers.isDemo(notes)

    private suspend fun seedBoards(
        boardDao: BoardDao,
        boardRepo: BoardRepository,
        taskDao: TaskDao,
        taskRepo: TaskRepositoryImpl,
        teamId: Int
    ) {
        val existing = boardDao.getAllOnce().filter { it.teamId == teamId }
        if (existing.any { DevSeedMarkers.isDemo(it.name) }) return
        val specs = listOf(
            BoardSpec(
                name = "Salida de balón 3+2",
                linkMatchingTask = true,
                scene = demoScene(
                    BoardObject("dev-salida-b1", BoardObjectType.BLUE_PLAYER, 0.22f, 0.78f, number = "1"),
                    BoardObject("dev-salida-b2", BoardObjectType.BLUE_PLAYER, 0.38f, 0.62f, number = "4"),
                    BoardObject("dev-salida-b3", BoardObjectType.BLUE_PLAYER, 0.62f, 0.62f, number = "5"),
                    BoardObject("dev-salida-r1", BoardObjectType.RED_PLAYER, 0.35f, 0.40f, number = "9"),
                    BoardObject("dev-salida-r2", BoardObjectType.RED_PLAYER, 0.65f, 0.40f, number = "11"),
                    BoardObject("dev-salida-ball", BoardObjectType.BALL, 0.50f, 0.72f)
                )
            ),
            BoardSpec(
                name = "Presión tras pérdida",
                scene = demoScene(
                    BoardObject("dev-presion-b1", BoardObjectType.BLUE_PLAYER, 0.30f, 0.45f, number = "6"),
                    BoardObject("dev-presion-b2", BoardObjectType.BLUE_PLAYER, 0.50f, 0.38f, number = "8"),
                    BoardObject("dev-presion-b3", BoardObjectType.BLUE_PLAYER, 0.70f, 0.45f, number = "10"),
                    BoardObject("dev-presion-r1", BoardObjectType.RED_PLAYER, 0.48f, 0.28f, number = "4"),
                    BoardObject("dev-presion-cone", BoardObjectType.CONE, 0.50f, 0.55f),
                    BoardObject("dev-presion-arrow", BoardObjectType.ARROW, 0.50f, 0.42f, points = listOf(
                        BoardNormPoint(0.50f, 0.50f),
                        BoardNormPoint(0.50f, 0.30f)
                    ))
                )
            ),
            BoardSpec(
                name = "Córner ofensivo",
                scene = demoScene(
                    BoardObject("dev-corner-b1", BoardObjectType.BLUE_PLAYER, 0.88f, 0.12f, number = "7"),
                    BoardObject("dev-corner-b2", BoardObjectType.BLUE_PLAYER, 0.72f, 0.22f, number = "9"),
                    BoardObject("dev-corner-b3", BoardObjectType.BLUE_PLAYER, 0.60f, 0.18f, number = "10"),
                    BoardObject("dev-corner-r1", BoardObjectType.RED_PLAYER, 0.78f, 0.20f, number = "5"),
                    BoardObject("dev-corner-goal", BoardObjectType.GOAL, 0.50f, 0.06f),
                    BoardObject("dev-corner-ball", BoardObjectType.BALL, 0.92f, 0.08f)
                )
            )
        )
        specs.forEach { spec ->
            val labeled = DevSeedMarkers.labeled(spec.name)
            if (existing.any { it.name == labeled }) return@forEach
            val id = boardRepo.add(
                Board(teamId = teamId, name = labeled, sceneJson = spec.scene)
            )
            if (!spec.linkMatchingTask) return@forEach
            val created = boardDao.getByIdOnce(id) ?: return@forEach
            val task = taskDao.getAllOnce().firstOrNull {
                it.teamId == teamId && it.name == labeled && it.deletedAt == null
            } ?: return@forEach
            if (task.boardSyncId.isNullOrBlank()) {
                taskRepo.setBoardSyncId(task.id, created.syncId)
            }
        }
    }

    private fun demoScene(vararg objects: BoardObject): String =
        BoardScene(objects = objects.toList()).toJson()

    private suspend fun occupiedDays(
        matchDao: MatchDao,
        fixtureDao: SeasonFixtureDao,
        trainingDao: TrainingDao,
        teamId: Int
    ): MutableSet<Long> {
        val days = mutableSetOf<Long>()
        matchDao.getAllMatchesOnce()
            .filter { it.teamId == teamId && it.deletedAt == null }
            .forEach { match ->
                (match.dateEpochDay ?: CalendarDate.toEpochDay(match.date))?.let(days::add)
            }
        fixtureDao.getAllOnce()
            .filter { it.teamId == teamId && it.deletedAt == null }
            .forEach { fixture ->
                (fixture.dateEpochDay ?: CalendarDate.toEpochDay(fixture.date))?.let(days::add)
            }
        trainingDao.getAllOnce()
            .filter { it.teamId == teamId && it.deletedAt == null }
            .forEach { days += it.dateEpochDay }
        return days
    }

    private data class TaskSpec(
        val name: String,
        val objective: String,
        val players: Int,
        val minutes: Int
    )

    private data class ClubSpec(
        val name: String,
        val shortName: String,
        val stadium: String,
        val kitColors: String
    )

    private data class BoardSpec(
        val name: String,
        val scene: String,
        val linkMatchingTask: Boolean = false
    )

    private data class OwnPlayerSpec(
        val name: String,
        val number: Int,
        val position: PlayerPosition,
        val laterality: Laterality = Laterality.DERECHA
    )

    private fun ownSquadSpecs(): List<OwnPlayerSpec> = listOf(
        OwnPlayerSpec("Álvaro Martín", 1, PlayerPosition.PORTERO),
        OwnPlayerSpec("Hugo Romero", 13, PlayerPosition.PORTERO),
        OwnPlayerSpec("Pablo García", 2, PlayerPosition.LATERAL_DERECHO),
        OwnPlayerSpec("Sergio Molina", 3, PlayerPosition.LATERAL_IZQUIERDO, Laterality.IZQUIERDA),
        OwnPlayerSpec("Mario López", 4, PlayerPosition.CENTRAL_DERECHO),
        OwnPlayerSpec("Carlos Ruiz", 5, PlayerPosition.CENTRAL_IZQUIERDO, Laterality.IZQUIERDA),
        OwnPlayerSpec("Daniel Navarro", 12, PlayerPosition.LATERAL_DERECHO),
        OwnPlayerSpec("Iván Torres", 15, PlayerPosition.CENTRAL_IZQUIERDO),
        OwnPlayerSpec("Alejandro Vega", 6, PlayerPosition.MEDIOCENTRO_DEFENSIVO),
        OwnPlayerSpec("Adrián Pérez", 8, PlayerPosition.MEDIOCENTRO_DEFENSIVO),
        OwnPlayerSpec("Marcos Jiménez", 10, PlayerPosition.MEDIOCENTRO_OFENSIVO),
        OwnPlayerSpec("Javier Moreno", 14, PlayerPosition.INTERIOR_DERECHO),
        OwnPlayerSpec("Lucas Sánchez", 16, PlayerPosition.INTERIOR_IZQUIERDO, Laterality.IZQUIERDA),
        OwnPlayerSpec("Diego Martín", 18, PlayerPosition.MEDIOCENTRO_OFENSIVO),
        OwnPlayerSpec("Antonio Romero", 7, PlayerPosition.EXTREMO_DERECHO),
        OwnPlayerSpec("Miguel García", 9, PlayerPosition.DELANTERO),
        OwnPlayerSpec("Álvaro Ruiz", 11, PlayerPosition.EXTREMO_IZQUIERDO, Laterality.IZQUIERDA),
        OwnPlayerSpec("Hugo Molina", 17, PlayerPosition.EXTREMO_DERECHO),
        OwnPlayerSpec("Pablo Torres", 19, PlayerPosition.DELANTERO),
        OwnPlayerSpec("Sergio López", 20, PlayerPosition.DELANTERO)
    )

    private fun com.luis.alhendinfc.data.local.OpponentClubEntity.toSeedClub() = OpponentClub(
        id = id,
        teamId = teamId,
        name = name,
        shortName = shortName,
        stadium = stadium,
        kitColors = kitColors,
        sortOrder = sortOrder,
        syncId = syncId
    )
}
