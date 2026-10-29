@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.eyirfitnessapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.habits.HabitLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.eyirfitnessapp.util.Dates

/** Splash gate + routing decision (spec §21), derived from one DB read. */
data class StartState(val ready: Boolean = false, val onboarded: Boolean = false)

/** Today's habit check-off count, shown on the dashboard (spec §12). */
data class HabitProgress(val done: Int, val total: Int) {
    val allDone: Boolean get() = total > 0 && done == total
}

class MainViewModel(private val repo: AppRepository) : ViewModel() {

    val start: StateFlow<StartState> = repo.observeProfile()
        .map { profile -> StartState(ready = true, onboarded = profile?.onboardingComplete == true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartState())

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(repo) as T
        }
    }
}

class TodayViewModel(private val repo: AppRepository) : ViewModel() {

    private val todayFlow = liveToday(repo, viewModelScope)

    val summary = todayFlow
        .flatMapLatest { day -> repo.observeSummary(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs = todayFlow
        .flatMapLatest { day -> repo.observeFoodLogs(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseLogs = todayFlow
        .flatMapLatest { day -> repo.observeExerciseLogs(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Streak: consecutive days (ending today) with ≥1 food log.
     * Recomputes whenever today's logs change or the day rolls over (previously a one-shot init).
     */
    val streak: StateFlow<Int> = combine(todayFlow, logs) { day, todayLogs -> day to todayLogs }
        .distinctUntilChanged()
        .mapLatest { (day, _) -> computeStreak(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private suspend fun computeStreak(today: String): Int {
        var streak = 0
        var cursor = today
        // Today counts if already logged; otherwise start from yesterday.
        if (repo.logsForDate(cursor).isEmpty()) cursor = Dates.addDays(cursor, -1)
        while (repo.logsForDate(cursor).isNotEmpty()) {
            streak++
            cursor = Dates.addDays(cursor, -1)
        }
        return streak
    }

    /** Habits checked off today vs. how many the user tracks (spec §12). */
    val habitProgress: StateFlow<HabitProgress> = todayFlow
        .flatMapLatest { day ->
            repo.observeHabits().map { habits ->
                HabitProgress(
                    done = habits.count { HabitLogic.isDoneToday(it, day) },
                    total = habits.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitProgress(0, 0))

    fun addWaterQuick(amountMl: Int) {
        viewModelScope.launch { repo.addWater(amountMl) }
    }

    fun deleteExerciseLog(id: Long) {
        viewModelScope.launch { repo.deleteExerciseLog(id) }
    }

    fun deleteFoodLog(id: Long) {
        viewModelScope.launch { repo.deleteFoodLog(id) }
    }

    companion object {
        fun factory(repo: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodayViewModel(repo) as T
        }
    }
}

