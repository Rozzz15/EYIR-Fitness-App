package com.example.eyirfitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.StreakMilestoneOverlay
import com.example.eyirfitnessapp.ui.screens.barcode.BarcodeScannerScreen
import com.example.eyirfitnessapp.ui.screens.diary.DiaryScreen
import com.example.eyirfitnessapp.ui.screens.diary.FoodDetailScreen
import com.example.eyirfitnessapp.ui.screens.diary.FoodSearchScreen
import com.example.eyirfitnessapp.ui.screens.diary.QuickCaloriesSheet
import com.example.eyirfitnessapp.ui.screens.exercise.ExerciseScreen
import com.example.eyirfitnessapp.ui.screens.exercise.RoutinesScreen
import com.example.eyirfitnessapp.ui.screens.fasting.FastingScreen
import com.example.eyirfitnessapp.ui.screens.habits.HabitsScreen
import com.example.eyirfitnessapp.ui.screens.goals.GoalsScreen
import com.example.eyirfitnessapp.ui.screens.nutrition.NutritionScreen
import com.example.eyirfitnessapp.ui.screens.onboarding.LoginScreen
import com.example.eyirfitnessapp.ui.screens.onboarding.OnboardingScreen
import com.example.eyirfitnessapp.ui.screens.onboarding.SplashScreen
import com.example.eyirfitnessapp.ui.screens.plans.GroceryScreen
import com.example.eyirfitnessapp.ui.screens.plans.PlansScreen
import com.example.eyirfitnessapp.ui.screens.plans.RecipeBuilderScreen
import com.example.eyirfitnessapp.ui.screens.plans.SavedMealsScreen
import com.example.eyirfitnessapp.ui.screens.plans.WeeklyPlannerScreen
import com.example.eyirfitnessapp.ui.screens.profile.ProfileScreen
import com.example.eyirfitnessapp.ui.screens.progress.MeasurementsScreen
import com.example.eyirfitnessapp.ui.screens.progress.ProgressScreen
import com.example.eyirfitnessapp.ui.screens.progress.WeightScreen
import com.example.eyirfitnessapp.ui.screens.settings.SettingsScreen
import com.example.eyirfitnessapp.ui.screens.today.TodayScreen
import com.example.eyirfitnessapp.ui.screens.today.WaterScreen
import com.example.eyirfitnessapp.ui.theme.BrandGradient
import com.example.eyirfitnessapp.ui.theme.EYIRFitnessAppTheme
import com.example.eyirfitnessapp.ui.theme.Elevation
import com.example.eyirfitnessapp.ui.viewmodels.MainViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * POST_NOTIFICATIONS is a runtime permission from Android 13 (spec §27:
 * reminders only post when the user allows them).
 */
private const val NOTIFICATION_PERMISSION_REQUEST = 4001

