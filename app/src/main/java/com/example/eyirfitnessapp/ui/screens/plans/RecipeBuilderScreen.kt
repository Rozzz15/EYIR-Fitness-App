package com.example.eyirfitnessapp.ui.screens.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import com.example.eyirfitnessapp.ui.viewmodels.RecipeViewModel
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.theme.Spacing

/**
 * Recipe builder (spec §16). Ingredients are entered with per-100g nutrition;
 * totals are computed for the whole recipe and per serving. Recipes persist
 * and can be logged to the diary with one tap (per-serving nutrition).
 */
@Composable
fun RecipeBuilderScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: RecipeViewModel = viewModel(factory = RecipeViewModel.factory(repository))
    val recipes by viewModel.recipes.collectAsState()
    val name by viewModel.name.collectAsState()
    val servingsText by viewModel.servingsText.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val notice by viewModel.notice.collectAsState()

    // Draft ingredient input fields
    var ingredientName by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var kcal100 by remember { mutableStateOf("") }
    var protein100 by remember { mutableStateOf("") }
    var carbs100 by remember { mutableStateOf("") }
    var fat100 by remember { mutableStateOf("") }

    var logMealFor by remember { mutableStateOf<Long?>(null) }
    var logMeal by remember { mutableStateOf(MealType.LUNCH) }

    val gramsValid = grams.toDoubleOrNull() != null
    val kcalValid = kcal100.toDoubleOrNull() != null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Recipes", onBack = onBack)
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

        AppCard {
            Text("New recipe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(value = name, onValueChange = viewModel::setName, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(value = servingsText, onValueChange = viewModel::setServings, label = { Text("Servings") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))

            Text("Add ingredient (per 100g values)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(value = ingredientName, onValueChange = { ingredientName = it }, label = { Text("Ingredient") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            Row {
                OutlinedTextField(value = grams, onValueChange = { grams = it }, label = { Text("Grams") }, modifier = Modifier.weight(1f), isError = grams.isNotBlank() && !gramsValid)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = kcal100, onValueChange = { kcal100 = it }, label = { Text("kcal/100g") }, modifier = Modifier.weight(1f), isError = kcal100.isNotBlank() && !kcalValid)
            }
            Spacer(Modifier.height(Spacing.sm))
            Row {
                OutlinedTextField(value = protein100, onValueChange = { protein100 = it }, label = { Text("P/100g") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = carbs100, onValueChange = { carbs100 = it }, label = { Text("C/100g") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = fat100, onValueChange = { fat100 = it }, label = { Text("F/100g") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Add ingredient",
                onClick = {
                    val g = grams.toDoubleOrNull() ?: return@PrimaryButton
                    val k = kcal100.toDoubleOrNull() ?: return@PrimaryButton
                    viewModel.addIngredient(
                        RecipeViewModel.DraftIngredient(
                            name = ingredientName.ifBlank { "Ingredient" },
                            grams = g,
                            kcal100 = k,
                            protein100 = protein100.toDoubleOrNull() ?: 0.0,
                            carbs100 = carbs100.toDoubleOrNull() ?: 0.0,
                            fat100 = fat100.toDoubleOrNull() ?: 0.0
                        )
                    )
                    ingredientName = ""; grams = ""; kcal100 = ""; protein100 = ""; carbs100 = ""; fat100 = ""
                },
                enabled = gramsValid && kcalValid
            )
        }
        Spacer(Modifier.height(Spacing.sm))

        ingredients.forEachIndexed { index, ing ->
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${ing.name} (${formatQty(ing.grams)}g)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "P ${"%.1f".format(ing.contribution.protein)}g • C ${"%.1f".format(ing.contribution.carbs)}g • F ${"%.1f".format(ing.contribution.fat)}g",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("${ing.contribution.calories.toInt()} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { viewModel.removeIngredient(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove ${ing.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xs))
        }

        viewModel.totals()?.let { total ->
            val servings = viewModel.servings()
            AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                Text("Nutrition per serving", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.sm))
                val per = total.perServing(servings)
                Text("${per.totalCalories.toInt()} kcal", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    "P ${"%.1f".format(per.totalProtein)}g • C ${"%.1f".format(per.totalCarbs)}g • F ${"%.1f".format(per.totalFat)}g",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Save recipe",
                onClick = { viewModel.save { } },
                enabled = viewModel.canSave(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
        }

        Text("Saved recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))

        if (recipes.isEmpty()) {
            EmptyState(
                title = "No recipes yet",
                subtitle = "Build one above — ingredients, servings, and the nutrition is calculated for you."
            )
        }

        recipes.forEach { recipe ->
            val per = recipe.perServing()
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${recipe.servings} servings • ${per.totalCalories.toInt()} kcal/serving",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "P ${"%.1f".format(per.totalProtein)}g • C ${"%.1f".format(per.totalCarbs)}g • F ${"%.1f".format(per.totalFat)}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.delete(recipe.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete ${recipe.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (logMealFor == recipe.id) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        MealType.entries.forEach { m ->
                            FilterChip(
                                selected = logMeal == m,
                                onClick = { logMeal = m },
                                label = { Text(m.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SecondaryCompactButton(
                        text = if (logMealFor == recipe.id) "Log 1 serving" else "Log serving…",
                        onClick = {
                            if (logMealFor == recipe.id) {
                                viewModel.logServing(recipe.id, logMeal)
                                logMealFor = null
                            } else {
                                logMealFor = recipe.id
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }
}

@Composable
private fun SecondaryCompactButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}

/** "120" not "120.0" — keeps quantity labels readable. */
private fun formatQty(q: Double): String =
    if (q == q.toLong().toDouble()) q.toLong().toString() else "%.1f".format(q)
