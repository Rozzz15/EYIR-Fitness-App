package com.example.eyirfitnessapp.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.BodyMeasurement
import com.example.eyirfitnessapp.domain.model.MeasurementPart
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.MetricCard
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.TrendLineChart
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.theme.extended
import com.example.eyirfitnessapp.ui.viewmodels.MeasurementsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Body measurements (spec §11): an interactive body map picks the site, the
 * selected site drives the entry form and its trend chart, latest readings
 * show the change against the previous log, and the full history sits below.
 */
@Composable
fun MeasurementsScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: MeasurementsViewModel = viewModel(factory = MeasurementsViewModel.factory(repository))
    val measurements by viewModel.measurements.collectAsState()
    val notice by viewModel.notice.collectAsState()

    var selectedPart by remember { mutableStateOf(MeasurementPart.WAIST) }
    var valueText by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    // Oldest → newest, so "latest" is the last entry and deltas read forward.
    val historyByPart = remember(measurements) {
        measurements
            .sortedWith(compareBy({ it.date }, { it.loggedAt }))
            .groupBy { it.part }
    }
    val latest = remember(historyByPart) { historyByPart.mapValues { (_, list) -> list.last() } }
    val lastLog = measurements.maxByOrNull { it.loggedAt }

    /** Tapping a body site or chip selects it and seeds the field with its last value. */
    fun selectPart(part: MeasurementPart) {
        if (part == selectedPart) return
        selectedPart = part
        valueText = historyByPart[part]?.lastOrNull()?.let { "%.1f".format(it.centimeters) } ?: ""
    }

    // Single LazyColumn: a LazyColumn nested inside a verticalScroll Column is
    // measured with infinite height and throws on first composition.
    // Top inset lives inside the sticky header (not contentPadding) so the
    // title doesn't jump up when the header pins.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Pinned so the back button survives scrolling to the form/history.
        // Horizontal inset comes from contentPadding, so only the background
        // is full-bleed within the content box to hide items sliding under.
        stickyHeader(key = "header") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = Spacing.xl)
            ) {
                ScreenHeader(title = "Measurements", onBack = onBack)
                Spacer(Modifier.height(Spacing.lg))
            }
        }
        item {
            if (notice != null) {
                Text(notice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MetricCard(
                    "Sites",
                    "${latest.size}/${MeasurementPart.entries.size}",
                    Modifier.weight(1f),
                    sub = "tracked"
                )
                MetricCard(
                    "Entries",
                    "${measurements.size}",
                    Modifier.weight(1f),
                    sub = "logged all-time"
                )
            }
        }
        item {
            AppCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Body map", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            lastLog?.let {
                                "Last updated " + SimpleDateFormat("MMM d", Locale.US).format(Date(it.loggedAt))
                            } ?: "Tap a site to start tracking it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${latest.size}/${MeasurementPart.entries.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                CoverageBar(progress = latest.size.toFloat() / MeasurementPart.entries.size)
                Spacer(Modifier.height(Spacing.md))
                BodyMap(selectedPart = selectedPart, latest = latest, onSelect = ::selectPart)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Highlighted site feeds the form and trend below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            AppCard {
                Text("New measurement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.md))
                Text("Body part", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MeasurementPart.entries.take(3).forEach { part ->
                        FilterChipPart(part, selectedPart == part) { selectPart(it) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MeasurementPart.entries.drop(3).forEach { part ->
                        FilterChipPart(part, selectedPart == part) { selectPart(it) }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Size (cm) — ${selectedPart.label}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton(
                    text = "Save measurement",
                    onClick = {
                        viewModel.add(selectedPart, valueText)
                        valueText = ""
                    },
                    enabled = (valueText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            AppCard {
                Text(
                    "Trend — ${selectedPart.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.md))
                val entries = historyByPart[selectedPart].orEmpty()
                when {
                    entries.size >= 2 -> {
                        val change = entries.last().centimeters - entries.first().centimeters
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "First %.1f cm".format(entries.first().centimeters),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DeltaPill(selectedPart, change)
                            Text(
                                "Latest %.1f cm".format(entries.last().centimeters),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        TrendLineChart(values = entries.map { it.centimeters }, unitLabel = "cm")
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "${entries.size} logs · ${shortDate(entries.first().loggedAt)} → ${shortDate(entries.last().loggedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    entries.size == 1 -> Text(
                        "Log ${selectedPart.label} on another day to see its trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Text(
                        "No ${selectedPart.label} readings yet — tap the site on the body map and save one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("Latest", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
        }
        if (latest.isEmpty()) {
            item {
                EmptyState(
                    title = "No measurements yet",
                    subtitle = "Track waist, chest, hips and more to see body changes the scale can't show.",
                    icon = Icons.Outlined.SquareFoot
                )
            }
        } else {
            items(MeasurementPart.entries.filter { latest.containsKey(it) }, key = { "part_${it.name}" }) { part ->
                val entries = historyByPart[part].orEmpty()
                val m = entries.last()
                val delta = if (entries.size >= 2) m.centimeters - entries[entries.lastIndex - 1].centimeters else null
                AppCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(part.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (entries.size >= 2) "${shortDate(m.loggedAt)} · vs previous"
                                else "${shortDate(m.loggedAt)} · first reading",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "%.1f cm".format(m.centimeters),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (delta != null) {
                                Spacer(Modifier.height(Spacing.xs))
                                DeltaPill(part, delta)
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
        }
        if (measurements.isEmpty()) {
            item {
                Text(
                    "Saved measurements will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(measurements.asReversed(), key = { it.id }) { m ->
            HistoryRow(m) { pendingDelete = m.id }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this measurement?") },
            text = { Text("This removes the entry from your history. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Body map ─────────────────────────────────────────────────────────────

/** Callout positions, dp from the body centre, for every site on the map. */
private val markerOffsets: Map<MeasurementPart, Pair<Dp, Dp>> = mapOf(
    MeasurementPart.NECK to (86.dp to (-92).dp),
    MeasurementPart.CHEST to (88.dp to (-44).dp),
    MeasurementPart.WAIST to ((-90).dp to (-6).dp),
    MeasurementPart.HIPS to (88.dp to 44.dp),
    MeasurementPart.ARMS to ((-90).dp to (-60).dp),
    MeasurementPart.THIGHS to ((-90).dp to 88.dp)
)

/** Where each site sits on the drawn figure, dp from the body centre. */
private val anchorOffsets: Map<MeasurementPart, Pair<Dp, Dp>> = mapOf(
    MeasurementPart.NECK to (0.dp to (-92).dp),
    MeasurementPart.CHEST to (0.dp to (-60).dp),
    MeasurementPart.WAIST to (0.dp to 4.dp),
    MeasurementPart.HIPS to (26.dp to 36.dp),
    MeasurementPart.ARMS to ((-52).dp to (-30).dp),
    MeasurementPart.THIGHS to ((-22).dp to 78.dp)
)

/**
 * Stylised front-facing figure with a tappable callout per measurement site.
 * Untracked sites show "—"; the selected site is highlighted.
 */
@Composable
private fun BodyMap(
    selectedPart: MeasurementPart,
    latest: Map<MeasurementPart, BodyMeasurement>,
    onSelect: (MeasurementPart) -> Unit
) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val guide = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val active = MaterialTheme.colorScheme.primary
    val untracked = MaterialTheme.colorScheme.outlineVariant

    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            fun at(dx: Float, dy: Float) = Offset(cx + dx.dp.toPx(), cy + dy.dp.toPx())
            fun strokeW(dpValue: Float) = dpValue.dp.toPx()

            // Limbs first — the torso fill covers their joints.
            listOf(
                Triple(-20f, 44f, -24f), Triple(20f, 44f, 24f)
            ).forEach { (sx, sy, ex) ->
                drawLine(
                    color = fill,
                    start = at(sx, sy),
                    end = at(ex, 124f),
                    strokeWidth = strokeW(26f),
                    cap = StrokeCap.Round
                )
            }
            listOf(-1, 1).forEach { side ->
                drawLine(
                    color = fill,
                    start = at(44f * side, -84f),
                    end = at(60f * side, 26f),
                    strokeWidth = strokeW(18f),
                    cap = StrokeCap.Round
                )
            }

            // Torso: sampled quadratic curves keep it smooth on any Compose version.
            val torso = Path()
            fun curve(from: Offset, control: Offset, to: Offset, steps: Int = 14) {
                for (i in 1..steps) {
                    val t = i.toFloat() / steps
                    val mt = 1f - t
                    val x = mt * mt * from.x + 2f * mt * t * control.x + t * t * to.x
                    val y = mt * mt * from.y + 2f * mt * t * control.y + t * t * to.y
                    torso.lineTo(x, y)
                }
            }
            val neckL = at(-10f, -98f)
            torso.moveTo(neckL.x, neckL.y)
            val shoulderL = at(-46f, -94f)
            curve(neckL, at(-30f, -97f), shoulderL)
            val armpitL = at(-44f, -66f)
            curve(shoulderL, at(-56f, -84f), armpitL)
            val waistL = at(-28f, 6f)
            curve(armpitL, at(-46f, -32f), waistL)
            val hipL = at(-44f, 40f)
            curve(waistL, at(-32f, 24f), hipL)
            val crotch = at(0f, 52f)
            curve(hipL, at(-26f, 52f), crotch)
            val hipR = at(44f, 40f)
            curve(crotch, at(26f, 52f), hipR)
            val waistR = at(28f, 6f)
            curve(hipR, at(32f, 24f), waistR)
            val armpitR = at(44f, -66f)
            curve(waistR, at(46f, -32f), armpitR)
            val shoulderR = at(46f, -94f)
            curve(armpitR, at(56f, -84f), shoulderR)
            val neckR = at(10f, -98f)
            curve(shoulderR, at(30f, -97f), neckR)
            curve(neckR, at(0f, -88f), neckL)
            torso.close()
            drawPath(torso, color = fill)
            drawPath(torso, color = outline, style = Stroke(width = strokeW(2f)))

            // Neck + head sit above the torso stroke, with a visible gap at the throat.
            drawLine(fill, at(0f, -106f), at(0f, -92f), strokeWidth = strokeW(14f), cap = StrokeCap.Round)
            val head = at(0f, -124f)
            drawCircle(color = fill, radius = strokeW(21f), center = head)
            drawCircle(color = outline, radius = strokeW(21f), center = head, style = Stroke(width = strokeW(2f)))

            // Anchor dot + connector line out to each callout.
            MeasurementPart.entries.forEach { part ->
                val anchor = anchorOffsets.getValue(part)
                val marker = markerOffsets.getValue(part)
                val from = at(anchor.first.value, anchor.second.value)
                val to = at(marker.first.value, marker.second.value)
                drawLine(color = guide, start = from, end = to, strokeWidth = strokeW(1.5f))
                if (part == selectedPart) {
                    drawCircle(color = active.copy(alpha = 0.18f), radius = strokeW(10f), center = from)
                }
                if (latest.containsKey(part)) {
                    drawCircle(color = active, radius = strokeW(4.5f), center = from)
                } else {
                    drawCircle(
                        color = untracked,
                        radius = strokeW(4f),
                        center = from,
                        style = Stroke(width = strokeW(2f))
                    )
                }
            }
        }

        MeasurementPart.entries.forEach { part ->
            BodyMarker(
                part = part,
                measurement = latest[part],
                selected = part == selectedPart,
                offset = markerOffsets.getValue(part),
                onClick = { onSelect(part) }
            )
        }
    }
}

/** Value + label callout floating beside the figure; tapping selects the site. */
@Composable
private fun BodyMarker(
    part: MeasurementPart,
    measurement: BodyMeasurement?,
    selected: Boolean,
    offset: Pair<Dp, Dp>,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val desc = "${part.label}, " + (measurement?.let { "%.1f centimeters".format(it.centimeters) } ?: "not measured yet")
    Surface(
        onClick = onClick,
        modifier = Modifier
            .offset(x = offset.first, y = offset.second)
            .width(72.dp)
            .semantics { contentDescription = desc },
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) primary else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                measurement?.let { "%.1f".format(it.centimeters) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                part.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ── Change indicator ─────────────────────────────────────────────────────

/** Neutral track + primary fill, so an empty bar never reads as 100 %. */
@Composable
private fun CoverageBar(progress: Float, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val radius = size.height / 2
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(radius, radius)
        )
        if (progress > 0f) {
            drawRoundRect(
                color = fill,
                size = Size((size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(radius * 2), size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

/** Waist/hips shrinking reads as progress; the other sites growing does. */
private fun favorableDelta(part: MeasurementPart, delta: Double): Boolean =
    if (delta == 0.0) true
    else if (part == MeasurementPart.WAIST || part == MeasurementPart.HIPS) delta < 0
    else delta > 0

/** Compact "▲ +0.8 cm" pill, green when the change is in a favorable direction. */
@Composable
private fun DeltaPill(part: MeasurementPart, delta: Double, modifier: Modifier = Modifier) {
    val color = when {
        delta == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        favorableDelta(part, delta) -> MaterialTheme.extended.success
        else -> MaterialTheme.extended.warning
    }
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (delta != 0.0) {
                Icon(
                    if (delta > 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
            }
            Text(
                "%+.1f cm".format(delta),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun shortDate(loggedAt: Long): String =
    SimpleDateFormat("MMM d", Locale.US).format(Date(loggedAt))

@Composable
private fun FilterChipPart(part: MeasurementPart, selected: Boolean, onSelect: (MeasurementPart) -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = { onSelect(part) },
        label = { Text(part.label) }
    )
}

@Composable
private fun HistoryRow(m: BodyMeasurement, onDelete: () -> Unit) {
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${m.part.label} — %.1f cm".format(m.centimeters), style = MaterialTheme.typography.bodyMedium)
                Text(
                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(m.loggedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${m.part.label} measurement",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
