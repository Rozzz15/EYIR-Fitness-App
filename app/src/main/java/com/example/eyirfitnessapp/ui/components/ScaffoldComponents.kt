package com.example.eyirfitnessapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.ui.theme.BrandGradient
import com.example.eyirfitnessapp.ui.theme.CardShape
import com.example.eyirfitnessapp.ui.theme.Elevation
import com.example.eyirfitnessapp.ui.theme.PillShape
import com.example.eyirfitnessapp.ui.theme.Spacing

/**
 * Bakes a translucent tint into an opaque card colour. Required for elevated
 * cards: a see-through fill lets the elevation shadow show up *inside* the
 * card, which muddies the colour.
 */
@Composable
fun tintedSurface(tint: Color): Color =
    if (tint.alpha >= 1f) tint
    else lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = tint.copy(alpha = 1f),
        fraction = tint.alpha
    )

/** Standard card surface: soft elevation + hairline border instead of a hard box. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    elevation: androidx.compose.ui.unit.Dp = Elevation.Card,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = tintedSurface(color),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = elevation,
        onClick = onClick ?: {}
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/**
 * Metric card with big number (spec §20).
 * All text is single-line with ellipsis so one long value can never wrap and
 * make its card taller than the rest of the row.
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    accent: androidx.compose.ui.graphics.Color? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    AppCard(modifier = modifier, color = color) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accent ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            softWrap = false
        )
        if (sub != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/** Food search result row (spec §8 search results). */
@Composable
fun FoodRow(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Spec §8: star toggle; null hides it. */
    onFavorite: (() -> Unit)? = null,
    /** Spec §2 multi-item selection: null = no checkbox, true/false = its state. */
    checked: Boolean? = null,
    onCheck: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Elevation.Card,
        onClick = onClick
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (checked != null) {
                    androidx.compose.material3.Checkbox(
                        checked = checked,
                        onCheckedChange = { onCheck?.invoke() }
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(food.name, style = MaterialTheme.typography.titleMedium)
                    val brandLine = listOfNotNull(food.brand, "${food.servingSize.toInt()} ${food.servingUnit}").joinToString(" • ")
                    Text(
                        brandLine.ifEmpty { "Generic" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${food.calories.round0()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (onFavorite != null) {
                    androidx.compose.material3.IconButton(onClick = onFavorite) {
                        androidx.compose.material3.Icon(
                            if (food.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (food.favorite) "Remove ${food.name} from favorites" else "Add ${food.name} to favorites",
                            tint = if (food.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Protein ${food.protein.round1()}g • Carbs ${food.carbs.round1()}g • Fat ${food.fat.round1()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Logged food row in the diary (spec §8). Set [inCard] to render flat inside a meal card. */
@Composable
fun LoggedFoodRow(
    log: FoodLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inCard: Boolean = false,
    onQuickAdd: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (inCard) Color.Transparent else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CardShape,
        border = if (inCard) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        shadowElevation = if (inCard) Elevation.Flat else Elevation.Card,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(log.name, style = MaterialTheme.typography.titleMedium)
                val factor = if (log.servingSize > 0) log.quantity / log.servingSize else 1.0
                val amount = "${log.quantity.stripTrailingZero()} ${log.servingUnit}"
                val servingNote = if (log.servingSize != 1.0 && factor != 1.0) {
                    " • ${factor.stripTrailingZero()}× serving"
                } else {
                    ""
                }
                Text(
                    amount + servingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${log.calories.round0()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (onQuickAdd != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onQuickAdd) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add ${log.name} again",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove ${log.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** Empty state (spec §34 error/empty handling). */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.SearchOff
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Error state with retry (spec §34: loading / success / empty / error / retry). */
@Composable
fun ErrorState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry, shape = PillShape) {
            Text("Try again")
        }
    }
}

/** Primary CTA: brand-gradient pill with a soft drop shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val background = if (enabled) {
        Brush.horizontalGradient(BrandGradient)
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .shadow(if (enabled) Elevation.Raised else Elevation.Flat, PillShape, clip = false)
            .background(background, PillShape),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Secondary outlined button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = PillShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Screen header with back navigation. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

private fun Double.round0(): String = "%.0f".format(this)
private fun Double.round1(): String = "%.1f".format(this)
private fun Double.stripTrailingZero(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else "%.1f".format(this)
