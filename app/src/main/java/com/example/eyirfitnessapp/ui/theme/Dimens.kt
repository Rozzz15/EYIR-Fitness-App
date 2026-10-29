package com.example.eyirfitnessapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Spec §5 radius scale — refined for a modern, elevated card system
object Radii {
    val Small = 10.dp
    val Chip = 12.dp
    val Medium = 14.dp
    val Large = 18.dp
    val Card = 20.dp
    val Hero = 28.dp
}

// Soft, layered elevation instead of hard 1dp boxes
object Elevation {
    val Flat = 0.dp
    val Card = 2.dp
    val Raised = 6.dp
    val Floating = 12.dp
}

// Spec §5 spacing scale (base 4dp)
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

val CardShape = RoundedCornerShape(Radii.Card)
val HeroShape = RoundedCornerShape(Radii.Hero)
val ChipShape = RoundedCornerShape(Radii.Chip)
val PillShape = RoundedCornerShape(999.dp)
