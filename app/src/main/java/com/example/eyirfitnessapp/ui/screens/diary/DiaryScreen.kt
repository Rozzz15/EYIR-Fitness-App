package com.example.eyirfitnessapp.ui.screens.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.eyirfitnessapp.Routes
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.LoggedFoodRow
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.theme.extended
import com.example.eyirfitnessapp.ui.viewmodels.DiaryViewModel
import com.example.eyirfitnessapp.util.Dates
import java.util.Calendar
import java.util.TimeZone

/** Material3 DatePicker stores UTC-anchored millis; Diary dates are local "yyyy-MM-dd". */
private fun toPickerMillis(isoDate: String): Long {
    val local = Calendar.getInstance().apply { timeInMillis = Dates.parseMillis(isoDate) }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Inverse of [toPickerMillis]: picker millis → local noon (daylight-safe) → iso date. */
private fun fromPickerMillis(pickerMillis: Long): String {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }
    return Dates.isoOf(
        Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        }.timeInMillis
    )
}

/** Diary (spec §8): all meals for a day, with edit/delete and copy-day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    repository: AppRepository,
    onNavigate: (String) -> Unit = {}
) {
    val viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.factory(repository))
    val date by viewModel.date.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val previousLogs by viewModel.previousLogs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val notice by viewModel.notice.collectAsState()

    var editing by remember { mutableStateOf<FoodLog?>(null) }
    var pendingCopy by remember { mutableStateOf(false) }
    /** Food row whose remove button was tapped — confirm before deleting (spec §34). */
    var pendingDelete by remember { mutableStateOf<FoodLog?>(null) }
    /** Which meal's copy menu is open (spec §2 copy food / meal / previous meal). */
    var copyMenuMeal by remember { mutableStateOf<MealType?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        // Date navigation header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.shiftDate(-1) },
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
            }
            // Tap the header date to jump straight to any day via the calendar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDatePicker = true }
                    .padding(vertical = Spacing.xs)
            ) {
                Text("Diary", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Dates.prettyDate(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Open calendar",
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = { viewModel.shiftDate(1) },
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
            }
        }
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

        // Summary card — remaining uses the same rule as Today (spec §9, §20)
        summary?.let { s ->
            AppCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SummaryStat(
                        value = "${s.caloriesConsumed.toInt()}",
                        label = "Eaten",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStat(
                        value = "${s.caloriesBurned.toInt()}",
                        label = "Burned",
                        accent = MaterialTheme.extended.burn,
                        modifier = Modifier.weight(1f)
                    )
                    val remaining = if (s.goal != null) {
                        NutritionEngine.caloriesRemaining(
                            s,
                            profile?.exerciseCaloriesAddToBudget != false
                        ).toString()
                    } else {
                        "—"
                    }
                    SummaryStat(
                        value = remaining,
                        label = "Remaining",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        SecondaryButton(
            text = "Copy previous day",
            onClick = {
                if (logs.isEmpty()) viewModel.copyPreviousDay() else pendingCopy = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.md))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MealType.entries.forEach { meal ->
                val mealLogs = logs.filter { it.mealType == meal }
                item(key = "meal_$meal") {
                    AppCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Text(
                                    "${mealLogs.sumOf { it.calories }.toInt()} kcal",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onNavigate(Routes.foodSearch(meal = meal, date = date)) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add food to ${meal.label}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Copy actions for this meal (spec §2)
                                IconButton(
                                    onClick = { copyMenuMeal = meal },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.MoreVert,
                                        contentDescription = "Copy options for ${meal.label}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = copyMenuMeal == meal,
                                    onDismissRequest = { copyMenuMeal = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Copy yesterday's ${meal.label}") },
                                        onClick = {
                                            copyMenuMeal = null
                                            viewModel.copyPreviousMeal(meal)
                                        }
                                    )
                                    HorizontalDivider()
                                    MealType.entries.filter { it != meal }.forEach { target ->
                                        DropdownMenuItem(
                                            text = { Text("Copy to ${target.label}") },
                                            onClick = {
                                                copyMenuMeal = null
                                                viewModel.copyMeal(meal, target)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (mealLogs.isEmpty()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "No food logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(Modifier.height(Spacing.xs))
                            mealLogs.forEach { log ->
                                LoggedFoodRow(
                                    log,
                                    onClick = { editing = log },
                                    inCard = true,
                                    onDelete = { pendingDelete = log }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.xxxl)) }
        }
    }

    // Confirm before appending a whole day (spec §34 rule 6)
    if (pendingCopy) {
        val previous = Dates.addDays(date, -1)
        AlertDialog(
            onDismissRequest = { pendingCopy = false },
            title = { Text("Copy previous day?") },
            text = {
                Text(
                    "Adds the ${previousLogs.size} item${if (previousLogs.size == 1) "" else "s"} logged on " +
                        "${Dates.prettyDate(previous)} on top of what is already on this day."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCopy = false
                    viewModel.copyPreviousDay()
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCopy = false }) { Text("Cancel") }
            }
        )
    }

    // One-tap remove from the meal row, with a confirm so a mis-tap can't wipe a meal.
    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${log.name}?") },
            text = {
                Text(
                    "Removes this entry from ${log.mealType.label} and that day's calorie total. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(log.id)
                    pendingDelete = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Calendar picker: tap the header date to jump anywhere without arrow-stepping
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = toPickerMillis(date),
            initialDisplayedMonthMillis = toPickerMillis(date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { viewModel.jumpToDate(fromPickerMillis(it)) }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.jumpToDate(Dates.today())
                            showDatePicker = false
                        }
                    ) { Text("Today") }
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    editing?.let { log ->
        val editFood by viewModel.editFood.collectAsState()
        LaunchedEffect(log.id) { viewModel.loadFoodForEdit(log) }
        EditLogDialog(
            log = log,
            food = editFood,
            onRememberGramWeight = { grams -> viewModel.rememberGramWeight(grams) },
            onCancel = { editing = null },
            onSave = { amount, meal ->
                viewModel.saveEdit(log.id, amount, meal)
                editing = null
            },
            onDelete = {
                viewModel.deleteLog(log.id)
                editing = null
            },
            onDuplicate = { meal ->
                // Spec §2 "copy food": new row, same amount, into the meal picked below
                viewModel.copyLog(log, meal)
                editing = null
            }
        )
    }
}

/**
 * Edit a logged entry: amount (rescaled), meal, duplicate, delete (spec §8).
 * The amount is canonical in the log's native unit; a grams toggle offers
 * scale-weight entry and converts back before saving.
 */
@Composable
private fun EditLogDialog(
    log: FoodLog,
    food: Food?,
    onRememberGramWeight: (grams: Double) -> Unit,
    onCancel: () -> Unit,
    onSave: (amount: Double, meal: MealType) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (meal: MealType) -> Unit
) {
    val fmt: (Double) -> String = { v ->
        if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.2f".format(v).trimEnd('0').trimEnd('.')
    }
    val servingSize = if (log.servingSize > 0) log.servingSize else 1.0
    val weightBased = log.servingUnit.equals("g", ignoreCase = true)
    /** Grams in ONE serving: weight-based logs are already grams. */
    val gramsPerServing: Double? =
        if (weightBased) log.servingSize
        else food?.let { NutritionEngine.gramsPerServing(it) }
    val canWeigh = !weightBased && food != null

    var gramsMode by remember(log.id) { mutableStateOf(false) }
    var askGrams by remember(log.id) { mutableStateOf(false) }
    var gramWeightText by remember(log.id) { mutableStateOf("") }
    var meal by remember(log.id) { mutableStateOf(log.mealType) }
    var nativeAmount by remember(log.id) { mutableStateOf(log.quantity) }
    var amountText by remember(log.id) { mutableStateOf(fmt(log.quantity)) }

    // Ask-once saved → the weight arrives on the food → switch into grams mode.
    LaunchedEffect(gramsPerServing, askGrams) {
        if (askGrams && gramsPerServing != null && gramsPerServing > 0) {
            askGrams = false
            gramsMode = true
            amountText = fmt(nativeAmount / servingSize * gramsPerServing)
        }
    }

    val setMode = { toGrams: Boolean ->
        if (toGrams && gramsPerServing == null) {
            askGrams = true
        } else {
            gramsMode = toGrams
            askGrams = false
            amountText = if (toGrams && gramsPerServing != null) {
                fmt(nativeAmount / servingSize * gramsPerServing)
            } else {
                fmt(nativeAmount)
            }
        }
    }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(log.name) },
        text = {
            Column {
                Text(
                    if (gramsMode) "Amount (g)" else "Amount (${log.servingUnit})",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(Spacing.sm))

                if (canWeigh) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        FilterChip(
                            selected = !gramsMode && !askGrams,
                            onClick = { setMode(false) },
                            label = { Text(log.servingUnit) }
                        )
                        FilterChip(
                            selected = gramsMode,
                            onClick = { setMode(true) },
                            label = { Text("g") }
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                if (askGrams) {
                    Text(
                        "How many grams does 1 ${log.servingUnit} weigh? " +
                            "Weigh it once — we'll remember it next time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = gramWeightText,
                            onValueChange = { gramWeightText = it },
                            singleLine = true,
                            isError = gramWeightText.isNotBlank() &&
                                gramWeightText.toDoubleOrNull()?.let { it <= 0 } != false,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                gramWeightText.toDoubleOrNull()?.takeIf { it > 0 }
                                    ?.let { onRememberGramWeight(it) }
                            },
                            enabled = gramWeightText.toDoubleOrNull()?.let { it > 0 } == true
                        ) { Text("Use grams") }
                    }
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { text ->
                            amountText = text
                            val value = text.toDoubleOrNull()?.takeIf { it > 0 }
                            if (value != null) {
                                nativeAmount = if (gramsMode && gramsPerServing != null) {
                                    value / gramsPerServing * servingSize
                                } else {
                                    value
                                }
                            }
                        },
                        singleLine = true,
                        isError = amount == null,
                        suffix = {
                            Text(
                                if (gramsMode) "g" else log.servingUnit,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Text("Meal", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    MealType.entries.forEach { m ->
                        FilterChip(
                            selected = meal == m,
                            onClick = { meal = m },
                            label = { Text(m.label) }
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                TextButton(
                    onClick = { onDuplicate(meal) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy entry to ${meal.label}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (amount != null) onSave(nativeAmount, meal) },
                enabled = amount != null && nativeAmount > 0
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
