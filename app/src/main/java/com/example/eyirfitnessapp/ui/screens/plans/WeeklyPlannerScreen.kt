package com.example.eyirfitnessapp.ui.screens.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.PlannedMeal
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import com.example.eyirfitnessapp.ui.viewmodels.PlannerViewModel
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.util.Dates
import kotlin.math.roundToInt

/**
 * Weekly meal planner (spec §17). Goal-aware generation, persisted slots,
 * log-to-diary per slot, and ingredient extraction into the grocery list.
 */
@Composable
fun WeeklyPlannerScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: PlannerViewModel = viewModel(factory = PlannerViewModel.factory(repository))
    val weekStart by viewModel.weekStart.collectAsState()
    val plan by viewModel.plan.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val goals by viewModel.goals.collectAsState()

    /** Meal slot currently being filled with a saved recipe; null when the picker is closed. */
    var pickingFor by remember { mutableStateOf<Pair<String, MealType>?>(null) }

    val goalKcal = goals?.dailyCalorieGoal

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Meal planner", onBack = onBack)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Week of ${Dates.prettyDate(weekStart)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        goalKcal?.let {
            Text(
                "Daily goal: $it kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.md))

        notice?.let { text ->
            AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = Spacing.sm), strokeWidth = 2.dp)
                Text("  Generating…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PrimaryButton(
                text = if (plan.isEmpty()) "Generate week plan" else "Regenerate plan",
                onClick = viewModel::generatePlan,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(
            text = "Send recipe ingredients to grocery list",
            onClick = viewModel::sendPlanToGrocery,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.lg))

        if (plan.isEmpty()) {
            AppCard {
                Text(
                    "No plan yet. Generate one and each meal will be matched to your daily calorie goal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }

        (0..6).forEach { dayOffset ->
            val day = Dates.addDays(weekStart, dayOffset)
            val daySlots = plan.filter { it.date == day }
            val dayKcal = daySlots.sumOf { it.plannedKcal ?: 0.0 }

            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Dates.prettyDate(day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (daySlots.isNotEmpty()) {
                        Text(
                            "${dayKcal.roundToInt()} kcal planned" +
                                if (goalKcal != null && PlanLogic.withinGoalTolerance(dayKcal, goalKcal)) " ✓" else "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                if (daySlots.isEmpty()) {
                    Text("Nothing planned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MealType.entries.forEach { meal ->
                    val slot = daySlots.firstOrNull { it.mealType == meal }
                    PlannedSlotRow(
                        slot = slot,
                        mealLabel = meal.label,
                        onLog = slot?.let { s -> { viewModel.logSlot(s) } },
                        onRemove = slot?.let { s -> { viewModel.deleteSlot(s.id) } },
                        onAdd = { pickingFor = day to meal }
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }

    pickingFor?.let { (day, meal) ->
        RecipePickerSheet(
            recipes = recipes,
            mealLabel = meal.label,
            dayLabel = Dates.prettyDate(day),
            onPick = { recipe ->
                viewModel.assignRecipe(day, meal, recipe)
                pickingFor = null
            },
            onDismiss = { pickingFor = null }
        )
    }
}

/**
 * Choose a saved recipe for one slot (spec §17 "add meal"). Recipe-backed slots
 * are what make grocery extraction (§18) possible, so this is the entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(
    recipes: List<com.example.eyirfitnessapp.domain.model.Recipe>,
    mealLabel: String,
    dayLabel: String,
    onPick: (com.example.eyirfitnessapp.domain.model.Recipe) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            Modifier.imePadding().padding(bottom = 24.dp)
        ) {
            Text(
                "Add $mealLabel • $dayLabel",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (recipes.isEmpty()) {
                EmptyState(
                    title = "No saved recipes yet",
                    subtitle = "Build one in Plans → Recipes, then come back to plan it into this day."
                )
            } else {
                recipes.forEach { recipe ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(recipe.name) },
                        supportingContent = {
                            Text("${recipe.servings} servings • ${recipe.perServing().totalCalories.roundToInt()} kcal each")
                        },
                        trailingContent = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { onPick(recipe) }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedSlotRow(
    slot: PlannedMeal?,
    mealLabel: String,
    onLog: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    onAdd: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(mealLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = Spacing.sm))
            Text(
                slot?.let { if (it.completed) "${it.title} ✓" else it.title } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (slot?.completed == true) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    slot == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    slot.completed -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        slot?.plannedKcal?.let { kcal ->
            Text(
                "${kcal.roundToInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.xs)
            )
        }
        if (slot != null && slot.completed.not() && onLog != null) {
            TextButton(onClick = onLog) { Text("Log") }
        }
        if (slot != null && slot.recipeId == null && onAdd != null) {
            IconButton(onClick = onAdd, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Plan a recipe for $mealLabel", tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (slot == null && onAdd != null) {
            TextButton(onClick = onAdd) { Text("Add") }
        }
        if (slot != null && onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${slot.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
