package com.example.eyirfitnessapp.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.progress.ProgressLogic
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.MetricCard
import com.example.eyirfitnessapp.ui.components.SegmentedSelector
import com.example.eyirfitnessapp.ui.components.TrendLineChart
import com.example.eyirfitnessapp.ui.components.WeeklyBarChart
import com.example.eyirfitnessapp.ui.theme.CarbsColor
import com.example.eyirfitnessapp.ui.theme.FatColor
import com.example.eyirfitnessapp.ui.theme.ProteinColor
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.ProgressViewModel
import com.example.eyirfitnessapp.ui.viewmodels.RangeViewModel

/**
 * Progress & analytics (spec §11 charts, §20 dashboard): weekly calories,
 * macro split, exercise minutes and weight trend — every chart scoped by a
 * range selector (7/30/90/1y/all), with entry points to measurements and habits.
 */
@Composable
fun ProgressScreen(repository: AppRepository, onNavigate: (String) -> Unit = {}) {
    val viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(repository))
    val rangeViewModel: RangeViewModel = viewModel(factory = RangeViewModel.factory(repository))

    val weeklyCalories by viewModel.weeklyCalories.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val range by rangeViewModel.range.collectAsState()
    val rangeCalories by rangeViewModel.calories.collectAsState()
    val rangeMacros by rangeViewModel.macros.collectAsState()
    val rangeExercise by rangeViewModel.exerciseMinutes.collectAsState()
    val rangeWeight by rangeViewModel.weight.collectAsState()
    val weightStats by rangeViewModel.weightStats.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val unit = profile?.weightUnit ?: WeightUnit.KG
    val rangeLabels = ProgressLogic.ChartRange.entries.map { it.label }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.lg))

        // ── Range selector (spec §11: 7 days / 30 days / 90 days / 1 year / all) ──
        // Equal-width segments: fits any screen width without chips running off the edge.
        AppCard {
            Text("Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            SegmentedSelector(
                options = rangeLabels,
                selectedIndex = ProgressLogic.ChartRange.entries.indexOf(range),
                onSelect = { index ->
                    ProgressLogic.ChartRange.entries.getOrNull(index)?.let { rangeViewModel.setRange(it) }
                }
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        val goal = goals?.dailyCalorieGoal ?: 2000

        AppCard {
            Text(
                "Calories — ${range.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            if (rangeCalories.any { it.second > 0 }) {
                WeeklyBarChart(data = rangeCalories, goalLine = goal.toDouble())
            } else {
                EmptyState(
                    title = "No data yet",
                    subtitle = "Log food to see your calorie trend.",
                    icon = Icons.Outlined.Insights
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text(
                "Macros — ${range.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            // Intrinsic height: all three cards match the tallest — never a ragged row.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                MetricCard(
                    "Protein", "${rangeMacros["Protein"]?.toInt() ?: 0}g",
                    Modifier.weight(1f).fillMaxHeight(),
                    accent = ProteinColor, color = ProteinColor.copy(alpha = 0.10f)
                )
                Spacer(Modifier.width(8.dp))
                MetricCard(
                    "Carbs", "${rangeMacros["Carbs"]?.toInt() ?: 0}g",
                    Modifier.weight(1f).fillMaxHeight(),
                    accent = CarbsColor, color = CarbsColor.copy(alpha = 0.10f)
                )
                Spacer(Modifier.width(8.dp))
                MetricCard(
                    "Fat", "${rangeMacros["Fat"]?.toInt() ?: 0}g",
                    Modifier.weight(1f).fillMaxHeight(),
                    accent = FatColor, color = FatColor.copy(alpha = 0.10f)
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text(
                "Exercise — ${range.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            if (rangeExercise.any { it.second > 0 }) {
                WeeklyBarChart(data = rangeExercise)
            } else {
                EmptyState(
                    title = "No workouts yet",
                    subtitle = "Log exercise to see your activity trend."
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text(
                "Weight — ${range.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))
            if (rangeWeight.size >= 2) {
                TrendLineChart(
                    values = rangeWeight.map { unit.fromKg(it.second) },
                    unitLabel = unit.label
                )
            } else {
                EmptyState(
                    title = "Not enough weigh-ins",
                    subtitle = "Log your weight at least twice to see a trend.",
                    icon = Icons.Outlined.MonitorWeight
                )
            }
            if (weightStats.entries > 0) {
                Spacer(Modifier.height(Spacing.md))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    MetricCard(
                        "Change",
                        formatKg(weightStats.changeKg, unit),
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        "To goal",
                        formatKg(weightStats.toGoalKg, unit),
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        "Per week",
                        formatKg(weightStats.weeklyChangeKg, unit),
                        Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // Weekly detail (always the last 7 days, independent of range).
        AppCard {
            Text("Calories — last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            if (weeklyCalories.any { it.second > 0 }) {
                WeeklyBarChart(data = weeklyCalories, goalLine = goal.toDouble())
            } else {
                EmptyState(
                    title = "No data yet",
                    subtitle = "Log food to see your weekly calorie trend.",
                    icon = Icons.Outlined.Insights
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // Entry points: body measurements + habit tracking (spec §11, §12).
        AppCard(onClick = { onNavigate(com.example.eyirfitnessapp.Routes.MEASUREMENTS) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Body measurements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Waist • chest • hips • neck • arms • thighs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Open",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        AppCard(onClick = { onNavigate(com.example.eyirfitnessapp.Routes.HABITS) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Healthy habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Water, exercise, sleep — streaks and weekly board",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Open",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }
}

/** "+1.2" / "-0.8" / "0.0" in the user's unit; null renders as a dash. */
private fun formatKg(value: Double?, unit: WeightUnit): String {
    if (value == null) return "—"
    val converted = unit.fromKg(value)
    val sign = if (converted > 0) "+" else ""
    return "$sign%.1f".format(converted)
}
