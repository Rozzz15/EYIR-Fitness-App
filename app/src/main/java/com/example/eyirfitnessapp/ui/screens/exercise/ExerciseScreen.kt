package com.example.eyirfitnessapp.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.Exercise
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.ExerciseViewModel

/**
 * Catalog categories whose natural input is sets × reps (the rest log minutes).
 * Kept in one place so the detail form and the catalog stay in sync.
 */
private val REPS_CATEGORIES = setOf(
    "Strength",
    "Bodyweight",
    "Machines",
    "Resistance Bands",
    "Core",
    "Plyometrics",
    "Pilates"
)

/** Exercise logging (spec §10): cardio duration or strength sets/reps. */
@Composable
fun ExerciseScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    onOpenRoutines: (() -> Unit)? = null
) {
    val viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.factory(repository))
    val exercises by viewModel.exercises.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val weightUnit = profile?.weightUnit ?: com.example.eyirfitnessapp.domain.model.WeightUnit.KG

    /** Exercise entry awaiting destructive confirmation (spec §34). */
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    var query by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("30") }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("10") }
    var weightText by remember { mutableStateOf("40") }

    val sel = selected
    Column(
        Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Exercise", onBack = onBack)
        // Null = the caller has no routines destination; comparing a lambda to a
        // fresh `{}` (the old guard) was always true and never hid this button.
        if (onOpenRoutines != null) {
            SecondaryButton(
                text = "Workout routines",
                onClick = onOpenRoutines,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        Spacer(Modifier.height(Spacing.lg))

        if (sel == null) {
            if (logs.isNotEmpty()) {
                Text("Logged today", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                logs.forEach { log ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(log.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                log.sets?.let { s ->
                                    "$s × ${log.reps} @ ${"%.0f".format(weightUnit.fromKg(log.weightKg ?: 0.0))} ${weightUnit.label}"
                                } ?: log.durationMinutes?.let { "${it.toInt()} min" } ?: "Logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${log.caloriesBurned.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { pendingDelete = log.id }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove ${log.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                label = { Text("Search exercises") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))
            Text("Today: ${logs.sumOf { it.caloriesBurned }.toInt()} kcal burned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.sm))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (exercises.isEmpty()) {
                    item {
                        EmptyState(title = "No exercises found", subtitle = "Try a different search term.")
                    }
                }
                items(exercises, key = { it.id }) { exercise ->
                    AppCard(onClick = { viewModel.select(exercise) }) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(exercise.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Spacer(Modifier.height(Spacing.xxxl)) }
            }
        } else {
            // ── Detail form ──
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppCard {
                    Text(sel.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(sel.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.lg))

                    // The expanded catalog's reps-style work (gym lifting, home
                    // bodyweight, machines, bands, core, plyo, Pilates) uses the
                    // sets/reps form; everything else is timed: Yoga and the
                    // other duration sports would produce logs with no minutes
                    // (0 in the exercise-minutes chart) in the sets/reps form.
                    val repsBased = REPS_CATEGORIES.any { it.equals(sel.category, ignoreCase = true) }
                    val isCardio = !repsBased
                    if (isCardio) {
                        Text("Duration (minutes)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        PrimaryButton(
                            text = "Log cardio",
                            onClick = {
                                minutesText.toDoubleOrNull()?.let { min ->
                                    if (min > 0) viewModel.logCardio(sel, min) { viewModel.select(null) }
                                }
                            },
                            enabled = (minutesText.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Sets", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = setsText, onValueChange = { setsText = it }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Spacing.md))
                        Text("Reps", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = repsText, onValueChange = { repsText = it }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Spacing.md))
                        Text("Weight (${weightUnit.label})", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Spacing.lg))
                        PrimaryButton(
                            text = "Log strength",
                            onClick = {
                                val sets = setsText.toIntOrNull()
                                val reps = repsText.toIntOrNull()
                                val wt = weightText.toDoubleOrNull()
                                if (sets != null && sets > 0 && reps != null && reps > 0) {
                                    // Stored in kg regardless of the unit shown (spec §24).
                                    val kg = wt?.let { weightUnit.toKg(it) } ?: 0.0
                                    viewModel.logStrength(sel, sets, reps, kg) { viewModel.select(null) }
                                }
                            },
                            enabled = (setsText.toIntOrNull() ?: 0) > 0 && (repsText.toIntOrNull() ?: 0) > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    SecondaryButton(text = "Cancel", onClick = { viewModel.select(null) })
                }
            }
        }
    }

    pendingDelete?.let { id ->
        val log = logs.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this workout?") },
            text = { Text("${log?.name ?: "This entry"} will be removed from today's exercise.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(id)
                    pendingDelete = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
