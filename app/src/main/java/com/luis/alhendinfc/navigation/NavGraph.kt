package com.luis.alhendinfc.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.ui.home.HomeScreen
import com.luis.alhendinfc.ui.live.LiveMatchScreen
import com.luis.alhendinfc.ui.live.LiveMatchViewModel
import com.luis.alhendinfc.ui.matches.MatchListScreen
import com.luis.alhendinfc.ui.matches.MatchSetupScreen
import com.luis.alhendinfc.ui.matches.MatchViewModel
import com.luis.alhendinfc.ui.players.PlayerDetailScreen
import com.luis.alhendinfc.ui.players.PlayerEditDialog
import com.luis.alhendinfc.ui.players.PlayerListScreen
import com.luis.alhendinfc.ui.players.PlayerViewModel
import com.luis.alhendinfc.ui.settings.EventTypesViewModel
import com.luis.alhendinfc.ui.settings.SettingsScreen
import com.luis.alhendinfc.ui.statistics.StatisticsScreen
import com.luis.alhendinfc.ui.statistics.StatisticsViewModel
import com.luis.alhendinfc.ui.team.TeamScreen
import com.luis.alhendinfc.ui.team.TeamViewModel

sealed class AppScreen(val route: String) {
    object Home : AppScreen("home")
    object Team : AppScreen("team")
    object Players : AppScreen("players/{teamId}") {
        fun createRoute(teamId: Int) = "players/$teamId"
    }
    object PlayerDetail : AppScreen("player_detail/{playerId}") {
        fun createRoute(playerId: Int) = "player_detail/$playerId"
    }
    object Matches : AppScreen("matches/{teamId}") {
        fun createRoute(teamId: Int) = "matches/$teamId"
    }
    object LiveMatch : AppScreen("live/{teamId}/{matchId}") {
        fun createRoute(teamId: Int, matchId: Int) = "live/$teamId/$matchId"
    }
    object Statistics : AppScreen("statistics/{teamId}") {
        fun createRoute(teamId: Int) = "statistics/$teamId"
    }
    object Settings : AppScreen("settings/{teamId}") {
        fun createRoute(teamId: Int) = "settings/$teamId"
    }
}

