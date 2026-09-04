package com.luis.alhendinfc.ui.live

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.Formation
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.domain.model.assignPlayersToFormation
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import com.luis.alhendinfc.domain.repository.MatchRepositoryImpl
import com.luis.alhendinfc.domain.repository.PlayerRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveMatchUiState(
    val period: Int = 1,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val teamGoals: Int = 0,
    val rivalGoals: Int = 0,
    val ready: Boolean = false,
    val showJerseyNumbers: Boolean = true,
    val showStarterTime: Boolean = true,
    /** segundos acumulados en campo por jugador */
    val secondsOnField: Map<Int, Int> = emptyMap(),
    /** posición relativa en campo (0..1) por jugador */
    val fieldPositions: Map<Int, Offset> = emptyMap(),
    val lastFeedback: String? = null
)

class LiveMatchViewModel(
    private val matchRepository: MatchRepositoryImpl,
    private val playerRepository: PlayerRepositoryImpl,
    private val customStatRepository: CustomStatTypeRepositoryImpl,
    private val matchId: Int,
    private val teamId: Int
) : ViewModel() {

    val match: StateFlow<Match?> = matchRepository.getMatchById(matchId)
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

    private val _ui = MutableStateFlow(LiveMatchUiState())
    val ui: StateFlow<LiveMatchUiState> = _ui.asStateFlow()

    private var tickerJob: Job? = null
    private var started = false
    private var positionsInitialized = false

    init {
        viewModelScope.launch {
            customStatRepository.ensureSampleCustomStats(teamId)
        }
        viewModelScope.launch {
            val m = match.filterNotNull().first()
            if (!started && m.status != MatchStatus.FINISHED) {
                started = true
                if (m.status == MatchStatus.OPEN) {
                    matchRepository.startLiveMatch(matchId)
                    matchRepository.updateMatch(
                        m.copy(status = MatchStatus.LIVE, homeScore = 0, awayScore = 0)
                    )
                    _ui.update { it.copy(teamGoals = 0, rivalGoals = 0, ready = true) }
                } else {
                    val teamGoals = if (m.isHome) (m.homeScore ?: 0) else (m.awayScore ?: 0)
                    val rivalGoals = if (m.isHome) (m.awayScore ?: 0) else (m.homeScore ?: 0)
                    _ui.update {
                        it.copy(teamGoals = teamGoals, rivalGoals = rivalGoals, ready = true)
                    }
                }
            }
        }

        viewModelScope.launch {
            events.collect { list ->
                val teamGoals = list.count { it.type == StatisticType.GOAL }
                val rivalGoals = list.count { it.type == StatisticType.RIVAL_GOAL }
                _ui.update { it.copy(teamGoals = teamGoals, rivalGoals = rivalGoals) }
                syncScore(teamGoals, rivalGoals)
            }
        }

        viewModelScope.launch {
            combine(matchPlayers, teamPlayers, match) { mps, players, m ->
                Triple(mps, players, m)
            }.collect { (mps, players, m) ->
                if (!positionsInitialized && m != null && mps.any { it.isOnField }) {
                    initFieldPositions(m, mps, players)
                    positionsInitialized = true
                }
            }
        }
    }

    private fun initFieldPositions(m: Match, mps: List<MatchPlayer>, players: List<Player>) {
        val formation = Formation.fromLabel(m.formation.ifBlank { Formation.F_4_3_3.label })
        val onField = players.filter { p -> mps.any { it.playerId == p.id && it.isOnField } }
        val assigned = assignPlayersToFormation(onField, formation)
        val map = mutableMapOf<Int, Offset>()
        assigned.forEach { (slot, player) ->
            if (player != null) {
                map[player.id] = Offset(slot.x, slot.y)
            }
        }
        // Portero / fallbacks for any on-field not assigned
        onField.forEach { p ->
            if (p.id !in map) {
                map[p.id] = Offset(p.position.fieldX, p.position.fieldY)
            }
        }
        _ui.update { it.copy(fieldPositions = map) }
    }

    fun toggleTimer() {
        if (_ui.value.isRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (_ui.value.isRunning) return
        _ui.update { it.copy(isRunning = true) }
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val maxSeconds = (match.value?.durationPerPart ?: 45) * 60
                val onFieldIds = matchPlayers.value.filter { it.isOnField }.map { it.playerId }.toSet()
                val sentOffIds = sentOffPlayerIds()
                val next = _ui.value.elapsedSeconds + 1
                val newTimes = _ui.value.secondsOnField.toMutableMap()
                onFieldIds.forEach { id ->
                    if (id !in sentOffIds) {
                        newTimes[id] = (newTimes[id] ?: 0) + 1
                    }
                }
                if (next >= maxSeconds) {
                    _ui.update {
                        it.copy(
                            elapsedSeconds = maxSeconds,
                            isRunning = false,
                            secondsOnField = newTimes
                        )
                    }
                    break
                } else {
                    _ui.update {
                        it.copy(elapsedSeconds = next, secondsOnField = newTimes)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        _ui.update { it.copy(isRunning = false) }
    }

    fun nextPeriod() {
        val maxParts = match.value?.numParts ?: 2
        if (_ui.value.period >= maxParts) return
        pauseTimer()
        _ui.update {
            it.copy(period = it.period + 1, elapsedSeconds = 0, isRunning = false)
        }
    }

    fun currentMinute(): Int = _ui.value.elapsedSeconds / 60

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
        _ui.update { state ->
            val positions = state.fieldPositions.toMutableMap()
            val origin = positions[playerId] ?: return@update state

            // Solo válido: soltar encima de otro → intercambiar.
            // Si no hay nadie debajo, vuelve a su posición original.
            val swapWithId = positions
                .asSequence()
                .filter { it.key != playerId }
                .minByOrNull { distance(it.value, drop) }
                ?.takeIf { distance(it.value, drop) < SWAP_DISTANCE }
                ?.key

            if (swapWithId != null) {
                val otherPos = positions[swapWithId] ?: return@update state
                positions[playerId] = otherPos
                positions[swapWithId] = origin
                state.copy(fieldPositions = positions)
            } else {
                // Forzar recompose al origen (por si la UI tenía un displayPos temporal)
                state.copy(fieldPositions = positions + (playerId to origin))
            }
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
            val prevYellows = if (playerId != null) {
                events.value.count {
                    it.playerId == playerId && it.type == StatisticType.YELLOW_CARD
                }
            } else 0
            val alreadySentOff = playerId != null && isSentOff(playerId)

            // Máximo 2 amarillas; expulsado no recibe más amarillas
            if (type == StatisticType.YELLOW_CARD && (prevYellows >= 2 || alreadySentOff)) {
                _ui.update {
                    it.copy(lastFeedback = "Ya tiene 2 amarillas (roja)")
                }
                return@launch
            }
            // No acumular varias rojas directas
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
            // Roja / 2ª amarilla: permanece en campo; el cronómetro deja de contar
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
            val outPos = _ui.value.fieldPositions[playerOutId]
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
            _ui.update { state ->
                val positions = state.fieldPositions.toMutableMap()
                positions.remove(playerOutId)
                val desired = outPos
                    ?: teamPlayers.value.firstOrNull { it.id == playerInId }
                        ?.let { Offset(it.position.fieldX, it.position.fieldY) }
                    ?: Offset(0.5f, 0.5f)
                positions[playerInId] = clampField(desired)
                state.copy(
                    fieldPositions = positions,
                    lastFeedback = "${currentMinute()}' · Cambio · $outName → $inName"
                )
            }
        }
    }

    fun addCustomEvent(typeCode: String, playerId: Int?) {
        viewModelScope.launch {
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
        event.type?.label
            ?: customStatTypes.value.firstOrNull { it.code == event.typeCode }?.label
            ?: event.typeCode

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
                // Restaurar posiciones aproximadas del cambio
                val outId = last.playerId
                val inId = last.relatedPlayerId
                if (outId != null && inId != null) {
                    _ui.update { state ->
                        val positions = state.fieldPositions.toMutableMap()
                        val inPos = positions.remove(inId)
                        if (inPos != null) positions[outId] = inPos
                        state.copy(fieldPositions = positions)
                    }
                }
            }
            matchRepository.deleteEvent(last.id)
            _ui.update { it.copy(lastFeedback = "Evento deshecho") }
        }
    }

    fun finishMatch(onDone: () -> Unit) {
        viewModelScope.launch {
            pauseTimer()
            val m = match.value ?: return@launch
            val finished = if (m.isHome) {
                m.copy(
                    status = MatchStatus.FINISHED,
                    homeScore = _ui.value.teamGoals,
                    awayScore = _ui.value.rivalGoals
                )
            } else {
                m.copy(
                    status = MatchStatus.FINISHED,
                    homeScore = _ui.value.rivalGoals,
                    awayScore = _ui.value.teamGoals
                )
            }
            matchRepository.finishMatch(finished)
            onDone()
        }
    }

    private suspend fun syncScore(teamGoals: Int, rivalGoals: Int) {
        val m = match.value ?: return
        if (m.status == MatchStatus.FINISHED) return
        val updated = if (m.isHome) {
            m.copy(status = MatchStatus.LIVE, homeScore = teamGoals, awayScore = rivalGoals)
        } else {
            m.copy(status = MatchStatus.LIVE, homeScore = rivalGoals, awayScore = teamGoals)
        }
        matchRepository.updateMatch(updated)
    }

    companion object {
        /** Distancia relativa (0..1) para considerar “soltado encima” e intercambiar. */
        private const val SWAP_DISTANCE = 0.12f

        fun factory(context: Context, matchId: Int, teamId: Int) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context.applicationContext)
                    @Suppress("UNCHECKED_CAST")
                    return LiveMatchViewModel(
                        MatchRepositoryImpl(db.matchDao(), db.matchEventDao()),
                        PlayerRepositoryImpl(db.playerDao(), db.matchDao()),
                        CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                        matchId,
                        teamId
                    ) as T
                }
            }
    }
}
