package com.example.eyirfitnessapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyirfitnessapp.ui.theme.CardShape
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.extended

/**
 * Equal-width segmented control that stays usable on any screen width.
 * Each segment gets a fair share of the row (weight 1f) so labels never
 * overflow; long labels ellipsize instead of pushing siblings off-screen.
 * Used for Progress ranges and Nutrition tabs.
 */
@Composable
fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp),
                    shape = PillShape,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    border = if (selected) null
                    else BorderStroke(1.dp, Color.Transparent)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            // Slightly tighter type so five segments still read on small phones.
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Circular progress ring used for calories, water and macro goals (spec §20). */
@Composable
fun CalorieRing(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    ringSize: androidx.compose.ui.unit.Dp = 168.dp,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    gradient: List<Color>? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val clamped = progress.coerceIn(0f, 1f)
    val brush = Brush.sweepGradient(
        gradient?.let { it + it.first() } ?: listOf(strokeColor, strokeColor)
    )
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = brush,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
    }
}

/** Horizontal macro progress card: consumed / goal (spec §9), tinted by macro color. */
@Composable
fun MacroCard(
    label: String,
    consumed: Double,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed / goal).toFloat().coerceIn(0f, 1f) else 0f
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = color.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${consumed.round1()} / $goal g",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress = progress, color = color)
        }
    }
}

@Composable
fun ProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(6.dp)) {
        val radius = size.height / 2
        val clamped = progress.coerceIn(0f, 1f)
        drawRoundRect(
            color = color.copy(alpha = 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        if (clamped > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(color, color.copy(alpha = 0.70f)),
                    endX = size.width
                ),
                size = androidx.compose.ui.geometry.Size(
                    (size.width * clamped).coerceAtLeast(radius * 2),
                    size.height
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}

/** Water progress bar with goal display (spec §12). */
@Composable
fun WaterProgressBar(
    consumedMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goalMl > 0) consumedMl.toFloat() / goalMl else 0f
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${"%.1f".format(consumedMl / 1000.0)}L / ${"%.1f".format(goalMl / 1000.0)}L",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extended.water
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(progress = progress.coerceIn(0f, 1f), color = MaterialTheme.extended.water)
    }
}

private fun Double.round1(): String = "%.1f".format(this)
