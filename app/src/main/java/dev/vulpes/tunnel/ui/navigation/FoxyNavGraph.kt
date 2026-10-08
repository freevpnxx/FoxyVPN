package dev.vulpes.tunnel.ui.navigation

import dev.vulpes.tunnel.vpn.FoxyVpnService
import dev.vulpes.tunnel.data.model.ConnectionState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.vulpes.tunnel.FoxyVpnApp
import dev.vulpes.tunnel.ui.screens.AccountScreen
import dev.vulpes.tunnel.ui.screens.HomeScreen
import dev.vulpes.tunnel.ui.screens.LoginScreen
import dev.vulpes.tunnel.ui.screens.LogsScreen
import dev.vulpes.tunnel.ui.screens.ServerListScreen
import dev.vulpes.tunnel.ui.screens.SettingsScreen
import dev.vulpes.tunnel.ui.screens.SplashScreen
import dev.vulpes.tunnel.ui.theme.ThemeController

object FoxyRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val LOGS = "logs"
}

@Composable
fun FoxyNavGraph(
    navController: NavHostController = rememberNavController(),
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    NavHost(navController = navController, startDestination = FoxyRoutes.SPLASH) {
        composable(FoxyRoutes.SPLASH) {
            SplashScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
                onNeedsLogin = {
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.LOGIN) {
            LoginScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.HOME) {
            HomeScreen(
                app = app,
                themeController = themeController,
                onRequestConnect = onRequestConnect,
                onDisconnect = onDisconnect,
                onOpenServers = { navController.navigate(FoxyRoutes.SERVERS) },
                onOpenSettings = { navController.navigate(FoxyRoutes.SETTINGS) },
                onOpenLogs = { navController.navigate(FoxyRoutes.LOGS) },
            )
        }
        composable(FoxyRoutes.SERVERS) {
            val context = LocalContext.current
            ServerListScreen(
                serverListClient = app.serverListClient,
                proxyStateStore = app.proxyStateStore,
                // Selecting a server while a tunnel is live must swap the connection over
                // immediately; otherwise the new choice only takes effect on the next connect.
                onServerSelected = {
                    if (FoxyVpnService.state.value != ConnectionState.DISCONNECTED) {
                        FoxyVpnService.switchServer(context)
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.SETTINGS) {
            SettingsScreen(
                settingsStore = app.settingsStore,
                onOpenLogs = { navController.navigate(FoxyRoutes.LOGS) },
                onOpenAccount = { navController.navigate(FoxyRoutes.ACCOUNT) },
                onSignOut = {
                    onDisconnect()
                    app.tokenStore.clear()
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.ACCOUNT) {
            AccountScreen(
                authRepository = app.authRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.LOGS) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
