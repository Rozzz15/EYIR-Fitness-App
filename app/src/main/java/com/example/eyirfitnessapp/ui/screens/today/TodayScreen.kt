package com.example.eyirfitnessapp.ui.screens.today

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.Routes
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.CalorieRing
import com.example.eyirfitnessapp.ui.components.ProgressBar
import com.example.eyirfitnessapp.ui.components.tintedSurface
import com.example.eyirfitnessapp.ui.theme.BrandGradient
import com.example.eyirfitnessapp.ui.theme.CardShape
import com.example.eyirfitnessapp.ui.theme.Elevation
import com.example.eyirfitnessapp.ui.theme.HeroShape
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Radii
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.theme.extended
import com.example.eyirfitnessapp.ui.viewmodels.TodayViewModel
import com.example.eyirfitnessapp.util.Dates
import java.util.Calendar

/**
 * Spec §20: Today answers "How am I doing today?"
 *
 * Layout reads top-down as: who am I → how am I doing (hero ring) → what have
 * I eaten (macros + meals) → what have I burned (water + exercise) → how am I
 * trending (weight + habits) → where to next (explore).
 */
@Composable
fun TodayScreen(
    repository: AppRepository,
    onNavigate: (String) -> Unit
) {
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(repository))
    val summary by viewModel.summary.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val habitProgress by viewModel.habitProgress.collectAsState()

    /** Exercise entry awaiting destructive confirmation (spec §34). */
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    /** Food entry awaiting destructive confirmation — accidental meal adds (spec §34). */
    var pendingFoodDelete by remember { mutableStateOf<FoodLog?>(null) }

    val goals = summary?.goal
    val consumed = summary?.caloriesConsumed ?: 0.0
    val burned = summary?.caloriesBurned ?: 0.0
    val goal = goals?.dailyCalorieGoal ?: 2000
    val addExercise = profile?.exerciseCaloriesAddToBudget != false
    val budget = summary?.let { NutritionEngine.budget(it, addExercise) } ?: (goal + burned)
    val remaining = (budget - consumed).toInt()
    val progress = if (budget > 0) (consumed / budget).toFloat() else 0f
    val usedPercent = if (budget > 0) ((consumed / budget) * 100).toInt() else 0

    val unit = profile?.weightUnit ?: WeightUnit.KG

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
    ) {
        DashboardHeader(
            name = profile?.name,
            streak = streak,
            onOpenProfile = { onNavigate(Routes.PROFILE) }
        )
        Spacer(Modifier.height(Spacing.xl))

        // 1. Calorie ring (spec §20) — signature gradient hero card
        HeroCalorieCard(
            progress = progress,
            remaining = remaining,
            goal = goal,
            burned = burned,
            consumed = consumed,
            usedPercent = usedPercent
        )
        Spacer(Modifier.height(Spacing.xxl))

        // 2. Macros
        if (goals != null) {
            SectionHeader("Macros", actionLabel = "Edit goals", onAction = { onNavigate(Routes.GOALS) })
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                MacroTile("Protein", summary?.protein ?: 0.0, goals.proteinGrams, MaterialTheme.extended.protein, Modifier.weight(1f))
                MacroTile("Carbs", summary?.carbs ?: 0.0, goals.carbsGrams, MaterialTheme.extended.carbs, Modifier.weight(1f))
                MacroTile("Fat", summary?.fat ?: 0.0, goals.fatGrams, MaterialTheme.extended.fat, Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.xxl))
        }

        // 3. Meals
        SectionHeader("Meals", actionLabel = "Log food", onAction = { onNavigate(Routes.foodSearch()) })
        MealType.entries.forEach { meal ->
            val mealLogs = logs.filter { it.mealType == meal }
            MealCard(
                meal = meal,
                logs = mealLogs,
                // Empty meal → straight to logging; already has food → diary so entries can be managed/removed.
                onClick = {
                    if (mealLogs.isEmpty()) onNavigate(Routes.foodSearch(meal = meal))
                    else onNavigate(Routes.DIARY)
                },
                onDeleteFood = { pendingFoodDelete = it }
            )
            Spacer(Modifier.height(Spacing.md))
        }

        // 4. Water (spec §12) — one tap to log a glass
        WaterCard(
            consumedMl = summary?.waterMl ?: 0,
            goalMl = profile?.waterGoalMl ?: 2500,
            onOpen = { onNavigate(Routes.WATER) },
            onQuickAdd = viewModel::addWaterQuick
        )
        Spacer(Modifier.height(Spacing.md))

        // 5. Exercise — show what was actually logged, not just the total
        ExerciseCard(
            burned = burned,
            logs = exerciseLogs,
            unit = unit,
            onOpen = { onNavigate(Routes.EXERCISE) },
            onDelete = { pendingDelete = it }
        )
        Spacer(Modifier.height(Spacing.xxl))

        // 6. Body trends
        SectionHeader("Progress")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile(
                title = "Weight",
                value = "%.1f %s".format(unit.fromKg(profile?.currentWeightKg ?: 0.0), unit.label),
                sub = "Goal %.1f %s".format(unit.fromKg(profile?.goalWeightKg ?: 0.0), unit.label),
                icon = Icons.Outlined.MonitorWeight,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.WEIGHT) }
            )
            MetricTile(
                title = "Habits",
                value = if (habitProgress.total == 0) "—" else "${habitProgress.done} / ${habitProgress.total}",
                sub = when {
                    habitProgress.total == 0 -> "None tracked yet"
                    habitProgress.allDone -> "All done today"
                    else -> "checked off today"
                },
                icon = Icons.Outlined.TaskAlt,
                tint = MaterialTheme.extended.warning,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.HABITS) }
            )
        }
        Spacer(Modifier.height(Spacing.md))

        // 7. Everything else, one tap away
        SectionHeader("Explore")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionTile(
                title = "Nutrition",
                subtitle = "Calories, macros & nutrients",
                icon = Icons.Outlined.Insights,
                tint = MaterialTheme.extended.protein,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.NUTRITION) }
            )
            ActionTile(
                title = "Fasting",
                subtitle = "Start or track a fast",
                icon = Icons.Outlined.Schedule,
                tint = MaterialTheme.extended.burn,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Routes.FASTING) }
            )
        }
        Spacer(Modifier.height(Spacing.md))
        DiaryCard(
            loggedToday = logs.size,
            calories = consumed,
            onClick = { onNavigate(Routes.DIARY) }
        )
        // Clear the quick-add FAB that floats over the bottom of the scroll.
        Spacer(Modifier.height(84.dp))
    }

    pendingDelete?.let { id ->
        val log = exerciseLogs.firstOrNull { it.id == id }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this workout?") },
            text = { Text("${log?.name ?: "This entry"} will be removed from today's exercise.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteExerciseLog(id)
                    pendingDelete = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    pendingFoodDelete?.let { log ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingFoodDelete = null },
            title = { Text("Remove ${log.name}?") },
            text = {
                Text(
                    "Removes this entry from ${log.mealType.label} and today's calorie total. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteFoodLog(log.id)
                    pendingFoodDelete = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingFoodDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    name: String?,
    streak: Int,
    onOpenProfile: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting().uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                name?.takeIf { it.isNotBlank() } ?: "Welcome back",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    Dates.prettyDate(Dates.today()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        StreakBadge(streak)
        Spacer(Modifier.width(Spacing.sm))
        ProfileAvatar(name, onOpenProfile)
    }
}

@Composable
private fun ProfileAvatar(name: String?, onClick: () -> Unit) {
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(BrandGradient))
            .clickable(onClickLabel = "Open profile", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (initial != null) {
            Text(
                initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = "Open profile", tint = Color.White)
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    val active = days > 0
    Surface(
        shape = PillShape,
        color = if (active) MaterialTheme.extended.warning.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (active) MaterialTheme.extended.warning
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (active) "$days day streak" else "No streak yet",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.extended.warning
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

// ── Hero: calories ──────────────────────────────────────────────────────────

@Composable
private fun HeroCalorieCard(
    progress: Float,
    remaining: Int,
    goal: Int,
    burned: Double,
    consumed: Double,
    usedPercent: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "calorieRing"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(Elevation.Floating, HeroShape, clip = false)
            .clip(HeroShape)
            .background(Brush.linearGradient(BrandGradient))
    ) {
        // Decorative glow — clipped to the card shape for depth
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = (-78).dp)
                .size(190.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-66).dp, y = 74.dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassChip("TODAY'S BUDGET")
                Spacer(Modifier.weight(1f))
                GlassChip("$usedPercent% used")
            }
            Spacer(Modifier.height(Spacing.lg))

            CalorieRing(
                progress = animatedProgress,
                centerValue = if (remaining >= 0) "$remaining" else "+${-remaining}",
                centerLabel = if (remaining >= 0) "Calories left" else "Calories over",
                strokeColor = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                gradient = listOf(Color.White, Color(0xFFD9F99D)),
                valueColor = Color.White,
                labelColor = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radii.Large))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(Icons.Outlined.Flag, "$goal", "Goal")
                HeroDivider()
                HeroStat(Icons.Outlined.LocalFireDepartment, "+${burned.toInt()}", "Burned")
                HeroDivider()
                HeroStat(Icons.Outlined.Restaurant, "${consumed.toInt()}", "Eaten")
            }
        }
    }
}

@Composable
private fun GlassChip(text: String) {
    Surface(shape = PillShape, color = Color.White.copy(alpha = 0.18f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HeroStat(icon: ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.22f))
    )
}

// ── Section scaffolding ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(start = Spacing.sm, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Tinted circular icon chip used across the dashboard. */
@Composable
private fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Radii.Medium))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size.value * 0.5).dp))
    }
}

