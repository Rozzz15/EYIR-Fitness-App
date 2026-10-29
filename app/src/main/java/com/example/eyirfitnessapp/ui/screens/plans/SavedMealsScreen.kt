package com.example.eyirfitnessapp.ui.screens.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.FoodSearchViewModel
import com.example.eyirfitnessapp.util.Dates

/**
 * Saved meals (spec §16): one-tap logging of previously logged foods.
 * The target meal is chosen up front and echoed back after every add, so a
 * log never lands somewhere the user didn't expect (spec §6 Rule 5).
 */
@Composable
fun SavedMealsScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: FoodSearchViewModel = viewModel(factory = FoodSearchViewModel.factory(repository))
    val recent by viewModel.recent.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val notice by viewModel.notice.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Saved meals", onBack = onBack)
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Foods you've logged before. Pick a meal, then tap to log again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)
        ) {
            MealType.entries.forEach { m ->
                FilterChip(
                    selected = mealType == m,
                    onClick = { viewModel.setMealType(m) },
                    label = { Text(m.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            "Logging to ${Dates.prettyDate(viewModel.targetDateLabel)} • ${mealType.label}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
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

        if (recent.isEmpty()) {
            EmptyState(
                title = "No saved meals yet",
                subtitle = "Foods you log appear here for one-tap re-logging."
            )
        }

        recent.groupBy { it.name }.forEach { (name, logs) ->
            val first = logs.first()
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Column {
                        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${first.quantity} × ${first.servingSize} ${first.servingUnit} • ${first.calories.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton(
                    text = "Log to ${mealType.label}",
                    onClick = { viewModel.addRecentLogToDiary(first) { } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }
}
