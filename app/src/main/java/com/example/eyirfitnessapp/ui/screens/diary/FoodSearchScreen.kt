package com.example.eyirfitnessapp.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.ErrorState
import com.example.eyirfitnessapp.ui.components.FoodRow
import com.example.eyirfitnessapp.ui.components.LoggedFoodRow
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.tintedSurface
import com.example.eyirfitnessapp.ui.theme.CardShape
import com.example.eyirfitnessapp.ui.theme.ChipShape
import com.example.eyirfitnessapp.ui.theme.Elevation
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Radii
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.AmountUnit
import com.example.eyirfitnessapp.ui.viewmodels.FoodSearchViewModel
import com.example.eyirfitnessapp.util.AppMessages
import com.example.eyirfitnessapp.util.Dates
import kotlin.math.abs

/**
 * Flow A (spec §7): Search → select food → amount → meal → add.
 * Meal and target date come from the caller (Diary meal row, Today meal card, FAB quick add).
 */
@Composable
fun FoodSearchScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    initialMeal: MealType? = null,
    targetDate: String? = null,
    onOpenFoodDetail: (Long) -> Unit = {}
) {
    val viewModel: FoodSearchViewModel = viewModel(factory = FoodSearchViewModel.factory(repository))
    val results by viewModel.results.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val selectionSeq by viewModel.selectionSeq.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val frequent by viewModel.frequent.collectAsState()
    val multiSelect by viewModel.multiSelect.collectAsState()
    val checkedIds by viewModel.checkedIds.collectAsState()
    val notice by viewModel.notice.collectAsState()

    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.configure(initialMeal, targetDate) }

    LaunchedEffect(query) {
        viewModel.search(query)
    }

    val selectedFood = selected
    if (selectedFood != null) {
        // ── Food detail + amount selection ──
        var amountText by remember { mutableStateOf("") }
        var gramWeightText by remember { mutableStateOf("") }
        val amountUnit by viewModel.amountUnit.collectAsState()
        val needsGramWeight by viewModel.needsGramWeight.collectAsState()
        val adding by viewModel.adding.collectAsState()
        val gramsMode = amountUnit == AmountUnit.GRAMS
        val weightBased = NutritionEngine.isWeightBased(selectedFood)

        LaunchedEffect(selectionSeq, amountUnit) {
            amountText = if (gramsMode) {
                NutritionEngine.toGrams(selectedFood, quantity)?.let { formatAmount(it) } ?: ""
            } else {
                formatAmount(quantity)
            }
            gramWeightText = ""
        }

        // What the user sees in the amount field: grams in scale mode, else the native unit.
        val shownAmount = if (gramsMode) NutritionEngine.toGrams(selectedFood, quantity) ?: quantity else quantity
        val shownUnit = if (gramsMode) "g" else selectedFood.servingUnit

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl)
        ) {
            ScreenHeader(title = selectedFood.name, onBack = { viewModel.clearSelection() })
            Spacer(Modifier.height(Spacing.md))

            val scaled = NutritionEngine.scale(selectedFood, quantity)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                color = tintedSurface(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.LocalFireDepartment, MaterialTheme.colorScheme.primary, 46.dp)
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text(
                                "${scaled.calories.toInt()} kcal",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Per ${formatAmount(shownAmount)} $shownUnit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Row(Modifier.fillMaxWidth()) {
                        MacroStat("Protein", scaled.protein, Color(0xFF7C3AED), Modifier.weight(1f))
                        MacroStat("Carbs", scaled.carbs, Color(0xFFF59E0B), Modifier.weight(1f))
                        MacroStat("Fat", scaled.fat, Color(0xFFF43F5E), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = { onOpenFoodDetail(selectedFood.id) },
                    shape = PillShape,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Nutrition facts", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { viewModel.toggleFavorite(selectedFood) },
                    shape = PillShape,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedFood.favorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedFood.favorite) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                ) {
                    Icon(
                        if (selectedFood.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selectedFood.favorite) "Favorited" else "Favorite",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Size options ("Egg: Small / Medium / Large / X-Large"). Picking one
            // sets the amount to that size's weight — nutrition scales from it and
            // the field stays editable (weigh the actual egg if you prefer).
            if (selectedFood.sizes.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Text("Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    val currentGrams = NutritionEngine.toGrams(selectedFood, quantity)
                    selectedFood.sizes.forEach { size ->
                        FilterChip(
                            selected = currentGrams != null && abs(currentGrams - size.grams) < 0.01,
                            onClick = { viewModel.selectSize(size) },
                            label = { Text("${size.label} ${size.grams.stripZeros()} g") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val servingGrams = NutritionEngine.toGrams(selectedFood, selectedFood.servingSize)
                if (gramsMode && servingGrams != null) {
                    Text(
                        "1 serving = ${servingGrams.stripZeros()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (selectedFood.servingSize != 1.0 || selectedFood.servingUnit != "serving") {
                    Text(
                        "1 serving = ${selectedFood.servingSize.stripZeros()} ${selectedFood.servingUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            // Unit toggle — only for count-based foods; weight-based foods (unit "g")
            // already take grams from the scale directly.
            if (!weightBased) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = !gramsMode && !needsGramWeight,
                        onClick = { viewModel.setAmountUnit(AmountUnit.NATIVE) },
                        label = { Text(selectedFood.servingUnit) }
                    )
                    FilterChip(
                        selected = gramsMode,
                        onClick = { viewModel.setAmountUnit(AmountUnit.GRAMS) },
                        label = { Text("g") }
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            if (needsGramWeight) {
                // Ask once: how much does one serving weigh? Saved on the food row.
                Text(
                    "How many grams does 1 ${selectedFood.servingUnit} weigh? " +
                        "Weigh it once — we'll remember it for next time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = gramWeightText,
                        onValueChange = { gramWeightText = it },
                        singleLine = true,
                        shape = ChipShape,
                        isError = gramWeightText.isNotBlank() &&
                            gramWeightText.toDoubleOrNull()?.let { it <= 0 } != false,
                        suffix = { Text("g", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Use grams",
                        onClick = {
                            gramWeightText.toDoubleOrNull()?.takeIf { it > 0 }?.let { viewModel.saveGramWeight(it) }
                        },
                        enabled = gramWeightText.toDoubleOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        amountText = text
                        val value = text.toDoubleOrNull()?.takeIf { it > 0 }
                        if (value != null) {
                            val native = if (gramsMode) NutritionEngine.fromGrams(selectedFood, value) else value
                            if (native != null && native > 0) viewModel.setQuantity(native)
                        }
                    },
                    singleLine = true,
                    shape = ChipShape,
                    suffix = { Text(shownUnit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (weightBased) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Type the weight in grams from your food scale.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(0.5, 1.0, 2.0, 3.0).forEach { factor ->
                        FilterChip(
                            selected = quantity == selectedFood.servingSize * factor,
                            onClick = {
                                val amount = selectedFood.servingSize * factor
                                viewModel.setQuantity(amount)
                                amountText = if (gramsMode) {
                                    NutritionEngine.toGrams(selectedFood, amount)?.let { formatAmount(it) }
                                        ?: formatAmount(amount)
                                } else {
                                    formatAmount(amount)
                                }
                            },
                            label = { Text("${factor.stripZeros()}×") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Text("Meal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MealType.entries.forEach { m ->
                    MealChoiceTile(
                        meal = m,
                        selected = mealType == m,
                        onClick = { viewModel.setMealType(m) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = if (adding) "Adding…" else "Add to diary",
                onClick = { viewModel.addSelectedToDiary(onBack) },
                enabled = viewModel.canAdd(quantity) && !adding,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    } else {
        // ── Search + recents ──
        Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
            ScreenHeader(
                title = "Add food",
                onBack = onBack,
                actions = {
                    Surface(
                        onClick = { viewModel.toggleMultiSelect() },
                        shape = PillShape,
                        color = if (multiSelect) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (multiSelect) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            if (multiSelect) "Done" else "Select several",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (multiSelect) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            )
            Spacer(Modifier.height(Spacing.sm))

            if (targetDate != null && targetDate != Dates.today()) {
                Surface(
                    shape = PillShape,
                    color = tintedSurface(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Logging for ${Dates.prettyDate(targetDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search foods") },
                placeholder = { Text("Name, brand or barcode") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = PillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.lg))
            Text("Log to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MealType.entries.forEach { m ->
                    MealChoiceTile(
                        meal = m,
                        selected = mealType == m,
                        onClick = { viewModel.setMealType(m) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (notice != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(notice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick add", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Straight to ${mealType.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QuickAddCard(
                    title = "200 kcal",
                    subtitle = "Instant, no details",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.quickAddCalories(200.0, 0.0, 0.0, 0.0, mealType, onBack) }
                )
                QuickAddCard(
                    title = "400 kcal",
                    subtitle = "30P · 40C · 13F",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.quickAddCalories(400.0, 30.0, 40.0, 13.0, mealType, onBack) }
                )
            }

            if (multiSelect && checkedIds.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton(
                    text = "Add ${checkedIds.size} to ${mealType.label}",
                    onClick = { viewModel.addCheckedToDiary { } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Spacing.md))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = Spacing.xl)
            ) {
                if (query.isBlank()) {
                    item { SectionLabel("Recent") }
                    if (recent.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Nothing logged yet",
                                subtitle = "Search below to add your first food."
                            )
                        }
                    }
                    items(recent, key = { "recent_${it.id}" }) { log ->
                        LoggedFoodRow(
                            log,
                            // Tap opens the food for review — never re-logs. Only the + button adds again,
                            // so an accidental tap can't pile extra calories onto a meal.
                            onClick = {
                                viewModel.openRecent(
                                    log,
                                    onMissingFood = {
                                        AppMessages.notice(
                                            "Use + to log \"${log.name}\" again — it's not tied to a food entry."
                                        )
                                    }
                                )
                            },
                            onQuickAdd = { viewModel.addRecentLogToDiary(log, onBack) }
                        )
                    }
                    if (favorites.isNotEmpty()) {
                        item { SectionLabel("Favorites") }
                        items(favorites, key = { "fav_${it.id}" }) { food ->
                            SearchRow(food, multiSelect, checkedIds.contains(food.id), viewModel)
                        }
                    }
                    if (frequent.isNotEmpty()) {
                        item { SectionLabel("Frequent") }
                        items(frequent, key = { "freq_${it.id}" }) { food ->
                            SearchRow(food, multiSelect, checkedIds.contains(food.id), viewModel)
                        }
                    }
                    item { SectionLabel("All foods") }
                }
                if (searchError != null) {
                    item {
                        ErrorState(
                            title = "Search failed",
                            subtitle = searchError!!,
                            onRetry = viewModel::retrySearch
                        )
                    }
                } else if (searching) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("  Searching…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(results, key = { "food_${it.id}" }) { food ->
                        SearchRow(food, multiSelect, checkedIds.contains(food.id), viewModel)
                    }
                } else {
                    if (results.isEmpty() && query.isNotBlank()) {
                        item {
                            EmptyState(
                                title = "No foods found",
                                subtitle = "Try a different search term."
                            )
                        }
                    }
                    items(results, key = { "food_${it.id}" }) { food ->
                        SearchRow(food, multiSelect, checkedIds.contains(food.id), viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

/**
 * One search result: tapping opens the portion flow, or toggles the checkbox
 * while multi-select is on. The star is always available (spec §8 favorites).
 */
@Composable
private fun SearchRow(
    food: com.example.eyirfitnessapp.domain.model.Food,
    multiSelect: Boolean,
    checked: Boolean,
    viewModel: FoodSearchViewModel
) {
    FoodRow(
        food = food,
        onClick = {
            if (multiSelect) viewModel.toggleChecked(food) else viewModel.select(food)
        },
        onFavorite = { viewModel.toggleFavorite(food) },
        checked = if (multiSelect) checked else null,
        onCheck = { viewModel.toggleChecked(food) }
    )
}

// ── Choice tiles ─────────────────────────────────────────────────────────────

private data class MealVisuals(val icon: ImageVector, val color: Color)

private fun mealVisuals(meal: MealType): MealVisuals = when (meal) {
    MealType.BREAKFAST -> MealVisuals(Icons.Outlined.FreeBreakfast, Color(0xFFF59E0B))
    MealType.LUNCH -> MealVisuals(Icons.Outlined.LunchDining, Color(0xFF16A34A))
    MealType.DINNER -> MealVisuals(Icons.Outlined.DinnerDining, Color(0xFF7C3AED))
    MealType.SNACKS -> MealVisuals(Icons.Outlined.Cookie, Color(0xFFF43F5E))
}

/** Meal picker tile: icon + label, tinted and bordered while selected. */
@Composable
private fun MealChoiceTile(
    meal: MealType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = mealVisuals(meal)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ChipShape,
        color = if (selected) tintedSurface(visuals.color.copy(alpha = 0.14f))
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) visuals.color.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (selected) Elevation.Card else Elevation.Flat
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                visuals.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) visuals.color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                meal.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) visuals.color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One-tap calorie card for the quick-add row (spec §3 fastest logging). */
@Composable
private fun QuickAddCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = Color(0xFFF97316)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CardShape,
        color = tintedSurface(tint.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        shadowElevation = Elevation.Card
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Outlined.LocalFireDepartment, tint, 38.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/** Scaled macro trio used in the amount panel. */
@Composable
private fun MacroStat(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${"%.1f".format(grams)} g",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Tinted rounded icon chip used in card headers. */
@Composable
private fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radii.Medium))
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

private fun formatAmount(value: Double): String = value.stripZeros()

private fun Double.stripZeros(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')
