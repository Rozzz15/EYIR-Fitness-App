package com.example.eyirfitnessapp.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.MetricCard
import com.example.eyirfitnessapp.ui.components.ProgressBar
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.theme.extended
import com.example.eyirfitnessapp.ui.viewmodels.HabitsViewModel
import com.example.eyirfitnessapp.util.Dates

/**
 * Habit tracking (spec §12): a "Today" list for quick check-off, a compact
 * week board (habits × the current week's days) for at-a-glance history,
 * and the catalog to start tracking more habits.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.factory(repository))
    val habits by viewModel.habits.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val today by viewModel.today.collectAsState()

    /** Habit id awaiting destructive confirmation (spec §34). */
    var pendingRemove by remember { mutableStateOf<Long?>(null) }

    val available = viewModel.availableCatalog(habits)
    val weekDays = viewModel.weekDays(profile?.weekStartDay ?: WeekStartDay.MONDAY)

    // Single LazyColumn: a LazyColumn nested inside a verticalScroll Column is
    // measured with infinite height and throws on first composition.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            ScreenHeader(title = "Habits", onBack = onBack)
            Spacer(Modifier.height(Spacing.lg))
        }
        item {
            if (notice != null) {
                Text(notice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
            }
        }
        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = "No habits tracked",
                    subtitle = "Add water, exercise, sleep and more below to build streaks.",
                    icon = Icons.Outlined.CheckCircle
                )
            }
        } else {
            item {
                val doneToday = habits.count { viewModel.isDoneToday(it) }
                val bestHabit = habits.maxByOrNull { viewModel.streakOf(it) }
                val bestStreak = bestHabit?.let { viewModel.streakOf(it) } ?: 0

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MetricCard(
                        label = "Today",
                        value = "$doneToday/${habits.size}",
                        sub = "habits done",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Best streak",
                        value = "$bestStreak",
                        sub = if (bestStreak > 0 && bestHabit != null) "days · ${bestHabit.label}" else "no streak yet",
                        accent = MaterialTheme.extended.warning,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                ProgressBar(
                    progress = doneToday.toFloat() / habits.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(habits.size, key = { habits[it].id }) { index ->
                val habit = habits[index]
                HabitCard(
                    habit = habit,
                    streak = viewModel.streakOf(habit),
                    rate7 = viewModel.rate7(habit),
                    doneToday = viewModel.isDoneToday(habit),
                    onToggle = { viewModel.toggleToday(habit) },
                    onRemove = { pendingRemove = habit.id },
                    onStreakPreview = { viewModel.previewMilestone(habit) }
                )
            }
        }
        item {
            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Week board", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${Dates.prettyDate(weekDays.first())} – ${Dates.prettyDate(weekDays.last())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            WeekBoard(
                habits = habits,
                weekDays = weekDays,
                today = today,
                onToggle = { habit, date -> viewModel.toggleDay(habit, date) }
            )
        }
        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("Add a habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
        }
        if (available.isEmpty() && habits.isNotEmpty()) {
            item {
                Text(
                    "You're tracking every habit in the catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                available.forEach { candidate ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.addHabit(candidate) },
                        label = { Text(candidate.label) },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = PillShape
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            SecondaryButton(
                text = "Preview streak celebration",
                onClick = { viewModel.previewCelebration() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }

    pendingRemove?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Stop tracking this habit?") },
            text = { Text("The habit disappears from your list. Its history is archived, not erased — re-add it anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeHabit(id)
                    pendingRemove = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    streak: Int,
    rate7: Double,
    doneToday: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onStreakPreview: () -> Unit
) {
    AppCard(
        modifier = Modifier.pointerInput(habit.id) {
            detectTapGestures(onLongPress = { onStreakPreview() })
        },
        color = if (doneToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(habit.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (streak > 0) {
                        Icon(
                            Icons.Outlined.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.extended.warning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        buildString {
                            append(if (streak > 0) "$streak day streak" else "No streak yet")
                            append(" • ${(rate7 * 100).toInt()}% this week")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.RemoveCircleOutline,
                    contentDescription = "Stop tracking ${habit.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (doneToday) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (doneToday) "Mark ${habit.label} not done today" else "Mark ${habit.label} done today",
                    tint = if (doneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

/**
 * Habits × days board for the current week (§12): one labelled row per habit
 * and one column per day, so the grid stays readable however many habits are
 * tracked. Filled cells are done days, the ringed cell marks today, hollow
 * cells are days the week hasn't reached yet; tap an unlocked cell to toggle.
 */
@Composable
private fun WeekBoard(
    habits: List<Habit>,
    weekDays: List<String>,
    today: String,
    onToggle: (Habit, String) -> Unit
) {
    AppCard {
        if (habits.isEmpty()) {
            Text(
                "Add a habit to see your weekly board.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@AppCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(BoardGap)) {
                    weekDays.forEach { date ->
                        val isToday = date == today
                        Box(
                            Modifier
                                .size(BoardCell)
                                .then(
                                    if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, PillShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                Dates.weekdayShort(date).take(2),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            habits.forEach { habit ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        habit.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(BoardGap)) {
                        weekDays.forEach { date ->
                            DayCell(
                                habitLabel = habit.label,
                                date = date,
                                isToday = date == today,
                                locked = date > today,
                                done = date in habit.completions,
                                onClick = { onToggle(habit, date) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Tap a day to check it off — days ahead unlock as the week goes.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val BoardCell = 24.dp
private val BoardGap = 4.dp

@Composable
private fun DayCell(
    habitLabel: String,
    date: String,
    isToday: Boolean,
    locked: Boolean,
    done: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(7.dp)
    val background = when {
        done -> MaterialTheme.colorScheme.primary
        isToday || locked -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val borderColor = when {
        done -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primary
        locked -> MaterialTheme.colorScheme.outlineVariant
        else -> Color.Transparent
    }
    val borderWidth = when {
        done -> 0.dp
        isToday -> 1.5.dp
        locked -> 1.dp
        else -> 0.dp
    }
    val state = when {
        done -> "done"
        locked -> "upcoming"
        else -> "not done"
    }

    Box(
        Modifier
            .size(BoardCell)
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .then(if (locked) Modifier else Modifier.clickable { onClick() })
            .semantics { contentDescription = "$habitLabel, ${Dates.prettyDate(date)}, $state" },
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