@Composable
fun AlhendinNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModel.factory(context.applicationContext)
    )
    val teams by teamViewModel.teams.collectAsState()
    val selectedTeam by teamViewModel.selectedTeam.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route
    ) {
        composable(AppScreen.Home.route) {
            HomeScreen(
                teams = teams,
                selectedTeam = selectedTeam,
                onNavigateToTeam = { navController.navigate(AppScreen.Team.route) },
                onNavigateToMatches = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Matches.createRoute(teamId))
                },
                onNavigateToStatistics = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Statistics.createRoute(teamId))
                },
                onNavigateToSettings = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Settings.createRoute(teamId))
                },
                onAddTeam = { teamViewModel.addTeam(it) },
                onSelectTeam = { teamViewModel.selectTeam(it) }
            )
        }

        composable(AppScreen.Team.route) {
            val teamId = selectedTeam?.id ?: -1
            val playerViewModel: PlayerViewModel = viewModel(
                key = "team_players_$teamId",
                factory = PlayerViewModel.factory(context.applicationContext, teamId)
            )
            val players by playerViewModel.players.collectAsState()

            TeamScreen(
                team = selectedTeam,
                playerCount = players.size,
                onSaveTeam = { teamViewModel.updateTeam(it) },
                onBack = { navController.popBackStack() },
                onViewPlayers = {
                    if (teamId != -1) {
                        navController.navigate(AppScreen.Players.createRoute(teamId))
                    }
                }
            )
        }

        composable(
            route = AppScreen.Players.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            PlayersRoute(
                teamId = teamId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.PlayerDetail.route,
            arguments = listOf(navArgument("playerId") { type = NavType.IntType })
        ) {
            navController.popBackStack()
        }

        composable(
            route = AppScreen.Matches.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            MatchesRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() },
                onContinueToLive = { matchId ->
                    navController.navigate(AppScreen.LiveMatch.createRoute(teamId, matchId))
                }
            )
        }

        composable(
            route = AppScreen.LiveMatch.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("matchId") { type = NavType.IntType }
            )
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            val matchId = backStack.arguments!!.getInt("matchId")
            LiveMatchRoute(
                teamId = teamId,
                matchId = matchId,
                team = selectedTeam,
                onFinished = {
                    navController.popBackStack(AppScreen.Matches.createRoute(teamId), inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.Statistics.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            StatisticsRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.Settings.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            SettingsRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PlayersRoute(
    teamId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.factory(context.applicationContext, teamId)
    )
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModel.factory(context.applicationContext)
    )

    val players by playerViewModel.players.collectAsState()
    val selectedTeam by teamViewModel.selectedTeam.collectAsState()
    val selectedPlayer by playerViewModel.selectedPlayer.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    if (showAddDialog || editingPlayer != null) {
        PlayerEditDialog(
            currentPlayer = editingPlayer,
            teamId = teamId,
            onConfirm = { player ->
                if (editingPlayer != null) {
                    playerViewModel.updatePlayer(player)
                    playerViewModel.selectPlayer(player)
                } else {
                    playerViewModel.addPlayer(player)
                }
                showAddDialog = false
                editingPlayer = null
            },
            onDismiss = {
                showAddDialog = false
                editingPlayer = null
            }
        )
    }

    if (selectedPlayer != null) {
        PlayerDetailScreen(
            player = selectedPlayer!!,
            team = selectedTeam,
            onEdit = { editingPlayer = selectedPlayer },
            onBack = { playerViewModel.selectPlayer(null) }
        )
    } else {
        PlayerListScreen(
            players = players,
            team = selectedTeam,
            onAddPlayer = { showAddDialog = true },
            onPlayerClick = { player -> playerViewModel.selectPlayer(player) },
            onBack = onBack
        )
    }
}

@Composable
private fun MatchesRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit,
    onContinueToLive: (matchId: Int) -> Unit
) {
    val context = LocalContext.current
    val matchViewModel: MatchViewModel = viewModel(
        key = "matches_$teamId",
        factory = MatchViewModel.factory(context.applicationContext, teamId)
    )

    val matches by matchViewModel.matches.collectAsState()
    val currentMatch by matchViewModel.currentMatch.collectAsState()
    val matchPlayers by matchViewModel.matchPlayers.collectAsState()
    val teamPlayers by matchViewModel.teamPlayers.collectAsState()

    when {
        currentMatch != null -> {
            MatchSetupScreen(
                match = currentMatch!!,
                matchPlayers = matchPlayers,
                teamPlayers = teamPlayers,
                team = team,
                onSave = { matchViewModel.saveMatch(it) },
                onPlayerCallup = { playerId, status -> matchViewModel.setPlayerCallup(playerId, status) },
                onDelete = { matchViewModel.deleteCurrentMatch() },
                onContinue = { saved ->
                    if (saved.status == MatchStatus.FINISHED) return@MatchSetupScreen
                    matchViewModel.saveMatch(saved)
                    onContinueToLive(saved.id)
                    matchViewModel.closeMatch()
                },
                onBack = { matchViewModel.closeMatch() }
            )
        }
        else -> {
            MatchListScreen(
                matches = matches,
                onBack = onBack,
                onCreateNew = { matchViewModel.createNewMatch { } },
                onOpenMatch = { match ->
                    if (match.status == MatchStatus.LIVE || match.status == MatchStatus.FINISHED) {
                        // FINALIZADO: abrir setup en solo lectura más adelante; por ahora setup
                        // EN VIVO: ir directo al cronómetro
                        if (match.status == MatchStatus.LIVE) {
                            onContinueToLive(match.id)
                        } else {
                            matchViewModel.openMatch(match.id)
                        }
                    } else {
                        matchViewModel.openMatch(match.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun LiveMatchRoute(
    teamId: Int,
    matchId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val liveVm: LiveMatchViewModel = viewModel(
        key = "live_$matchId",
        factory = LiveMatchViewModel.factory(context.applicationContext, matchId, teamId)
    )

    val match by liveVm.match.collectAsState()
    val ui by liveVm.ui.collectAsState()
    val events by liveVm.events.collectAsState()
    val matchPlayers by liveVm.matchPlayers.collectAsState()
    val teamPlayers by liveVm.teamPlayers.collectAsState()
    val customStatTypes by liveVm.customStatTypes.collectAsState()

    val onFieldIds = remember(matchPlayers) {
        matchPlayers.filter { it.isOnField }.map { it.playerId }.toSet()
    }
    val onField = remember(onFieldIds, teamPlayers) {
        teamPlayers.filter { it.id in onFieldIds }
    }
    val onBench = remember(matchPlayers, teamPlayers) {
        val benchIds = matchPlayers
            .filter { !it.isOnField && it.callupStatus != com.luis.alhendinfc.domain.model.CallupStatus.NONE }
            .map { it.playerId }
            .toSet()
        teamPlayers.filter { it.id in benchIds }
    }

    val current = match ?: return

    val yellowCards = remember(events) {
        events.filter { it.type == StatisticType.YELLOW_CARD }
            .groupingBy { it.playerId ?: -1 }
            .eachCount()
            .filterKeys { it > 0 }
    }
    val redCards = remember(events, yellowCards) {
        val fromRed = events.filter { it.type == StatisticType.RED_CARD }
            .groupingBy { it.playerId ?: -1 }
            .eachCount()
            .filterKeys { it > 0 }
        // Doble amarilla = roja visual (sin evento ROJA extra)
        val fromDoubleYellow = yellowCards
            .filter { it.value >= 2 }
            .mapValues { 1 }
        (fromRed.keys + fromDoubleYellow.keys).associateWith { id ->
            maxOf(fromRed[id] ?: 0, fromDoubleYellow[id] ?: 0)
        }
    }

    LiveMatchScreen(
        match = current,
        team = team,
        ui = ui,
        events = events,
        customStatTypes = customStatTypes,
        playersOnField = onField,
        playersOnBench = onBench,
        allPlayers = teamPlayers,
        yellowCards = yellowCards,
        redCards = redCards,
        onToggleTimer = liveVm::toggleTimer,
        onNextPeriod = liveVm::nextPeriod,
        onAddEvent = { type, playerId -> liveVm.addSimpleEvent(type, playerId) },
        onAddCustomEvent = { code, playerId -> liveVm.addCustomEvent(code, playerId) },
        eventLabel = liveVm::eventLabel,
        onSubstitution = { outId, inId -> liveVm.addSubstitution(outId, inId) },
        onMovePlayer = { id, x, y -> liveVm.movePlayerOnField(id, x, y) },
        onSetShowJerseyNumbers = liveVm::setShowJerseyNumbers,
        onSetShowStarterTime = liveVm::setShowStarterTime,
        onClearFeedback = liveVm::clearFeedback,
        onUndo = liveVm::undoLastEvent,
        onFinish = { liveVm.finishMatch(onFinished) },
        onBack = onBack
    )
}

@Composable
private fun SettingsRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: EventTypesViewModel = viewModel(
        key = "settings_$teamId",
        factory = EventTypesViewModel.factory(context.applicationContext, teamId)
    )
    val types by vm.types.collectAsState()

    SettingsScreen(
        team = team,
        types = types,
        onAdd = vm::add,
        onUpdate = vm::update,
        onToggleActive = vm::setActive,
        onDelete = vm::delete,
        onBack = onBack
    )
}

@Composable
private fun StatisticsRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val statsVm: StatisticsViewModel = viewModel(
        key = "stats_$teamId",
        factory = StatisticsViewModel.factory(context.applicationContext, teamId)
    )
    val stats by statsVm.playerStats.collectAsState()
    val finished by statsVm.finishedMatches.collectAsState()

    StatisticsScreen(
        team = team,
        stats = stats,
        finishedMatchCount = finished.size,
        onBack = onBack
    )
}