/** Small pill used for the "at a glance" numbers on each card. */
@Composable
private fun StatPill(text: String, color: Color) {
    Surface(shape = PillShape, color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ── Macros ──────────────────────────────────────────────────────────────────

@Composable
private fun MacroTile(
    label: String,
    consumed: Double,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed / goal).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "macroBar"
    )
    val left = (goal - consumed).toInt().coerceAtLeast(0)

    Surface(
        modifier = modifier,
        shape = CardShape,
        color = tintedSurface(color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        shadowElevation = Elevation.Card
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(color, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${consumed.toInt()}g",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "$left g left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress = animatedProgress, color = color)
        }
    }
}

// ── Meals ───────────────────────────────────────────────────────────────────

private data class MealVisuals(val icon: ImageVector, val color: Color)

private fun mealVisuals(meal: MealType): MealVisuals = when (meal) {
    MealType.BREAKFAST -> MealVisuals(Icons.Outlined.FreeBreakfast, Color(0xFFF59E0B))
    MealType.LUNCH -> MealVisuals(Icons.Outlined.LunchDining, Color(0xFF16A34A))
    MealType.DINNER -> MealVisuals(Icons.Outlined.DinnerDining, Color(0xFF7C3AED))
    MealType.SNACKS -> MealVisuals(Icons.Outlined.Cookie, Color(0xFFF43F5E))
}

