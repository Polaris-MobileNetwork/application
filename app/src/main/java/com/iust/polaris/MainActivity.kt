package com.iust.polaris

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iust.polaris.data.local.SettingsManager
import com.iust.polaris.ui.components.HandlePermissions
import com.iust.polaris.ui.screens.MetricsCollectionScreen
import com.iust.polaris.ui.screens.MetricsDisplayScreen
import com.iust.polaris.ui.screens.SettingsScreen
import com.iust.polaris.ui.screens.TestsScreen
import com.iust.polaris.ui.theme.PolarisAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class AppScreen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Monitor : AppScreen("monitor", "Monitor", Icons.Filled.Home)
    object Metrics : AppScreen("metrics", "Metrics", Icons.Filled.List)
    object Tests : AppScreen("tests", "Tests", Icons.Filled.Check)
    object Settings : AppScreen("settings", "Settings")
}

val bottomNavItems = listOf(
    AppScreen.Monitor,
    AppScreen.Metrics,
    AppScreen.Tests
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toList()

        setContent {
            val themePreference by settingsManager.themePreferenceFlow.collectAsState(initial = "System")

            val useDarkTheme = when (themePreference) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            PolarisAppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HandlePermissions(permissions = requiredPermissions) {
                        MainAppScaffold()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentScreen = bottomNavItems.find { it.route == currentDestination?.route }
        ?: if (currentDestination?.route == AppScreen.Settings.route) AppScreen.Settings else null

    val canNavigateBack = currentDestination?.route == AppScreen.Settings.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (canNavigateBack) currentScreen?.label ?: "Polaris" else "Polaris") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (!canNavigateBack) {
                        IconButton(onClick = { navController.navigate(AppScreen.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val isTopLevelDestination = currentDestination?.route in bottomNavItems.map { it.route }
            if (isTopLevelDestination) {
                BottomNavigationBar(navController = navController, currentDestination = currentDestination)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentDestination: androidx.navigation.NavDestination?) {
    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Monitor.route,
        modifier = modifier
    ) {
        composable(AppScreen.Monitor.route) {
            MetricsCollectionScreen()
        }
        composable(AppScreen.Metrics.route) {
            MetricsDisplayScreen()
        }
        composable(AppScreen.Tests.route) {
            TestsScreen()
        }
        composable(AppScreen.Settings.route) {
            SettingsScreen()
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    }
}
