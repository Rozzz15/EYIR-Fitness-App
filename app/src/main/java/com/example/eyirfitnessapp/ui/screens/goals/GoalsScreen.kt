@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.eyirfitnessapp.ui.screens.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.Goals
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.MacroTargetMode
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
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
import com.example.eyirfitnessapp.ui.viewmodels.GoalsViewModel
import kotlin.math.roundToInt

/**
 * Goals & nutrition targets (spec Â§21 profile inputs + goal selection,
 * Â§9 percentage/grams macro targets, Â§22 "Calories / Macros" settings).
 * Everything here is edited locally and written in one save.
 *
 * Layout: gradient target hero â†’ section cards (goal, activity, body, calories,
 * macros) â†’ footer actions. Every value is previewed live before saving.
 */
@Composable
fun GoalsScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.factory(repository))
    val profile by viewModel.profile.collectAsState()
    val storedMacros by viewModel.storedMacros.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()

    val p = profile

    // â”€â”€ Editable state, seeded once from the stored profile â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var seeded by remember { mutableStateOf(false) }
    var goalType by remember { mutableStateOf(GoalType.MAINTAIN) }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var ageText by remember { mutableStateOf("30") }
    var heightCm by remember { mutableFloatStateOf(175f) }
    var weightDisplay by remember { mutableFloatStateOf(75f) }
    var goalWeightDisplay by remember { mutableFloatStateOf(70f) }
    var weeklyGoalKg by remember { mutableFloatStateOf(0.5f) }
    var unit by remember { mutableStateOf(WeightUnit.KG) }
    var autoCalories by remember { mutableStateOf(true) }
    var manualCalories by remember { mutableStateOf("2000") }

    var mode by remember { mutableStateOf(MacroTargetMode.PERCENT) }
    var proteinPct by remember { mutableFloatStateOf(30f) }
    var carbsPct by remember { mutableFloatStateOf(40f) }
    var fatPct by remember { mutableFloatStateOf(30f) }
    var proteinG by remember { mutableStateOf("150") }
    var carbsG by remember { mutableStateOf("200") }
    var fatG by remember { mutableStateOf("65") }

    LaunchedEffect(p, storedMacros) {
        if (!seeded && p != null) {
            goalType = p.goalType
            activity = p.activityLevel
            sex = p.sex
            ageText = p.age.toString()
            heightCm = p.heightCm.toFloat()
            unit = p.weightUnit
            weightDisplay = p.weightUnit.fromKg(p.currentWeightKg).toFloat()
            goalWeightDisplay = p.weightUnit.fromKg(p.goalWeightKg).toFloat()
            weeklyGoalKg = p.weeklyGoalKg.toFloat()
            autoCalories = p.dailyCalorieGoal == null
            manualCalories = (p.dailyCalorieGoal ?: NutritionEngine.computedCalorieGoal(p)).toString()

            val stored = storedMacros
            if (stored != null) {
                mode = stored.mode
                proteinPct = stored.proteinPercent.toFloat()
                carbsPct = stored.carbsPercent.toFloat()
                fatPct = stored.fatPercent.toFloat()
                proteinG = stored.proteinGrams.toString()
                carbsG = stored.carbsGrams.toString()
                fatG = stored.fatGrams.toString()
            }
            seeded = true
        }
    }

    if (p == null) {
        Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
            ScreenHeader(title = "Goals & macros", onBack = onBack)
            Spacer(Modifier.height(Spacing.lg))
            EmptyState(
                title = "No profile yet",
                subtitle = "Run onboarding first â€” your goals are built from your profile.",
            )
        }
        return
    }

    val current = p
    val draftProfile = current.copy(
        sex = sex,
        age = ageText.toIntOrNull()?.coerceIn(14, 90) ?: current.age,
        heightCm = heightCm.toDouble(),
        currentWeightKg = unit.toKg(weightDisplay.toDouble()),
        goalWeightKg = unit.toKg(goalWeightDisplay.toDouble()),
        activityLevel = activity,
        goalType = goalType,
        weeklyGoalKg = weeklyGoalKg.toDouble(),
        weightUnit = unit,
        dailyCalorieGoal = if (autoCalories) null else manualCalories.toIntOrNull()
    )
    val draftMacros = MacroGoals(
        mode = mode,
        proteinGrams = proteinG.toIntOrNull() ?: 0,
        carbsGrams = carbsG.toIntOrNull() ?: 0,
        fatGrams = fatG.toIntOrNull() ?: 0,
        proteinPercent = proteinPct.toInt(),
        carbsPercent = carbsPct.toInt(),
        fatPercent = fatPct.toInt()
    )
    // Pure domain call: what the rest of the app will see after saving.
    val preview = NutritionEngine.macroGoals(draftProfile, draftMacros)

    /**
     * A cleared/partial manual field would silently store null and flip the app
     * back to the automatic estimate while the UI still says "Set manually" —
     * so saving is blocked until the manual number is real.
     */
    val manualCaloriesValid = mode == MacroTargetMode.GRAMS ||
        autoCalories || (manualCalories.toIntOrNull() ?: 0) in 800..10000

    val pctTotal = proteinPct.toInt() + carbsPct.toInt() + fatPct.toInt()

    /**
     * Age is typed in now; a blank or out-of-range value shows an inline error
     * and blocks saving instead of silently storing a coerced number.
     */
    val ageValid = (ageText.toIntOrNull() ?: -1) in 14..90

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        ScreenHeader(title = "Goals & macros", onBack = onBack)
        Spacer(Modifier.height(Spacing.md))

        // â”€â”€ Live preview hero â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        TargetHero(preview = preview, goalLabel = goalType.label)
        Spacer(Modifier.height(Spacing.xxl))

        // â”€â”€ Goal selection (spec Â§21) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        SectionCard(
            icon = Icons.Outlined.Flag,
            tint = MaterialTheme.colorScheme.primary,
            title = "Your goal",
            subtitle = "Pick the outcome you are training for"
        ) {
            GoalDropdown(
                selected = goalType,
                onSelect = { goalType = it }
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    goalHint(goalType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (goalType.hasWeeklyPace) {
                Spacer(Modifier.height(Spacing.lg))
                InsetPanel {
                    SliderRow(
                        label = "Weekly pace",
                        valueText = "${"%.2f".format(weeklyGoalKg)} kg / week",
                        value = weeklyGoalKg,
                        valueRange = 0.1f..1.0f,
                        steps = 17,
                        onValueChange = { weeklyGoalKg = it },
                        caption = "Steady change is easier to keep off than a crash pace."
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // â”€â”€ Activity (spec Â§21) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        SectionCard(
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            tint = MaterialTheme.extended.warning,
            title = "Activity level",
            subtitle = "Used to estimate your daily burn"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActivityLevel.entries.forEach { level ->
                    ActivityRow(
                        level = level,
                        selected = activity == level,
                        onClick = { activity = level }
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // â”€â”€ Profile inputs (spec Â§21) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        SectionCard(
            icon = Icons.Outlined.Accessibility,
            tint = MaterialTheme.colorScheme.tertiary,
            title = "About you",
            subtitle = "These feed the BMR and calorie estimate"
        ) {
            FieldRow(label = "Sex") {
                SegmentedControl(
                    options = Sex.entries,
                    selected = sex,
                    onSelect = { sex = it },
                    label = { it.label },
                    modifier = Modifier.width(160.dp)
                )
            }
            Spacer(Modifier.height(Spacing.md))
            FieldRow(label = "Age") {
                Column {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { input ->
                            ageText = input.filter { c -> c.isDigit() }.take(2)
                        },
                        isError = !ageValid,
                        singleLine = true,
                        trailingIcon = { Text("yrs", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(Radii.Medium),
                        modifier = Modifier.width(132.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (!ageValid) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Enter an age between 14 and 90.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            SliderRow(
                label = "Height",
                valueText = "${heightCm.toInt()} cm",
                value = heightCm,
                valueRange = 130f..220f,
                onValueChange = { heightCm = it }
            )
            Spacer(Modifier.height(Spacing.md))
            FieldRow(label = "Weight unit") {
                SegmentedControl(
                    options = WeightUnit.entries,
                    selected = unit,
                    onSelect = { newUnit ->
                        // Convert what's on screen so the number stays true.
                        val kg = unit.toKg(weightDisplay.toDouble())
                        val goalKg = unit.toKg(goalWeightDisplay.toDouble())
                        unit = newUnit
                        weightDisplay = newUnit.fromKg(kg).toFloat()
                        goalWeightDisplay = newUnit.fromKg(goalKg).toFloat()
                    },
                    label = { it.label },
                    modifier = Modifier.width(110.dp)
                )
            }
            Spacer(Modifier.height(Spacing.md))
            SliderRow(
                label = "Current weight",
                valueText = "${"%.1f".format(weightDisplay)} ${unit.label}",
                value = weightDisplay,
                valueRange = if (unit == WeightUnit.KG) 35f..200f else 77f..440f,
                onValueChange = { weightDisplay = it }
            )
            Spacer(Modifier.height(Spacing.md))
            SliderRow(
                label = "Goal weight",
                valueText = "${"%.1f".format(goalWeightDisplay)} ${unit.label}",
                value = goalWeightDisplay,
                valueRange = if (unit == WeightUnit.KG) 35f..200f else 77f..440f,
                onValueChange = { goalWeightDisplay = it },
                caption = weightDeltaCaption(weightDisplay, goalWeightDisplay, unit)
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        // â”€â”€ Calories (spec Â§9, Â§22 Nutrition) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        SectionCard(
            icon = Icons.Outlined.LocalFireDepartment,
            tint = MaterialTheme.extended.burn,
            title = "Calories",
            subtitle = "Where your daily energy target comes from"
        ) {
            if (mode == MacroTargetMode.GRAMS) {
                // Gram mode: the engine derives kcal from P/C/F, so an editable
                // calorie field would be a lie. Show what the app will actually use.
                HighlightPanel(
                    icon = Icons.Outlined.PieChart,
                    eyebrow = "From your gram targets",
                    value = "${preview.dailyCalorieGoal} kcal / day",
                    caption = "Protein x 4 + carbs x 4 + fat x 9, from the grams below."
                )
            } else {
                SegmentedControl(
                    options = listOf(true, false),
                    selected = autoCalories,
                    onSelect = { autoCalories = it },
                    label = { if (it) "Calculate for me" else "Set manually" },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
                if (autoCalories) {
                    HighlightPanel(
                        icon = Icons.Outlined.AutoAwesome,
                        eyebrow = "Estimated for you",
                        value = "${NutritionEngine.computedCalorieGoal(draftProfile)} kcal / day",
                        caption = "BMR Ã— activity level, adjusted by your weekly pace."
                    )
                } else {
                    OutlinedTextField(
                        value = manualCalories,
                        onValueChange = { manualCalories = it.filter { c -> c.isDigit() } },
                        label = { Text("Daily calorie goal") },
                        trailingIcon = { Text("kcal", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        isError = !manualCaloriesValid,
                        shape = RoundedCornerShape(Radii.Medium),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (!manualCaloriesValid) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "Enter a daily goal between 800 and 10000 kcal.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        // â”€â”€ Macro targets (spec Â§9) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        SectionCard(
            icon = Icons.Outlined.PieChart,
            tint = MaterialTheme.extended.protein,
            title = "Macro targets",
            subtitle = "How your calories split across protein, carbs and fat"
        ) {
            SegmentedControl(
                options = MacroTargetMode.entries,
                selected = mode,
                onSelect = { mode = it },
                label = { it.label },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))

            if (mode == MacroTargetMode.PERCENT) {
                MacroSplitBar(
                    proteinPct = proteinPct,
                    carbsPct = carbsPct,
                    fatPct = fatPct
                )
                Spacer(Modifier.height(Spacing.lg))
                MacroSlider(
                    label = "Protein",
                    value = proteinPct,
                    onChange = { proteinPct = it },
                    color = MaterialTheme.extended.protein
                )
                MacroSlider(
                    label = "Carbs",
                    value = carbsPct,
                    onChange = { carbsPct = it },
                    color = MaterialTheme.extended.carbs
                )
                MacroSlider(
                    label = "Fat",
                    value = fatPct,
                    onChange = { fatPct = it },
                    color = MaterialTheme.extended.fat
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (pctTotal == 100) "Adds up to exactly 100%"
                        else "Total $pctTotal% â€” scaled to 100% when saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pctTotal == 100) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    if (pctTotal != 100) {
                        Spacer(Modifier.width(Spacing.sm))
                        ValuePill("$pctTotal%", MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    GramField(
                        label = "Protein",
                        value = proteinG,
                        color = MaterialTheme.extended.protein,
                        modifier = Modifier.weight(1f)
                    ) { proteinG = it }
                    GramField(
                        label = "Carbs",
                        value = carbsG,
                        color = MaterialTheme.extended.carbs,
                        modifier = Modifier.weight(1f)
                    ) { carbsG = it }
                    GramField(
                        label = "Fat",
                        value = fatG,
                        color = MaterialTheme.extended.fat,
                        modifier = Modifier.weight(1f)
                    ) { fatG = it }
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Gram targets are used as-is â€” the calorie total follows from them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        if (error != null) {
            ErrorBanner(error!!)
            Spacer(Modifier.height(Spacing.md))
        }

        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "Calorie and macro targets are estimates, not medical advice. " +
                    "Consult a qualified professional for medical or dietary conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SecondaryButton(text = "Cancel", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = if (saving) "Saving…" else "Save goals",
                onClick = {
                    viewModel.save(draftProfile, draftMacros, onDone = onBack)
                },
                enabled = !saving && manualCaloriesValid && ageValid,
                modifier = Modifier.weight(1f)
            )
        }
        // Clear the persistent quick-add FAB sitting over the bottom-end corner.
        Spacer(Modifier.height(84.dp))
    }
}

// â”€â”€ Hero â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** Gradient hero showing the targets the rest of the app will use once saved. */
@Composable
private fun TargetHero(preview: Goals, goalLabel: String) {
    val kcal by animateIntAsState(
        targetValue = preview.dailyCalorieGoal,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "heroKcal"
    )
    val protein by animateIntAsState(preview.proteinGrams, tween(500), label = "heroProtein")
    val carbs by animateIntAsState(preview.carbsGrams, tween(500), label = "heroCarbs")
    val fat by animateIntAsState(preview.fatGrams, tween(500), label = "heroFat")

    val pKcal = preview.proteinGrams * 4.0
    val cKcal = preview.carbsGrams * 4.0
    val fKcal = preview.fatGrams * 9.0
    val totalKcal = pKcal + cKcal + fKcal
    val pPct = if (totalKcal > 0) (pKcal / totalKcal * 100).roundToInt() else 33
    val cPct = if (totalKcal > 0) (cKcal / totalKcal * 100).roundToInt() else 34
    val fPct = if (totalKcal > 0) (fKcal / totalKcal * 100).roundToInt() else 33

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(Elevation.Floating, HeroShape, clip = false)
            .clip(HeroShape)
            .background(Brush.linearGradient(BrandGradient))
    ) {
        // Decorative glow â€” clipped to the card shape for depth
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = (-78).dp)
                .size(190.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-66).dp, y = 74.dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassChip("DAILY TARGET")
                Spacer(Modifier.weight(1f))
                GlassChip(goalLabel.uppercase())
            }
            Spacer(Modifier.height(Spacing.xl))

            Text(
                "$kcal",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "kcal / day",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f)
            )

            Spacer(Modifier.height(Spacing.xl))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radii.Large))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroMacroStat(protein, "Protein", pPct)
                HeroDivider()
                HeroMacroStat(carbs, "Carbs", cPct)
                HeroDivider()
                HeroMacroStat(fat, "Fat", fPct)
            }
        }
    }
}

@Composable
private fun GlassChip(text: String) {
    Surface(shape = PillShape, color = Color.White.copy(alpha = 0.18f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HeroMacroStat(grams: Int, label: String, percent: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            "${grams}g",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "$label $percent%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(34.dp)
            .background(Color.White.copy(alpha = 0.22f))
    )
}

// â”€â”€ Section scaffolding â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** Card with an icon badge header; the workhorse of this screen. */
@Composable
private fun SectionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        content()
    }
}

/** Tinted rounded icon chip used in card headers. */
@Composable
private fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Radii.Medium))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size.value * 0.5).dp)
        )
    }
}

/** Sunken inset used for grouped sub-controls (pace, highlights). */
@Composable
private fun InsetPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(Spacing.md), content = content)
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = tintedSurface(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.30f))
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Label on the left, control on the right (sex, unitsâ€¦). */
@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

// â”€â”€ Controls â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** Pill segmented control with an animated selection (2â€“3 short options). */
@Composable
private fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "segmentBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "segmentFg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(background)
                        .clickable { onSelect(option) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Read-only field that opens a menu of the five goal options. */
@Composable
private fun GoalDropdown(selected: GoalType, onSelect: (GoalType) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // Open on press: a quick tap flips Press/Release within one frame, so we
    // subscribe to the interaction stream instead of keying a LaunchedEffect.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) expanded = true
        }
    }
    val caret by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "goalCaret"
    )

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            interactionSource = interactionSource,
            label = { Text("Your goal") },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded) "Close goal menu" else "Open goal menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(caret)
                )
            },
            shape = RoundedCornerShape(Radii.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GoalType.entries.forEach { goal ->
                val isSelected = goal == selected
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                goal.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                goalHint(goal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            goalIcon(goal),
                            contentDescription = null,
                            tint = if (isSelected) primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = primary)
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onSelect(goal)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** One-line explanation shown under the goal field and inside each menu option. */
private fun goalHint(goal: GoalType): String = when (goal) {
    GoalType.LOSE -> "Calorie deficit until goal weight"
    GoalType.MAINTAIN -> "Hold your current weight"
    GoalType.GAIN -> "Calorie surplus to gain weight"
    GoalType.IMPROVE_NUTRITION -> "Balanced plates, no calorie counting"
    GoalType.BUILD_HABITS -> "Routines first, strict targets later"
}

/** Leading icon for each goal option in the menu. */
private fun goalIcon(goal: GoalType): ImageVector = when (goal) {
    GoalType.LOSE -> Icons.AutoMirrored.Outlined.TrendingDown
    GoalType.MAINTAIN -> Icons.Outlined.Balance
    GoalType.GAIN -> Icons.AutoMirrored.Outlined.TrendingUp
    GoalType.IMPROVE_NUTRITION -> Icons.Outlined.Restaurant
    GoalType.BUILD_HABITS -> Icons.Outlined.Checklist
}

/** One activity option: check + label + description + BMR factor pill. */
@Composable
private fun ActivityRow(level: ActivityLevel, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = if (selected) tintedSurface(primary.copy(alpha = 0.08f))
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    level.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    level.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            ValuePill("${level.factor}Ã—", if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Label + value pill + accent slider, with optional range caption. */
@Composable
private fun SliderRow(
    label: String?,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    accent: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null
) {
    Column(modifier) {
        if (label != null || valueText.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                ValuePill(valueText, accent)
            }
            Spacer(Modifier.height(Spacing.xs))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.16f)
            )
        )
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ValuePill(text: String, color: Color) {
    Surface(shape = PillShape, color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Auto vs. manual calorie highlight panel. */
@Composable
private fun HighlightPanel(
    icon: ImageVector,
    eyebrow: String,
    value: String,
    caption: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = tintedSurface(primary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.18f))
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, primary, size = 40.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// â”€â”€ Macros â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** Stacked calorie-split bar (protein / carbs / fat). */
@Composable
private fun MacroSplitBar(proteinPct: Float, carbsPct: Float, fatPct: Float) {
    val protein = MaterialTheme.extended.protein
    val carbs = MaterialTheme.extended.carbs
    val fat = MaterialTheme.extended.fat

    val p = proteinPct.coerceAtLeast(0f)
    val c = carbsPct.coerceAtLeast(0f)
    val f = fatPct.coerceAtLeast(0f)
    val total = p + c + f
    val weights = if (total > 0f) listOf(p / total, c / total, f / total)
    else listOf(1f, 1f, 1f)

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                Modifier
                    .weight(weights[0])
                    .height(10.dp)
                    .clip(PillShape)
                    .background(protein)
            )
            Box(
                Modifier
                    .weight(weights[1])
                    .height(10.dp)
                    .clip(PillShape)
                    .background(carbs)
            )
            Box(
                Modifier
                    .weight(weights[2])
                    .height(10.dp)
                    .clip(PillShape)
                    .background(fat)
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth()) {
            SplitLegend("Protein", proteinPct, protein, Modifier.weight(1f))
            SplitLegend("Carbs", carbsPct, carbs, Modifier.weight(1f))
            SplitLegend("Fat", fatPct, fat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SplitLegend(label: String, pct: Float, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "$label ${pct.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Percentage slider in the macro's own colour. */
@Composable
private fun MacroSlider(label: String, value: Float, onChange: (Float) -> Unit, color: Color) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                modifier = Modifier.weight(1f)
            )
            ValuePill("${value.toInt()}%", color)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..70f,
            steps = 69,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun GramField(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
            label = { Text("grams") },
            singleLine = true,
            shape = RoundedCornerShape(Radii.Medium),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = color,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

// â”€â”€ Text helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun ActivityLevel.subtitle(): String = when (this) {
    ActivityLevel.SEDENTARY -> "Little or no exercise"
    ActivityLevel.LIGHT -> "1â€“3 light sessions a week"
    ActivityLevel.MODERATE -> "3â€“5 moderate sessions a week"
    ActivityLevel.VERY_ACTIVE -> "6â€“7 hard sessions a week"
    ActivityLevel.EXTRA_ACTIVE -> "Athlete-level daily training"
}

private fun weightDeltaCaption(current: Float, goal: Float, unit: WeightUnit): String {
    val delta = goal - current
    val abs = "%.1f".format(kotlin.math.abs(delta))
    return when {
        delta < 0f -> "Plan to lose $abs ${unit.label}"
        delta > 0f -> "Plan to gain $abs ${unit.label}"
        else -> "Already at your goal weight"
    }
}

