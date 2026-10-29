@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.eyirfitnessapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.diary.DiaryLogic
import com.example.eyirfitnessapp.domain.export.DataExport
import com.example.eyirfitnessapp.domain.model.DailySummary
import com.example.eyirfitnessapp.domain.model.Exercise
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.FastingSession
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.FoodSize
import com.example.eyirfitnessapp.domain.model.GroceryItem
import com.example.eyirfitnessapp.domain.model.Goals
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.NutrientTotals
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.PlannedMeal
import com.example.eyirfitnessapp.domain.model.Recipe
import com.example.eyirfitnessapp.domain.model.ThemeMode
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import com.example.eyirfitnessapp.domain.model.WeightLog
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import com.example.eyirfitnessapp.util.AppMessages
import com.example.eyirfitnessapp.util.Dates
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Today's date for long-lived view models.
 *
 * A plain `repo.today()` property is snapshotted when the view model is built, so a session that
 * crosses midnight keeps writing to / observing the previous day. This flow re-reads the date every
 * 30s and pushes the new value downstream, so every keyed flow rolls over with it.
 */
internal fun liveToday(repo: AppRepository, scope: CoroutineScope): StateFlow<String> =
    flow {
        while (true) {
            emit(repo.today())
            delay(30_000)
        }
    }.stateIn(scope, SharingStarted.Eagerly, repo.today())

/**
 * UI callbacks (navigate / pop back) must run on the main thread. ViewModels
 * launch on the main dispatcher, but a Room suspend call resumes on an IO
 * thread — so a bare `onDone()` after a write can hit NavController off-main
 * and crash. Route every completion callback through this hop.
 */
internal suspend fun onMain(callback: () -> Unit) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { callback() }
}

/** Onboarding: collects profile and computes goals via NutritionEngine (spec §21). */
class OnboardingViewModel(private val repo: AppRepository) : ViewModel() {

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun complete(profile: UserProfile) {
        repo.saveProfile(profile.copy(onboardingComplete = true))
    }

    fun computedGoals(profile: UserProfile) = NutritionEngine.macroGoals(profile)

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OnboardingViewModel(repo) as T
        }
    }
}

/**
 * Which unit the Amount field is editing: the food's own serving unit
 * ("medium", "slice") or grams entered from a food scale.
 */
enum class AmountUnit { NATIVE, GRAMS }

