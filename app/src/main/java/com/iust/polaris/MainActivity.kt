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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.iust.polaris.data.local.SettingsManager
import com.iust.polaris.ui.components.HandlePermissions
import com.iust.polaris.ui.screens.MetricsCollectionScreen
import com.iust.polaris.ui.screens.MetricsDisplayScreen
import com.iust.polaris.ui.screens.SettingsScreen
import com.iust.polaris.ui.screens.TestsScreen
import com.iust.polaris.ui.theme.PolarisAppTheme
import com.iust.polaris.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Define all possible navigation destinations
sealed class AppScreen(val route: String, val label: String, val icon: ImageVector? = null) {
    object Monitor : AppScreen("monitor", "Monitor", Icons.Filled.Home)
    object Metrics : AppScreen("metrics", "Metrics", Icons.Filled.List)
    object Tests : AppScreen("tests", "Tests", Icons.Filled.Check)
    object Settings : AppScreen("settings", "Settings") // Settings has no bottom nav icon
}

// Define only the items that appear in the bottom navigation bar
val bottomNavItems = listOf(
    AppScreen.Monitor,
    AppScreen.Metrics,
    AppScreen.Tests
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }.toList()

        setContent {
            val themePreference by settingsManager.themePreferenceFlow.collectAsState(initial = "System")

            val useDarkTheme = when (themePreference) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme() // "System" or any other value
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

    // Determine the current top-level screen to manage UI elements like titles
    val currentScreen = bottomNavItems.find { it.route == currentDestination?.route }
        ?: if (currentDestination?.route == AppScreen.Settings.route) AppScreen.Settings else null

    // Hoist the ViewModel to the Scaffold level to share it between screens
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Effect to show snackbar messages from the ViewModel
    LaunchedEffect(Unit) {
        mainViewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message = message, withDismissAction = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen?.label ?: "Polaris") },
                actions = {
                    // Show settings icon only on top-level screens
                    if (currentDestination?.route in bottomNavItems.map { it.route }) {
                        IconButton(onClick = { navController.navigate(AppScreen.Settings.route) }) {
                            Icon(Icons.Filled.Settings, "Settings")
                        }
                    }
                },
                navigationIcon = {
                    // Show back arrow only on the Settings screen
                    if (currentDestination?.route == AppScreen.Settings.route) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Only show the bottom bar for top-level destinations
            if (currentDestination?.route in bottomNavItems.map { it.route }) {
                BottomNavigationBar(navController = navController, currentDestination = currentDestination)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            mainViewModel = mainViewModel, // Pass the shared ViewModel
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentDestination: androidx.navigation.NavDestination?) {
    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
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
fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Monitor.route,
        modifier = modifier
    ) {
        composable(AppScreen.Monitor.route) {
            MetricsCollectionScreen(
                uiState = mainViewModel.uiState.collectAsState().value,
                onToggleCollection = mainViewModel::onToggleCollection
            )
        }
        composable(AppScreen.Metrics.route) {
            MetricsDisplayScreen(
                uiState = mainViewModel.uiState.collectAsState().value,
                onSyncClicked = mainViewModel::onSyncClicked
            )
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
