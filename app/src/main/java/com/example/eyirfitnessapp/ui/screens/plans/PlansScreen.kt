package com.example.eyirfitnessapp.ui.screens.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eyirfitnessapp.Routes
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.tintedSurface
import com.example.eyirfitnessapp.ui.theme.Spacing

/** Plans hub (spec §16-§19). */
@Composable
fun PlansScreen(repository: AppRepository, onNavigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        Text("Plans", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.lg))

        val items = listOf(
            Triple("Meal Planner", "Plan your week", Routes.PLANNER) to Icons.Default.FormatListBulleted,
            Triple("Recipes", "Build recipes with auto nutrition", Routes.RECIPES) to Icons.Default.MenuBook,
            Triple("Grocery List", "From planned meals", Routes.GROCERY) to Icons.Default.ShoppingCart,
            Triple("Saved Meals", "One-tap logging combos", Routes.SAVED_MEALS) to Icons.Default.Fastfood,
            Triple("Fasting Timer", "16:8, 14:10 and more", Routes.FASTING) to Icons.Default.Schedule
        )
        items.forEach { (item, icon) ->
            AppCard(onClick = { onNavigate(item.third) }) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(tintedSurface(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(item.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }
}