/** Food search + details + add-to-diary flow (spec §7 Flow A, §8). */
class FoodSearchViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    /** Date being logged to. Follows "today" live unless the caller pinned a specific day. */
    private var targetDateOverride: String? = null
    private val targetDate: String get() = targetDateOverride ?: todayFlow.value

    private val _results = MutableStateFlow<List<Food>>(emptyList())
    val results: StateFlow<List<Food>> = _results.asStateFlow()

    private val _recent = MutableStateFlow<List<FoodLog>>(emptyList())
    val recent: StateFlow<List<FoodLog>> = _recent.asStateFlow()

    private val _favorites = MutableStateFlow<List<Food>>(emptyList())
    val favorites: StateFlow<List<Food>> = _favorites.asStateFlow()

    private val _frequent = MutableStateFlow<List<Food>>(emptyList())
    val frequent: StateFlow<List<Food>> = _frequent.asStateFlow()

    /** Multi-item selection (spec §2): check several results, log them together. */
    private val _multiSelect = MutableStateFlow(false)
    val multiSelect: StateFlow<Boolean> = _multiSelect.asStateFlow()

    private val _checkedIds = MutableStateFlow<Set<Long>>(emptySet())
    val checkedIds: StateFlow<Set<Long>> = _checkedIds.asStateFlow()

    private val _selected = MutableStateFlow<Food?>(null)
    val selected: StateFlow<Food?> = _selected.asStateFlow()

    private val _quantity = MutableStateFlow(1.0)
    val quantity: StateFlow<Double> = _quantity.asStateFlow()

    /** Amount field unit; [_quantity] always stays in the food's native serving unit. */
    private val _amountUnit = MutableStateFlow(AmountUnit.NATIVE)
    val amountUnit: StateFlow<AmountUnit> = _amountUnit.asStateFlow()

    /** Grams chosen but "1 serving = ? g" unknown → show the ask-once prompt. */
    private val _needsGramWeight = MutableStateFlow(false)
    val needsGramWeight: StateFlow<Boolean> = _needsGramWeight.asStateFlow()

    /** Size the user tapped before the gram weight was known; applied after saving. */
    private var pendingSize: FoodSize? = null

    /** Busy while a log write is in flight — blocks double-taps (duplicate rows + double pop). */
    private val _adding = MutableStateFlow(false)
    val adding: StateFlow<Boolean> = _adding.asStateFlow()

    /**
     * One-shot exit guard: the "Add" callbacks pop the screen, and a second tap
     * while the first Room write is still running used to fire a SECOND pop —
     * [today, diary, food_search] lost two entries and landed on Home.
     */
    private var completed = false

    private val _mealType = MutableStateFlow(MealType.BREAKFAST)
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    /** Bumped on every new selection so the amount field can re-seed (same food, new amount). */
    private val _selectionSeq = MutableStateFlow(0)
    val selectionSeq: StateFlow<Int> = _selectionSeq.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null
    private var lastQuery: String = ""

    /** Target date shown to the user when it isn't today (spec §6 Rule 5: feedback). */
    val targetDateLabel: String
        get() = targetDate

    init {
        viewModelScope.launch {
            _recent.value = repo.recentLogs()
            _frequent.value = repo.frequentFoods()
        }
        viewModelScope.launch {
            repo.observeFavorites().collect { _favorites.value = it }
        }
    }

    /**
     * Debounced search: one query per pause in typing rather than one per
     * keystroke (spec §31 "responsive while typing"), with explicit
     * loading + error + retry states (spec §34).
     */
    fun search(query: String) {
        lastQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searching.value = true
            delay(250)
            try {
                _results.value = repo.searchFoods(query)
                _searchError.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _results.value = emptyList()
                _searchError.value = "Food search isn't responding."
            }
            _searching.value = false
        }
    }

    fun retrySearch() = search(lastQuery)

    fun select(food: Food) {
        _selected.value = food
        _quantity.value = food.servingSize
        resetAmountUnit()
        _selectionSeq.value++
    }

    /** Open a recent log's food in the detail flow with the logged amount pre-filled. */
    fun openRecent(log: FoodLog, onMissingFood: () -> Unit) {
        viewModelScope.launch {
            val foodId = log.foodId?.takeIf { it > 0 }
            val food = foodId?.let { repo.foodById(it) }
            if (food == null) {
                if (foodId != null) {
                    // The product was deleted since it was logged: re-inserting a copy
                    // would silently duplicate the diary, so say so and write nothing.
                    AppMessages.error("\"${log.name}\" is no longer in your food library, so it can't be re-logged.")
                } else {
                    // Standalone rows (quick add, recipe serving) have no food to open.
                    onMissingFood()
                }
                return@launch
            }
            _selected.value = food
            _quantity.value = log.quantity
            resetAmountUnit()
            _selectionSeq.value++
        }
    }

    fun clearSelection() {
        _selected.value = null
        _quantity.value = 1.0
        resetAmountUnit()
    }

    private fun resetAmountUnit() {
        _amountUnit.value = AmountUnit.NATIVE
        _needsGramWeight.value = false
        pendingSize = null
    }

    /** Size chip tapped: set the amount to that size's weight ([FoodSize]). */
    fun selectSize(size: FoodSize) {
        val food = _selected.value ?: return
        if (NutritionEngine.gramsPerServing(food) == null) {
            // Ask-once first; the size is applied as soon as the weight is saved.
            pendingSize = size
            _needsGramWeight.value = true
            return
        }
        applySize(food, size)
    }

    private fun applySize(food: Food, size: FoodSize) {
        val native = NutritionEngine.fromGrams(food, size.grams) ?: return
        _quantity.value = native
        // Weight-based foods are grams already; count foods switch to gram entry
        // so the amount (and the diary row) reads "50 g", not "0.89 large".
        _amountUnit.value =
            if (NutritionEngine.isWeightBased(food)) AmountUnit.NATIVE else AmountUnit.GRAMS
        _selectionSeq.value++
    }

    /**
     * Switch the Amount field between the food's own unit and grams.
     * Grams on a count-based food with unknown weight opens the ask-once
     * prompt instead; [saveGramWeight] completes the switch.
     */
    fun setAmountUnit(unit: AmountUnit) {
        if (unit == AmountUnit.NATIVE) {
            _amountUnit.value = AmountUnit.NATIVE
            _needsGramWeight.value = false
            return
        }
        val food = _selected.value ?: return
        if (NutritionEngine.gramsPerServing(food) != null) {
            _amountUnit.value = AmountUnit.GRAMS
            _needsGramWeight.value = false
        } else {
            _needsGramWeight.value = true
        }
    }

    /** Ask-once: remember "1 serving = grams" on the food, then enter gram mode. */
    fun saveGramWeight(grams: Double) {
        val food = _selected.value ?: return
        if (grams <= 0) return
        viewModelScope.launch {
            repo.setGramsPerServing(food, grams)
            val updated = repo.foodById(food.id) ?: food.copy(gramsPerServing = grams)
            if (_selected.value?.id == food.id) _selected.value = updated
            _needsGramWeight.value = false
            val size = pendingSize
            pendingSize = null
            if (size != null) {
                applySize(updated, size)
            } else {
                _amountUnit.value = AmountUnit.GRAMS
            }
        }
    }

    /** Spec §8: favorite / unfavorite from any list the food appears in. */
    fun toggleFavorite(food: Food) {
        val next = !food.favorite
        viewModelScope.launch {
            repo.setFavorite(food, next)
            // Optimistic: every list holding this food flips immediately (spec §34 rule 7).
            _results.value = _results.value.map { if (it.id == food.id) it.copy(favorite = next) else it }
            _favorites.value = if (next) {
                (_favorites.value.filter { it.id != food.id } + food.copy(favorite = next)).sortedBy { it.name }
            } else {
                _favorites.value.filterNot { it.id == food.id }
            }
            _frequent.value = _frequent.value.map { if (it.id == food.id) it.copy(favorite = next) else it }
            _selected.value = _selected.value?.let { if (it.id == food.id) it.copy(favorite = next) else it }
        }
    }

    fun toggleMultiSelect() {
        _multiSelect.value = !_multiSelect.value
        if (!_multiSelect.value) _checkedIds.value = emptySet()
    }

    fun toggleChecked(food: Food) {
        _checkedIds.value = _checkedIds.value.toMutableSet().apply {
            if (!add(food.id)) remove(food.id)
        }
    }

    /** Every food currently listed (results, favorites, frequent) by id. */
    private fun visibleFood(id: Long): Food? =
        _results.value.firstOrNull { it.id == id }
            ?: _favorites.value.firstOrNull { it.id == id }
            ?: _frequent.value.firstOrNull { it.id == id }

    /** Log each checked food at its default serving (spec §2 multi-item selection). */
    fun addCheckedToDiary(onDone: () -> Unit) {
        val ids = _checkedIds.value
        if (ids.isEmpty()) return
        if (_adding.value) return
        _adding.value = true
        val meal = _mealType.value
        viewModelScope.launch {
            try {
                var added = 0
                ids.forEach { id ->
                    val food = visibleFood(id) ?: return@forEach
                    writeLog(food, food.servingSize, meal)
                    added++
                }
                _recent.value = repo.recentLogs()
                _frequent.value = repo.frequentFoods()
                _checkedIds.value = emptySet()
                _multiSelect.value = false
                // Stays on screen, so the inline notice is actually visible.
                showNotice("Added $added item${if (added == 1) "" else "s"} to ${meal.label}")
                onMain(onDone)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                AppMessages.error("Couldn't add those foods to your diary. Please try again.")
            } finally {
                _adding.value = false
            }
        }
    }

    fun setQuantity(q: Double) {
        _quantity.value = q
    }

    fun setMealType(type: MealType) {
        _mealType.value = type
    }

    /** Configure targets coming from another screen (Diary meal row / Today meal card). */
    fun configure(meal: MealType?, date: String?) {
        if (meal != null) _mealType.value = meal
        targetDateOverride = date?.takeIf { it.isNotBlank() }
        completed = false
        _adding.value = false
    }

    /** True when the amount field holds a positive number. */
    fun canAdd(q: Double) = q > 0.0

    /** Pops back at most once per screen — see [completed]. */
    private suspend fun finish(onDone: () -> Unit) {
        if (completed) return
        completed = true
        onMain(onDone)
    }

    /** Add the currently selected food with its scaled nutrition (spec §9 scale). */
    fun addSelectedToDiary(onDone: () -> Unit) {
        val food = _selected.value ?: return
        val q = _quantity.value
        if (!canAdd(q)) return
        if (_adding.value) return
        _adding.value = true
        viewModelScope.launch {
            try {
                val meal = _mealType.value
                writeLog(food, q, meal)
                _recent.value = repo.recentLogs()
                _frequent.value = repo.frequentFoods()
                // The screen pops on success, so the confirmation has to travel
                // with the user — an inline notice would never be seen.
                AppMessages.notice("Added ${food.name} to ${meal.label}")
                finish(onDone)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Room writes are suspend fns, not flows: nothing else surfaces
                // this. Report it and leave `completed` false so the tap retries.
                AppMessages.error("Couldn't add ${food.name} to your diary. Please try again.")
            } finally {
                _adding.value = false
            }
        }
    }

    /** One scaled write — shared by single and multi-item logging. */
    private suspend fun writeLog(food: Food, quantity: Double, meal: MealType) {
        val scaled = NutritionEngine.scale(food, quantity)
        // Gram entry (size chips / scale typing) snapshots the log in grams so the
        // diary reads "50 g" instead of a converted count like "0.89 large".
        var logQuantity = quantity
        var logServingSize = food.servingSize
        var logServingUnit = food.servingUnit
        val gramsPerServing = NutritionEngine.gramsPerServing(food)
        if (_amountUnit.value == AmountUnit.GRAMS && gramsPerServing != null) {
            NutritionEngine.toGrams(food, quantity)?.let { grams ->
                logQuantity = grams
                logServingSize = gramsPerServing
                logServingUnit = "g"
            }
        }
        repo.addFoodLog(
            FoodLog(
                foodId = food.id,
                name = food.name,
                brand = food.brand,
                mealType = meal,
                quantity = logQuantity,
                servingSize = logServingSize,
                servingUnit = logServingUnit,
                calories = scaled.calories,
                protein = scaled.protein,
                carbs = scaled.carbs,
                fat = scaled.fat,
                date = targetDate,
                loggedAt = System.currentTimeMillis()
            )
        )
    }

    fun addRecentLogToDiary(log: FoodLog, onDone: () -> Unit) {
        if (_adding.value) return
        _adding.value = true
        val meal = _mealType.value
        val written = log.copy(
            id = 0,
            mealType = meal,
            date = targetDate,
            loggedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                val foodId = log.foodId?.takeIf { it > 0 }
                if (foodId != null && repo.foodById(foodId) == null) {
                    AppMessages.error("\"${log.name}\" is no longer in your food library, so it can't be re-logged.")
                    return@launch
                }
                repo.addFoodLog(written)
                _recent.value = repo.recentLogs()
                _frequent.value = repo.frequentFoods()
                AppMessages.notice("Added ${written.name} to ${meal.label}")
                finish(onDone)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                AppMessages.error("Couldn't add ${log.name} to your diary. Please try again.")
            } finally {
                _adding.value = false
            }
        }
    }

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            delay(4_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** Quick add (spec §8): calories + optional macros without a food entry. */
    fun quickAddCalories(calories: Double, protein: Double, carbs: Double, fat: Double, meal: MealType, onDone: () -> Unit) {
        if (_adding.value) return
        _adding.value = true
        viewModelScope.launch {
            try {
                repo.addFoodLog(
                    FoodLog(
                        foodId = null,
                        name = "Quick add",
                        mealType = meal,
                        quantity = 1.0,
                        servingSize = 1.0,
                        servingUnit = "serving",
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        date = targetDate,
                        loggedAt = System.currentTimeMillis()
                    )
                )
                _recent.value = repo.recentLogs()
                _frequent.value = repo.frequentFoods()
                // Pops on success → global snackbar, and the recents list has to
                // show the new row next time the screen opens.
                AppMessages.notice("Added ${calories.toInt()} kcal to ${meal.label}")
                finish(onDone)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                AppMessages.error("Couldn't log those calories. Please try again.")
            } finally {
                _adding.value = false
            }
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FoodSearchViewModel(repo) as T
        }
    }
}

