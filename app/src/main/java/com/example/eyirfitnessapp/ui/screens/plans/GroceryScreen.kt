package com.example.eyirfitnessapp.ui.screens.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import com.example.eyirfitnessapp.ui.viewmodels.GroceryViewModel
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing

/**
 * Grocery list (spec §18): persisted items in fixed categories with
 * quantities, check-off, delete and clear-completed.
 */
@Composable
fun GroceryScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: GroceryViewModel = viewModel(factory = GroceryViewModel.factory(repository))
    val items by viewModel.items.collectAsState()
    val notice by viewModel.notice.collectAsState()

    var newText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        ScreenHeader(title = "Grocery list", onBack = onBack)
        Spacer(Modifier.height(Spacing.md))

        notice?.let { text ->
            AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                label = { Text("Add item") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.sm))
            PrimaryButton(
                text = "Add",
                onClick = {
                    viewModel.add(newText)
                    newText = ""
                },
                enabled = newText.isNotBlank()
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${items.count { !it.completed }} to buy • ${items.count { it.completed }} done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SecondaryButton(
                text = "Clear done",
                onClick = viewModel::clearCompleted,
                modifier = Modifier.height(40.dp)
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        if (items.isEmpty()) {
            EmptyState(
                title = "List is empty",
                subtitle = "Add items above, or generate them from planned recipes in the Meal Planner."
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PlanLogic.CATEGORIES.forEach { category ->
                val inCategory = items.filter { it.category == category }
                if (inCategory.isNotEmpty()) {
                    item(key = "cat_$category") {
                        Text(
                            category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                        )
                    }
                    items(inCategory, key = { it.id }) { item ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.completed,
                                    onCheckedChange = { checked -> viewModel.setCompleted(item.id, checked) }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (item.completed) FontWeight.Normal else FontWeight.Medium,
                                        textDecoration = if (item.completed) TextDecoration.LineThrough else null
                                    )
                                    item.quantity?.let { qty ->
                                        Text(
                                            formatQty(qty) + (item.unit?.let { " $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.delete(item.id) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete ${item.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.xxxl)) }
        }
    }
}

/** "500" not "500.0" — keeps quantity labels readable. */
private fun formatQty(q: Double): String =
    if (q == q.toLong().toDouble()) q.toLong().toString() else "%.1f".format(q)
