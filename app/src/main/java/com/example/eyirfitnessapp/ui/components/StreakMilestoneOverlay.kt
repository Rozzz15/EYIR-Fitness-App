package com.example.eyirfitnessapp.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyirfitnessapp.ui.theme.Radii
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

private const val CELEBRATION_SECONDS = 2.15f

private val ConfettiColors = listOf(
    Color(0xFF22C55E), // vivid green
    Color(0xFF84CC16), // lime
    Color(0xFFF59E0B), // gold
    Color(0xFFFBBF24), // light gold
    Color(0xFF0EA5E9), // sky
    Color(0xFFF43F5E), // rose
    Color(0xFF8B5CF6), // violet
    Color(0xFFF97316), // orange
    Color(0xFF16A34A)  // deep green
)

private val BadgeTop = Color(0xFF22C55E)
private val BadgeBottom = Color(0xFF15803D)
private val CardTop = Color(0xFF064E3B)
private val CardBottom = Color(0xFF14532D)
private val GoldAccent = Color(0xFFFBBF24)

/**
 * Goal-achievement celebration (spec §12): a heavy checkmark pop with a
 * shockwave ring, a dense confetti burst that falls with gravity, and a
 * high-contrast streak card — ~2.4s total. Draws only; never consumes
 * touches. Honors the system "remove animations" setting with a still frame.
 */
@Composable
fun StreakMilestoneOverlay(
    habitLabel: String,
    days: Int,
    onDismiss: () -> Unit
) {
    val reducedMotion = rememberReducedMotion()

    val badgeScale = remember { Animatable(0f) }
    val checkDraw = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textPop = remember { Animatable(0.82f) }
    val dimIn = remember { Animatable(0f) }
    val exitAlpha = remember { mutableStateOf(1f) }

    LaunchedEffect(days, reducedMotion, habitLabel) {
        if (reducedMotion) {
            badgeScale.snapTo(1f)
            checkDraw.snapTo(1f)
            textAlpha.snapTo(1f)
            textPop.snapTo(1f)
            confetti.snapTo(0.45f)
            dimIn.snapTo(1f)
            delay(2_000)
        } else {
            launch {
                dimIn.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }
            launch {
                badgeScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                )
                // Impact pulse right as the check lands.
                delay(120)
                badgeScale.animateTo(1.14f, tween(90, easing = FastOutSlowInEasing))
                badgeScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            launch {
                delay(80)
                checkDraw.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
            }
            launch {
                delay(160)
                confetti.animateTo(1f, tween((CELEBRATION_SECONDS * 1000).toInt(), easing = LinearEasing))
            }
            launch {
                delay(500)
                textAlpha.animateTo(1f, tween(180, easing = LinearEasing))
                textPop.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            delay(2_000)
            launch { textAlpha.animateTo(0f, tween(170, easing = LinearEasing)) }
            delay(200)
        }
        launch {
            if (reducedMotion) exitAlpha.value = 0f
            else {
                val fade = Animatable(1f)
                fade.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                exitAlpha.value = fade.value
            }
        }
        delay(210)
        onDismiss()
    }

    // No pointerInput / clickable anywhere — touches fall through to the app.
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        // Dim + vignette: draws only, so the screen stays tappable underneath.
        Canvas(Modifier.fillMaxSize()) {
            val d = dimIn.value
            if (d <= 0f) return@Canvas
            drawRect(color = Color.Black.copy(alpha = 0.52f * d))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f * d)),
                    center = center,
                    radius = size.maxDimension * 0.72f
                )
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            drawShockwave(progress = confetti.value)
            drawConfettiBurst(progress = confetti.value)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .shadow(28.dp, RoundedCornerShape(50), clip = false)
                    .size(132.dp)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = center
                    // Outer soft glow.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(BadgeTop.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        radius = r * 1.35f
                    )
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(BadgeTop, BadgeBottom),
                            start = Offset(c.x - r, c.y - r),
                            end = Offset(c.x + r, c.y + r)
                        ),
                        radius = r
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = r - 3.dp.toPx(),
                        style = Stroke(2.5.dp.toPx())
                    )
                    if (checkDraw.value > 0f) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.28f, h * 0.52f)
                            lineTo(w * 0.44f, h * 0.67f)
                            lineTo(w * 0.74f, h * 0.35f)
                        }
                        val measure = PathMeasure().apply { setPath(path, false) }
                        val drawn = Path()
                        measure.getSegment(0f, measure.length * checkDraw.value, drawn, true)
                        drawPath(
                            drawn,
                            Color.White,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // High-contrast streak card — never blends into the screen below.
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = textAlpha.value
                        scaleX = textPop.value
                        scaleY = textPop.value
                    }
                    .shadow(16.dp, RoundedCornerShape(Radii.Large))
                    .clip(RoundedCornerShape(Radii.Large))
                    .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(GoldAccent, Color(0xFFF59E0B))),
                        RoundedCornerShape(Radii.Large)
                    )
                    .padding(horizontal = 26.dp, vertical = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$days-DAY STREAK!",
                        style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Keep it going.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = GoldAccent
                    )
                }
            }
        }
    }
}

