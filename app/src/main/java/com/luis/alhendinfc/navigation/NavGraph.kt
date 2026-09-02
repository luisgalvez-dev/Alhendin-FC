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
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.ui.home.HomeScreen
import com.luis.alhendinfc.ui.matches.MatchListScreen
import com.luis.alhendinfc.ui.matches.MatchSetupScreen
import com.luis.alhendinfc.ui.matches.MatchViewModel
import com.luis.alhendinfc.ui.players.PlayerDetailScreen
import com.luis.alhendinfc.ui.players.PlayerEditDialog
import com.luis.alhendinfc.ui.players.PlayerListScreen
import com.luis.alhendinfc.ui.players.PlayerViewModel
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
    onBack: () -> Unit
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

    if (currentMatch != null) {
        MatchSetupScreen(
            match = currentMatch!!,
            matchPlayers = matchPlayers,
            teamPlayers = teamPlayers,
            team = team,
            onSave = { matchViewModel.saveMatch(it) },
            onPlayerCallup = { playerId, status -> matchViewModel.setPlayerCallup(playerId, status) },
            onDelete = { matchViewModel.deleteCurrentMatch() },
            onBack = { matchViewModel.closeMatch() }
        )
    } else {
        MatchListScreen(
            matches = matches,
            onBack = onBack,
            onCreateNew = {
                matchViewModel.createNewMatch { /* match is auto-opened by openMatch */ }
            },
            onOpenMatch = { match -> matchViewModel.openMatch(match.id) }
        )
    }
}
