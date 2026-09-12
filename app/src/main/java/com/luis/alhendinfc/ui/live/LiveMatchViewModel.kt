package com.luis.alhendinfc.ui.live

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.EventLabels
import com.luis.alhendinfc.domain.model.Formation
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.model.assignPlayersToFormation
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LiveMatchUiState(
    val period: Int = 1,
    val teamGoals: Int = 0,
    val rivalGoals: Int = 0,
    val ready: Boolean = false,
    val showJerseyNumbers: Boolean = true,
    val showStarterTime: Boolean = true,
    val lastFeedback: String? = null
)

/** Reloj separado: evita recomponer todo el campo cada segundo. */
data class LiveClockState(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false
)

class LiveMatchViewModel(
    private val matchRepository: MatchRepositoryImpl,
    private val playerRepository: PlayerRepositoryImpl,
    private val customStatRepository: CustomStatTypeRepositoryImpl,
    private val attachmentRepository: AttachmentRepository,
    private val calendarRepository: SeasonCalendarRepository,
    private val matchId: Int,
    private val teamId: Int
) : ViewModel() {

    /**
     * Metadatos del partido para la UI. Ignora ticks de cronómetro / JSON de posiciones
     * para no recomponer toda la pantalla al persistir el reloj.
     */
    val match: StateFlow<Match?> = matchRepository.getMatchById(matchId)
        .distinctUntilChanged(::sameMatchForUi)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matchPlayers: StateFlow<List<MatchPlayer>> = matchRepository.getMatchPlayers(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamPlayers: StateFlow<List<Player>> = playerRepository.getPlayersByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<MatchEvent>> = matchRepository.getMatchEvents(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customStatTypes: StateFlow<List<CustomStatType>> =
        customStatRepository.getActiveByTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rivalShieldPath: StateFlow<String?> = combine(
        match,
        calendarRepository.getClubs(teamId),
        attachmentRepository.getActiveByType(AttachmentParentType.OPPONENT_SHIELD)
    ) { current, clubs, shields ->
        val bySync = SharedMedia.byParentSyncId(shields)
        val club = current?.opponentClubId?.let { id -> clubs.firstOrNull { it.id == id } }
        SharedMedia.rivalDisplayPath(
            opponentClubId = current?.opponentClubId,
            clubLegacyUri = club?.shieldUri,
            matchLegacyUri = current?.rivalShieldUri,
            clubShared = club?.syncId?.let { bySync[it] }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val teamShields: StateFlow<Map<String, Attachment>> =
        attachmentRepository.getActiveByType(AttachmentParentType.TEAM_SHIELD)
            .map { SharedMedia.byParentSyncId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _ui = MutableStateFlow(LiveMatchUiState())
    val ui: StateFlow<LiveMatchUiState> = _ui.asStateFlow()

    private val _clock = MutableStateFlow(LiveClockState())
    val clock: StateFlow<LiveClockState> = _clock.asStateFlow()

    private val _fieldSeconds = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val fieldSeconds: StateFlow<Map<Int, Int>> = _fieldSeconds.asStateFlow()

    /** Posiciones aparte: un gol/feedback no recompone el campo entero. */
    private val _fieldPositions = MutableStateFlow<Map<Int, Offset>>(emptyMap())
    val fieldPositions: StateFlow<Map<Int, Offset>> = _fieldPositions.asStateFlow()

    private var tickerJob: Job? = null
    private var persistJob: Job? = null
    private var started = false
    private var positionsInitialized = false
    private var lastSyncedTeamGoals = -1
    private var lastSyncedRivalGoals = -1
    /** Ancla de pared mientras el cronómetro corre: elapsed ≈ (now - anchor) / 1000. */
    private var clockAnchorWallMs: Long = 0L
    /** Una vez true, no se escriben campos de live (evita races al terminar). */
    private var finishing = false

    init {
        viewModelScope.launch {
            val m = match.filterNotNull().first()
            if (!started && m.status != MatchStatus.FINISHED) {
                started = true
                if (m.status == MatchStatus.OPEN) {
                    matchRepository.prepareLiveField(matchId)
                    _ui.update { it.copy(teamGoals = 0, rivalGoals = 0, period = 1, ready = true) }
                    _clock.value = LiveClockState(elapsedSeconds = 0, isRunning = false)
                    _fieldSeconds.value = emptyMap()
                } else {
                    val teamGoals = if (m.isHome) (m.homeScore ?: 0) else (m.awayScore ?: 0)
                    val rivalGoals = if (m.isHome) (m.awayScore ?: 0) else (m.homeScore ?: 0)
                    _ui.update {
                        it.copy(
                            teamGoals = teamGoals,
                            rivalGoals = rivalGoals,
                            period = m.livePeriod.coerceAtLeast(1),
                            ready = true
                        )
                    }
                    restoreClockFromMatch(m)
                }
            }
        }

        viewModelScope.launch {
            events.collect { list ->
                val teamGoals = list.count { it.type == StatisticType.GOAL }
                val rivalGoals = list.count { it.type == StatisticType.RIVAL_GOAL }
                val current = _ui.value
                if (current.teamGoals != teamGoals || current.rivalGoals != rivalGoals) {
                    _ui.update { it.copy(teamGoals = teamGoals, rivalGoals = rivalGoals) }
                }
                if (teamGoals != lastSyncedTeamGoals || rivalGoals != lastSyncedRivalGoals) {
                    lastSyncedTeamGoals = teamGoals
                    lastSyncedRivalGoals = rivalGoals
                    syncScore(teamGoals, rivalGoals)
                }
            }
        }

        viewModelScope.launch {
            combine(matchPlayers, teamPlayers, match) { mps, players, m ->
                Triple(mps, players, m)
            }.collect { (mps, players, m) ->
                if (m == null || players.isEmpty()) return@collect
                val onFieldCount = mps.count { it.isOnField }
                if (onFieldCount == 0) return@collect

                if (!positionsInitialized) {
                    val saved = m.decodeFieldPositions()
                        .mapValues { Offset(it.value.first, it.value.second) }
                    if (saved.isNotEmpty()) {
                        applySavedOrFormationPositions(m, mps, players, saved)
                    } else {
                        initFieldPositions(m, mps, players)
                    }
                    positionsInitialized = true
                } else {
                    val missing = mps
                        .filter { it.isOnField && it.playerId !in _fieldPositions.value }
                        .mapNotNull { mp -> players.firstOrNull { it.id == mp.playerId } }
                    if (missing.isNotEmpty()) {
                        placeMissingPlayers(m, missing)
                    }
                }
            }
        }
    }

    /**
     * Si el cronómetro estaba en marcha al salir, avanza con el tiempo real y sigue corriendo.
     */
    private suspend fun restoreClockFromMatch(m: Match) {
        val maxSeconds = m.durationPerPart.coerceAtLeast(1) * 60
        val savedElapsed = m.liveElapsedSeconds.coerceAtLeast(0)
        val shouldContinue = m.liveClockRunning && m.liveClockAnchorWallMs > 0L

        if (shouldContinue) {
            val wallElapsed = ((System.currentTimeMillis() - m.liveClockAnchorWallMs) / 1000L)
                .toInt()
                .coerceIn(0, maxSeconds)
            val delta = (wallElapsed - savedElapsed).coerceAtLeast(0)
            val fieldMap = m.decodeFieldSeconds().toMutableMap()
            if (delta > 0) {
                val mps = matchPlayers.first()
                val onFieldIds = mps.filter { it.isOnField }.map { it.playerId }.toSet()
                val list = events.value
                val sentOff = onFieldIds.filter { id ->
                    val pe = list.filter { it.playerId == id }
                    pe.any { it.type == StatisticType.RED_CARD } ||
                        pe.count { it.type == StatisticType.YELLOW_CARD } >= 2
                }.toSet()
                onFieldIds.forEach { id ->
                    if (id !in sentOff) {
                        fieldMap[id] = (fieldMap[id] ?: 0) + delta
                    }
                }
            }
            _fieldSeconds.value = fieldMap
            clockAnchorWallMs = m.liveClockAnchorWallMs
            _clock.value = LiveClockState(elapsedSeconds = wallElapsed, isRunning = false)
            if (wallElapsed >= maxSeconds) {
                persistLiveClock(running = false)
            } else {
                startTimer()
            }
        } else {
            clockAnchorWallMs = 0L
            _clock.value = LiveClockState(elapsedSeconds = savedElapsed, isRunning = false)
            _fieldSeconds.value = m.decodeFieldSeconds()
        }
    }

    override fun onCleared() {
        val wasRunning = _clock.value.isRunning
        tickerJob?.cancel()
        persistJob?.cancel()
        if (!finishing) {
            // No bloquear el hilo principal al salir del partido.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                withContext(NonCancellable) {
                    persistLiveClock(running = wasRunning)
                }
            }
        }
        super.onCleared()
    }

    private fun initFieldPositions(m: Match, mps: List<MatchPlayer>, players: List<Player>) {
        applySavedOrFormationPositions(m, mps, players, emptyMap())
    }

    private fun applySavedOrFormationPositions(
        m: Match,
        mps: List<MatchPlayer>,
        players: List<Player>,
        saved: Map<Int, Offset>
    ) {
        val formation = Formation.fromLabel(m.formation.ifBlank { Formation.F_4_3_3.label })
        val onField = players
            .filter { p -> mps.any { it.playerId == p.id && it.isOnField } }
            .distinctBy { it.id }
        val map = mutableMapOf<Int, Offset>()

        onField.forEach { p ->
            saved[p.id]?.let { map[p.id] = clampField(it) }
        }

        val needFormation = onField.filter { it.id !in map }
        if (needFormation.isNotEmpty()) {
            val usedSlots = mutableSetOf<Int>()
            formation.slots.forEachIndexed { index, slot ->
                val taken = map.values.any { pos ->
                    val dx = pos.x - slot.x
                    val dy = pos.y - slot.y
                    dx * dx + dy * dy < 0.0025f
                }
                if (taken) usedSlots.add(index)
            }
            val assigned = assignPlayersToFormation(needFormation, formation)
            assigned.forEachIndexed { index, (slot, player) ->
                if (player != null && player.id !in map && index !in usedSlots) {
                    map[player.id] = Offset(slot.x, slot.y)
                    usedSlots.add(index)
                }
            }
            val stillMissing = onField.filter { it.id !in map }
            stillMissing.forEachIndexed { i, p ->
                val free = formation.slots.indices.firstOrNull { it !in usedSlots }
                if (free != null) {
                    val slot = formation.slots[free]
                    map[p.id] = Offset(slot.x, slot.y)
                    usedSlots.add(free)
                } else {
                    map[p.id] = clampField(
                        Offset(p.position.fieldX + i * 0.04f, p.position.fieldY)
                    )
                }
            }
        }

        separateOverlaps(map)
        _fieldPositions.value = map
    }

    private fun placeMissingPlayers(m: Match, missing: List<Player>) {
        val formation = Formation.fromLabel(m.formation.ifBlank { Formation.F_4_3_3.label })
        val map = _fieldPositions.value.toMutableMap()
        val usedSlots = mutableSetOf<Int>()
        formation.slots.forEachIndexed { index, slot ->
            val taken = map.values.any { pos ->
                val dx = pos.x - slot.x
                val dy = pos.y - slot.y
                dx * dx + dy * dy < 0.0025f
            }
            if (taken) usedSlots.add(index)
        }
        missing.distinctBy { it.id }.forEach { p ->
            if (p.id in map) return@forEach
            val free = formation.slots.indices.firstOrNull { it !in usedSlots }
            if (free != null) {
                val slot = formation.slots[free]
                map[p.id] = Offset(slot.x, slot.y)
                usedSlots.add(free)
            } else {
                map[p.id] = clampField(Offset(p.position.fieldX, p.position.fieldY))
            }
        }
        separateOverlaps(map)
        _fieldPositions.value = map
    }

    /** Separa marcadores casi en la misma coordenada (evita el “doble” visual). */
    private fun separateOverlaps(map: MutableMap<Int, Offset>) {
        val ids = map.keys.toList()
        for (i in ids.indices) {
            for (j in i + 1 until ids.size) {
                val a = map[ids[i]] ?: continue
                val b = map[ids[j]] ?: continue
                val dx = a.x - b.x
                val dy = a.y - b.y
                if (dx * dx + dy * dy < 0.0016f) {
                    map[ids[j]] = clampField(Offset(b.x, (b.y + 0.06f).coerceAtMost(0.90f)))
                }
            }
        }
    }

    fun toggleTimer() {
        if (_clock.value.isRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (finishing || _clock.value.isRunning) return
        viewModelScope.launch {
            ensureLive()
            if (finishing || _clock.value.isRunning) return@launch
            val elapsed = _clock.value.elapsedSeconds
            clockAnchorWallMs = System.currentTimeMillis() - elapsed * 1000L
            _clock.update { it.copy(isRunning = true) }
            persistLiveClockAsync(running = true)
            tickerJob?.cancel()
            tickerJob = viewModelScope.launch {
                var ticksSincePersist = 0
                while (true) {
                    delay(1000)
                    val maxSeconds = (match.value?.durationPerPart ?: 45) * 60
                    val onFieldIds = matchPlayers.value.filter { it.isOnField }.map { it.playerId }.toSet()
                    val sentOffIds = sentOffPlayerIds()
                    val fromWall = if (clockAnchorWallMs > 0L) {
                        ((System.currentTimeMillis() - clockAnchorWallMs) / 1000L).toInt()
                    } else {
                        _clock.value.elapsedSeconds + 1
                    }
                    val next = fromWall.coerceAtMost(maxSeconds)
                    val gained = (next - _clock.value.elapsedSeconds).coerceAtLeast(0)
                    if (gained > 0) {
                        val newTimes = _fieldSeconds.value.toMutableMap()
                        onFieldIds.forEach { id ->
                            if (id !in sentOffIds) {
                                newTimes[id] = (newTimes[id] ?: 0) + gained
                            }
                        }
                        _fieldSeconds.value = newTimes
                    }
                    if (next >= maxSeconds) {
                        _clock.value = LiveClockState(elapsedSeconds = maxSeconds, isRunning = false)
                        clockAnchorWallMs = 0L
                        persistLiveClock(running = false)
                        break
                    } else {
                        _clock.value = LiveClockState(elapsedSeconds = next, isRunning = true)
                        ticksSincePersist++
                        if (ticksSincePersist >= 10) {
                            ticksSincePersist = 0
                            persistLiveClock(running = true)
                        }
                    }
                }
            }
        }
    }

    private suspend fun ensureLive() {
        if (finishing) return
        if (match.value?.status == MatchStatus.OPEN) {
            matchRepository.startLiveMatch(matchId)
        }
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        clockAnchorWallMs = 0L
        _clock.update { it.copy(isRunning = false) }
        if (!finishing) persistLiveClockAsync(running = false)
    }

    fun nextPeriod() {
        val maxParts = match.value?.numParts ?: 2
        if (_ui.value.period >= maxParts) return
        pauseTimer()
        clockAnchorWallMs = 0L
        _clock.value = LiveClockState(elapsedSeconds = 0, isRunning = false)
        _ui.update { it.copy(period = it.period + 1) }
        persistLiveClockAsync(running = false)
    }

    private fun persistLiveClockAsync(running: Boolean) {
        if (finishing) return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            persistLiveClock(running)
        }
    }

    private suspend fun persistLiveClock(running: Boolean) {
        if (finishing) return
        val elapsed = _clock.value.elapsedSeconds
        val anchor = if (running) {
            if (clockAnchorWallMs > 0L) clockAnchorWallMs
            else System.currentTimeMillis() - elapsed * 1000L
        } else {
            0L
        }
        if (running) clockAnchorWallMs = anchor
        matchRepository.updateLiveClock(
            matchId = matchId,
            elapsedSeconds = elapsed,
            running = running,
            anchorWallMs = anchor,
            period = _ui.value.period,
            fieldSecondsJson = Match.encodeFieldSeconds(_fieldSeconds.value)
        )
    }

    fun currentMinute(): Int = _clock.value.elapsedSeconds / 60

    fun setShowJerseyNumbers(value: Boolean) {
        _ui.update { it.copy(showJerseyNumbers = value) }
    }

    fun setShowStarterTime(value: Boolean) {
        _ui.update { it.copy(showStarterTime = value) }
    }

    fun clearFeedback() {
        _ui.update { it.copy(lastFeedback = null) }
    }

    fun movePlayerOnField(playerId: Int, x: Float, y: Float) {
        val drop = clampField(Offset(x, y))
        val positions = _fieldPositions.value.toMutableMap()
        val origin = positions[playerId] ?: return

        val swapWithId = positions
            .asSequence()
            .filter { it.key != playerId }
            .minByOrNull { distance(it.value, drop) }
            ?.takeIf { distance(it.value, drop) < SWAP_DISTANCE }
            ?.key

        if (swapWithId != null) {
            val otherPos = positions[swapWithId] ?: return
            positions[playerId] = otherPos
            positions[swapWithId] = origin
            _fieldPositions.value = positions
            persistPositionsAsync(positions)
        } else {
            _fieldPositions.value = positions + (playerId to origin)
        }
    }

    private fun persistPositionsAsync(positions: Map<Int, Offset>) {
        if (finishing) return
        viewModelScope.launch {
            if (finishing) return@launch
            matchRepository.updateFieldPositions(
                matchId,
                Match.encodeFieldPositions(
                    positions.mapValues { it.value.x to it.value.y }
                )
            )
        }
    }

    private fun clampField(pos: Offset): Offset =
        Offset(pos.x.coerceIn(0.06f, 0.94f), pos.y.coerceIn(0.10f, 0.90f))

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun addSimpleEvent(type: StatisticType, playerId: Int?) {
        viewModelScope.launch {
            ensureLive()
            val prevYellows = if (playerId != null) {
                events.value.count {
                    it.playerId == playerId && it.type == StatisticType.YELLOW_CARD
                }
            } else 0
            val alreadySentOff = playerId != null && isSentOff(playerId)

            if (type == StatisticType.YELLOW_CARD && (prevYellows >= 2 || alreadySentOff)) {
                _ui.update {
                    it.copy(lastFeedback = "Ya tiene 2 amarillas (roja)")
                }
                return@launch
            }
            if (type == StatisticType.RED_CARD && playerId != null &&
                events.value.any { it.playerId == playerId && it.type == StatisticType.RED_CARD }
            ) {
                _ui.update { it.copy(lastFeedback = "Ya tiene tarjeta roja") }
                return@launch
            }

            matchRepository.addEvent(
                MatchEvent.builtin(
                    matchId = matchId,
                    type = type,
                    playerId = playerId,
                    minute = currentMinute(),
                    period = _ui.value.period
                )
            )
            val name = playerId?.let { id ->
                teamPlayers.value.firstOrNull { it.id == id }?.let { p ->
                    p.alias.ifBlank { p.name.split(" ").first() }
                }
            } ?: "Rival"
            val feedback = when {
                type == StatisticType.YELLOW_CARD && prevYellows + 1 >= 2 ->
                    "${currentMinute()}' · 2ª amarilla (roja) · $name"
                else ->
                    "${currentMinute()}' · ${type.label} · $name"
            }
            _ui.update { it.copy(lastFeedback = feedback) }
        }
    }

    /** Expulsado: roja directa o doble amarilla (sin evento ROJA extra). */
    fun isSentOff(playerId: Int): Boolean {
        val list = events.value.filter { it.playerId == playerId }
        return list.any { it.type == StatisticType.RED_CARD } ||
            list.count { it.type == StatisticType.YELLOW_CARD } >= 2
    }

    private fun sentOffPlayerIds(): Set<Int> {
        return events.value
            .mapNotNull { it.playerId }
            .distinct()
            .filter { isSentOff(it) }
            .toSet()
    }

    fun addSubstitution(playerOutId: Int, playerInId: Int) {
        viewModelScope.launch {
            ensureLive()
            val outPos = _fieldPositions.value[playerOutId]
            matchRepository.setPlayerOnField(matchId, playerOutId, false)
            matchRepository.setPlayerOnField(matchId, playerInId, true)
            matchRepository.addEvent(
                MatchEvent.builtin(
                    matchId = matchId,
                    type = StatisticType.SUBSTITUTION,
                    playerId = playerOutId,
                    relatedPlayerId = playerInId,
                    minute = currentMinute(),
                    period = _ui.value.period
                )
            )
            val outName = teamPlayers.value.firstOrNull { it.id == playerOutId }
                ?.let { it.alias.ifBlank { it.name.split(" ").first() } } ?: "?"
            val inName = teamPlayers.value.firstOrNull { it.id == playerInId }
                ?.let { it.alias.ifBlank { it.name.split(" ").first() } } ?: "?"
            val positions = _fieldPositions.value.toMutableMap()
            positions.remove(playerOutId)
            val desired = outPos
                ?: teamPlayers.value.firstOrNull { it.id == playerInId }
                    ?.let { Offset(it.position.fieldX, it.position.fieldY) }
                ?: Offset(0.5f, 0.5f)
            positions[playerInId] = clampField(desired)
            _fieldPositions.value = positions
            _ui.update {
                it.copy(lastFeedback = "${currentMinute()}' · Cambio · $outName → $inName")
            }
            persistPositionsAsync(positions)
        }
    }

    fun addCustomEvent(typeCode: String, playerId: Int?) {
        viewModelScope.launch {
            ensureLive()
            val label = customStatTypes.value.firstOrNull { it.code == typeCode }?.label
                ?: typeCode
            matchRepository.addEvent(
                MatchEvent.custom(
                    matchId = matchId,
                    typeCode = typeCode,
                    playerId = playerId,
                    minute = currentMinute(),
                    period = _ui.value.period
                )
            )
            val name = playerId?.let { id ->
                teamPlayers.value.firstOrNull { it.id == id }?.let { p ->
                    p.alias.ifBlank { p.name.split(" ").first() }
                }
            } ?: (match.value?.rival?.ifBlank { null } ?: "Rival")
            _ui.update {
                it.copy(lastFeedback = "${currentMinute()}' · $label · $name")
            }
        }
    }

    fun eventLabel(event: MatchEvent): String =
        EventLabels.resolve(
            event,
            customStatTypes.value.associate { it.code to it.label }
        )

    fun cardCounts(playerId: Int): Pair<Int, Int> {
        val list = events.value.filter { it.playerId == playerId }
        val yellow = list.count { it.type == StatisticType.YELLOW_CARD }
        val redDirect = list.count { it.type == StatisticType.RED_CARD }
        val red = if (redDirect > 0 || yellow >= 2) maxOf(redDirect, 1) else 0
        return yellow to red
    }

    fun undoLastEvent() {
        viewModelScope.launch {
            val last = events.value.lastOrNull() ?: return@launch
            if (last.type == StatisticType.SUBSTITUTION) {
                last.playerId?.let { matchRepository.setPlayerOnField(matchId, it, true) }
                last.relatedPlayerId?.let { matchRepository.setPlayerOnField(matchId, it, false) }
                val outId = last.playerId
                val inId = last.relatedPlayerId
                if (outId != null && inId != null) {
                    val positions = _fieldPositions.value.toMutableMap()
                    val inPos = positions.remove(inId)
                    if (inPos != null) positions[outId] = inPos
                    _fieldPositions.value = positions
                }
            }
            matchRepository.deleteEvent(last.id)
            _ui.update { it.copy(lastFeedback = "Evento deshecho") }
        }
    }

    fun finishMatch(onDone: () -> Unit) {
        viewModelScope.launch {
            if (finishing) return@launch
            finishing = true
            persistJob?.cancel()
            tickerJob?.cancel()
            clockAnchorWallMs = 0L
            _clock.update { it.copy(isRunning = false) }
            val isHome = match.value?.isHome != false
            val homeScore: Int
            val awayScore: Int
            if (isHome) {
                homeScore = _ui.value.teamGoals
                awayScore = _ui.value.rivalGoals
            } else {
                homeScore = _ui.value.rivalGoals
                awayScore = _ui.value.teamGoals
            }
            val positionsJson = Match.encodeFieldPositions(
                _fieldPositions.value.mapValues { it.value.x to it.value.y }
            )
            matchRepository.markMatchFinished(
                matchId = matchId,
                homeScore = homeScore,
                awayScore = awayScore,
                livePeriod = _ui.value.period,
                liveElapsedSeconds = _clock.value.elapsedSeconds,
                fieldSecondsJson = Match.encodeFieldSeconds(_fieldSeconds.value),
                fieldPositionsJson = positionsJson
            )
            onDone()
        }
    }

    private suspend fun syncScore(teamGoals: Int, rivalGoals: Int) {
        if (finishing) return
        val isHome = match.value?.isHome ?: return
        val homeScore: Int
        val awayScore: Int
        if (isHome) {
            homeScore = teamGoals
            awayScore = rivalGoals
        } else {
            homeScore = rivalGoals
            awayScore = teamGoals
        }
        matchRepository.updateLiveScore(matchId, homeScore, awayScore)
    }

    companion object {
        /** Distancia relativa (0..1) para considerar “soltado encima” e intercambiar. */
        private const val SWAP_DISTANCE = 0.12f

        /** Campos de UI: excluye reloj y JSON de posiciones para no reemitir cada tick. */
        private fun sameMatchForUi(a: Match?, b: Match?): Boolean {
            if (a === b) return true
            if (a == null || b == null) return false
            return a.id == b.id &&
                a.status == b.status &&
                a.rival == b.rival &&
                a.stadium == b.stadium &&
                a.date == b.date &&
                a.time == b.time &&
                a.matchday == b.matchday &&
                a.isHome == b.isHome &&
                a.durationPerPart == b.durationPerPart &&
                a.numParts == b.numParts &&
                a.formation == b.formation &&
                a.notes == b.notes &&
                a.homeScore == b.homeScore &&
                a.awayScore == b.awayScore &&
                a.opponentClubId == b.opponentClubId &&
                a.rivalShieldUri == b.rivalShieldUri
        }

        fun factory(context: Context, matchId: Int, teamId: Int) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context.applicationContext)
                    @Suppress("UNCHECKED_CAST")
                    val attachments = AttachmentRepositoryImpl(db.attachmentDao())
                    return LiveMatchViewModel(
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao(), null, db.opponentClubDao()),
                        PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                        CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                        attachments,
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        matchId,
                        teamId
                    ) as T
                }
            }
    }
}