/** Expanding ring fired the instant the check lands. */
private fun DrawScope.drawShockwave(progress: Float) {
    if (progress <= 0f) return
    val wave = ((progress - 0.05f) / 0.22f).coerceIn(0f, 1f)
    if (wave <= 0f || wave >= 1f) return
    val cx = size.width / 2f
    val cy = size.height * 0.42f
    val maxR = size.minDimension * 0.42f
    drawCircle(
        color = BadgeTop.copy(alpha = (1f - wave) * 0.45f),
        radius = 40.dp.toPx() + wave * maxR,
        center = Offset(cx, cy),
        style = Stroke(width = (3.5f - 2.5f * wave).dp.toPx())
    )
}

/**
 * Dense radial confetti burst that decelerates horizontally and accelerates
 * downward under gravity, spinning as it falls and fading near the end.
 */
private fun DrawScope.drawConfettiBurst(progress: Float) {
    if (progress <= 0f) return
    val t = progress * CELEBRATION_SECONDS
    val cx = size.width / 2f
    val cy = size.height * 0.42f
    val d = density
    val gravity = 1050f * d
    val drag = 1.05f

    repeat(54) { i ->
        val delayS = noise(i * 3 + 1) * 0.26f
        if (t <= delayS) return@repeat
        val te = t - delayS

        val angle = noise(i * 5 + 2) * (2.0 * Math.PI).toFloat()
        val speed = (300f + noise(i * 7 + 3) * 520f) * d
        val decay = (1f - exp(-drag * te)) / drag

        val x = cx + cos(angle) * speed * decay
        val y = cy + sin(angle) * speed * decay * 0.6f + 0.5f * gravity * te * te

        val life = (CELEBRATION_SECONDS - te) / 0.65f
        val alpha = life.coerceIn(0f, 1f)
        if (alpha <= 0f) return@repeat

        val spin = (noise(i * 11 + 4) - 0.5f) * 1100f
        val degrees = spin * te
        val big = i % 5 == 0
        val w = (if (big) 8f else 5.5f + noise(i * 13 + 5) * 4f) * d
        val h = if (i % 4 == 0) w else (if (big) 14f else 10f + noise(i * 17 + 6) * 8f) * d
        val color = ConfettiColors[i % ConfettiColors.size]

        rotate(degrees = degrees, pivot = Offset(x, y)) {
            drawRect(
                color = color,
                topLeft = Offset(x - w / 2f, y - h / 2f),
                size = Size(w, h),
                alpha = alpha
            )
        }
    }
}

/** Deterministic pseudo-random in 0..1 from an integer seed. */
private fun noise(seed: Int): Float {
    val x = sin(seed * 127.1f + 311.7f) * 43758.5453f
    return x - floor(x)
}

/**
 * True when the user (or their accessibility setup) has asked the system to
 * remove animations — respected by showing a still frame instead of motion.
 */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val resolver = context.contentResolver
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
                Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
