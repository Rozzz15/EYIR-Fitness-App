package com.example.eyirfitnessapp.ui.screens.diary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.theme.Spacing

/**
 * Nutrition facts for one food (spec §4 "Food Details", §9 micronutrients).
 * Read-only: the amount/meal/add flow stays in FoodSearchScreen.
 * A [foodId] of 0 means "no food chosen" — an honest empty state, not a fake result.
 */
@Composable
fun FoodDetailScreen(repository: AppRepository, foodId: Long, onBack: () -> Unit) {
    var food by remember(foodId) { mutableStateOf<Food?>(null) }
    var loading by remember(foodId) { mutableStateOf(true) }

    LaunchedEffect(foodId) {
        food = if (foodId > 0) repository.foodById(foodId) else null
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        ScreenHeader(title = "Nutrition facts", onBack = onBack)
        Spacer(Modifier.height(Spacing.lg))

        when {
            loading -> Row(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Text("  Loading…", style = MaterialTheme.typography.bodyMedium)
            }

            food == null -> EmptyState(
                title = "No food selected",
                subtitle = "Search for a food first, then open its nutrition facts from there."
            )

            else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                FoodFacts(food!!)
            }
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }
}

@Composable
private fun FoodFacts(food: Food) {
    Column {
        Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            food.brand ?: "Generic",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Per ${food.servingSize.stripZeros()} ${food.servingUnit}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))

        FactRow("Calories", "${food.calories.round1()} kcal", emphasise = true)
        HorizontalDivider()
        FactRow("Protein", "${food.protein.round1()} g")
        FactRow("Carbohydrates", "${food.carbs.round1()} g")
        FactRow("Fat", "${food.fat.round1()} g")

        Spacer(Modifier.height(Spacing.md))
        Text("Micronutrients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        FactRow("Fiber", "${food.fiber.round1()} g")
        FactRow("Sugar", "${food.sugar.round1()} g")
        FactRow("Sodium", "${food.sodium.round0()} mg")

        if (food.barcode != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Barcode ${food.barcode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FactRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasise) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            value,
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun Double.round1(): String = "%.1f".format(this)
private fun Double.round0(): String = "%.0f".format(this)
private fun Double.stripZeros(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')
