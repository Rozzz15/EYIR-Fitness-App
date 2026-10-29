@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.eyirfitnessapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.habits.HabitLogic
import com.example.eyirfitnessapp.domain.model.BodyMeasurement
import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.domain.model.MeasurementPart
import com.example.eyirfitnessapp.domain.model.RoutineItem
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine
import com.example.eyirfitnessapp.domain.progress.ProgressLogic
import com.example.eyirfitnessapp.domain.routines.RoutineLogic
import com.example.eyirfitnessapp.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Progress range selector (spec §11 chart ranges) ──────────────────────

/**
 * Progress data with a user-chosen chart range. Food + exercise + weight +
 * measurements flow through one selector state, so all four charts stay in
 * step (spec §11: 7/30/90/1y/all).
 */
class RangeViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    private val _range = MutableStateFlow(ProgressLogic.ChartRange.DAYS_30)
    val range: StateFlow<ProgressLogic.ChartRange> = _range.asStateFlow()

    fun setRange(range: ProgressLogic.ChartRange) {
        _range.value = range
    }

    /** Daily calorie averages within the selected window (bucketed for long ranges). */
    val calories: StateFlow<List<Pair<String, Double>>> = combine(todayFlow, _range) { today, range ->
        today to range
    }.distinctUntilChanged().flatMapLatest { (today, range) ->
        val days = ProgressLogic.window(range, today)
        val from = if (days.isEmpty()) Dates.addDays(today, -364) else days.first()
        repo.observeLogsBetween(from, today).map { logs ->
            val byDay = days.associateWith { day -> logs.sumOf { if (it.date == day) it.calories else 0.0 } }
            ProgressLogic.downsample(byDay.map { (d, v) -> d to v }, ProgressLogic.bucketSize(range))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Macro totals within the window (whole window, not bucketed). */
    val macros: StateFlow<Map<String, Double>> = combine(todayFlow, _range) { today, range ->
        today to range
    }.distinctUntilChanged().flatMapLatest { (today, range) ->
        val days = ProgressLogic.window(range, today)
        val from = if (days.isEmpty()) Dates.addDays(today, -364) else days.first()
        repo.observeLogsBetween(from, today).map { logs ->
            val inWindow = logs.filter { days.isEmpty() || it.date in days.toSet() }
            mapOf(
                "Protein" to inWindow.sumOf { it.protein },
                "Carbs" to inWindow.sumOf { it.carbs },
                "Fat" to inWindow.sumOf { it.fat }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Exercise minutes per day in the window — exercise frequency (spec §20). */
    val exerciseMinutes: StateFlow<List<Pair<String, Double>>> = combine(todayFlow, _range) { today, range ->
        today to range
    }.distinctUntilChanged().flatMapLatest { (today, range) ->
        val days = ProgressLogic.window(range, today)
        val from = if (days.isEmpty()) Dates.addDays(today, -364) else days.first()
        repo.observeExerciseLogsBetween(from, today).map { logs ->
            val byDay = days.associateWith { day ->
                logs.filter { it.date == day }
                    .sumOf { ProgressLogic.effectiveMinutes(it.durationMinutes, it.sets, it.reps) }
            }
            ProgressLogic.downsample(byDay.map { (d, v) -> d to v }, ProgressLogic.bucketSize(range))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Weight points inside the window (all-time naturally trims to history). */
    val weight: StateFlow<List<Pair<String, Double>>> = combine(todayFlow, _range) { today, range ->
        today to range
    }.distinctUntilChanged().flatMapLatest { (today, range) ->
        repo.observeWeightLogs().map { logs ->
            val days = ProgressLogic.window(range, today)
            val inWindow = if (days.isEmpty()) logs else logs.filter { it.date in days.toSet() }
            inWindow.map { it.date to it.weightKg }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Weight metrics for the selected window (spec §11 Metrics). */
    val weightStats: StateFlow<ProgressLogic.WeightStats> = combine(
        weight, repo.observeProfile()
    ) { points, profile ->
        ProgressLogic.weightStats(points, profile?.goalWeightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressLogic.WeightStats(null, null, null, null, null, 0))

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RangeViewModel(repo) as T
        }
    }
}

// ── Workout routines (spec §10) ──────────────────────────────────────────

/** Reusable routines: build, save, log-to-today, delete (spec §10 + history). */
class RoutinesViewModel(private val repo: AppRepository) : ViewModel() {

    val routines: StateFlow<List<WorkoutRoutine>> = repo.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Exercises from the catalog, offered as searchable lines in the builder. */
    private val _catalog = MutableStateFlow<List<com.example.eyirfitnessapp.domain.model.Exercise>>(emptyList())
    val catalog: StateFlow<List<com.example.eyirfitnessapp.domain.model.Exercise>> = _catalog.asStateFlow()

    /** Expanded routine with items; null = list view. */
    private val _openRoutine = MutableStateFlow<WorkoutRoutine?>(null)
    val openRoutine: StateFlow<WorkoutRoutine?> = _openRoutine.asStateFlow()

    /** Builder draft: name + editable lines. */
    private val _draftName = MutableStateFlow("")
    val draftName: StateFlow<String> = _draftName.asStateFlow()

    private val _draftItems = MutableStateFlow<List<RoutineLogic.DraftItem>>(emptyList())
    val draftItems: StateFlow<List<RoutineLogic.DraftItem>> = _draftItems.asStateFlow()

    /** Routine being logged into the builder (edit mode) or null when creating. */
    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    init {
        viewModelScope.launch { _catalog.value = repo.searchExercises("") }
    }

    fun searchCatalog(query: String) {
        viewModelScope.launch { _catalog.value = repo.searchExercises(query) }
    }

    fun open(routine: WorkoutRoutine) {
        viewModelScope.launch {
            _openRoutine.value = repo.routineWithItems(routine.id) ?: routine
        }
    }

    fun closeDetail() {
        _openRoutine.value = null
    }

    fun startNewRoutine() {
        _editingId.value = null
        _draftName.value = ""
        _draftItems.value = listOf(RoutineLogic.DraftItem())
    }

    fun startEdit(routine: WorkoutRoutine) {
        viewModelScope.launch {
            val full = repo.routineWithItems(routine.id) ?: return@launch
            _editingId.value = full.id
            _draftName.value = full.name
            _draftItems.value = full.items.map { item ->
                RoutineLogic.DraftItem(
                    name = item.name,
                    category = item.category,
                    durationMinutes = item.durationMinutes,
                    sets = item.sets,
                    reps = item.reps,
                    weightKg = item.weightKg,
                    exerciseId = item.exerciseId
                )
            }.ifEmpty { listOf(RoutineLogic.DraftItem()) }
        }
    }

    fun cancelEdit() {
        _editingId.value = null
        _draftName.value = ""
        _draftItems.value = emptyList()
    }

    fun setDraftName(name: String) {
        _draftName.value = name
    }

    fun updateItem(index: Int, item: RoutineLogic.DraftItem) {
        _draftItems.value = _draftItems.value.mapIndexed { i, cur -> if (i == index) item else cur }
    }

    fun addItem() {
        _draftItems.value = _draftItems.value + RoutineLogic.DraftItem()
    }

    fun removeItem(index: Int) {
        _draftItems.value = _draftItems.value.filterIndexed { i, _ -> i != index }
    }

    fun canSaveDraft(): Boolean =
        RoutineLogic.canSave(_draftName.value, _draftItems.value.filter { it.isValid }.map { it.toItem() })

    fun saveDraft(onDone: () -> Unit = {}) {
        if (!canSaveDraft()) return
        val items = _draftItems.value.filter { it.isValid }.map { it.toItem() }
        val editing = _editingId.value
        viewModelScope.launch {
            val existing = editing?.let { repo.routineWithItems(it) }
            repo.saveRoutine(
                WorkoutRoutine(
                    id = editing ?: 0L,
                    name = _draftName.value.trim(),
                    note = existing?.note,
                    items = items,
                    createdAt = existing?.createdAt ?: 0L,
                    updatedAt = existing?.updatedAt ?: 0L
                )
            )
            cancelEdit()
            showNotice("Routine saved")
            onMain(onDone)
        }
    }

    /** Log every line of the routine into today's exercise diary (spec §10). */
    fun logRoutine(routine: WorkoutRoutine) {
        viewModelScope.launch {
            val weight = profile.value?.currentWeightKg ?: 75.0
            val count = repo.logRoutine(routine, repo.today(), weight)
            showNotice("Logged $count exercise${if (count == 1) "" else "s"} from ${routine.name}")
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            repo.deleteRoutine(id)
            _openRoutine.value = null
            showNotice("Routine deleted")
        }
    }

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            kotlinx.coroutines.delay(4_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RoutinesViewModel(repo) as T
        }
    }
}

// ── Body measurements (spec §11) ─────────────────────────────────────────

/** Measurements: per-part entry, latest values, and history (spec §11). */
class MeasurementsViewModel(private val repo: AppRepository) : ViewModel() {

    val measurements: StateFlow<List<BodyMeasurement>> = repo.observeMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Latest measurement per part, keyed by part. */
    fun latestByPart(): Map<MeasurementPart, BodyMeasurement> =
        measurements.value
            .sortedBy { it.date }
            .groupBy { it.part }
            .mapValues { (_, list) -> list.last() }

    fun add(part: MeasurementPart, cmText: String, date: String = repo.today()) {
        val cm = cmText.toDoubleOrNull()
        if (cm == null || cm <= 0) {
            showNotice("Enter a size in cm")
            return
        }
        if (cm > 400) {
            showNotice("Enter a size between 1 and 400 cm")
            return
        }
        viewModelScope.launch {
            repo.addMeasurement(part, cm, date)
            showNotice("${part.label} saved")
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteMeasurement(id) }
    }

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            kotlinx.coroutines.delay(4_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MeasurementsViewModel(repo) as T
        }
    }
}

// ── Habits (spec §12) ────────────────────────────────────────────────────

/** Habit tracking: pick habits, check off days, streaks and completion (§12). */
class HabitsViewModel(private val repo: AppRepository) : ViewModel() {

    init {
        // Catch up auto-tickable habits from today's already-logged activity.
        viewModelScope.launch { repo.reconcileAutoHabits() }
    }

    private val todayFlow = liveToday(repo, viewModelScope)

    /** Today (yyyy-MM-dd) as the repository sees it — the board's current day. */
    val today: StateFlow<String> = todayFlow

    /** Preferences that shape the board — notably the user's start-of-week day (§22). */
    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val habits: StateFlow<List<Habit>> = repo.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Yesterday's completions, for one-tap "check off yesterday" recovery. */
    val yesterday: StateFlow<String> = todayFlow
        .map { Dates.addDays(it, -1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Dates.addDays(repo.today(), -1))

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var lastStreaks: Map<Long, Int> = emptyMap()
    private var baselineReady = false

    init {
        // Fire the celebration the moment a streak crosses 7, 14, 21 …
        viewModelScope.launch {
            combine(habits, todayFlow) { list, day -> list to day }
                .collect { (list, day) ->
                    val streaks = list.associate { it.id to HabitLogic.streak(it, day) }
                    if (baselineReady) {
                        streaks.forEach { (id, s) ->
                            val hit = HabitLogic.milestone(lastStreaks[id] ?: 0, s)
                            if (hit != null) {
                                val label = list.firstOrNull { it.id == id }?.label ?: return@forEach
                                com.example.eyirfitnessapp.util.CelebrationBus.show(label, hit)
                            }
                        }
                    }
                    lastStreaks = streaks
                    baselineReady = true
                }
        }
    }

    /**
     * Demo trigger (styling review): celebrate the next weekly milestone from
     * the habit's current streak — long-press the habit card on the Habits screen.
     */
    fun previewMilestone(habit: Habit) {
        val streak = HabitLogic.streak(habit, todayFlow.value)
        com.example.eyirfitnessapp.util.CelebrationBus.show(habit.label, (streak / 7 + 1) * 7)
    }

    /** One-tap demo from the Habits screen's "Preview" button. */
    fun previewCelebration() {
        val habit = habits.value.firstOrNull()
        if (habit != null) previewMilestone(habit)
        else com.example.eyirfitnessapp.util.CelebrationBus.show("Streak", 5)
    }

    /** Catalog entries the user has not started tracking yet. */
    fun availableCatalog(habits: List<Habit>): List<HabitLogic.CatalogHabit> =
        HabitLogic.CATALOG.filter { c -> habits.none { it.key == c.key } }

    fun addHabit(catalog: HabitLogic.CatalogHabit) {
        viewModelScope.launch {
            repo.addHabit(catalog.key, catalog.label, catalog.icon)
            showNotice("Tracking \"${catalog.label}\"")
        }
    }

    fun removeHabit(id: Long) {
        viewModelScope.launch {
            repo.removeHabit(id)
            showNotice("Habit removed")
        }
    }

    fun toggleToday(habit: Habit) {
        viewModelScope.launch { repo.toggleHabitDay(habit.id, todayFlow.value) }
    }

    fun toggleDay(habit: Habit, date: String) {
        viewModelScope.launch { repo.toggleHabitDay(habit.id, date) }
    }

    fun streakOf(habit: Habit): Int = HabitLogic.streak(habit, todayFlow.value)

    fun isDoneToday(habit: Habit): Boolean = HabitLogic.isDoneToday(habit, todayFlow.value)

    fun rate7(habit: Habit): Double = HabitLogic.completionRate(habit, todayFlow.value, 7)

    fun rate30(habit: Habit): Double = HabitLogic.completionRate(habit, todayFlow.value, 30)

    /**
     * The current week's 7 dates, oldest first (§12 habit board columns).
     * Buckets follow the profile's start-of-week day instead of always Monday.
     */
    fun weekDays(
        startDay: WeekStartDay = profile.value?.weekStartDay ?: WeekStartDay.MONDAY
    ): List<String> {
        val startOfWeek = Dates.weekStart(todayFlow.value, startDay)
        return (0..6).map { Dates.addDays(startOfWeek, it) }
    }

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            kotlinx.coroutines.delay(4_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HabitsViewModel(repo) as T
        }
    }
}
