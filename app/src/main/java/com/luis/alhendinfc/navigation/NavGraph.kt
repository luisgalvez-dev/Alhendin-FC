package com.luis.alhendinfc.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luis.alhendinfc.cloud.AlhendinCloud
import com.luis.alhendinfc.cloud.auth.AuthSession
import com.luis.alhendinfc.data.sync.SyncUiStatus
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luis.alhendinfc.domain.model.HomeModule
import com.luis.alhendinfc.domain.model.MatchStatus
import com.luis.alhendinfc.domain.model.MatchLifecycle
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.StatisticType
import com.luis.alhendinfc.ui.calendar.CalendarScreen
import com.luis.alhendinfc.ui.calendar.CalendarViewModel
import com.luis.alhendinfc.ui.calendar.MonthCalendarScreen
import com.luis.alhendinfc.ui.calendar.MonthCalendarViewModel
import com.luis.alhendinfc.ui.home.HomeScreen
import com.luis.alhendinfc.ui.home.HomeViewModel
import com.luis.alhendinfc.ui.live.LiveMatchScreen
import com.luis.alhendinfc.ui.live.LiveMatchViewModel
import com.luis.alhendinfc.ui.matches.MatchListScreen
import com.luis.alhendinfc.ui.matches.MatchSetupScreen
import com.luis.alhendinfc.ui.matches.MatchViewModel
import com.luis.alhendinfc.ui.pizarra.BoardListScreen
import com.luis.alhendinfc.ui.pizarra.BoardListViewModel
import com.luis.alhendinfc.ui.pizarra.PizarraScreen
import com.luis.alhendinfc.ui.pizarra.PizarraViewModel
import com.luis.alhendinfc.ui.rivals.RivalDetailScreen
import com.luis.alhendinfc.ui.rivals.RivalDetailViewModel
import com.luis.alhendinfc.ui.rivals.RivalListScreen
import com.luis.alhendinfc.ui.rivals.RivalListViewModel
import com.luis.alhendinfc.ui.players.PlayerDetailScreen
import com.luis.alhendinfc.ui.players.PlayerEditDialog
import com.luis.alhendinfc.ui.players.PlayerListScreen
import com.luis.alhendinfc.ui.players.PlayerViewModel
import com.luis.alhendinfc.ui.settings.AllModulesScreen
import com.luis.alhendinfc.ui.settings.CustomizeHomeScreen
import com.luis.alhendinfc.ui.settings.CustomizeHomeViewModel
import com.luis.alhendinfc.ui.settings.EventTypesViewModel
import com.luis.alhendinfc.ui.settings.SettingsScreen
import com.luis.alhendinfc.ui.statistics.StatisticsScreen
import com.luis.alhendinfc.ui.statistics.StatisticsViewModel
import com.luis.alhendinfc.ui.tasks.TaskListScreen
import com.luis.alhendinfc.ui.tasks.TaskViewModel
import com.luis.alhendinfc.ui.team.TeamScreen
import com.luis.alhendinfc.ui.team.TeamViewModel
import com.luis.alhendinfc.ui.training.TrainingEditScreen
import com.luis.alhendinfc.ui.training.TrainingEditViewModel

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
    object Fixtures : AppScreen("fixtures/{teamId}") {
        fun createRoute(teamId: Int) = "fixtures/$teamId"
    }
    object TrainingEdit : AppScreen("training/{teamId}?trainingId={trainingId}&epochDay={epochDay}") {
        fun createRoute(teamId: Int, trainingId: Int = -1, epochDay: Long = -1L) =
            "training/$teamId?trainingId=$trainingId&epochDay=$epochDay"
    }
    object Tasks : AppScreen("tasks/{teamId}") {
        fun createRoute(teamId: Int) = "tasks/$teamId"
    }
    object Rivals : AppScreen("rivals/{teamId}") {
        fun createRoute(teamId: Int) = "rivals/$teamId"
    }
    object RivalDetail : AppScreen("rival/{teamId}/{clubId}") {
        fun createRoute(teamId: Int, clubId: Int) = "rival/$teamId/$clubId"
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
    object AllModules : AppScreen("all_modules/{teamId}") {
        fun createRoute(teamId: Int) = "all_modules/$teamId"
    }
    object Pizarra : AppScreen("pizarra/{teamId}") {
        fun createRoute(teamId: Int) = "pizarra/$teamId"
    }
    object PizarraEditor : AppScreen("pizarra/{teamId}/edit/{boardId}?taskId={taskId}") {
        fun createRoute(teamId: Int, boardId: Int, taskId: Int = -1) =
            "pizarra/$teamId/edit/$boardId?taskId=$taskId"
    }
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
            val cloud = remember { AlhendinCloud.getInstance(context.applicationContext) }
            val syncStatus by cloud.engine.status.collectAsStateWithLifecycle()

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
                onNavigateToTasks = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Tasks.createRoute(teamId))
                },
                onNavigateToRivals = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Rivals.createRoute(teamId))
                },
                onNavigateToPizarra = {
                    val teamId = selectedTeam?.id ?: return@HomeScreen
                    navController.navigate(AppScreen.Pizarra.createRoute(teamId))
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
                onSelectTeam = { teamViewModel.selectTeam(it) },
                connectionLabel = when (syncStatus) {
                    SyncUiStatus.OFFLINE -> "Sin conexión"
                    SyncUiStatus.PENDING -> "Pendiente de sincronizar"
                    SyncUiStatus.UNAVAILABLE -> "Firebase no configurado"
                    SyncUiStatus.SYNCED -> null
                }
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
            MonthCalendarRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() },
                onOpenMatch = { matchId ->
                    navController.navigate(AppScreen.Matches.createRoute(teamId, matchId))
                },
                onOpenTraining = { trainingId ->
                    navController.navigate(AppScreen.TrainingEdit.createRoute(teamId, trainingId))
                },
                onAddTraining = { epochDay ->
                    navController.navigate(
                        AppScreen.TrainingEdit.createRoute(teamId, trainingId = -1, epochDay = epochDay)
                    )
                },
                onOpenFixtures = {
                    navController.navigate(AppScreen.Fixtures.createRoute(teamId))
                }
            )
        }

        composable(
            route = AppScreen.Fixtures.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            CalendarRoute(
                teamId = teamId,
                team = selectedTeam,
                onBack = { navController.popBackStack() },
                onOpenMatch = { matchId ->
                    navController.navigate(AppScreen.Matches.createRoute(teamId, matchId))
                },
                onOpenRivals = {
                    navController.navigate(AppScreen.Rivals.createRoute(teamId))
                }
            )
        }

        composable(
            route = AppScreen.TrainingEdit.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("trainingId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("epochDay") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            val trainingId = backStack.arguments!!.getInt("trainingId")
            val epochDay = backStack.arguments!!.getLong("epochDay")
            TrainingRoute(
                teamId = teamId,
                trainingId = trainingId,
                epochDay = epochDay,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.Tasks.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            TasksRoute(
                teamId = teamId,
                team = selectedTeam,
                onOpenBoard = { boardId, taskId ->
                    navController.navigate(AppScreen.PizarraEditor.createRoute(teamId, boardId, taskId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.Rivals.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            RivalsRoute(
                teamId = teamId,
                team = selectedTeam,
                onOpenClub = { clubId ->
                    navController.navigate(AppScreen.RivalDetail.createRoute(teamId, clubId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.RivalDetail.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("clubId") { type = NavType.IntType }
            )
        ) { backStack ->
            val clubId = backStack.arguments!!.getInt("clubId")
            RivalDetailRoute(
                clubId = clubId,
                onBack = { navController.popBackStack() }
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
                onAllModules = { navController.navigate(AppScreen.AllModules.createRoute(teamId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppScreen.CustomizeHome.route) {
            CustomizeHomeRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = AppScreen.AllModules.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(context.applicationContext)
            )
            LaunchedEffect(selectedTeam?.id) {
                homeVm.setTeamId(selectedTeam?.id)
            }
            val liveMatch by homeVm.liveMatch.collectAsStateWithLifecycle()
            AllModulesScreen(
                onOpenModule = { module ->
                    navController.navigateToModule(module, selectedTeam?.id ?: teamId, liveMatch?.id)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppScreen.Pizarra.route,
            arguments = listOf(navArgument("teamId") { type = NavType.IntType })
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            BoardLibraryRoute(
                teamId = teamId,
                onBack = { navController.popBackStack() },
                onOpen = { boardId ->
                    navController.navigate(AppScreen.PizarraEditor.createRoute(teamId, boardId))
                }
            )
        }

        composable(
            route = AppScreen.PizarraEditor.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("boardId") { type = NavType.IntType },
                navArgument("taskId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStack ->
            val teamId = backStack.arguments!!.getInt("teamId")
            val boardId = backStack.arguments!!.getInt("boardId")
            val taskId = backStack.arguments!!.getInt("taskId")
            PizarraRoute(
                teamId = teamId,
                boardId = boardId,
                taskId = taskId,
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
    val reports by matchViewModel.reports.collectAsStateWithLifecycle()

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
                reports = reports,
                onSave = { matchViewModel.saveMatch(it) },
                onPlayerCallup = { playerId, status -> matchViewModel.setPlayerCallup(playerId, status) },
                onDelete = { matchViewModel.deleteCurrentMatch() },
                onAddReport = matchViewModel::addReport,
                onDeleteReport = matchViewModel::deleteReport,
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
                    if (match.status == MatchStatus.FINISHED) {
                        matchViewModel.openMatch(match.id)
                    } else if (MatchLifecycle.isShownAsLive(match)) {
                        onContinueToLive(match.id)
                    } else {
                        matchViewModel.openMatch(match.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun TasksRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onOpenBoard: (boardId: Int, taskId: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: TaskViewModel = viewModel(
        key = "tasks_$teamId",
        factory = TaskViewModel.factory(context.applicationContext, teamId)
    )
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val taskImages by vm.taskImages.collectAsStateWithLifecycle()
    val boards by vm.boardsState.collectAsStateWithLifecycle()

    TaskListScreen(
        team = team,
        tasks = tasks,
        taskImages = taskImages,
        boards = boards,
        searchQuery = searchQuery,
        onSearchChange = vm::setQuery,
        onAdd = { task, imageUri -> vm.add(task, imageUri) },
        onUpdate = { task, imageUri, remove -> vm.update(task, imageUri, remove) },
        onDelete = vm::delete,
        onCreateBoard = { task -> vm.createBoardForTask(task) { id -> onOpenBoard(id, task.id) } },
        onAssignBoard = vm::assignBoard,
        onOpenBoard = { board -> onOpenBoard(board.id, -1) },
        onClearBoard = vm::clearBoard,
        onBack = onBack
    )
}

@Composable
private fun RivalsRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onOpenClub: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: RivalListViewModel = viewModel(
        key = "rivals_$teamId",
        factory = RivalListViewModel.factory(context.applicationContext, teamId)
    )
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    RivalListScreen(
        team = team,
        clubs = clubs,
        searchQuery = query,
        onSearchChange = vm::setQuery,
        onOpenClub = { onOpenClub(it.id) },
        onAddClub = { name, shortName, stadium, shield, kit ->
            vm.addClub(name, shortName, stadium, shield, kit) { id -> onOpenClub(id) }
        },
        onDeleteClub = vm::deleteClub,
        onBack = onBack
    )
}

@Composable
private fun RivalDetailRoute(
    clubId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: RivalDetailViewModel = viewModel(
        key = "rival_$clubId",
        factory = RivalDetailViewModel.factory(context.applicationContext, clubId)
    )
    val club by vm.club.collectAsStateWithLifecycle()
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    val playerQuery by vm.playerSearch.collectAsStateWithLifecycle()
    val links by vm.links.collectAsStateWithLifecycle()
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    val matchReports by vm.matchReports.collectAsStateWithLifecycle()
    RivalDetailScreen(
        club = club,
        analysis = analysis,
        players = players,
        playerQuery = playerQuery,
        links = links,
        attachments = attachments,
        matchReports = matchReports,
        onSaveClub = vm::saveClub,
        onSaveAnalysis = vm::saveAnalysis,
        onPlayerQuery = vm::setPlayerQuery,
        onAddPlayer = vm::addPlayer,
        onUpdatePlayer = vm::updatePlayer,
        onDeletePlayer = vm::deletePlayer,
        onAddLink = vm::addLink,
        onUpdateLink = vm::updateLink,
        onDeleteLink = vm::deleteLink,
        onMoveLink = vm::moveLink,
        onAddFile = vm::addFile,
        onDeleteAttachment = vm::deleteAttachment,
        onBack = onBack
    )
}

@Composable
private fun MonthCalendarRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit,
    onOpenMatch: (matchId: Int) -> Unit,
    onOpenTraining: (trainingId: Int) -> Unit,
    onAddTraining: (epochDay: Long) -> Unit,
    onOpenFixtures: () -> Unit
) {
    val context = LocalContext.current
    val vm: MonthCalendarViewModel = viewModel(
        key = "month_calendar_$teamId",
        factory = MonthCalendarViewModel.factory(context.applicationContext, teamId)
    )
    val visibleMonth by vm.visibleMonth.collectAsStateWithLifecycle()
    val dayContents by vm.dayContents.collectAsStateWithLifecycle()

    MonthCalendarScreen(
        team = team,
        visibleMonth = visibleMonth,
        dayContents = dayContents,
        onPreviousMonth = vm::previousMonth,
        onNextMonth = vm::nextMonth,
        onGoToToday = vm::goToToday,
        onOpenMatch = onOpenMatch,
        onPrepareFixture = { row ->
            vm.openOrPrepareMatch(row) { matchId -> onOpenMatch(matchId) }
        },
        onOpenTraining = onOpenTraining,
        onAddTraining = onAddTraining,
        onOpenFixtures = onOpenFixtures,
        onBack = onBack
    )
}

@Composable
private fun TrainingRoute(
    teamId: Int,
    trainingId: Int,
    epochDay: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: TrainingEditViewModel = viewModel(
        key = "training_${teamId}_${trainingId}_$epochDay",
        factory = TrainingEditViewModel.factory(
            context.applicationContext,
            teamId,
            trainingId,
            epochDay
        )
    )
    val training by vm.training.collectAsStateWithLifecycle()
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    val libraryTasks by vm.libraryTasks.collectAsStateWithLifecycle()
    val sessionTasks by vm.sessionTasks.collectAsStateWithLifecycle()
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    val pendingFiles by vm.pendingFiles.collectAsStateWithLifecycle()
    val opponentClubId by vm.opponentClubId.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    TrainingEditScreen(
        isNew = vm.isNew,
        dateLabel = vm.dateLabel,
        training = training,
        clubs = clubs,
        libraryTasks = libraryTasks,
        sessionTasks = sessionTasks,
        attachments = attachments,
        pendingFiles = pendingFiles,
        opponentClubId = opponentClubId,
        notes = notes,
        error = error,
        busy = busy,
        onOpponentChange = vm::setOpponentClubId,
        onNotesChange = vm::setNotes,
        onAddTask = vm::addTask,
        onRemoveTask = vm::removeTask,
        onMoveTask = vm::moveTask,
        onAddFile = vm::addPendingFile,
        onRemovePendingFile = vm::removePendingFile,
        onDeleteAttachment = vm::deleteAttachment,
        onSave = { vm.save { onBack() } },
        onDelete = { vm.deleteTraining { onBack() } },
        onBack = onBack
    )
}

@Composable
private fun CalendarRoute(
    teamId: Int,
    team: com.luis.alhendinfc.domain.model.Team?,
    onBack: () -> Unit,
    onOpenMatch: (matchId: Int) -> Unit,
    onOpenRivals: () -> Unit
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
        onOpenRivals = onOpenRivals,
        onBack = onBack
    )
}

@Composable
private fun BoardLibraryRoute(
    teamId: Int,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit
) {
    val context = LocalContext.current
    val vm: BoardListViewModel = viewModel(
        key = "boards_$teamId",
        factory = BoardListViewModel.factory(context.applicationContext, teamId)
    )
    val boards by vm.boardsState.collectAsStateWithLifecycle()
    BoardListScreen(
        boards = boards,
        onBack = onBack,
        onOpen = { onOpen(it.id) },
        onCreate = { name, _ -> vm.create(name) { id -> onOpen(id) } },
        onRename = { board, name -> vm.rename(board, name) },
        onDuplicate = { board, _ -> vm.duplicate(board) { id -> onOpen(id) } },
        onDelete = vm::delete,
        tasksUsing = vm::tasksUsing
    )
}

@Composable
private fun PizarraRoute(
    teamId: Int,
    boardId: Int,
    taskId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: PizarraViewModel = viewModel(
        key = "board_edit_${boardId}_$taskId",
        factory = PizarraViewModel.factory(context.applicationContext, boardId, teamId, taskId)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    PizarraScreen(
        state = state,
        onBack = {
            if (state.dirty) vm.save()
            onBack()
        },
        onSave = vm::save,
        onTool = vm::setTool,
        onColor = vm::setColor,
        onStrokeWidth = vm::setStrokeWidth,
        onUseField = vm::useDefaultField,
        onImagePicked = vm::setImage,
        onVideoPicked = vm::setVideo,
        onTogglePlay = { vm.setVideoPlaying(!state.isVideoPlaying) },
        onSeekBack = { vm.seekVideo(-10_000L) },
        onSeekForward = { vm.seekVideo(10_000L) },
        onPress = vm::onPress,
        onDrag = vm::onDrag,
        onRelease = vm::onRelease,
        onUndo = vm::undo,
        onClear = vm::clearAll,
        onPendingText = vm::setPendingText,
        onDeleteSelected = vm::deleteSelected,
        onUpdateNumber = vm::updateSelectedNumber,
        onUpdateText = vm::updateSelectedText,
        onSavedConsumed = vm::consumeSaved,
        onMediaErrorConsumed = vm::consumeMediaError
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
    onAllModules: () -> Unit,
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
    val cloud = remember { AlhendinCloud.getInstance(context.applicationContext) }
    val session by cloud.session.collectAsStateWithLifecycle()
    val syncStatus by cloud.engine.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ready = session as? AuthSession.Ready

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
        onAllModules = onAllModules,
        accountName = ready?.user?.displayName,
        accountEmail = ready?.user?.email,
        workspaceId = ready?.workspaceId,
        syncStatusLabel = when (syncStatus) {
            SyncUiStatus.SYNCED -> "Sincronizado"
            SyncUiStatus.PENDING -> "Pendiente"
            SyncUiStatus.OFFLINE -> "Sin conexión"
            SyncUiStatus.UNAVAILABLE -> "Firebase no configurado"
        },
        onSignOut = {
            scope.launch { cloud.signOut() }
        },
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

private fun NavHostController.navigateToModule(
    module: HomeModule,
    teamId: Int,
    liveMatchId: Int?
) {
    val validTeam = teamId > 0
    when (module) {
        HomeModule.TEAM -> navigate(AppScreen.Team.route)
        HomeModule.MATCHES -> if (validTeam) navigate(AppScreen.Matches.createRoute(teamId))
        HomeModule.CALENDAR -> if (validTeam) navigate(AppScreen.Calendar.createRoute(teamId))
        HomeModule.TASKS -> if (validTeam) navigate(AppScreen.Tasks.createRoute(teamId))
        HomeModule.RIVALS -> if (validTeam) navigate(AppScreen.Rivals.createRoute(teamId))
        HomeModule.PIZARRA -> if (validTeam) navigate(AppScreen.Pizarra.createRoute(teamId))
        HomeModule.STATISTICS -> if (validTeam) navigate(AppScreen.Statistics.createRoute(teamId))
        HomeModule.SETTINGS -> if (validTeam) navigate(AppScreen.Settings.createRoute(teamId))
        HomeModule.NEXT_MATCH -> if (validTeam) navigate(AppScreen.Calendar.createRoute(teamId))
        HomeModule.LIVE_MATCH -> when {
            validTeam && liveMatchId != null ->
                navigate(AppScreen.LiveMatch.createRoute(teamId, liveMatchId))
            validTeam -> navigate(AppScreen.Matches.createRoute(teamId))
        }
    }
}
