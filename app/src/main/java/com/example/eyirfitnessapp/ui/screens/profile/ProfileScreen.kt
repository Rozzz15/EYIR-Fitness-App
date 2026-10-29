package com.example.eyirfitnessapp.ui.screens.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.MetricCard
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.BrandGradient
import com.example.eyirfitnessapp.ui.theme.CarbsColor
import com.example.eyirfitnessapp.ui.theme.FatColor
import com.example.eyirfitnessapp.ui.theme.ProteinColor
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.ui.viewmodels.ProfileViewModel
import com.example.eyirfitnessapp.util.AppMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Profile (spec §22): identity, metrics and daily targets; settings live in Settings. */
@Composable
fun ProfileScreen(repository: AppRepository, onNavigate: (String) -> Unit = {}) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(repository))
    val profile by viewModel.profile.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    /** Tapping the avatar opens the system photo picker; the copy is stored privately. */
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked ->
        if (picked != null) {
            scope.launch {
                val path = copyAvatarToPrivateFile(context, picked)
                if (path != null) viewModel.setAvatar(path)
                else AppMessages.error("Couldn't load that image. Please try again.")
            }
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }

    val p = profile
    if (p == null) {
        Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.lg))
            Text("No profile found on this device.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tappable avatar: a picked photo replaces the letter initial
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                val avatar = rememberAvatarBitmap(p.avatarUri)
                if (avatar != null) {
                    Image(
                        bitmap = avatar,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(BrandGradient), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.name.trim().take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Small camera badge so the avatar is discoverable as a button
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 2.dp)
                        .size(18.dp)
                        .background(
                            androidx.compose.ui.graphics.Color.White,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = "Change profile photo",
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    )
                    IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit name",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                p.email?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            MetricCard(
                "Current",
                "%.1f".format(p.weightUnit.fromKg(p.currentWeightKg)) + " ${p.weightUnit.label}",
                Modifier.weight(1f).fillMaxHeight()
            )
            Spacer(Modifier.width(8.dp))
            MetricCard(
                "Goal",
                "%.1f".format(p.weightUnit.fromKg(p.goalWeightKg)) + " ${p.weightUnit.label}",
                Modifier.weight(1f).fillMaxHeight(),
                accent = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
            Text("Daily targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            goals?.let { g ->
                Text("${g.dailyCalorieGoal} kcal", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Spacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TargetStat("Protein", "${g.proteinGrams}g", ProteinColor, Modifier.weight(1f))
                    TargetStat("Carbs", "${g.carbsGrams}g", CarbsColor, Modifier.weight(1f))
                    TargetStat("Fat", "${g.fatGrams}g", FatColor, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text("Goals & nutrition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Targets, macro split and your full nutrition report.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = "Edit goals & macros",
                onClick = { onNavigate(com.example.eyirfitnessapp.Routes.GOALS) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "Nutrition report",
                onClick = { onNavigate(com.example.eyirfitnessapp.Routes.NUTRITION) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        AppCard {
            Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Units, theme, start of week, export, sign-in, privacy and " +
                    "deleting your data (spec §22).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = "Open settings",
                onClick = { onNavigate(com.example.eyirfitnessapp.Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(Spacing.xxxl))
    }

    // Rename: tap the name (or its pencil) → simple editor dialog
    if (showRenameDialog) {
        var nameText by remember { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit name") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text("Your name") },
                    isError = nameText.isBlank(),
                    supportingText = {
                        if (nameText.isBlank()) Text("Enter a name to continue")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameText.isNotBlank(),
                    onClick = {
                        viewModel.rename(nameText)
                        showRenameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Copy the picked photo into app-private storage. The photo picker only
 * grants temporary read access to its content URI, so storing the URI itself
 * would break after the first restart; a private file never expires.
 */
private suspend fun copyAvatarToPrivateFile(context: Context, source: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "avatars").apply { mkdirs() }
            val out = File(dir, "avatar-${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            out.absolutePath
        } catch (_: Exception) {
            null
        }
    }

/**
 * Decode the stored avatar at display resolution (56 dp circle); decode in
 * the background so a large gallery photo never janks first composition.
 */
@Composable
private fun rememberAvatarBitmap(path: String?): androidx.compose.ui.graphics.ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(path) {
        val file = path?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file, bounds)
            if (bounds.outWidth <= 0 && bounds.outHeight <= 0) {
                // Missing/unreadable file → fall back to the letter initial
                return@withContext null
            }
            var sample = 1
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
            BitmapFactory.decodeFile(
                file,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )?.asImageBitmap()
        }
    }
    return bitmap
}

@Composable
private fun TargetStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}
