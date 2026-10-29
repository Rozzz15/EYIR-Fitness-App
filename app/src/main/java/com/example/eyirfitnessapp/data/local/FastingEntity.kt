package com.example.eyirfitnessapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_sessions")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val targetHours: Int,
    val status: String // ACTIVE, COMPLETED, ABANDONED
)
