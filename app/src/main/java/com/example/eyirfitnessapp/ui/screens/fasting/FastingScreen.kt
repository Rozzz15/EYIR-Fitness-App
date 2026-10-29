package com.example.eyirfitnessapp.ui.screens.fasting

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.FastingSession
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.CalorieRing
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.FastingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fasting timer (spec §19): presets (incl. custom), live state, history. */
@Composable
fun FastingScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: FastingViewModel = viewModel(factory = FastingViewModel.factory(repository))
    val active by viewModel.active.collectAsState()
    val history by viewModel.history.collectAsState()

    var presetHours by remember { mutableIntStateOf(16) }
    var customText by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tick while the screen is open so elapsed/remaining stay live.
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000L)
        }
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val dayFmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    // Single LazyColumn: a LazyColumn nested inside a verticalScroll Column is
    // measured with infinite height and throws on first composition.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            ScreenHeader(title = "Fasting", onBack = onBack)
            Spacer(Modifier.height(Spacing.lg))
        }
        item {
            val a = active
            if (a != null) {
                val elapsedMs = a.elapsedMs(now)
                val progress = (elapsedMs.toDouble() / a.targetMs).toFloat().coerceIn(0f, 1f)
                val remainingMs = (a.targetMs - elapsedMs).coerceAtLeast(0L)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CalorieRing(
                        progress = progress,
                        centerValue = "${elapsedMs / 3_600_000L}h ${(elapsedMs % 3_600_000L) / 60_000L}m",
                        centerLabel = "FASTING"
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        if (remainingMs > 0) {
                            "${remainingMs / 3_600_000L}h ${(remainingMs % 3_600_000L) / 60_000L}m remaining"
                        } else {
                            "Target reached — you can end the fast"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingMs > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        "Started ${timeFmt.format(Date(a.startTime))} • target ${a.targetHours}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(text = "End fast", onClick = viewModel::end, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Choose a preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(12, 14, 16, 18, 20).forEach { h ->
                        val eatingWindow = 24 - h
                        FilterChip(
                            selected = presetHours == h,
                            onClick = {
                                presetHours = h
                                customText = ""
                            },
                            label = { Text("$h:$eatingWindow") }
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = customText.isNotEmpty(),
                        onClick = { customText = customText.ifEmpty { "10" } },
                        label = { Text("Custom") }
                    )
                    if (customText.isNotEmpty()) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { text -> customText = text.filter { it.isDigit() }.take(2) },
                            label = { Text("Hours") },
                            singleLine = true,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                val customHours = customText.toIntOrNull()
                val targetHours = if (customText.isNotEmpty()) customHours else presetHours
                PrimaryButton(
                    text = "Start fast",
                    enabled = targetHours != null && targetHours in 1..48,
                    onClick = { targetHours?.let { viewModel.start(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                if (customText.isNotEmpty() && (customHours == null || customHours !in 1..48)) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Enter a whole number of hours between 1 and 48.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
        }
        if (history.isEmpty()) {
            item {
                Text(
                    "No finished fasts yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(history, key = { it.id }) { session ->
            FastHistoryRow(session = session, dayFmt = dayFmt, timeFmt = timeFmt)
        }
        item { Spacer(Modifier.height(Spacing.xxxl)) }
    }
}

@Composable
private fun FastHistoryRow(
    session: FastingSession,
    dayFmt: SimpleDateFormat,
    timeFmt: SimpleDateFormat
) {
    val elapsed = session.elapsedMs()
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${session.targetHours}h fast", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Started ${dayFmt.format(Date(session.startTime))}" +
                        (session.endTime?.let { " • ended ${timeFmt.format(Date(it))}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Fasted ${elapsed / 3_600_000L}h ${(elapsed % 3_600_000L) / 60_000L}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (session.completedOnTarget) "✓ Completed" else "Ended early",
                color = if (session.completedOnTarget) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
