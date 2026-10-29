package com.example.eyirfitnessapp.ui.screens.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.MetricCard
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.TrendLineChart
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.WeightViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Weight entry + history (spec §11). */
@Composable
fun WeightScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: WeightViewModel = viewModel(factory = WeightViewModel.factory(repository))
    val profile by viewModel.profile.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var weightText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    /** Row awaiting destructive confirmation (spec §34: never delete without asking). */
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    val unit = profile?.weightUnit ?: com.example.eyirfitnessapp.domain.model.WeightUnit.KG

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        ScreenHeader(title = "Weight", onBack = onBack)
        Spacer(Modifier.height(Spacing.lg))

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md)
        ) {
            MetricCard(
                "Current",
                "%.1f".format(unit.fromKg(profile?.currentWeightKg ?: 0.0)),
                Modifier.weight(1f).fillMaxHeight(),
                sub = unit.label
            )
            MetricCard(
                "Goal", "%.1f".format(unit.fromKg(profile?.goalWeightKg ?: 0.0)),
                Modifier.weight(1f).fillMaxHeight(),
                sub = unit.label,
                accent = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text("New weigh-in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Weight (${unit.label})") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Save weight",
                onClick = {
                    val parsed = weightText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        viewModel.save(unit.toKg(parsed), note)
                        weightText = ""
                        note = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = weightText.toDoubleOrNull()?.let { it > 0 } ?: false
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        if (logs.size >= 2) {
            AppCard {
                Text("Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.md))
                TrendLineChart(
                    values = logs.map { unit.fromKg(it.weightKg) },
                    unitLabel = unit.label
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
            items(logs.asReversed(), key = { it.id }) { log ->
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${"%.1f".format(unit.fromKg(log.weightKg))} ${unit.label}", style = MaterialTheme.typography.titleMedium)
                            log.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Text(
                            SimpleDateFormat("MMM d", Locale.US).format(Date(log.loggedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.material3.IconButton(onClick = { pendingDelete = log.id }) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete weigh-in",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.xxxl)) }
        }
    }

    pendingDelete?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this weigh-in?") },
            text = { Text("This removes the entry from your history. It can't be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
