package com.example.eyirfitnessapp.ui.screens.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.ui.components.AppCard
import com.example.eyirfitnessapp.ui.components.EmptyState
import com.example.eyirfitnessapp.ui.components.PrimaryButton
import com.example.eyirfitnessapp.ui.components.ScreenHeader
import com.example.eyirfitnessapp.ui.components.SecondaryButton
import com.example.eyirfitnessapp.ui.theme.Spacing
import com.example.eyirfitnessapp.util.AppMessages
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch

/**
 * Barcode flow (spec §13): camera scan → food result → serving → meal → add,
 * with manual entry and product creation as fallbacks (emulators, camera-less
 * devices, denied permission, unknown codes).
 */
@Composable
fun BarcodeScannerScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    onSearchManually: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var barcode by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Food?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var meal by remember { mutableStateOf(MealType.SNACKS) }
    var quantityText by remember { mutableStateOf("") }

    /** Stops analysis after a hit so the card below isn't rewritten mid-review. */
    var scanning by remember { mutableStateOf(true) }
    var cameraFailed by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        // Denied → keep the screen useful: manual entry stays fully functional.
        if (granted) cameraFailed = false
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Manual product creation fields (spec §13 "Product Creation")
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newBrand by remember { mutableStateOf("") }
    var newBarcode by remember { mutableStateOf("") }
    var newServingSize by remember { mutableStateOf("100") }
    var newServingUnit by remember { mutableStateOf("g") }
    var newKcal by remember { mutableStateOf("") }
    var newProtein by remember { mutableStateOf("") }
    var newCarbs by remember { mutableStateOf("") }
    var newFat by remember { mutableStateOf("") }
    var newFiber by remember { mutableStateOf("") }
    var newSugar by remember { mutableStateOf("") }
    var newSodium by remember { mutableStateOf("") }

    /** Blocks double-taps: a second tap would insert a duplicate row and pop twice (back to Home). */
    var logging by remember { mutableStateOf(false) }

    /** Manual or camera-driven lookup — one path so both behave identically. */
    fun lookup(code: String) {
        val query = code.trim()
        if (query.isEmpty()) return
        scope.launch {
            barcode = query
            val found = repository.foodByBarcode(query)
            result = found
            notFound = found == null
            if (found == null) newBarcode = query
        }
    }

    // Spec Flow B: the result carries a serving amount before it hits the diary.
    val servingFood = result
    LaunchedEffect(servingFood) {
        quantityText = servingFood?.let { trimAmount(it.servingSize) } ?: ""
    }
    val quantity = quantityText.toDoubleOrNull()?.takeIf { it > 0 }

    fun insertAndExit(food: Food, amount: Double) {
        scope.launch {
            try {
                val scaled = NutritionEngine.scale(food, amount)
                repository.addFoodLog(
                    FoodLog(
                        foodId = food.id,
                        name = food.name,
                        brand = food.brand,
                        mealType = meal,
                        quantity = amount,
                        servingSize = food.servingSize,
                        servingUnit = food.servingUnit,
                        calories = scaled.calories,
                        protein = scaled.protein,
                        carbs = scaled.carbs,
                        fat = scaled.fat,
                        date = repository.today(),
                        loggedAt = System.currentTimeMillis()
                    )
                )
                // Pops on success → the confirmation has to be app-wide.
                AppMessages.notice("Added ${food.name} to ${meal.label}")
                onBack()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                logging = false
                AppMessages.error("Couldn't log ${food.name}. Please try again.")
            }
        }
    }

    fun logFood(food: Food, amount: Double?) {
        if (logging || amount == null) return
        logging = true
        insertAndExit(food, amount)
    }

    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        ScreenHeader(title = "Scan barcode", onBack = onBack)
        Spacer(Modifier.height(Spacing.lg))

        // ── Camera (spec Flow B) ──
        if (hasCameraPermission && !cameraFailed) {
            if (scanning) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    CameraScanView(
                        onDetected = { code ->
                            scanning = false
                            lookup(code)
                        },
                        onFailure = { cameraFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Reticle: a fixed frame keeps the code inside the analysed region.
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(200.dp, 120.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    )
                    Text(
                        "Point the camera at the barcode",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.sm)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Scanning… enter the digits manually below if the camera can't read it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AppCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Camera paused", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "You can scan another code whenever you're ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    SecondaryButton(
                        text = "Scan again",
                        onClick = { scanning = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (!hasCameraPermission) {
            AppCard {
                Text(
                    "Camera access is off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Allow the camera to scan barcodes, or type the digits below — both log the same way.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton(
                    text = "Allow camera",
                    onClick = {
                        permissionRequested = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (result == null && !showCreate) {
                AppCard {
                    Text("Enter barcode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode digits") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
                    PrimaryButton(
                        text = "Look up",
                        onClick = { lookup(barcode) },
                        enabled = barcode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryButton(
                        text = "Create this product",
                        onClick = {
                            newBarcode = barcode.trim()
                            showCreate = true
                            notFound = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (notFound) {
                    Spacer(Modifier.height(Spacing.md))
                    EmptyState(
                        title = "We couldn't find this food",
                        subtitle = "Create it manually, search by name, or scan another code."
                    )
                    SecondaryButton(
                        text = "Search foods manually",
                        onClick = onSearchManually,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                AppCard(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Text("Example codes from the built-in list", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "1000000000015 Cheddar Crackers • 1000000000022 Protein Bar • 1000000000060 Cola",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            servingFood?.let { food ->
                AppCard {
                    Text(food.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    food.brand?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(Spacing.md))

                    Text("Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { text ->
                            quantityText = text.filter { it.isDigit() || it == '.' }
                        },
                        singleLine = true,
                        suffix = { Text(food.servingUnit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = quantityText.isNotBlank() && quantity == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (quantity == null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Enter a number greater than zero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(Modifier.height(Spacing.sm))
                        val scaled = NutritionEngine.scale(food, quantity)
                        Text(
                            "${scaled.calories.toInt()} kcal per ${trimAmount(quantity)} ${food.servingUnit}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "P ${scaled.protein.toInt()}g • C ${scaled.carbs.toInt()}g • F ${scaled.fat.toInt()}g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    Text("Meal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        MealType.entries.forEach { m ->
                            FilterChip(
                                selected = meal == m,
                                onClick = { meal = m },
                                label = { Text(m.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton(
                        text = if (logging) "Logging…" else "Log it",
                        onClick = { logFood(food, quantity) },
                        enabled = !logging && quantity != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryButton(
                        text = "Scan another",
                        onClick = {
                            result = null
                            notFound = false
                            barcode = ""
                            scanning = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showCreate) {
                AppCard {
                    Text("New product", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.md))

                    LabeledField(newName, { newName = it }, "Name")
                    LabeledField(newBrand, { newBrand = it }, "Brand (optional)")
                    LabeledField(newBarcode, { newBarcode = it }, "Barcode (optional)", numeric = true)

                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        LabeledField(
                            value = newServingSize,
                            onValueChange = { newServingSize = it },
                            label = "Serving size",
                            numeric = true,
                            modifier = Modifier.weight(1f)
                        )
                        LabeledField(
                            value = newServingUnit,
                            onValueChange = { newServingUnit = it },
                            label = "Unit",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(Spacing.sm))
                    LabeledField(newKcal, { newKcal = it }, "Calories per serving", numeric = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        LabeledField(newProtein, { newProtein = it }, "Protein g", numeric = true, modifier = Modifier.weight(1f))
                        LabeledField(newCarbs, { newCarbs = it }, "Carbs g", numeric = true, modifier = Modifier.weight(1f))
                        LabeledField(newFat, { newFat = it }, "Fat g", numeric = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        LabeledField(newFiber, { newFiber = it }, "Fiber g", numeric = true, modifier = Modifier.weight(1f))
                        LabeledField(newSugar, { newSugar = it }, "Sugar g", numeric = true, modifier = Modifier.weight(1f))
                        LabeledField(newSodium, { newSodium = it }, "Sodium mg", numeric = true, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    val serving = newServingSize.toDoubleOrNull()
                    val kcal = newKcal.toDoubleOrNull()
                    val canCreate = newName.isNotBlank() &&
                        serving != null && serving > 0 &&
                        kcal != null && kcal >= 0 &&
                        listOf(newProtein, newCarbs, newFat, newFiber, newSugar, newSodium)
                            .all { it.isBlank() || (it.toDoubleOrNull() ?: -1.0) >= 0.0 }

                    PrimaryButton(
                        text = if (logging) "Logging…" else "Create & log",
                        onClick = {
                            if (!canCreate || logging) return@PrimaryButton
                            logging = true
                            scope.launch {
                                try {
                                    val id = repository.createFood(
                                        Food(
                                            name = newName.trim(),
                                            brand = newBrand.trim().ifBlank { null },
                                            barcode = newBarcode.trim().ifBlank { null },
                                            servingSize = serving!!,
                                            servingUnit = newServingUnit.trim().ifBlank { "serving" },
                                            calories = kcal!!,
                                            protein = newProtein.toDoubleOrNull() ?: 0.0,
                                            carbs = newCarbs.toDoubleOrNull() ?: 0.0,
                                            fat = newFat.toDoubleOrNull() ?: 0.0,
                                            fiber = newFiber.toDoubleOrNull() ?: 0.0,
                                            sugar = newSugar.toDoubleOrNull() ?: 0.0,
                                            sodium = newSodium.toDoubleOrNull() ?: 0.0,
                                            source = "user"
                                        )
                                    )
                                    val food = repository.foodById(id)
                                    // Guard already set — go straight to the write + pop.
                                    if (food != null) insertAndExit(food, food.servingSize) else onBack()
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    logging = false
                                    AppMessages.error("Couldn't create that product. Please try again.")
                                }
                            }
                        },
                        enabled = canCreate && !logging,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryButton(text = "Cancel", onClick = { showCreate = false }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }
}

/**
 * Live camera preview with an ML Kit barcode analyser (spec Flow B → Camera →
 * Scan). Analysis runs on a dedicated executor; frames are dropped while the
 * caller isn't [scanning] so a detected code can't be overwritten.
 */
@Composable
private fun CameraScanView(
    onDetected: (String) -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val disposed = remember { AtomicBoolean(false) }
    val handled = remember { AtomicBoolean(false) }
    val failures = remember { AtomicInteger(0) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
    val latestOnDetected = rememberUpdatedState(onDetected)
    val latestOnFailure = rememberUpdatedState(onFailure)

    // Formats retail codes (EAN/UPC) plus the codes printed on packaging.
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
        )
    }

    AndroidViewPreview(previewView, modifier)

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                if (disposed.get()) {
                    runCatching { future.get().unbindAll() }
                } else {
                    try {
                        val provider = future.get()
                        cameraProviderRef.set(provider)
                        provider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { proxy ->
                                proxy.setAnalyzer(analysisExecutor) { imageProxy ->
                                    analyzeFrame(
                                        imageProxy = imageProxy,
                                        scanner = scanner,
                                        handled = handled,
                                        failures = failures,
                                        onDetected = latestOnDetected.value,
                                        onFailure = latestOnFailure.value
                                    )
                                }
                            }

                        val selector =
                            if (runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    } catch (_: Exception) {
                        // No camera / bind failure (emulator without one) → manual entry.
                        latestOnFailure.value()
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed.set(true)
            cameraProviderRef.getAndSet(null)?.let { provider ->
                runCatching { provider.unbindAll() }
            }
            analysisExecutor.shutdown()
            runCatching { scanner.close() }
        }
    }
}

/**
 * One analysis frame: decode it, then report the first code exactly once.
 *
 * Kept as a named declaration (rather than an inline lambda) so the opt-in for
 * [ImageProxy.getImage] sits directly on the call site — lint's
 * UnsafeOptInUsageError does not follow `@OptIn` across nested lambdas.
 */
@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    handled: AtomicBoolean,
    failures: AtomicInteger,
    onDetected: (String) -> Unit,
    onFailure: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (handled.get() || mediaImage == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val code = barcodes.firstOrNull()?.rawValue
            if (!code.isNullOrBlank() && handled.compareAndSet(false, true)) {
                onDetected(code)
            }
        }
        .addOnFailureListener {
            // Models can't initialise on devices without the required Play
            // Services component — fall back to manual entry instead of
            // scanning forever.
            if (failures.incrementAndGet() >= 3) {
                handled.set(true)
                onFailure()
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}

/** Thin [AndroidView] wrapper kept separate so the camera effect stays readable. */
@Composable
private fun AndroidViewPreview(previewView: PreviewView, modifier: Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(factory = { previewView }, modifier = modifier)
}

private fun trimAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')

@Composable
private fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(if (numeric) it.filter { c -> c.isDigit() || c == '.' } else it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
