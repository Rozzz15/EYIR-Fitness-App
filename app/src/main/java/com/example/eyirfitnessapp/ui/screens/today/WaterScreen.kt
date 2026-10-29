package com.example.eyirfitnessapp.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.CalorieRing
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.WaterViewModel

/** Water tracker (spec §12): quick amounts + custom entry + progress. */
@Composable
fun WaterScreen(repository: AppRepository, onBack: () -> Unit) {
    val viewModel: WaterViewModel = viewModel(factory = WaterViewModel.factory(repository))
    val waterMl by viewModel.waterMl.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val goalMl = profile?.waterGoalMl ?: 2500

    var customText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = "Water", onBack = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xl))

        CalorieRing(
            progress = if (goalMl > 0) waterMl.toFloat() / goalMl else 0f,
            centerValue = "${"%.1f".format(waterMl / 1000.0)}L",
            centerLabel = "of ${"%.1f".format(goalMl / 1000.0)}L",
            ringSize = 180.dp
        )
        Spacer(Modifier.height(Spacing.xl))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SecondaryButton(text = "+250 ml", onClick = { viewModel.add(250) })
            SecondaryButton(text = "+500 ml", onClick = { viewModel.add(500) })
        }
        Spacer(Modifier.height(Spacing.md))

        AppCard {
            Text("Custom amount", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it },
                label = { Text("Amount (ml)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Add",
                onClick = {
                    customText.toIntOrNull()?.let { ml ->
                        if (ml > 0) {
                            viewModel.add(ml)
                            customText = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (customText.toIntOrNull() ?: 0) > 0
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        SecondaryButton(text = "Undo last", onClick = { viewModel.deleteLast() })
    }
}
