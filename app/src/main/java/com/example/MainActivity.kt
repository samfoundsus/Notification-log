package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.NotificationLogTheme
import com.example.util.NotificationAccessUtil
import com.example.util.PackageManagerAppIconProvider

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(0, 0),
            navigationBarStyle = SystemBarStyle.auto(0, 0)
        )
        setContent {
            NotificationLogApp()
        }
    }
}

@Composable
fun NotificationLogApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember(appContext) { AppDatabase.getInstance(appContext) }
    val notificationRepository = remember(database) {
        NotificationRepository(database.notificationDao())
    }
    val settingsRepository = remember(appContext) {
        SettingsRepository(appContext)
    }
    val iconProvider = remember(appContext) {
        PackageManagerAppIconProvider(appContext)
    }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(notificationRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )

    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    var hasNotificationAccess by remember {
        mutableStateOf(NotificationAccessUtil.isNotificationAccessGranted(appContext))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appContext) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess =
                    NotificationAccessUtil.isNotificationAccessGranted(appContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NotificationLogTheme(
        themeOption = settingsState.themeOption,
        dynamicColor = settingsState.dynamicColorEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (!hasNotificationAccess) {
                SetupScreen(
                    onOpenNotificationAccessSettings = {
                        NotificationAccessUtil.openNotificationListenerSettings(context)
                    },
                    onOpenBatteryOptimizationSettings = {
                        NotificationAccessUtil.openBatteryOptimizationSettings(context)
                    }
                )
            } else {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    enterTransition = {
                        fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 8 },
                                animationSpec = tween(220)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(180)) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 12 },
                                animationSpec = tween(180)
                            )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 12 },
                                animationSpec = tween(220)
                            )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(180)) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth / 8 },
                                animationSpec = tween(180)
                            )
                    }
                ) {
                    composable(route = Routes.HOME) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            iconProvider = iconProvider,
                            onNavigateToSettings = {
                                navController.navigate(Routes.SETTINGS) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(route = Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            isNotificationAccessEnabled = hasNotificationAccess,
                            onOpenNotificationAccessSettings = {
                                NotificationAccessUtil.openNotificationListenerSettings(context)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
