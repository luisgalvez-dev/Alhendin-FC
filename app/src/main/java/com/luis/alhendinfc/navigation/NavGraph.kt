package com.luis.alhendinfc.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.ui.calendar.CalendarScreen
import com.luis.alhendinfc.ui.calendar.CalendarViewModel
import com.luis.alhendinfc.ui.home.HomeScreen
import com.luis.alhendinfc.ui.home.HomeViewModel
import com.luis.alhendinfc.ui.live.LiveMatchScreen
import com.luis.alhendinfc.ui.live.LiveMatchViewModel
import com.luis.alhendinfc.ui.matches.MatchListScreen
import com.luis.alhendinfc.ui.matches.MatchSetupScreen
import com.luis.alhendinfc.ui.matches.MatchViewModel
import com.luis.alhendinfc.ui.pizarra.PizarraScreen
import com.luis.alhendinfc.ui.pizarra.PizarraViewModel
import com.luis.alhendinfc.ui.players.PlayerDetailScreen
import com.luis.alhendinfc.ui.players.PlayerEditDialog
import com.luis.alhendinfc.ui.players.PlayerListScreen
import com.luis.alhendinfc.ui.players.PlayerViewModel
import com.luis.alhendinfc.ui.settings.CustomizeHomeScreen
import com.luis.alhendinfc.ui.settings.CustomizeHomeViewModel
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
    object Matches : AppScreen("matches/{teamId}?openMatchId={openMatchId}") {
        fun createRoute(teamId: Int, openMatchId: Int = -1) =
            "matches/$teamId?openMatchId=$openMatchId"
    }
    object Calendar : AppScreen("calendar/{teamId}") {
        fun createRoute(teamId: Int) = "calendar/$teamId"
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
    object CustomizeHome : AppScreen("customize_home")
    object Pizarra : AppScreen("pizarra")
}