/**
 * Centralized route definitions (spec §34 navigation rules).
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"

    const val TODAY = "today"
    const val DIARY = "diary"
    const val PROGRESS = "progress"
    const val PLANS = "plans"
    const val PROFILE = "profile"

    const val FOOD_SEARCH = "food_search"
    const val FOOD_SEARCH_PATTERN = "food_search?meal={meal}&date={date}"
    const val FOOD_DETAIL = "food_detail"
    const val FOOD_DETAIL_PATTERN = "food_detail?foodId={foodId}"
    const val BARCODE = "barcode"
    const val WATER = "water"
    const val WEIGHT = "weight"
    const val EXERCISE = "exercise"
    const val ROUTINES = "routines"
    const val MEASUREMENTS = "measurements"
    const val HABITS = "habits"
    const val FASTING = "fasting"
    const val RECIPES = "recipes"
    const val PLANNER = "planner"
    const val GROCERY = "grocery"
    const val SAVED_MEALS = "saved_meals"
    const val GOALS = "goals"
    const val NUTRITION = "nutrition"
    const val SETTINGS = "settings"

    /** Always emits the full query string so it matches [FOOD_SEARCH_PATTERN]. */
    fun foodSearch(
        meal: com.example.eyirfitnessapp.domain.model.MealType? = null,
        date: String? = null
    ): String = "$FOOD_SEARCH?meal=${meal?.name ?: ""}&date=${date ?: ""}"

    /** Nutrition-facts detail for one food; matches [FOOD_DETAIL_PATTERN]. */
    fun foodDetail(foodId: Long): String = "$FOOD_DETAIL?foodId=$foodId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppRepository.get(applicationContext)

        // Reminders (spec §27): register the channel, ask for the 33+ runtime
        // permission once, and keep alarms in sync with the stored preferences.
        com.example.eyirfitnessapp.notifications.Notifications.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        lifecycleScope.launch { com.example.eyirfitnessapp.notifications.Notifications.rescheduleAll(applicationContext) }

        setContent {
            // Spec §22 Preferences → Theme: follow the stored mode, defaulting to system.
            val themeMode by remember {
                repository.observeProfile()
                    .map { it?.themeMode ?: com.example.eyirfitnessapp.domain.model.ThemeMode.SYSTEM }
            }.collectAsState(initial = com.example.eyirfitnessapp.domain.model.ThemeMode.SYSTEM)
            EYIRFitnessAppTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                RootApp(repository)
            }
        }
    }

    /**
     * Spec §27: notifications are opt-in at the OS level on Android 13+. The
     * settings toggles stay authoritative — this only gates whether the OS
     * will actually show the reminders the user enabled.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_TABS = listOf(
    TabItem(Routes.TODAY, "Today", Icons.Default.Home),
    TabItem(Routes.DIARY, "Diary", Icons.Default.Restaurant),
    TabItem(Routes.PROGRESS, "Progress", Icons.Default.ShowChart),
    TabItem(Routes.PLANS, "Plans", Icons.Default.MenuBook),
    TabItem(Routes.PROFILE, "Profile", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootApp(repository: AppRepository) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(repository))

    // Seed food/exercise reference data on first launch (offline-first, spec §26)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repository.seedIfEmpty()
    }

    val start by mainViewModel.start.collectAsState()

    var showQuickAdd by remember { mutableStateOf(false) }
    var showQuickCalories by remember { mutableStateOf(false) }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showChrome = currentRoute in BOTTOM_TABS.map { it.route }

    // Spec §3: the quick-add FAB is persistent — it stays available on detail
    // screens too, and only steps aside for splash/onboarding/sign-in.
    val showFab = currentRoute != null &&
        currentRoute != Routes.SPLASH &&
        currentRoute != Routes.ONBOARDING &&
        currentRoute != Routes.LOGIN

    // App-wide message surface (spec §6 Rule 5 + §34): repository read failures
    // and one-shot confirmations from screens that pop on success both land here.
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(snackbarHostState) {
        launch { com.example.eyirfitnessapp.util.AppMessages.errors.collect { snackbarHostState.showSnackbar(it) } }
        launch { com.example.eyirfitnessapp.util.AppMessages.notices.collect { snackbarHostState.showSnackbar(it) } }
    }

    if (!start.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Achievement celebrations: the whole app blurs + recedes behind the overlay.
    val celebration by com.example.eyirfitnessapp.util.CelebrationBus.active.collectAsState()
    val blurProgress by animateFloatAsState(
        targetValue = if (celebration != null) 1f else 0f,
        animationSpec = tween(300),
        label = "celebrationBlur"
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f - 0.045f * blurProgress
                    scaleY = 1f - 0.045f * blurProgress
                    transformOrigin = TransformOrigin.Center
                }
                .blur(26.dp * blurProgress)
        ) {
            Scaffold(
                snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showChrome) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                // Pop up to the Today root: `findStartDestination()` is
                                // SPLASH, which was removed at launch, so this popUpTo
                                // used to be a no-op and every tab tap PUSHED — the stack
                                // grew ([today, diary, today, …]) and any stray pop
                                // revealed Home unexpectedly.
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.TODAY) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(Elevation.Floating, RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(BrandGradient), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showQuickAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Quick add", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val next = if (start.onboarded) Routes.TODAY else Routes.ONBOARDING
                        navController.navigate(next) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    repository = repository,
                    onComplete = {
                        navController.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                )
            }
            composable(Routes.LOGIN) { LoginScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.TODAY) {
                TodayScreen(repository, onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.DIARY) {
                DiaryScreen(repository, onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(repository, onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.PLANS) {
                PlansScreen(repository, onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(repository, onNavigate = { route -> navController.navigate(route) })
            }
            composable(Routes.GOALS) {
                GoalsScreen(repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.NUTRITION) {
                NutritionScreen(repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                    onWiped = {
                        // Data gone → start over at onboarding (fresh single-user DB).
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Routes.FOOD_SEARCH_PATTERN,
                arguments = listOf(
                    androidx.navigation.navArgument("meal") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument("date") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val mealArg = entry.arguments?.getString("meal").orEmpty()
                val dateArg = entry.arguments?.getString("date").orEmpty()
                FoodSearchScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    initialMeal = com.example.eyirfitnessapp.domain.model.MealType.entries
                        .firstOrNull { it.name == mealArg },
                    targetDate = dateArg.ifBlank { null },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetail(foodId)) }
                )
            }
            composable(
                route = Routes.FOOD_DETAIL_PATTERN,
                arguments = listOf(
                    androidx.navigation.navArgument("foodId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val foodId = entry.arguments?.getLong("foodId") ?: 0L
                FoodDetailScreen(repository, foodId = foodId, onBack = { navController.popBackStack() })
            }
            composable(Routes.BARCODE) {
                BarcodeScannerScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onSearchManually = {
                        navController.navigate(Routes.foodSearch()) { launchSingleTop = true }
                    }
                )
            }
            composable(Routes.WATER) { WaterScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.WEIGHT) { WeightScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.EXERCISE) {
                ExerciseScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenRoutines = { navController.navigate(Routes.ROUTINES) { launchSingleTop = true } }
                )
            }
            composable(Routes.ROUTINES) { RoutinesScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.MEASUREMENTS) { MeasurementsScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.HABITS) { HabitsScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.FASTING) { FastingScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.RECIPES) { RecipeBuilderScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.PLANNER) { WeeklyPlannerScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.GROCERY) { GroceryScreen(repository, onBack = { navController.popBackStack() }) }
            composable(Routes.SAVED_MEALS) { SavedMealsScreen(repository, onBack = { navController.popBackStack() }) }
            }
        }
        }

        celebration?.let { c ->
            StreakMilestoneOverlay(
                habitLabel = c.habitLabel,
                days = c.days,
                onDismiss = { com.example.eyirfitnessapp.util.CelebrationBus.clear() }
            )
        }
    }

    if (showFab && showQuickAdd) {
        ModalBottomSheet(onDismissRequest = { showQuickAdd = false }) {
            QuickAddMenu(
                onAction = { route ->
                    showQuickAdd = false
                    navController.navigate(route) { launchSingleTop = true }
                },
                // Its own sheet: reusing the Add Food route made this a silent
                // no-op whenever that screen was already open (launchSingleTop).
                onQuickCalories = {
                    showQuickAdd = false
                    showQuickCalories = true
                }
            )
        }
    }

    if (showFab && showQuickCalories) {
        QuickCaloriesSheet(
            repository = repository,
            onDismiss = { showQuickCalories = false }
        )
    }
}

/** Quick add menu (spec §3): the fastest logging entry points. */
@Composable
private fun QuickAddMenu(onAction: (String) -> Unit, onQuickCalories: () -> Unit) {
    data class Entry(val label: String, val icon: ImageVector, val onClick: () -> Unit)
    val entries = listOf(
        Entry("Add Food", Icons.Default.RestaurantMenu) { onAction(Routes.foodSearch()) },
        Entry("Scan Barcode", Icons.Default.QrCodeScanner) { onAction(Routes.BARCODE) },
        Entry("Log Water", Icons.Default.LocalDrink) { onAction(Routes.WATER) },
        Entry("Log Exercise", Icons.Default.DirectionsRun) { onAction(Routes.EXERCISE) },
        Entry("Log Weight", Icons.Default.MonitorWeight) { onAction(Routes.WEIGHT) },
        Entry("Quick Calories", Icons.Default.Calculate, onQuickCalories),
        Entry("Start Fast", Icons.Default.Schedule) { onAction(Routes.FASTING) },
        Entry("Measurements", Icons.Default.Straighten) { onAction(Routes.MEASUREMENTS) },
        Entry("Habits", Icons.Default.TaskAlt) { onAction(Routes.HABITS) }
    )
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.imePadding().padding(bottom = 24.dp)
    ) {
        Text(
            "Quick add",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        entries.forEach { entry ->
            QuickAddRow(label = entry.label, icon = entry.icon, onClick = entry.onClick)
        }
    }
}

@Composable
private fun QuickAddRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}