@Composable
private fun MealCard(
    meal: MealType,
    logs: List<FoodLog>,
    onClick: () -> Unit,
    onDeleteFood: (FoodLog) -> Unit = {}
) {
    val visuals = mealVisuals(meal)
    val kcal = logs.sumOf { it.calories }.toInt()

    AppCard(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(visuals.icon, visuals.color, size = 46.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    meal.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (logs.isEmpty()) "Nothing logged yet"
                    else "${logs.size} item${if (logs.size == 1) "" else "s"} logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (logs.isNotEmpty()) {
                Spacer(Modifier.width(Spacing.sm))
                StatPill("$kcal kcal", visuals.color)
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (logs.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.sm))
            logs.take(2).forEach { log ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        log.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "${log.calories.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onDeleteFood(log) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove ${log.name}",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (logs.size > 2) {
                Text(
                    "+${logs.size - 2} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Water ───────────────────────────────────────────────────────────────────

@Composable
private fun WaterCard(
    consumedMl: Int,
    goalMl: Int,
    onOpen: () -> Unit,
    onQuickAdd: (Int) -> Unit
) {
    val water = MaterialTheme.extended.water
    val progress = if (goalMl > 0) consumedMl.toFloat() / goalMl else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "waterBar"
    )

    AppCard(onClick = onOpen, color = water.copy(alpha = 0.10f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.WaterDrop, water)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Water", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "%.1f L of %.1f L".format(consumedMl / 1000.0, goalMl / 1000.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatPill("${(progress * 100).toInt()}%", water)
        }
        Spacer(Modifier.height(Spacing.lg))
        ProgressBar(progress = animatedProgress, color = water)
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WaterQuickAddChip("+250 ml", water) { onQuickAdd(250) }
            Spacer(Modifier.width(Spacing.sm))
            WaterQuickAddChip("+500 ml", water) { onQuickAdd(500) }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "History",
                    style = MaterialTheme.typography.labelLarge,
                    color = water,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = water
                )
            }
        }
    }
}

@Composable
private fun WaterQuickAddChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.40f)),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Exercise ────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    burned: Double,
    logs: List<ExerciseLog>,
    unit: WeightUnit,
    onOpen: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val burn = MaterialTheme.extended.burn

    AppCard(onClick = onOpen, color = burn.copy(alpha = 0.10f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.AutoMirrored.Outlined.DirectionsRun, burn)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (logs.isEmpty()) "Nothing burned yet"
                    else "${logs.size} workout${if (logs.size == 1) "" else "s"} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatPill("+${burned.toInt()} kcal", burn)
        }

        if (logs.isEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Tap to log a workout — burned calories go back into today's budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        logs.forEach { log ->
            Spacer(Modifier.height(Spacing.sm))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(log.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        log.detailLabel(unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${log.caloriesBurned.toInt()} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = burn
                )
                IconButton(onClick = { onDelete(log.id) }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove ${log.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** "30 min" / "3 × 10 @ 40 kg" — one line describing how the entry was logged. */
private fun ExerciseLog.detailLabel(
    unit: WeightUnit = WeightUnit.KG
): String = when {
    sets != null && reps != null ->
        "$sets × $reps" + (weightKg?.let { " @ ${"%.0f".format(unit.fromKg(it))} ${unit.label}" } ?: "")
    durationMinutes != null -> "${durationMinutes.toInt()} min"
    else -> "Logged"
}

// ── Progress + explore ──────────────────────────────────────────────────────

@Composable
private fun MetricTile(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, size = 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(modifier = modifier, onClick = onClick) {
        IconBadge(icon, tint, size = 42.dp)
        Spacer(Modifier.height(Spacing.md))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiaryCard(
    loggedToday: Int,
    calories: Double,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.AutoMirrored.Outlined.MenuBook, MaterialTheme.colorScheme.primary, size = 46.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Full diary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$loggedToday item${if (loggedToday == 1) "" else "s"} logged · ${calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
