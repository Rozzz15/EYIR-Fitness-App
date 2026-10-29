package com.example.eyirfitnessapp.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Single app-wide channel for messages that should reach the user (spec §6
 * Rule 5: every important action needs feedback; spec §34: every operation
 * needs a real error state, and raw backend errors are never shown). Sources
 * post a friendly message; RootApp renders both flows in a snackbar.
 */
object AppMessages {

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = _errors

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val notices: SharedFlow<String> = _notices

    fun error(message: String) {
        _errors.tryEmit(message)
    }

    /** Confirmation for an action that left the current screen (spec §6 Rule 5). */
    fun notice(message: String) {
        _notices.tryEmit(message)
    }
}
