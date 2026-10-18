package com.gymtracker.app.presentation.gallery

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.presentation.components.AppAlertDialog
import com.gymtracker.app.presentation.components.AppBadge
import com.gymtracker.app.presentation.components.AppButton
import com.gymtracker.app.presentation.components.AppButtonStyle
import com.gymtracker.app.presentation.components.AppModalBottomSheet
import com.gymtracker.app.presentation.components.AppTextField
import com.gymtracker.app.presentation.components.BadgeVariant
import com.gymtracker.app.presentation.components.ChipGroup
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.ErrorState
import com.gymtracker.app.presentation.components.InfoTag
import com.gymtracker.app.presentation.components.LoadingState
import com.gymtracker.app.presentation.components.MetricCard
import com.gymtracker.app.presentation.components.MetricRow
import com.gymtracker.app.presentation.components.OfflineState
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.TagRow
import com.gymtracker.app.presentation.theme.AppTheme
import com.gymtracker.app.presentation.theme.GymTrackerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewGalleryScreen(
    onBack: (() -> Unit)? = null,
) {
    var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }
    var showDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    GymTrackerTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Compose Visual System Gallery") },
                        actions = {
                            IconButton(onClick = {
                                themeMode = if (themeMode == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Toggle Light/Dark Theme",
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = AppTheme.spacing.phoneGutter)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.large),
                ) {
                    Spacer(Modifier.height(AppTheme.spacing.xSmall))

                    // Typography & Brand Header
                    SectionTitle("1. Typography Scale")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(AppTheme.spacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xSmall),
                        ) {
                            Text(
                                "28/34 Screen Title",
                                style = AppTheme.customTypography.screenTitle,
                            )
                            Text(
                                "20/26 Section Title",
                                style = AppTheme.customTypography.sectionTitle,
                            )
                            Text(
                                "16/24 Body Text with standard scaling and line height.",
                                style = AppTheme.customTypography.body,
                            )
                            Text(
                                "14/20 Label Text",
                                style = AppTheme.customTypography.label,
                            )
                            Text(
                                "32/38 Metric Tabular: 12,345 kg",
                                style = AppTheme.customTypography.metric,
                            )
                        }
                    }

                    // Button Variants
                    SectionTitle("2. Button Variants")
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.small)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.small),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AppButton(
                                text = "Primary",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                style = AppButtonStyle.Primary,
                                icon = Icons.Default.Add,
                            )
                            AppButton(
                                text = "Secondary",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                style = AppButtonStyle.Secondary,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.small),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AppButton(
                                text = "Destructive",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                style = AppButtonStyle.Destructive,
                            )
                            AppButton(
                                text = "Outlined",
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                style = AppButtonStyle.Outlined,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.small),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AppButton(
                                text = "Loading",
                                onClick = {},
                                loading = true,
                                modifier = Modifier.weight(1f),
                            )
                            AppButton(
                                text = "Disabled State",
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Field States
                    SectionTitle("3. Text Fields")
                    var defaultFieldValue by remember { mutableStateOf("Bench Press") }
                    var errorFieldValue by remember { mutableStateOf("Invalid weight input") }
                    AppTextField(
                        value = defaultFieldValue,
                        onValueChange = { defaultFieldValue = it },
                        label = "Workout Name",
                        helperText = "Enter a descriptive name for your custom routine",
                    )
                    AppTextField(
                        value = errorFieldValue,
                        onValueChange = { errorFieldValue = it },
                        label = "Weight (kg)",
                        errorMessage = "Weight must be a positive number",
                    )

                    // Badges & Tags
                    SectionTitle("4. Badges & Info Tags")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xSmall),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xSmall),
                    ) {
                        AppBadge("Success PR", variant = BadgeVariant.Success)
                        AppBadge("Warning Rest", variant = BadgeVariant.Warning)
                        AppBadge("Error Failed", variant = BadgeVariant.Error)
                        AppBadge("Info HealthSync", variant = BadgeVariant.Info)
                        AppBadge("Neutral Set", variant = BadgeVariant.Neutral)
                    }

                    Text("Non-clickable TagRow (No click action semantics):", style = MaterialTheme.typography.bodySmall)
                    TagRow(tags = listOf("Chest", "Barbell", "Hypertrophy", "Intermediate"))

                    var selectedChip by remember { mutableStateOf<String?>("Chest") }
                    Text("Interactive ChipGroup (Selection state):", style = MaterialTheme.typography.bodySmall)
                    ChipGroup(
                        items = listOf("Chest", "Back", "Legs", "Arms"),
                        selected = selectedChip,
                        onSelected = { selectedChip = it },
                    )

                    // Metrics & Cards
                    SectionTitle("5. Metrics & Cards")
                    MetricRow(
                        metrics = listOf(
                            "Total Volume" to "14,250 kg",
                            "Duration" to "58:42",
                            "Sets Completed" to "24",
                        )
                    )

                    // Dialogs & Bottom Sheets
                    SectionTitle("6. Dialogs & Sheets")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.small),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AppButton(
                            text = "Show Alert Dialog",
                            onClick = { showDialog = true },
                            modifier = Modifier.weight(1f),
                            style = AppButtonStyle.Outlined,
                        )
                        AppButton(
                            text = "Show Bottom Sheet",
                            onClick = { showSheet = true },
                            modifier = Modifier.weight(1f),
                            style = AppButtonStyle.Outlined,
                        )
                    }

                    // Feedback States
                    SectionTitle("7. Feedback & Reusable UI States")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column {
                            EmptyState(
                                title = "No Workout History",
                                detail = "You haven't logged any workouts yet. Start your first workout to track progress.",
                                actionLabel = "Start Workout",
                                onActionClick = {},
                            )
                            LoadingState(message = "Loading workout data...")
                            ErrorState(
                                message = "Unable to connect to sync server. Please check network settings.",
                                onRetry = {},
                            )
                            OfflineState(
                                onRetry = {},
                            )
                        }
                    }

                    Spacer(Modifier.height(AppTheme.spacing.xLarge))
                }
            }

            if (showDialog) {
                AppAlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = "Discard Workout?",
                    text = "Are you sure you want to discard this active workout session? Unsaved progress will be lost.",
                    confirmButtonText = "Discard",
                    onConfirm = { showDialog = false },
                    isDestructive = true,
                )
            }

            if (showSheet) {
                AppModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    title = "Workout Options",
                ) {
                    Text(
                        "Configure rest timers, default plate calculation weights, and auto-logging preferences.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(AppTheme.spacing.small))
                    AppButton(
                        text = "Save Preferences",
                        onClick = { showSheet = false },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Light Theme", showBackground = true)
@Composable
fun PreviewGalleryLight() {
    GymTrackerTheme(themeMode = ThemeMode.LIGHT) {
        PreviewGalleryContent()
    }
}

@Preview(name = "Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewGalleryDark() {
    GymTrackerTheme(themeMode = ThemeMode.DARK) {
        PreviewGalleryContent()
    }
}

@Preview(name = "Large Font Scale", fontScale = 1.5f, showBackground = true)
@Composable
fun PreviewGalleryLargeFont() {
    GymTrackerTheme(themeMode = ThemeMode.LIGHT) {
        PreviewGalleryContent()
    }
}

@Composable
private fun PreviewGalleryContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("Buttons & Controls")
            AppButton(text = "Primary Button Action", onClick = {}, modifier = Modifier.fillMaxWidth())
            AppButton(
                text = "Very Long Button Text Label For Accessibility Wrap Check",
                onClick = {},
                style = AppButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(text = "Destructive", onClick = {}, style = AppButtonStyle.Destructive, modifier = Modifier.fillMaxWidth())

            AppTextField(
                value = "Extremely long field input value exceeding container bounds to verify clipping and wrapping behavior",
                onValueChange = {},
                label = "Long Field Title",
                errorMessage = "Validation error message displaying inline below the input field",
            )

            MetricRow(
                metrics = listOf(
                    "Volume" to "12,345 kg",
                    "Pace" to "02:45 / km",
                )
            )

            TagRow(tags = listOf("Tag 1", "Tag 2", "Long Tag Description Item"))

            EmptyState(
                title = "Empty State Container",
                detail = "Detailed explanation with contextual next action button.",
                actionLabel = "Create Routine",
                onActionClick = {},
            )
        }
    }
}