/** Diary screen: view, edit, delete, copy day (spec §8). */
class DiaryViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    /** Null while the diary tracks today; set once the user steps to another day. */
    private val pinnedDate = MutableStateFlow<String?>(null)

    val date: StateFlow<String> =
        combine(todayFlow, pinnedDate) { today, pinned -> pinned ?: today }
            .stateIn(viewModelScope, SharingStarted.Eagerly, repo.today())

    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val summary: StateFlow<DailySummary?> = diarySummaryFlow(date, repo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs: StateFlow<List<FoodLog>> = date
        .flatMapLatest { day -> repo.observeFoodLogs(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The day being copied FROM, kept live so the "Copy previous day" dialog
     * can quote the real item count instead of today's.
     */
    val previousLogs: StateFlow<List<FoodLog>> = date
        .flatMapLatest { day -> repo.observeFoodLogs(Dates.addDays(day, -1)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Food behind the entry currently being edited — needed for gram conversion. */
    private val _editFood = MutableStateFlow<Food?>(null)
    val editFood: StateFlow<Food?> = _editFood.asStateFlow()

    fun loadFoodForEdit(log: FoodLog) {
        viewModelScope.launch {
            _editFood.value = log.foodId?.takeIf { it > 0 }?.let { repo.foodById(it) }
        }
    }

    /** Ask-once from the edit dialog: remember "1 serving = grams" on the food. */
    fun rememberGramWeight(grams: Double) {
        val food = _editFood.value ?: return
        if (grams <= 0) return
        viewModelScope.launch {
            repo.setGramsPerServing(food, grams)
            _editFood.value = repo.foodById(food.id) ?: food.copy(gramsPerServing = grams)
        }
    }

    fun shiftDate(days: Int) {
        val target = Dates.addDays(date.value, days)
        pinnedDate.value = if (target == todayFlow.value) null else target
    }

    /** Jump straight to [target] (yyyy-MM-dd) from the calendar picker. */
    fun jumpToDate(target: String) {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(target)) return
        pinnedDate.value = if (target == todayFlow.value) null else target
    }

    /** True when the diary is currently pinned to a past/future day instead of today. */
    fun isPinned(): Boolean = pinnedDate.value != null

    fun deleteLog(id: Long) {
        viewModelScope.launch { repo.deleteFoodLog(id) }
    }

    /** Save amount + meal in ONE write (two separate full-row updates would race). */
    fun saveEdit(id: Long, quantity: Double, meal: MealType) {
        if (quantity <= 0) return
        viewModelScope.launch { applyEdit(id, quantity, meal) }
    }

    private suspend fun applyEdit(id: Long, quantity: Double?, meal: MealType?) {
        val log = logs.value.firstOrNull { it.id == id } ?: return
        val targetQuantity = quantity ?: log.quantity
        if (targetQuantity <= 0) return
        val factor = if (log.quantity > 0) targetQuantity / log.quantity else 1.0
        repo.updateFoodLog(
            log.copy(
                mealType = meal ?: log.mealType,
                quantity = targetQuantity,
                calories = log.calories * factor,
                protein = log.protein * factor,
                carbs = log.carbs * factor,
                fat = log.fat * factor
            )
        )
    }

    fun copyPreviousDay() {
        viewModelScope.launch {
            val to = date.value
            val from = Dates.addDays(to, -1)
            val copied = repo.copyDay(from = from, to = to)
            showNotice(
                if (copied > 0) "Copied $copied item${if (copied == 1) "" else "s"} from ${Dates.prettyDate(from)}"
                else "Nothing logged on ${Dates.prettyDate(from)}"
            )
        }
    }

    /** Spec §2: duplicate one entry on the current day (optionally into another meal). */
    fun copyLog(log: FoodLog, meal: MealType = log.mealType) {
        viewModelScope.launch {
            val copy = DiaryLogic.duplicated(log, date.value, meal)
            repo.addFoodLog(copy)
            showNotice("Copied ${copy.name} to ${meal.label}")
        }
    }

    /** Spec §2: duplicate a whole meal section into another meal of the same day. */
    fun copyMeal(from: MealType, to: MealType) {
        viewModelScope.launch {
            val day = date.value
            val copies = DiaryLogic.copyMealLogs(repo.logsForDate(day), day, from, to)
            copies.forEach { repo.addFoodLog(it) }
            showNotice(copiedCount(copies.size, "${from.label} to ${to.label}"))
        }
    }

    /** Spec §2: bring yesterday's [meal] onto the current day. */
    fun copyPreviousMeal(meal: MealType) {
        viewModelScope.launch {
            val day = date.value
            val previous = DiaryLogic.previousDay(day)
            val copies = DiaryLogic.previousMealLogs(repo.logsForDate(previous), day, meal)
            copies.forEach { repo.addFoodLog(it) }
            showNotice(
                if (copies.isNotEmpty()) copiedCount(copies.size, "${meal.label} from ${Dates.prettyDate(previous)}")
                else "Nothing logged in ${meal.label} yesterday"
            )
        }
    }

    private fun copiedCount(count: Int, where: String) =
        "Copied $count item${if (count == 1) "" else "s"} $where"

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            kotlinx.coroutines.delay(4_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DiaryViewModel(repo) as T
        }
    }
}

private fun diarySummaryFlow(dateFlow: kotlinx.coroutines.flow.Flow<String>, repo: AppRepository): kotlinx.coroutines.flow.Flow<DailySummary> =
    dateFlow.flatMapLatest { date -> repo.observeSummary(date) }

/** Water tracker (spec §12). */
class WaterViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    val waterMl: StateFlow<Int> = todayFlow
        .flatMapLatest { day -> repo.observeWaterLogs(day) }
        .map { list -> list.sumOf { it.amountMl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(amountMl: Int) {
        viewModelScope.launch { repo.addWater(amountMl) }
    }

    fun deleteLast() {
        viewModelScope.launch {
            val day = todayFlow.value
            val logs = repo.waterBetween(day, day)
            logs.lastOrNull()?.let { repo.deleteWater(it.id) }
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WaterViewModel(repo) as T
        }
    }
}

/** Weight logging + history (spec §11). */
class WeightViewModel(private val repo: AppRepository) : ViewModel() {

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs: StateFlow<List<WeightLog>> = repo.observeWeightLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(weightKg: Double, note: String?) {
        viewModelScope.launch {
            repo.addWeight(
                WeightLog(
                    weightKg = weightKg,
                    note = note?.takeIf { it.isNotBlank() },
                    date = repo.today(),
                    loggedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteWeight(id) }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WeightViewModel(repo) as T
        }
    }
}

/** Exercise search + logging (spec §10). */
class ExerciseViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _selected = MutableStateFlow<Exercise?>(null)
    val selected: StateFlow<Exercise?> = _selected.asStateFlow()

    val logs = todayFlow
        .flatMapLatest { day -> repo.observeExerciseLogs(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { _exercises.value = repo.searchExercises("") }
    }

    fun search(query: String) {
        viewModelScope.launch { _exercises.value = repo.searchExercises(query) }
    }

    fun select(exercise: Exercise?) {
        _selected.value = exercise
    }

    fun logCardio(exercise: Exercise, minutes: Double, onDone: () -> Unit) {
        val weight = profile.value?.currentWeightKg ?: 75.0
        val burned = repo.cardioCalories(exercise.metValue, weight, minutes)
        viewModelScope.launch {
            repo.addExerciseLog(
                ExerciseLog(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    category = exercise.category,
                    durationMinutes = minutes,
                    sets = null,
                    reps = null,
                    weightKg = null,
                    caloriesBurned = burned,
                    date = todayFlow.value,
                    loggedAt = System.currentTimeMillis()
                )
            )
            onMain(onDone)
        }
    }

    fun logStrength(exercise: Exercise, sets: Int, reps: Int, weightKg: Double, onDone: () -> Unit) {
        val weight = profile.value?.currentWeightKg ?: 75.0
        val minutes = sets * reps * 2.5 / 60.0 * 4 + sets * 1.5
        val burned = repo.cardioCalories(exercise.metValue, weight, minutes)
        viewModelScope.launch {
            repo.addExerciseLog(
                ExerciseLog(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    category = exercise.category,
                    durationMinutes = null,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    caloriesBurned = burned,
                    date = todayFlow.value,
                    loggedAt = System.currentTimeMillis()
                )
            )
            onMain(onDone)
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch { repo.deleteExerciseLog(id) }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExerciseViewModel(repo) as T
        }
    }
}

/** Intermittent fasting (spec §19): presets, timer, history — via the repository like every other feature. */
class FastingViewModel(private val repo: AppRepository) : ViewModel() {

    val sessions: StateFlow<List<FastingSession>> = repo.observeFasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val active: StateFlow<FastingSession?> = sessions
        .map { list -> list.firstOrNull { it.status == "ACTIVE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<FastingSession>> = sessions
        .map { list -> list.filter { it.status != "ACTIVE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start(targetHours: Int) {
        viewModelScope.launch { repo.startFast(targetHours) }
    }

    fun end() {
        val session = active.value ?: return
        viewModelScope.launch { repo.endFast(session) }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FastingViewModel(repo) as T
        }
    }
}

/** Progress charts + weekly analytics (spec §11, §20). Recomputes whenever logs change. */
class ProgressViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Effective targets (profile + stored macro override) — AppRepository.observeGoals. */
    val goals: StateFlow<com.example.eyirfitnessapp.domain.model.Goals?> = repo.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weightLogs = repo.observeWeightLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class WeekStats(
        val calories: List<Pair<String, Double>> = emptyList(),
        val macros: Map<String, Double> = emptyMap()
    )

    private val weekStats: StateFlow<WeekStats> = todayFlow
        .map { Dates.lastDays(it, 7) }
        .distinctUntilChanged()
        .flatMapLatest { days -> repo.observeLogsBetween(days.first(), days.last()).map { logs -> days to logs } }
        .map { (days, logs) ->
            WeekStats(
                calories = days.map { day ->
                    Dates.weekdayShort(day) to logs.sumOf { if (it.date == day) it.calories else 0.0 }
                },
                macros = mapOf(
                    "Protein" to logs.sumOf { it.protein },
                    "Carbs" to logs.sumOf { it.carbs },
                    "Fat" to logs.sumOf { it.fat }
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekStats())

    val weeklyCalories: StateFlow<List<Pair<String, Double>>> = weekStats
        .map { it.calories }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyMacros: StateFlow<Map<String, Double>> = weekStats
        .map { it.macros }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(repo) as T
        }
    }
}

/** Recipes: build, save, list, log one serving (spec §16). */
class RecipeViewModel(private val repo: AppRepository) : ViewModel() {

    val recipes: StateFlow<List<Recipe>> = repo.observeRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One ingredient row of the recipe being built (per-100g inputs). */
    data class DraftIngredient(
        val name: String,
        val grams: Double,
        val kcal100: Double,
        val protein100: Double,
        val carbs100: Double,
        val fat100: Double
    ) {
        val contribution: PlanLogic.IngredientContribution
            get() = PlanLogic.scaleFromPer100g(grams, kcal100, protein100, carbs100, fat100)
    }

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _servingsText = MutableStateFlow("2")
    val servingsText: StateFlow<String> = _servingsText.asStateFlow()

    private val _ingredients = MutableStateFlow<List<DraftIngredient>>(emptyList())
    val ingredients: StateFlow<List<DraftIngredient>> = _ingredients.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun setName(v: String) { _name.value = v }

    fun setServings(v: String) { _servingsText.value = v }

    fun addIngredient(ingredient: DraftIngredient) {
        _ingredients.value = _ingredients.value + ingredient
    }

    fun removeIngredient(index: Int) {
        _ingredients.value = _ingredients.value.filterIndexed { i, _ -> i != index }
    }

    /** Whole-recipe totals from the current draft; null when empty. */
    fun totals(): PlanLogic.RecipeTotals? =
        _ingredients.value.takeIf { it.isNotEmpty() }
            ?.let { PlanLogic.totalsOf(it.map { d -> d.contribution }) }

    fun servings(): Int = _servingsText.value.toIntOrNull()?.takeIf { it > 0 } ?: 1

    fun canSave(): Boolean = _name.value.isNotBlank() && _ingredients.value.isNotEmpty()

    fun save(onDone: () -> Unit) {
        if (!canSave()) return
        val now = System.currentTimeMillis()
        val totals = totals() ?: return
        viewModelScope.launch {
            repo.saveRecipe(
                recipe = Recipe(
                    name = _name.value.trim(),
                    servings = servings(),
                    totalCalories = totals.totalCalories,
                    totalProtein = totals.totalProtein,
                    totalCarbs = totals.totalCarbs,
                    totalFat = totals.totalFat,
                    createdAt = now,
                    updatedAt = now
                ),
                items = _ingredients.value.map { Triple(it.name, it.grams, it.contribution) }
            )
            clearDraft()
            showNotice("Recipe saved")
            onMain(onDone)
        }
    }

    fun clearDraft() {
        _name.value = ""
        _servingsText.value = "2"
        _ingredients.value = emptyList()
    }

    fun delete(recipeId: Long) {
        viewModelScope.launch {
            repo.deleteRecipe(recipeId)
            showNotice("Recipe deleted")
        }
    }

    /** One-tap logging of a recipe serving into today's diary (spec §16). */
    fun logServing(recipeId: Long, meal: MealType) {
        viewModelScope.launch {
            val inserted = repo.logRecipeServing(recipeId, meal, repo.today())
            showNotice(if (inserted != null) "Logged 1 serving to ${meal.label}" else "Recipe not found")
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
                RecipeViewModel(repo) as T
        }
    }
}

/** Weekly meal planner (spec §17): generate, log slots, grocery extraction. */
class PlannerViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    private val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Week window for the planner — starts on the user's chosen day (spec §22). */
    val weekStart: StateFlow<String> = combine(todayFlow, profile) { today, p ->
            Dates.weekStart(today, p?.weekStartDay ?: WeekStartDay.MONDAY)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Dates.weekStart(repo.today()))

    /** Effective targets (profile + stored macro override) — AppRepository.observeGoals. */
    val goals: StateFlow<com.example.eyirfitnessapp.domain.model.Goals?> = repo.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: StateFlow<List<PlannedMeal>> = weekStart
        .flatMapLatest { start -> repo.observePlan(start, Dates.addDays(start, 6)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saved recipes, so a slot can be backed by one (the only path that feeds grocery extraction). */
    val recipes: StateFlow<List<Recipe>> = repo.observeRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Fill Mon–Sun × 4 meals with goal-aware suggestions (spec §17). */
    fun generatePlan() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val start = weekStart.value
                val goal = goals.value?.dailyCalorieGoal
                for (dayOffset in 0..6) {
                    val day = Dates.addDays(start, dayOffset)
                    val usedTitles = mutableSetOf<String>()
                    var plannedToday = 0.0
                    MealType.entries.forEachIndexed { mealIndex, meal ->
                        val idea = PlanLogic.pickIdea(
                            mealIndex = mealIndex,
                            usedTitles = usedTitles,
                            remainingKcal = (goal ?: 2000) - plannedToday,
                            dailyKcalGoal = goal
                        )
                        usedTitles += idea.title
                        plannedToday += idea.kcal
                        repo.setPlanSlot(
                            date = day,
                            mealType = meal,
                            title = idea.title,
                            recipeId = null,
                            plannedKcal = idea.kcal.toDouble()
                        )
                    }
                }
                showNotice("Week plan generated")
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteSlot(id: Long) {
        viewModelScope.launch { repo.deletePlanSlot(id) }
    }

    /**
     * Plan a saved recipe into [date] + [mealType] (spec §17 "add meal").
     * This is what gives a slot a non-null `recipeId`, which is the only input
     * [AppRepository.extractGroceryFromPlan] can read.
     */
    fun assignRecipe(date: String, mealType: MealType, recipe: Recipe) {
        val perServing = recipe.perServing()
        viewModelScope.launch {
            repo.setPlanSlot(
                date = date,
                mealType = mealType,
                title = recipe.name,
                recipeId = recipe.id,
                plannedKcal = perServing.totalCalories
            )
            showNotice("${recipe.name} planned for ${mealType.label} on ${Dates.prettyDate(date)}")
        }
    }

    /** Log a planned slot into the day's diary and mark it done (spec §17). */
    fun logSlot(slot: PlannedMeal) {
        viewModelScope.launch {
            repo.logPlannedMeal(slot)
            repo.setPlanSlotCompleted(slot.id, completed = true)
            showNotice("\"${slot.title}\" logged to ${Dates.prettyDate(slot.date)}")
        }
    }

    /** Extract ingredients from recipe-backed slots into the grocery list (spec §18). */
    fun sendPlanToGrocery() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val start = weekStart.value
                val end = Dates.addDays(start, 6)
                val recipeSlots = plan.value.count { it.recipeId != null }
                val added = repo.extractGroceryFromPlan(start, end)
                showNotice(
                    when {
                        added > 0 -> "$added ingredient${if (added == 1) "" else "s"} added to grocery list"
                        recipeSlots == 0 ->
                            "No recipes in this week's plan — tap Add on a meal to plan one first"
                        else -> "Nothing new — those ingredients are already on the list"
                    }
                )
            } finally {
                _busy.value = false
            }
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
                PlannerViewModel(repo) as T
        }
    }
}

/** Grocery list (spec §18): persisted, categorized, check-off, clear completed. */
class GroceryViewModel(private val repo: AppRepository) : ViewModel() {

    val items: StateFlow<List<GroceryItem>> = repo.observeGrocery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.addGroceryItem(trimmed) }
    }

    fun setCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { repo.setGroceryCompleted(id, completed) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteGroceryItem(id) }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            val done = items.value.count { it.completed }
            repo.clearCompletedGrocery()
            showNotice(if (done > 0) "Cleared $done item${if (done == 1) "" else "s"}" else "Nothing checked off")
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
                GroceryViewModel(repo) as T
        }
    }
}

