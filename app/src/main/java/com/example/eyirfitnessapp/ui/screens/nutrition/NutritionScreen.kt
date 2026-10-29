package com.example.eyirfitnessapp.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.MacroCard
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SegmentedSelector
import com.example.eyirfitnessapp.ui.components.tintedSurface
import com.example.eyirfitnessapp.ui.theme.CarbsColor
import com.example.eyirfitnessapp.ui.theme.FatColor
import com.example.eyirfitnessapp.ui.theme.ProteinColor
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.NutritionViewModel

private val TABS = listOf("Calories", "Macros", "Nutrients", "Meals", "Trends")

/**
 * Nutrition page (spec §20): five tabs over today's data plus a 7-day trend
 * summary. Every number comes from the same summaries the dashboard uses.
 */
@Composable
fun NutritionScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.factory(repository))
    val summary by viewModel.summary.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val nutrients by viewModel.nutrients.collectAsState()
    val week by viewModel.weekReport.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    val addExercise = profile?.exerciseCaloriesAddToBudget ?: true

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Nutrition", onBack = onBack)
        Spacer(Modifier.height(Spacing.md))

        // Equal-width segments so the five tabs fit every screen width.
        SegmentedSelector(
            options = TABS,
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(Spacing.lg))

        when (tab) {
            0 -> CaloriesTab(
                summary = summary,
                addExercise = addExercise,
                onToggleExercise = { viewModel.setExerciseCaloriesInBudget(it) }
            )
            1 -> MacrosTab(summary = summary, goalsCalories = goals?.dailyCalorieGoal)
            2 -> NutrientsTab(nutrients = nutrients, hasLogs = logs.isNotEmpty())
            3 -> MealsTab(logs = logs)
            else -> TrendsTab(week = week, unit = profile?.weightUnit)
        }

        Spacer(Modifier.height(Spacing.xxxl))
    }
}

@Composable
private fun CaloriesTab(
    summary: com.example.eyirfitnessapp.domain.model.DailySummary?,
    addExercise: Boolean,
    onToggleExercise: (Boolean) -> Unit
) {
    if (summary == null) {
        EmptyState(title = "Nothing yet", subtitle = "Log a meal to see today's calories.")
        return
    }
    AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
        Text("Calories remaining", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "${NutritionEngine.caloriesRemaining(summary, addExercise)}",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.md))
        CalorieLine("Goal", "${summary.goal?.dailyCalorieGoal ?: 0} kcal")
        CalorieLine("Exercise", "+${summary.caloriesBurned.toInt()} kcal")
        CalorieLine("Food", "−${summary.caloriesConsumed.toInt()} kcal")
        CalorieLine("Water", "${summary.waterMl} ml")
    }
    Spacer(Modifier.height(Spacing.md))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Add exercise calories to budget", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "When off, only your base goal sets the budget (spec §9).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = addExercise, onCheckedChange = onToggleExercise)
        }
    }
}

