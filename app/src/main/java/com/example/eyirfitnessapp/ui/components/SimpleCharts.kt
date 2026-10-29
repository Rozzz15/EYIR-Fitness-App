package com.example.eyirfitnessapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Simple bar chart for weekly calories (spec §20 weekly analytics). */
@Composable
fun WeeklyBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    goalLine: Double? = null
) {
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val max = maxOf(data.maxOfOrNull { it.second } ?: 0.0, goalLine ?: 0.0, 1.0)

    Column(modifier.fillMaxWidth()) {
        // TalkBack reads a spoken summary instead of an image (spec §31 a11y).
        val summary = buildString {
            append("Weekly calorie chart. ")
            append(
                data.joinToString(", ") { (label, value) ->
                    "$label ${value.roundToInt()} calories"
                }
            )
            goalLine?.let { append(". Goal line at ${it.roundToInt()} calories") }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = summary }
        ) {
            val goalPx = if (goalLine != null) 8.dp.toPx() else 0f
            val barArea = size.height - goalPx
            val barWidth = size.width / (data.size * 2)

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, barArea),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            data.forEachIndexed { i, (_, value) ->
                val h = (value / max * barArea).toFloat().coerceAtMost(barArea)
                if (h > 0f) {
                    val x = barWidth * (2 * i + 0.5f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.55f)),
                            startY = barArea - h,
                            endY = barArea
                        ),
                        topLeft = Offset(x = x, y = barArea - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }

            goalLine?.let { goal ->
                val y = barArea - (goal / max * barArea).toFloat()
                drawLine(
                    color = goalColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        // Long ranges (1 year / all time) have many buckets — show every Nth
        // label plus the last one so text never collides on a phone width.
        val labelStep = when {
            data.size <= 8 -> 1
            data.size <= 16 -> 2
            else -> (data.size + 5) / 6
        }
        Row(Modifier.fillMaxWidth()) {
            data.forEachIndexed { i, (label, _) ->
                val isLast = i == data.lastIndex
                val showLabel = i % labelStep == 0 || isLast
                Text(
                    if (showLabel) label else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Simple line chart for weight trend (spec §11 progress chart). */
@Composable
fun TrendLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    unitLabel: String = ""
) {
    val lineColor = MaterialTheme.colorScheme.primary

    val summary = when {
        values.isEmpty() -> "Trend chart, no data yet"
        values.size == 1 -> "Trend chart, single point ${fmtTrend(values[0])} $unitLabel".trim()
        else -> {
            val delta = values.last() - values.first()
            val trend = when {
                delta > 0.05 -> "up"
                delta < -0.05 -> "down"
                else -> "steady"
            }
            "Trend chart, ${values.size} points. From ${fmtTrend(values.first())} " +
                "to ${fmtTrend(values.last())} $unitLabel, trending $trend"
                .trim()
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = summary }
    ) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0

        fun point(i: Int): Offset {
            val x = size.width * i / (values.size - 1)
            val normalized = ((values[i] - min) / range).toFloat()
            val y = size.height * 0.9f - normalized * size.height * 0.8f
            return Offset(x, y)
        }

        val path = Path()
        values.indices.forEach { i ->
            val p = point(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }

        val area = Path()
        area.addPath(path)
        area.lineTo(point(values.size - 1).x, size.height)
        area.lineTo(point(0).x, size.height)
        area.close()
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f))
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        values.indices.forEach { i ->
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = point(i))
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = point(i))
        }
    }
}

/** 72 or 72.4 — no trailing ".0" in a spoken summary. */
private fun fmtTrend(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
