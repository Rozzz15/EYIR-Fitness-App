package com.example.eyirfitnessapp.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.Routes
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.ThemeMode
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.SettingsViewModel
import java.io.File

/**
 * Settings (spec §22). Every control here does real work: preferences persist
 * to the profile row, export writes an actual JSON file, delete wipes the DB.
 * Anything the build does not do is stated plainly instead of faked (§34 rule 3).
 */
@Composable
fun SettingsScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onWiped: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
    val profile by viewModel.profile.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val exportPath by viewModel.exportPath.collectAsState()
    val context = LocalContext.current

    var confirmRestart by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val p = profile

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)

        notice?.let { text ->
            AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // ── Account ────────────────────────────────────────────────────────
        SectionTitle("Account")
        AppCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Email", style = MaterialTheme.typography.titleSmall)
                    Text(
                        p?.email ?: "Not signed in — stored on this device only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = if (p?.email == null) "Sign in" else "Manage sign-in",
                onClick = { onNavigate(Routes.LOGIN) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = if (busy) "Exporting…" else "Export data (JSON)",
                onClick = { viewModel.exportData(context) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            exportPath?.let { path ->
                Spacer(Modifier.height(Spacing.sm))
                TextButton(
                    onClick = { shareExport(context, path, viewModel) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share export file") }
            }
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "Restart setup wizard",
                onClick = { confirmRestart = true },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "Delete all data",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        // ── Preferences ────────────────────────────────────────────────────
        SectionTitle("Preferences")
        AppCard {
            if (p != null) {
                ChoiceChips(
                    label = "Theme",
                    options = ThemeMode.entries.map { it.label },
                    selectedIndex = ThemeMode.entries.indexOf(p.themeMode),
                    onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) }
                )
                ChoiceChips(
                    label = "Start of week",
                    options = WeekStartDay.entries.map { it.label },
                    selectedIndex = WeekStartDay.entries.indexOf(p.weekStartDay),
                    onSelect = { viewModel.setWeekStartDay(WeekStartDay.entries[it]) }
                )
                ChoiceChips(
                    label = "Weight units",
                    options = WeightUnit.entries.map { it.label },
                    selectedIndex = WeightUnit.entries.indexOf(p.weightUnit),
                    onSelect = { viewModel.setWeightUnit(WeightUnit.entries[it]) }
                )
                Text("Language", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "English — the only language bundled with this build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Loading preferences…", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // ── Nutrition ──────────────────────────────────────────────────────
        SectionTitle("Nutrition")
        AppCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Exercise calories", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add burned calories back into today's budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = p?.exerciseCaloriesAddToBudget ?: true,
                    onCheckedChange = { viewModel.setExerciseCaloriesInBudget(it) }
                )
            }
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = "Calories & macro goals",
                onClick = { onNavigate(Routes.GOALS) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "Nutrition report",
                onClick = { onNavigate(Routes.NUTRITION) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        // ── Notifications ──────────────────────────────────────────────────
        SectionTitle("Notifications")
        AppCard {
            NotifRow("Meal reminders", "Don't forget to log lunch", p?.notifMeals ?: true) {
                viewModel.setNotifMeals(it, context)
            }
            NotifRow("Water reminders", "Nudges when you're behind on water", p?.notifWater ?: true) {
                viewModel.setNotifWater(it, context)
            }
            NotifRow("Weigh-in reminders", "A morning nudge to log your weight", p?.notifWeighIn ?: true) {
                viewModel.setNotifWeighIn(it, context)
            }
            NotifRow("Fasting reminders", "A nudge to start your fasting window", p?.notifFasting ?: true) {
                viewModel.setNotifFasting(it, context)
            }
            NotifRow("Weekly report", "A Sunday-evening summary of your week", p?.notifWeeklyReport ?: true) {
                viewModel.setNotifWeeklyReport(it, context)
            }
            NotifRow("Habit reminders", "An evening nudge to check off your habits", p?.notifHabits ?: true) {
                viewModel.setNotifHabits(it, context)
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Reminders are delivered locally on this device. If Android asks " +
                    "to allow notifications, accepting lets these reach you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        // ── Privacy ────────────────────────────────────────────────────────
        SectionTitle("Privacy")
        AppCard {
            PrivacyRow("Analytics", "Off — no analytics SDK is bundled.")
            // The barcode scanner ships its model in the APK, but the ML Kit
            // libraries still declare INTERNET — so claim only what is true.
            PrivacyRow("Data sharing", "None — your logs never leave this device.")
            PrivacyRow("Camera", "Used only to read barcodes on this device.")
            PrivacyRow("Connected apps", "None — nothing is uploaded.")
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Your logs, plans and profile live in one local database on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        // ── About ──────────────────────────────────────────────────────────
        SectionTitle("About")
        AppCard {
            Text(
                "EYIR Fitness — offline-first tracking. Calorie targets are " +
                    "estimates; consult a qualified professional for medical or " +
                    "dietary conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Restart setup?") },
            text = { Text("You'll walk through the goal wizard again. Diary, plans and progress stay on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    viewModel.restartOnboarding { onNavigate(Routes.ONBOARDING) }
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "Erases your profile, diary, plans, weights and history from " +
                        "this device. This cannot be undone. Export your data first " +
                        "if you want a copy."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllData(onWiped)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

/** Opens the system share sheet for an export written by [SettingsViewModel.exportData]. */
private fun shareExport(
    context: android.content.Context,
    path: String,
    viewModel: SettingsViewModel
) {
    val file = File(path)
    if (!file.exists()) {
        viewModel.showNotice("That export file no longer exists — export again.")
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "EYIR Fitness data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share export"))
    }.onFailure {
        viewModel.showNotice("Couldn't open a share target on this device.")
    }
}

@Composable
private fun NotifRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

@Composable
private fun ChoiceChips(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(Spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEachIndexed { index, text ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = { Text(text) }
            )
        }
    }
    Spacer(Modifier.height(Spacing.md))
}

@Composable
private fun PrivacyRow(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f)
        )
    }
}