@Composable
private fun CalorieLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MacrosTab(
    summary: com.example.eyirfitnessapp.domain.model.DailySummary?,
    goalsCalories: Int?
) {
    if (summary == null) {
        EmptyState(title = "Nothing yet", subtitle = "Log a meal to see today's macros.")
        return
    }
    val goalProtein = summary.goal?.proteinGrams ?: 0
    val goalCarbs = summary.goal?.carbsGrams ?: 0
    val goalFat = summary.goal?.fatGrams ?: 0

    MacroCard("Protein", summary.protein, goalProtein, ProteinColor, Modifier.fillMaxWidth())
    Spacer(Modifier.height(Spacing.sm))
    MacroCard("Carbs", summary.carbs, goalCarbs, CarbsColor, Modifier.fillMaxWidth())
    Spacer(Modifier.height(Spacing.sm))
    MacroCard("Fat", summary.fat, goalFat, FatColor, Modifier.fillMaxWidth())

    Spacer(Modifier.height(Spacing.md))
    AppCard {
        Text("Share of calories eaten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        val eaten = summary.caloriesConsumed
        val pctOfCalories = { grams: Double, kcalPerGram: Double ->
            if (eaten > 0) grams * kcalPerGram / eaten * 100 else 0.0
        }
        CalorieLine("Protein", "${"%.0f".format(pctOfCalories(summary.protein, 4.0))}%")
        CalorieLine("Carbs", "${"%.0f".format(pctOfCalories(summary.carbs, 4.0))}%")
        CalorieLine("Fat", "${"%.0f".format(pctOfCalories(summary.fat, 9.0))}%")
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Daily calorie target: ${goalsCalories ?: 0} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NutrientsTab(
    nutrients: com.example.eyirfitnessapp.domain.model.NutrientTotals,
    hasLogs: Boolean
) {
    if (!hasLogs) {
        EmptyState(title = "Nothing yet", subtitle = "Log a meal to see fiber, sugar and sodium.")
        return
    }
    AppCard {
        Text("Micronutrients today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        CalorieLine("Fiber", "${"%.1f".format(nutrients.fiber)} g")
        CalorieLine("Sugar", "${"%.1f".format(nutrients.sugar)} g")
        CalorieLine("Sodium", "${"%.0f".format(nutrients.sodium)} mg")
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "No personal targets are set for these yet — the values come from the " +
                "nutrition facts of the foods you logged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(Spacing.md))
    EmptyState(
        title = "Potassium, calcium, iron…",
        subtitle = "Not tracked in this build — only what the food database stores is shown."
    )
}

@Composable
private fun MealsTab(logs: List<com.example.eyirfitnessapp.domain.model.FoodLog>) {
    if (logs.isEmpty()) {
        EmptyState(title = "No meals logged", subtitle = "Everything you log today shows up here by meal.")
        return
    }
    MealType.entries.forEach { meal ->
        val mealLogs = logs.filter { it.mealType == meal }
        if (mealLogs.isEmpty()) return@forEach
        val visuals = mealVisuals(meal)
        AppCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(visuals.color.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            visuals.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = visuals.color
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        meal.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "${mealLogs.sumOf { it.calories }.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            mealLogs.forEach { log ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            log.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${log.quantity.stripTrailingZero()} ${log.servingUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${log.calories.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = Spacing.sm)
                    )
                    Text(
                        "P${log.protein.toInt()} C${log.carbs.toInt()} F${log.fat.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

private data class MealVisuals(val icon: ImageVector, val color: Color)

private fun mealVisuals(meal: MealType): MealVisuals = when (meal) {
    MealType.BREAKFAST -> MealVisuals(Icons.Outlined.FreeBreakfast, Color(0xFFF59E0B))
    MealType.LUNCH -> MealVisuals(Icons.Outlined.LunchDining, Color(0xFF16A34A))
    MealType.DINNER -> MealVisuals(Icons.Outlined.DinnerDining, Color(0xFF7C3AED))
    MealType.SNACKS -> MealVisuals(Icons.Outlined.Cookie, Color(0xFFF43F5E))
}

private fun Double.stripTrailingZero(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')

@Composable
private fun TrendsTab(
    week: NutritionViewModel.WeeklyReport,
    unit: com.example.eyirfitnessapp.domain.model.WeightUnit?
) {
    AppCard {
        Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        CalorieLine("Average calories", "${week.avgCalories.toInt()} kcal")
        CalorieLine("Average protein", "${week.avgProtein.toInt()} g")
        CalorieLine("Average carbs", "${week.avgCarbs.toInt()} g")
        CalorieLine("Average fat", "${week.avgFat.toInt()} g")
    }
    Spacer(Modifier.height(Spacing.sm))
    AppCard {
        Text("Consistency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        CalorieLine("Days logged", "${week.loggedDays} / 7")
        CalorieLine("Days with exercise", "${week.exerciseDays} / 7")
        week.weightDeltaKg?.let { delta ->
            val displayUnit = unit ?: com.example.eyirfitnessapp.domain.model.WeightUnit.KG
            val shown = displayUnit.fromKg(delta)
            val arrow = if (delta > 0) "up" else if (delta < 0) "down" else "steady"
            CalorieLine(
                "Weight change",
                "${if (shown >= 0) "+" else ""}${"%.1f".format(shown)} ${displayUnit.label} ($arrow)"
            )
        } ?: run {
            CalorieLine("Weight change", "Needs 2+ weigh-ins")
        }
    }
}
