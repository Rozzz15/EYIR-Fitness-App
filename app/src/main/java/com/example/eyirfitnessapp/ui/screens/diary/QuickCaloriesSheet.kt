package com.example.eyirfitnessapp.ui.screens.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.util.AppMessages
import kotlinx.coroutines.launch

/**
 * Spec §3 Quick Add → "Quick Calories": calories (and optional macros) logged
 * straight to the diary with no food entry — the fastest logging path in the
 * app. Confirms through the global snackbar because the sheet closes on save
 * (spec §6 Rule 5: every important action needs feedback).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaloriesSheet(
    repository: AppRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var meal by remember { mutableStateOf(MealType.BREAKFAST) }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val calories = caloriesText.toDoubleOrNull()
    val canLog = calories != null && calories > 0.0 && !saving

    // Macros must never exceed the calories they claim to explain — a plain
    // typo like 500P/500C/500F would log 10,000 kcal worth of macros.
    val macros = listOf(proteinText, carbsText, fatText).map { it.toDoubleOrNull() ?: 0.0 }
    val macroKcal = macros[0] * 4.0 + macros[1] * 4.0 + macros[2] * 9.0
    val macroWarning = calories != null && calories > 0 && macroKcal > calories * 1.5

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xxl)
        ) {
            Text("Quick calories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Straight to your diary — no food entry needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.lg))
            Text("Meal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            // Two rows of two: four equal-width chips squeeze "Breakfast"
            // onto a second line and break the chip.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MealType.entries.take(2).forEach { m ->
                    FilterChip(
                        selected = meal == m,
                        onClick = { meal = m },
                        label = { Text(m.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MealType.entries.drop(2).forEach { m ->
                    FilterChip(
                        selected = meal == m,
                        onClick = { meal = m },
                        label = { Text(m.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            NumberField(caloriesText, { caloriesText = it }, "Calories", required = true)
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MacroField(proteinText, { proteinText = it }, "Protein g", Modifier.weight(1f))
                MacroField(carbsText, { carbsText = it }, "Carbs g", Modifier.weight(1f))
                MacroField(fatText, { fatText = it }, "Fat g", Modifier.weight(1f))
            }

            if (macroWarning) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Those macros add up to more than the calories entered — check for a typo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = if (saving) "Logging…" else "Log ${calories?.toInt() ?: ""} kcal".trim(),
                onClick = {
                    if (!canLog) return@PrimaryButton
                    saving = true
                    val kcal = calories!!
                    scope.launch {
                        try {
                            repository.addFoodLog(
                                FoodLog(
                                    foodId = null,
                                    name = "Quick add",
                                    brand = null,
                                    mealType = meal,
                                    quantity = 1.0,
                                    servingSize = 1.0,
                                    servingUnit = "serving",
                                    calories = kcal,
                                    protein = macros[0],
                                    carbs = macros[1],
                                    fat = macros[2],
                                    date = repository.today(),
                                    loggedAt = System.currentTimeMillis()
                                )
                            )
                            AppMessages.notice("Added ${kcal.toInt()} kcal to ${meal.label}")
                            onDismiss()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            saving = false
                            AppMessages.error("Couldn't log those calories. Please try again.")
                        }
                    }
                },
                enabled = canLog,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(if (required) "$label *" else label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MacroField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}
