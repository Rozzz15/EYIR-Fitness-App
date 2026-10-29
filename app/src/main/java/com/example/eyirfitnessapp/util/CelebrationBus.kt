package com.example.eyirfitnessapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One pending celebration, hosted by the root scaffold so the whole app can blur behind it. */
data class Celebration(val habitLabel: String, val days: Int)

/**
 * App-wide one-shot channel for achievement popups (spec §12 milestones).
 * Screens/view models post; [com.example.eyirfitnessapp.MainActivity] renders
 * and clears — keeping the blur + overlay in a single place.
 */
object CelebrationBus {
    private val _active = MutableStateFlow<Celebration?>(null)
    val active: StateFlow<Celebration?> = _active.asStateFlow()

    fun show(habitLabel: String, days: Int) {
        _active.value = Celebration(habitLabel, days)
    }

    fun clear() {
        _active.value = null
    }
}