@Composable
fun AlhendinNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModel.factory(context.applicationContext)
    )
    val teams by teamViewModel.teams.collectAsStateWithLifecycle()
    val selectedTeam by teamViewModel.selectedTeam.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route
    ) {
        composable(AppScreen.Home.route) {
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(context.applicationContext)
            )
            LaunchedEffect(selectedTeam?.id) {
                homeVm.setTeamId(selectedTeam?.id)
            }
            val layoutConfig by homeVm.layoutConfig.collectAsStateWithLifecycle()
            val nextFixture by homeVm.nextFixture.collectAsStateWithLifecycle()
            val liveMatch by homeVm.liveMatch.collectAsStateWithLifecycle()

            HomeScreen(
                teams = teams,
                selectedTeam = selectedTeam,
                layoutConfig = layoutConfig,
                nextFixture = nextFixture,
                liveMatch = liveMatch,
                onNavigateToTeam = { navController.navigate(AppScreen.Team.route) },
                onNavigateToMatches = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Matches.createRoute(teamId))
                },
                onNavigateToCalendar = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Calendar.createRoute(teamId))
                },
                onNavigateToPizarra = {
                    navController.navigate(AppScreen.Pizarra.route)
                },
                onNavigateToStatistics = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Statistics.createRoute(teamId))
                },
                onNavigateToSettings = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Settings.createRoute(teamId))
                },
                onNavigateToLive = { matchId ->
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.LiveMatch.createRoute(teamId, matchId))
                },
                onNavigateToNextMatch = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Calendar.createRoute(teamId))
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
            val players by playerViewModel.players.collectAsStateWithLifecycle()

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
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("openMatchId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            val openMatchId = backStack.arguments!!.getInt("openMatchId")
            MatchesRoute(
                teamId = teamId,
                team = selectedTeam,
                openMatchId = openMatchId,
                onBack = { navController.popBackStack() },
                onContinueToLive = { matchId ->
                    navController.navigate(AppScreen.LiveMatch.createRoute(teamId, matchId))
                }
            )
        }

        composable(
            route = AppScreen.Calendar.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            CalendarRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() },
                onOpenMatch = { matchId ->
                    navController.navigate(AppScreen.Matches.createRoute(teamId, matchId))
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
                    navController.navigate(AppScreen.Matches.createRoute(teamId)) {
                        popUpTo(AppScreen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
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
                onCustomizeHome = { navController.navigate(AppScreen.CustomizeHome.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppScreen.CustomizeHome.route) {
            CustomizeHomeRoute(onBack = { navController.popBackStack() })
        }

        composable(AppScreen.Pizarra.route) {
            PizarraRoute(onBack = { navController.popBackStack() })
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

    val players by playerViewModel.players.collectAsStateWithLifecycle()
    val selectedTeam by teamViewModel.selectedTeam.collectAsStateWithLifecycle()
    val selectedPlayer by playerViewModel.selectedPlayer.collectAsStateWithLifecycle()

    val statsVm: StatisticsViewModel = viewModel(
        key = "stats_$teamId",
        factory = StatisticsViewModel.factory(context.applicationContext, teamId)
    )
    val teamStats by statsVm.playerStats.collectAsStateWithLifecycle()
    val selectedStats = remember(selectedPlayer, teamStats) {
        selectedPlayer?.let { p -> teamStats.find { it.player.id == p.id } }
    }

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
            seasonStats = selectedStats,
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
    openMatchId: Int = -1,
    onBack: () -> Unit,
    onContinueToLive: (matchId: Int) -> Unit
) {
    val context = LocalContext.current
    val matchViewModel: MatchViewModel = viewModel(
        key = "matches_$teamId",
        factory = MatchViewModel.factory(context.applicationContext, teamId)
    )

    val matches by matchViewModel.matches.collectAsStateWithLifecycle()
    val currentMatch by matchViewModel.currentMatch.collectAsStateWithLifecycle()
    val matchPlayers by matchViewModel.matchPlayers.collectAsStateWithLifecycle()
    val teamPlayers by matchViewModel.teamPlayers.collectAsStateWithLifecycle()
    val fixtures by matchViewModel.fixtures.collectAsStateWithLifecycle()
    val matchEvents by matchViewModel.matchEvents.collectAsStateWithLifecycle()

    LaunchedEffect(openMatchId) {
        if (openMatchId > 0) {
            matchViewModel.openMatch(openMatchId)
        }
    }

    when {
        currentMatch != null -> {
            MatchSetupScreen(
                match = currentMatch!!,
                matchPlayers = matchPlayers,
                teamPlayers = teamPlayers,
                team = team,
                fixtures = fixtures,
                matchEvents = matchEvents,
                eventLabel = matchViewModel::eventLabel,
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
private fun CalendarRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit,
    onOpenMatch: (matchId: Int) -> Unit
) {
    val context = LocalContext.current
    val vm: CalendarViewModel = viewModel(
        key = "calendar_$teamId",
        factory = CalendarViewModel.factory(context.applicationContext, teamId)
    )
    val fixtures by vm.fixtures.collectAsStateWithLifecycle()
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    val matches by vm.matches.collectAsStateWithLifecycle()

    CalendarScreen(
        team = team,
        fixtures = fixtures,
        clubs = clubs,
        matches = matches,
        onPrepareMatch = { row ->
            vm.openOrPrepareMatch(row) { matchId ->
                onOpenMatch(matchId)
            }
        },
        onSaveFixture = vm::saveFixture,
        onAddFixture = vm::addFixture,
        onDeleteFixture = vm::deleteFixture,
        onAddClub = vm::addClub,
        onUpdateClub = vm::updateClub,
        onDeleteClub = vm::deleteClub,
        onBack = onBack
    )
}

@Composable
private fun PizarraRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: PizarraViewModel = viewModel(
        factory = PizarraViewModel.factory(context.applicationContext)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    PizarraScreen(
        state = state,
        onBack = onBack,
        onTool = vm::setTool,
        onColor = vm::setColor,
        onStrokeWidth = vm::setStrokeWidth,
        onUseField = vm::useDefaultField,
        onImagePicked = vm::setImage,
        onVideoPicked = vm::setVideo,
        onTogglePlay = { vm.setVideoPlaying(!state.isVideoPlaying) },
        onSeekBack = { vm.seekVideo(-10_000L) },
        onSeekForward = { vm.seekVideo(10_000L) },
        onStartStroke = vm::startStroke,
        onUpdateStroke = vm::updateStroke,
        onFinishStroke = vm::finishStroke,
        onUndo = vm::undo,
        onClear = vm::clearAll
    )
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

    val match by liveVm.match.collectAsStateWithLifecycle()
    val ui by liveVm.ui.collectAsStateWithLifecycle()
    val fieldPositions by liveVm.fieldPositions.collectAsStateWithLifecycle()
    val events by liveVm.events.collectAsStateWithLifecycle()
    val matchPlayers by liveVm.matchPlayers.collectAsStateWithLifecycle()
    val teamPlayers by liveVm.teamPlayers.collectAsStateWithLifecycle()
    val customStatTypes by liveVm.customStatTypes.collectAsStateWithLifecycle()

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
        clock = liveVm.clock,
        fieldSeconds = liveVm.fieldSeconds,
        fieldPositions = fieldPositions,
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
        onFinish = { onDone -> liveVm.finishMatch(onDone) },
        onBack = {
            if (current.status == MatchStatus.FINISHED) onFinished() else onBack()
        }
    )
}

@Composable
private fun SettingsRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onCustomizeHome: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: EventTypesViewModel = viewModel(
        key = "settings_$teamId",
        factory = EventTypesViewModel.factory(context.applicationContext, teamId)
    )
    val types by vm.types.collectAsStateWithLifecycle()
    val backupBusy by vm.backupBusy.collectAsStateWithLifecycle()
    val backupMessage by vm.backupMessage.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importBackup(uri)
    }

    SettingsScreen(
        team = team,
        types = types,
        backupBusy = backupBusy,
        backupMessage = backupMessage,
        onClearBackupMessage = vm::clearBackupMessage,
        onExportBackup = {
            exportLauncher.launch("alhendin_backup_${System.currentTimeMillis()}.zip")
        },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        },
        onAdd = vm::add,
        onUpdate = vm::update,
        onToggleActive = vm::setActive,
        onDelete = vm::delete,
        onCustomizeHome = onCustomizeHome,
        onBack = onBack
    )
}

@Composable
private fun CustomizeHomeRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: CustomizeHomeViewModel = viewModel(
        factory = CustomizeHomeViewModel.factory(context.applicationContext)
    )
    val config by vm.layoutConfig.collectAsStateWithLifecycle()

    CustomizeHomeScreen(
        config = config,
        onToggle = vm::setEnabled,
        onMoveUp = vm::moveUp,
        onMoveDown = vm::moveDown,
        onReset = vm::resetDefaults,
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
    val stats by statsVm.playerStats.collectAsStateWithLifecycle()
    val finished by statsVm.finishedMatches.collectAsStateWithLifecycle()

    StatisticsScreen(
        team = team,
        stats = stats,
        finishedMatchCount = finished.size,
        onBack = onBack
    )
}
