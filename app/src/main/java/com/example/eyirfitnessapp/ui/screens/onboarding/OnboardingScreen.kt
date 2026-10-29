@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.eyirfitnessapp.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.components.tintedSurface
import com.example.eyirfitnessapp.ui.theme.BrandGradient
import com.example.eyirfitnessapp.ui.theme.CardShape
import com.example.eyirfitnessapp.ui.theme.Elevation
import com.example.eyirfitnessapp.ui.theme.HeroShape
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Radii
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.theme.extended
import com.example.eyirfitnessapp.ui.viewmodels.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STEP_COUNT = 5

/**
 * Onboarding wizard (spec §21), restyled on the shared design system:
 * gradient hero for the welcome, card-based inputs, branded selectors and
 * sliders, and an animated results card — same 5 steps and save logic as
 * before (spec §34 UI rules: no ad-hoc colors or radii).
 */
@Composable
fun OnboardingScreen(
    repository: AppRepository,
    onComplete: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(repository))
    val existing by viewModel.profile.collectAsState()
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var age by remember { mutableFloatStateOf(30f) }
    var heightCm by remember { mutableFloatStateOf(175f) }
    var weightKg by remember { mutableFloatStateOf(75f) }
    var goalWeightKg by remember { mutableFloatStateOf(70f) }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var goalType by remember { mutableStateOf(GoalType.MAINTAIN) }
    var weeklyGoalKg by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(Unit) {
        // Prefill defaults from an existing profile if present
        existing?.let {
            name = it.name
            sex = it.sex
            age = it.age.toFloat()
            heightCm = it.heightCm.toFloat()
            weightKg = it.currentWeightKg.toFloat()
            goalWeightKg = it.goalWeightKg.toFloat()
            activity = it.activityLevel
            goalType = it.goalType
            weeklyGoalKg = it.weeklyGoalKg.toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl)
    ) {
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(Spacing.lg))
            StepHeader(step)
            Spacer(Modifier.height(Spacing.xl))
            StepTitle(step)
            Spacer(Modifier.height(Spacing.xl))

            AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 }) togetherWith
                        fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 24 }
                } else {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 24 }) togetherWith
                        fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 24 }
                }
            },
            label = "onboarding-step"
        ) { current ->
            Column {
                when (current) {
                    0 -> WelcomeStep()
                    1 -> AboutYouStep(
                        name = name,
                        onNameChange = { name = it },
                        sex = sex,
                        onSexChange = { sex = it },
                        age = age,
                        onAgeChange = { age = it }
                    )
                    2 -> BodyMetricsStep(
                        heightCm = heightCm,
                        onHeightChange = { heightCm = it },
                        weightKg = weightKg,
                        onWeightChange = { weightKg = it },
                        goalWeightKg = goalWeightKg,
                        onGoalWeightChange = { goalWeightKg = it }
                    )
                    3 -> GoalStep(
                        goalType = goalType,
                        onGoalTypeChange = { goalType = it },
                        activity = activity,
                        onActivityChange = { activity = it },
                        weeklyGoalKg = weeklyGoalKg,
                        onWeeklyGoalChange = { weeklyGoalKg = it }
                    )
                    else -> PlanStep(
                        viewModel = viewModel,
                        profile = previewProfile(
                            name = name, sex = sex, age = age, heightCm = heightCm,
                            weightKg = weightKg, goalWeightKg = goalWeightKg,
                            activity = activity, goalType = goalType, weeklyGoalKg = weeklyGoalKg
                        )
                    )
                }
            }
        }

        }

        Spacer(Modifier.height(Spacing.xl))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (step > 0) {
                SecondaryButton(text = "Back", onClick = { step-- }, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                text = if (step < STEP_COUNT - 1) "Continue" else "Start tracking",
                onClick = {
                    if (step < STEP_COUNT - 1) {
                        step++
                    } else {
                        scope.launch {
                            viewModel.complete(
                                previewProfile(
                                    name = name, sex = sex, age = age, heightCm = heightCm,
                                    weightKg = weightKg, goalWeightKg = goalWeightKg,
                                    activity = activity, goalType = goalType, weeklyGoalKg = weeklyGoalKg
                                )
                            )
                            // Room resumes on an IO thread — navigation must be main-thread.
                            withContext(Dispatchers.Main.immediate) { onComplete() }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

/** Build the UserProfile from the current wizard state — used for preview and final save. */
private fun previewProfile(
    name: String,
    sex: Sex,
    age: Float,
    heightCm: Float,
    weightKg: Float,
    goalWeightKg: Float,
    activity: ActivityLevel,
    goalType: GoalType,
    weeklyGoalKg: Float
): UserProfile = UserProfile(
    name = name.ifBlank { "Friend" },
    sex = sex,
    age = age.toInt(),
    heightCm = heightCm.toDouble(),
    currentWeightKg = weightKg.toDouble(),
    goalWeightKg = goalWeightKg.toDouble(),
    activityLevel = activity,
    goalType = goalType,
    weeklyGoalKg = weeklyGoalKg.toDouble()
)

// ── Chrome ──────────────────────────────────────────────────────────────────

/** Segmented "Step N of 5" progress bar. */
@Composable
private fun StepHeader(step: Int) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            repeat(STEP_COUNT) { i ->
                val done = i < step
                val active = i == step
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(PillShape)
                        .background(
                            when {
                                done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Step ${step + 1} of $STEP_COUNT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/** Gradient hero for step 0 only; plain large title on later steps. */
@Composable
private fun StepTitle(step: Int) {
    if (step == 0) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(BrandGradient), HeroShape)
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .shadow(Elevation.Raised, CircleShape)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MonitorWeight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                // Small badges echoing the app's trackers (water, meals)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .background(MaterialTheme.extended.warning, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.RestaurantMenu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                "Welcome!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Track calories, macros, water, exercise and weight — all offline-first.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Text(
            when (step) {
                1 -> "About you"
                2 -> "Your body metrics"
                3 -> "Your goal"
                else -> "Your plan"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Steps ───────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(
            Icons.Outlined.RestaurantMenu to "Log meals in seconds — search, history or quick-add",
            Icons.Outlined.WaterDrop to "Water, exercise and measurements on one Today hub",
            Icons.AutoMirrored.Outlined.TrendingUp to "Progress charts and streaks that keep you honest"
        ).forEach { (icon, line) ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutYouStep(
    name: String,
    onNameChange: (String) -> Unit,
    sex: Sex,
    onSexChange: (Sex) -> Unit,
    age: Float,
    onAgeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your name") },
            shape = RoundedCornerShape(Radii.Medium),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column {
            Text("Sex", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Sex.entries.forEach { s ->
                    FilterChip(
                        selected = sex == s,
                        onClick = { onSexChange(s) },
                        label = { Text(s.label) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Column {
            Text(
                "Age: ${age.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LabeledSlider(
                value = age,
                onValueChange = onAgeChange,
                valueRange = 14f..90f,
                minLabel = "14",
                maxLabel = "90"
            )
        }
    }
}

@Composable
private fun BodyMetricsStep(
    heightCm: Float,
    onHeightChange: (Float) -> Unit,
    weightKg: Float,
    onWeightChange: (Float) -> Unit,
    goalWeightKg: Float,
    onGoalWeightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Column {
            Text(
                "Height: ${heightCm.toInt()} cm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LabeledSlider(
                value = heightCm,
                onValueChange = onHeightChange,
                valueRange = 130f..220f,
                minLabel = "130",
                maxLabel = "220"
            )
        }
        Column {
            Text(
                "Weight: ${"%.0f".format(weightKg)} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LabeledSlider(
                value = weightKg,
                onValueChange = onWeightChange,
                valueRange = 35f..200f,
                minLabel = "35",
                maxLabel = "200"
            )
        }
        Column {
            Text(
                "Goal weight: ${"%.0f".format(goalWeightKg)} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LabeledSlider(
                value = goalWeightKg,
                onValueChange = onGoalWeightChange,
                valueRange = 35f..200f,
                minLabel = "35",
                maxLabel = "200"
            )
        }
    }
}

@Composable
private fun GoalStep(
    goalType: GoalType,
    onGoalTypeChange: (GoalType) -> Unit,
    activity: ActivityLevel,
    onActivityChange: (ActivityLevel) -> Unit,
    weeklyGoalKg: Float,
    onWeeklyGoalChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Column {
            Text("Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GoalType.entries.forEach { g ->
                    FilterChip(
                        selected = goalType == g,
                        onClick = { onGoalTypeChange(g) },
                        label = { Text(g.label) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Column {
            Text("Activity level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActivityLevel.entries.forEach { level ->
                    val selected = activity == level
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(
                                if (selected) tintedSurface(MaterialTheme.colorScheme.primaryContainer)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CardShape
                            )
                            .clickable { onActivityChange(level) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            level.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = goalType.hasWeeklyPace) {
            Column {
                Text(
                    "Weekly goal: ${"%.2f".format(weeklyGoalKg)} kg/week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LabeledSlider(
                    value = weeklyGoalKg,
                    onValueChange = onWeeklyGoalChange,
                    valueRange = 0.1f..1.0f,
                    minLabel = "0.1",
                    maxLabel = "1.0"
                )
            }
        }
    }
}

@Composable
private fun PlanStep(
    viewModel: OnboardingViewModel,
    profile: UserProfile
) {
    val goals = viewModel.computedGoals(profile)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            "Your estimated daily targets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(BrandGradient), HeroShape)
                .padding(Spacing.xxl)
        ) {
            Text(
                "${goals.dailyCalorieGoal} calories/day",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Protein ${goals.proteinGrams}g • Carbs ${goals.carbsGrams}g • Fat ${goals.fatGrams}g",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
        MacroRow("Protein", goals.proteinGrams, MaterialTheme.extended.protein)
        MacroRow("Carbs", goals.carbsGrams, MaterialTheme.extended.carbs)
        MacroRow("Fat", goals.fatGrams, MaterialTheme.extended.fat)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "These are estimates only. Consult a qualified professional for medical or dietary conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, grams: Int, color: Color) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${grams}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/** Branded slider with range hints beneath it (spec §34 UI rules: standard M3 slider). */
@Composable
private fun LabeledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    minLabel: String,
    maxLabel: String
) {
    Column {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
