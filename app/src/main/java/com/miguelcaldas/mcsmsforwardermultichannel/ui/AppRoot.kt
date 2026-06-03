package com.miguelcaldas.mcsmsforwardermultichannel.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miguelcaldas.mcsmsforwardermultichannel.ui.channels.ChannelDetailScreen
import com.miguelcaldas.mcsmsforwardermultichannel.ui.channels.ChannelType
import com.miguelcaldas.mcsmsforwardermultichannel.ui.channels.ChannelsScreen
import com.miguelcaldas.mcsmsforwardermultichannel.ui.filters.FiltersScreen
import com.miguelcaldas.mcsmsforwardermultichannel.ui.filters.RegexTesterScreen
import com.miguelcaldas.mcsmsforwardermultichannel.ui.log.LogScreen
import com.miguelcaldas.mcsmsforwardermultichannel.ui.navigation.TopLevelDestination
import com.miguelcaldas.mcsmsforwardermultichannel.ui.status.StatusScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Status.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Status.route) {
                StatusScreen(
                    onOpenChannels = {
                        navController.navigate(TopLevelDestination.Channels.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenFilters = {
                        navController.navigate("filters")
                    },
                )
            }
            composable(TopLevelDestination.Channels.route) {
                ChannelsScreen(
                    onOpenChannel = { type ->
                        navController.navigate("channel_detail/${type.name}")
                    },
                    onOpenFilters = {
                        navController.navigate("filters")
                    },
                )
            }
            composable(
                route = "channel_detail/{type}",
                arguments = listOf(navArgument("type") { this.type = NavType.StringType }),
            ) { entry ->
                val typeName = entry.arguments?.getString("type") ?: ChannelType.WhatsApp.name
                ChannelDetailScreen(
                    type = ChannelType.valueOf(typeName),
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(TopLevelDestination.Activity.route) {
                LogScreen()
            }
            composable("filters") {
                FiltersScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenTester = {
                        navController.navigate("regex_tester")
                    },
                )
            }
            composable("regex_tester") {
                RegexTesterScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