/** Nutrition report (spec §20 Nutrition Page: Calories/Macros/Nutrients/Meals/Trends). */
class NutritionViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    val summary: StateFlow<DailySummary?> = todayFlow
        .flatMapLatest { repo.observeSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs: StateFlow<List<FoodLog>> = todayFlow
        .flatMapLatest { repo.observeFoodLogs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<Goals?> = repo.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Micronutrients resolved from the logged foods (spec §9). */
    val nutrients: StateFlow<NutrientTotals> = todayFlow
        .flatMapLatest { day -> repo.observeFoodLogs(day).map { dayLogs -> repo.nutrientTotals(dayLogs) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutrientTotals())

    data class WeeklyReport(
        val avgCalories: Double = 0.0,
        val avgProtein: Double = 0.0,
        val avgCarbs: Double = 0.0,
        val avgFat: Double = 0.0,
        val loggedDays: Int = 0,
        val exerciseDays: Int = 0,
        val weightDeltaKg: Double? = null
    )

    val weekReport: StateFlow<WeeklyReport> = todayFlow
        .map { Dates.lastDays(it, 7) }
        .distinctUntilChanged()
        .flatMapLatest { days ->
            val from = days.first()
            combine(
                repo.observeLogsBetween(from, days.last()),
                repo.observeExerciseLogsBetween(from, days.last()),
                repo.observeWeightLogs()
            ) { food, exercise, weights ->
                weeklyReport(days, food, exercise, weights)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyReport())

    private fun weeklyReport(
        days: List<String>,
        food: List<FoodLog>,
        exercise: List<ExerciseLog>,
        weights: List<WeightLog>
    ): WeeklyReport {
        val n = days.size
        val window = days.first()..days.last()
        val inWindow = weights.filter { it.date in window }.sortedBy { it.date }
        return WeeklyReport(
            avgCalories = food.sumOf { it.calories } / n,
            avgProtein = food.sumOf { it.protein } / n,
            avgCarbs = food.sumOf { it.carbs } / n,
            avgFat = food.sumOf { it.fat } / n,
            loggedDays = days.count { d -> food.any { it.date == d } },
            exerciseDays = days.count { d -> exercise.any { it.date == d } },
            weightDeltaKg = if (inWindow.size >= 2) inWindow.last().weightKg - inWindow.first().weightKg else null
        )
    }

    /** Spec §9: the user chooses whether burned calories widen the budget. */
    fun setExerciseCaloriesInBudget(enabled: Boolean) {
        viewModelScope.launch {
            repo.profileOnce()?.let { repo.saveProfile(it.copy(exerciseCaloriesAddToBudget = enabled)) }
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NutritionViewModel(repo) as T
        }
    }
}

/** Goals & targets (spec §9 macro targets, §21 goals). One atomic save of profile + macro goal. */
class GoalsViewModel(private val repo: AppRepository) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Stored macro target; null means "engine default split". */
    val storedMacros: StateFlow<MacroGoals?> = repo.observeMacroGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Resolved targets (what the rest of the app will use). */
    val goals: StateFlow<Goals?> = repo.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Writes profile and macro goal together. Partial success would leave the
     * two disagreeing, so failure reports honestly instead of pretending.
     */
    fun save(profileUpdate: UserProfile, macros: MacroGoals, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                repo.saveProfile(profileUpdate)
                repo.saveMacroGoals(macros)
                _saving.value = false
                _saved.value = true
                onMain(onDone)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _saving.value = false
                _error.value = "Goals weren't saved. Please try again."
            }
        }
    }

    fun dismissMessages() {
        _saved.value = false
        _error.value = null
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GoalsViewModel(repo) as T
        }
    }
}
/** Profile (spec §22): identity, metrics and daily targets. */
class ProfileViewModel(private val repo: AppRepository) : ViewModel() {

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val goals: StateFlow<com.example.eyirfitnessapp.domain.model.Goals?> = repo.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Rename from the profile screen; the flow pushes the change everywhere. */
    fun rename(newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { repo.renameProfile(clean) }
    }

