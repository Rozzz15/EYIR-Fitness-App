package com.example.eyirfitnessapp.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine
import com.example.eyirfitnessapp.domain.routines.RoutineLogic
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.RoutinesViewModel

/**
 * Workout routines (spec §10): create reusable routines like "Push Day",
 * log a whole routine to today with one tap, edit and delete — plus the
 * routine list acting as an exercise-history entry point.
 */
@Composable
fun RoutinesScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.factory(repository))
    val routines by viewModel.routines.collectAsState()
    val openRoutine by viewModel.openRoutine.collectAsState()
    val draftName by viewModel.draftName.collectAsState()
    val draftItems by viewModel.draftItems.collectAsState()
    val editingId by viewModel.editingId.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val unit = profile?.weightUnit ?: WeightUnit.KG

    /** Routine id awaiting destructive confirmation (spec §34). */
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    var pendingLog by remember { mutableStateOf<WorkoutRoutine?>(null) }

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        ScreenHeader(title = "Routines", onBack = onBack)
        Spacer(Modifier.height(Spacing.lg))

        if (notice != null) {
            Text(notice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.sm))
        }

        if (editingId != null || draftItems.isNotEmpty()) {
            // ── Builder (new or edit) ──
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = viewModel::setDraftName,
                    label = { Text("Routine name (e.g. Push Day)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.lg))
                draftItems.forEachIndexed { index, item ->
                    AppCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Exercise ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { viewModel.removeItem(index) }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Remove exercise ${index + 1}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { viewModel.updateItem(index, item.copy(name = it)) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = item.sets?.toString() ?: "",
                                onValueChange = { viewModel.updateItem(index, item.copy(sets = it.toIntOrNull())) },
                                label = { Text("Sets") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.reps?.toString() ?: "",
                                onValueChange = { viewModel.updateItem(index, item.copy(reps = it.toIntOrNull())) },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.weightKg?.let { "%.0f".format(unit.fromKg(it)) } ?: "",
                                onValueChange = { text ->
                                    viewModel.updateItem(index, item.copy(weightKg = text.toDoubleOrNull()?.let { unit.toKg(it) }))
                                },
                                label = { Text("kg") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = item.durationMinutes?.let { "%.0f".format(it) } ?: "",
                            onValueChange = { viewModel.updateItem(index, item.copy(durationMinutes = it.toDoubleOrNull())) },
                            label = { Text("Duration (minutes) — cardio") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
                SecondaryButton(text = "+ Add exercise", onClick = { viewModel.addItem() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SecondaryButton(text = "Cancel", onClick = { viewModel.cancelEdit() }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Save routine",
                        onClick = { viewModel.saveDraft() },
                        enabled = viewModel.canSaveDraft(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        } else {
            // ── List view ──
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                PrimaryButton(text = "New routine", onClick = { viewModel.startNewRoutine() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.lg))

                if (routines.isEmpty()) {
                    EmptyState(
                        title = "No routines yet",
                        subtitle = "Build a reusable workout like \"Push Day\" and log it with one tap.",
                        icon = Icons.Outlined.FitnessCenter
                    )
                }
                routines.forEach { routine ->
                    AppCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${routine.items.size} exercise${if (routine.items.size == 1) "" else "s"} • ~${RoutineLogic.estimatedMinutes(routine).toInt()} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.startEdit(routine) }) {
                                Text("Edit")
                            }
                            IconButton(onClick = { pendingDelete = routine.id }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete ${routine.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        routine.items.take(4).forEach { item ->
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "• ${item.name} — ${item.detailLabel(unit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (routine.items.size > 4) {
                            Text(
                                "…and ${routine.items.size - 4} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                        SecondaryButton(
                            text = "Log to today",
                            onClick = { pendingLog = routine },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this routine?") },
            text = { Text("The routine and its exercise list will be removed. Today's logged workouts are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoutine(id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    pendingLog?.let { routine ->
        AlertDialog(
            onDismissRequest = { pendingLog = null },
            title = { Text("Log \"${routine.name}\"?") },
            text = {
                Text(
                    "Adds all ${routine.items.size} exercise${if (routine.items.size == 1) "" else "s"} to today's " +
                        "exercise diary (~${RoutineLogic.estimatedMinutes(routine).toInt()} min)."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logRoutine(routine)
                    pendingLog = null
                }) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLog = null }) { Text("Cancel") }
            }
        )
    }
}