    /** Path of the copied avatar file, or null to fall back to the initial. */
    fun setAvatar(localPath: String?) {
        viewModelScope.launch { repo.setAvatar(localPath) }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(repo) as T
        }
    }
}

/** Settings (spec §22): preferences, export, delete data, account links. */
class SettingsViewModel(private val repo: AppRepository) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Absolute path of the last successful export, for sharing (spec §22). */
    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    private fun update(transform: (UserProfile) -> UserProfile) {
        profile.value?.let { p -> viewModelScope.launch { repo.saveProfile(transform(p)) } }
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setWeekStartDay(day: WeekStartDay) = update { it.copy(weekStartDay = day) }

    fun setWeightUnit(unit: WeightUnit) = update { it.copy(weightUnit = unit) }

    fun setExerciseCaloriesInBudget(enabled: Boolean) =
        update { it.copy(exerciseCaloriesAddToBudget = enabled) }

    /**
     * Spec §27: reminder toggles. The profile row is written first, then the
     * alarm schedule is rebuilt immediately — otherwise "off" keeps firing and
     * "on" waits for the next app launch.
     */
    fun setNotifMeals(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifMeals = enabled) }

    fun setNotifWater(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifWater = enabled) }

    fun setNotifWeighIn(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifWeighIn = enabled) }

    fun setNotifFasting(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifFasting = enabled) }

    fun setNotifWeeklyReport(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifWeeklyReport = enabled) }

    fun setNotifHabits(enabled: Boolean, context: Context) = updateNotif(context) { it.copy(notifHabits = enabled) }

    private fun updateNotif(context: Context, transform: (UserProfile) -> UserProfile) {
        profile.value?.let { p ->
            viewModelScope.launch {
                repo.saveProfile(transform(p))
                com.example.eyirfitnessapp.notifications.Notifications.rescheduleAll(context)
            }
        }
    }

    /**
     * Spec §22 Account → Export data: writes the JSON snapshot to this app's
     * own external files directory and exposes the path for sharing.
     */
    fun exportData(context: Context) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val json = repo.exportData()
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val stamp = DataExport.isoTimestamp(System.currentTimeMillis())
                    .replace(Regex("[^0-9TZ]"), "")
                val file = File(dir, "eyir-export-$stamp.json")
                file.writeText(json)
                file.absolutePath
            }
            _busy.value = false
            result
                .onSuccess { path ->
                    _exportPath.value = path
                    showNotice("Export saved to $path")
                }
                .onFailure { e ->
                    showNotice("Export failed: ${e.message ?: "unknown error"}")
                }
        }
    }

    /** Re-enter the profile/goal wizard (spec §21) — no data loss. */
    fun restartOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            profile.value?.let { repo.saveProfile(it.copy(onboardingComplete = false)) }
            onMain(onDone)
        }
    }

    /**
     * Spec §22 Account → Delete account: wipes every local table. The screen
     * must confirm first (§34 rule 6); [onDone] runs after the wipe.
     */
    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            repo.wipeAllData()
            _busy.value = false
            onMain(onDone)
        }
    }

    fun showNotice(text: String) {
        viewModelScope.launch {
            _notice.value = text
            kotlinx.coroutines.delay(6_000)
            if (_notice.value == text) _notice.value = null
        }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(repo) as T
        }
    }
}
