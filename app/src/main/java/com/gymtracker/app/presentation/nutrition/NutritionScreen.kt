package com.gymtracker.app.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.BodyFatFormula
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.BodyFatCalculator
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.WeightLogChartCard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val selectedDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val profile: UserProfileEntity? = null,
    val log: NutritionLogEntity? = null,
    val meals: List<MealEntity> = emptyList(),
    val water: WaterLogEntity? = null,
    val weights: List<WeightLogEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    private val _selectedDateEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDateEpochDay: StateFlow<Long> = _selectedDateEpochDay

    val state: StateFlow<NutritionUiState> = combine(
        _selectedDateEpochDay,
        repository.observeUserProfile(),
        repository.observeWeightLogs(),
    ) { dateEpoch, profile, weights ->
        Triple(dateEpoch, profile, weights)
    }.flatMapLatest { (dateEpoch, profile, weights) ->
        combine(
            repository.observeNutritionLog(dateEpoch),
            repository.observeMeals(dateEpoch),
            repository.observeWater(dateEpoch),
        ) { log, meals, water ->
            NutritionUiState(
                selectedDateEpochDay = dateEpoch,
                profile = profile,
                log = log,
                meals = meals,
                water = water,
                weights = weights,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    fun setSelectedDate(dateEpochDay: Long) {
        _selectedDateEpochDay.value = dateEpochDay
    }

    fun changeDateByDays(days: Long) {
        _selectedDateEpochDay.value += days
    }

    fun resetToToday() {
        _selectedDateEpochDay.value = LocalDate.now().toEpochDay()
    }

    var recentlyDeletedMeal: MealEntity? = null
        private set

    fun saveMeal(
        id: String?,
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        serving: String = "1 serving",
        mealCategory: String = "Snacks",
    ) {
        val dateEpoch = _selectedDateEpochDay.value
        viewModelScope.launch {
            if (id == null) {
                repository.addMeal(
                    MealEntity(
                        dateEpochDay = dateEpoch,
                        name = name.ifBlank { "Food item" },
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                        serving = serving.ifBlank { "1 serving" },
                        mealCategory = mealCategory,
                    )
                )
            } else {
                repository.updateMeal(
                    MealEntity(
                        id = id,
                        dateEpochDay = dateEpoch,
                        name = name.ifBlank { "Food item" },
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                        serving = serving.ifBlank { "1 serving" },
                        mealCategory = mealCategory,
                    )
                )
            }
        }
    }

    fun deleteMeal(id: String, onDeleted: ((MealEntity) -> Unit)? = null) {
        viewModelScope.launch {
            val meal = state.value.meals.firstOrNull { it.id == id }
            if (meal != null) {
                recentlyDeletedMeal = meal
                repository.deleteMeal(id)
                onDeleted?.invoke(meal)
            }
        }
    }

    fun restoreDeletedMeal() {
        val meal = recentlyDeletedMeal ?: return
        viewModelScope.launch {
            repository.addMeal(meal)
            recentlyDeletedMeal = null
        }
    }

    fun addWater(deltaMl: Int, goalMl: Int) {
        val dateEpoch = _selectedDateEpochDay.value
        viewModelScope.launch {
            repository.addWater(dateEpochDay = dateEpoch, deltaMl = deltaMl, goalMl = goalMl)
        }
    }

    fun setWaterTotal(milliliters: Int, goalMl: Int) {
        val dateEpoch = _selectedDateEpochDay.value
        viewModelScope.launch {
            val validMl = milliliters.coerceAtLeast(0)
            repository.upsertWater(WaterLogEntity(dateEpochDay = dateEpoch, milliliters = validMl, goalMl = goalMl))
        }
    }

    fun addWeight(weightKg: Double) {
        viewModelScope.launch { repository.addWeightLog(WeightLogEntity(weightKg = weightKg)) }
    }

    fun deleteWeightLog(id: String) {
        viewModelScope.launch { repository.deleteWeightLog(id) }
    }
}

val MEAL_CATEGORIES = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

@Composable
fun NutritionScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NutritionContent(state = state, viewModel = viewModel, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NutritionContent(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
    onNavigateBack: (() -> Unit)? = null,
) {
    val profile = state.profile ?: UserProfileEntity()
    val log = state.log ?: NutritionLogEntity(
        dateEpochDay = state.selectedDateEpochDay,
        calorieGoal = profile.calorieGoal,
        proteinGoalG = profile.proteinGoalG,
        carbsGoalG = profile.carbsGoalG,
        fatGoalG = profile.fatGoalG,
        fiberGoalG = profile.fiberGoalG,
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeMealCategory by remember { mutableStateOf<String?>(null) }
    var editingMeal by remember { mutableStateOf<MealEntity?>(null) }
    var showFoodSheet by remember { mutableStateOf(false) }

    var showEditWaterDialog by remember { mutableStateOf(false) }
    var waterInputStr by remember { mutableStateOf("") }

    val formattedDateHeader = remember(state.selectedDateEpochDay) {
        val localDate = LocalDate.ofEpochDay(state.selectedDateEpochDay)
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
        if (localDate == today) "Today, ${localDate.format(formatter)}"
        else localDate.format(formatter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Daily Nutrition & Water",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            formattedDateHeader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Date Selector Navigation Bar
            item {
                DateSelectorHeader(
                    selectedDateEpochDay = state.selectedDateEpochDay,
                    onPreviousDay = { viewModel.changeDateByDays(-1) },
                    onNextDay = { viewModel.changeDateByDays(1) },
                    onResetToday = { viewModel.resetToToday() },
                )
            }

            // 2. Compact Calories & Macros Dashboard with clear target separation
            item {
                MacroDashboard(
                    log = log,
                    water = state.water ?: WaterLogEntity(dateEpochDay = state.selectedDateEpochDay, goalMl = profile.waterGoalMl),
                    profile = profile
                )
            }

            // 3. Water Control Section
            item {
                WaterTrackerCard(
                    water = state.water ?: WaterLogEntity(dateEpochDay = state.selectedDateEpochDay, goalMl = profile.waterGoalMl),
                    goalMl = profile.waterGoalMl,
                    onAddWater = { delta -> viewModel.addWater(delta, profile.waterGoalMl) },
                    onEditWater = {
                        waterInputStr = (state.water?.milliliters ?: 0).toString()
                        showEditWaterDialog = true
                    }
                )
            }

            // 4. Meal Sections (Breakfast, Lunch, Dinner, Snacks)
            item {
                SectionTitle("Meals & Food Log")
            }

            items(MEAL_CATEGORIES, key = { it }) { category ->
                val categoryMeals = state.meals.filter {
                    it.mealCategory.equals(category, ignoreCase = true) ||
                            (category == "Snacks" && !MEAL_CATEGORIES.any { cat -> cat.equals(it.mealCategory, ignoreCase = true) })
                }
                val categoryCalories = categoryMeals.sumOf { it.calories }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (categoryMeals.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "$categoryCalories kcal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (categoryMeals.isEmpty()) {
                            Text(
                                "No items logged for $category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                categoryMeals.forEach { meal ->
                                    FoodRowItem(
                                        meal = meal,
                                        onEdit = {
                                            editingMeal = meal
                                            activeMealCategory = category
                                            showFoodSheet = true
                                        },
                                        onDelete = {
                                            viewModel.deleteMeal(meal.id) { deletedMeal ->
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Deleted ${deletedMeal.name}",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.restoreDeletedMeal()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                editingMeal = null
                                activeMealCategory = category
                                showFoodSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add food to $category")
                        }
                    }
                }
            }

            // 5. Calculators & Body Measurement Charts
            item { BmrTdeePanel(profile) }
            item { BodyFatPanel(profile) }
            item {
                WeightLogChartCard(
                    weights = state.weights,
                    unitSystem = profile.unitSystem,
                    onAddWeight = viewModel::addWeight,
                    onDeleteWeight = viewModel::deleteWeightLog,
                )
            }
        }
    }

    // Food Add/Edit Sheet
    if (showFoodSheet) {
        MealFormBottomSheet(
            editingMeal = editingMeal,
            defaultCategory = activeMealCategory ?: "Snacks",
            selectedDateEpochDay = state.selectedDateEpochDay,
            onDismiss = {
                showFoodSheet = false
                editingMeal = null
            },
            onSave = { id, name, calories, protein, carbs, fat, fiber, serving, category ->
                viewModel.saveMeal(id, name, calories, protein, carbs, fat, fiber, serving, category)
                showFoodSheet = false
                editingMeal = null
            }
        )
    }

    // Water Edit Dialog
    if (showEditWaterDialog) {
        AlertDialog(
            onDismissRequest = { showEditWaterDialog = false },
            title = { Text("Set Water Total") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter total water consumed today in milliliters (ml):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = waterInputStr,
                        onValueChange = { waterInputStr = it },
                        label = { Text("Water (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = waterInputStr.toIntOrNull() ?: 0
                        viewModel.setWaterTotal(parsed, profile.waterGoalMl)
                        showEditWaterDialog = false
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditWaterDialog = false },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DateSelectorHeader(
    selectedDateEpochDay: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToday: () -> Unit,
) {
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    val today = LocalDate.now()
    val isToday = selectedDate == today
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Previous Day"
                        role = Role.Button
                    }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isToday) "Today, ${selectedDate.format(formatter)}" else selectedDate.format(formatter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isToday) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onResetToday,
                        modifier = Modifier
                            .height(28.dp)
                            .defaultMinSize(minWidth = 48.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Jump to Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Next Day"
                        role = Role.Button
                    }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroDashboard(
    log: NutritionLogEntity,
    water: WaterLogEntity,
    profile: UserProfileEntity,
) {
    val calorieTargetStr = if (log.calorieGoal > 0) "${log.calorieGoal} kcal" else "Not set"
    val proteinTargetStr = if (log.proteinGoalG > 0) "${log.proteinGoalG.toInt()}g" else "Not set"
    val carbsTargetStr = if (log.carbsGoalG > 0) "${log.carbsGoalG.toInt()}g" else "Not set"
    val fatTargetStr = if (log.fatGoalG > 0) "${log.fatGoalG.toInt()}g" else "Not set"
    val fiberTargetStr = if (log.fiberGoalG > 0) "${log.fiberGoalG.toInt()}g" else "Not set"

    val calorieProgress = if (log.calorieGoal > 0) (log.calories.toFloat() / log.calorieGoal).coerceIn(0f, 1f) else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Primary Calories Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Calories Consumed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${log.calories}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "kcal",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        calorieTargetStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (log.calorieGoal > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { calorieProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (log.calorieGoal > 0) "${(calorieProgress * 100).toInt()}% of daily target" else "Target not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (log.calorieGoal > 0) {
                        val remaining = log.calorieGoal - log.calories
                        Text(
                            if (remaining >= 0) "$remaining kcal remaining" else "${-remaining} kcal over target",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Macros Breakdown Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroChip(label = "Protein", consumed = "${log.proteinG.toInt()}g", target = proteinTargetStr, modifier = Modifier.weight(1f))
                MacroChip(label = "Carbs", consumed = "${log.carbsG.toInt()}g", target = carbsTargetStr, modifier = Modifier.weight(1f))
                MacroChip(label = "Fat", consumed = "${log.fatG.toInt()}g", target = fatTargetStr, modifier = Modifier.weight(1f))
                MacroChip(label = "Fiber", consumed = "${log.fiberG.toInt()}g", target = fiberTargetStr, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    consumed: String,
    target: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(consumed, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Target: $target", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun WaterTrackerCard(
    water: WaterLogEntity,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    onEditWater: () -> Unit,
) {
    val progress = if (goalMl > 0) (water.milliliters.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val goalStr = if (goalMl > 0) "$goalMl ml" else "Not set"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text("Hydration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${water.milliliters} / $goalStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(
                    onClick = onEditWater,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Water Total", tint = MaterialTheme.colorScheme.primary)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onAddWater(250) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+250 ml")
                }
                Button(
                    onClick = { onAddWater(500) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+500 ml")
                }
                OutlinedButton(
                    onClick = { onAddWater(-250) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("-250 ml")
                }
            }
        }
    }
}

@Composable
private fun FoodRowItem(
    meal: MealEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meal.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${meal.serving} • ${meal.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "P ${meal.proteinG.toInt()}g • C ${meal.carbsG.toInt()}g • F ${meal.fatG.toInt()}g • Fib ${meal.fiberG.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${meal.name}", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${meal.name}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MealFormBottomSheet(
    editingMeal: MealEntity?,
    defaultCategory: String,
    selectedDateEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        serving: String,
        category: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editingMeal) { mutableStateOf(editingMeal?.name.orEmpty()) }
    var serving by remember(editingMeal) { mutableStateOf(editingMeal?.serving ?: "1 serving") }
    var category by remember(editingMeal) { mutableStateOf(editingMeal?.mealCategory ?: defaultCategory) }

    var caloriesStr by remember(editingMeal) { mutableStateOf(editingMeal?.calories?.toString().orEmpty()) }
    var proteinStr by remember(editingMeal) { mutableStateOf(editingMeal?.proteinG?.toString().orEmpty()) }
    var carbsStr by remember(editingMeal) { mutableStateOf(editingMeal?.carbsG?.toString().orEmpty()) }
    var fatStr by remember(editingMeal) { mutableStateOf(editingMeal?.fatG?.toString().orEmpty()) }
    var fiberStr by remember(editingMeal) { mutableStateOf(editingMeal?.fiberG?.toString().orEmpty()) }

    var isAttemptedSubmit by remember { mutableStateOf(false) }

    val calories = caloriesStr.toIntOrNull()
    val protein = proteinStr.toDoubleOrNull()
    val carbs = carbsStr.toDoubleOrNull()
    val fat = fatStr.toDoubleOrNull()
    val fiber = fiberStr.toDoubleOrNull() ?: 0.0

    val isNameValid = name.isNotBlank()
    val areMacrosValid = calories != null && calories >= 0 &&
            protein != null && protein >= 0.0 &&
            carbs != null && carbs >= 0.0 &&
            fat != null && fat >= 0.0 &&
            fiber >= 0.0
    val isValid = isNameValid && areMacrosValid

    val selectedDateText = remember(selectedDateEpochDay) {
        val d = LocalDate.ofEpochDay(selectedDateEpochDay)
        d.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = if (editingMeal != null) "Edit Food Entry" else "Add Food Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Logging for $selectedDateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Meal Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name") },
                placeholder = { Text("e.g., Grilled Chicken Breast") },
                isError = isAttemptedSubmit && !isNameValid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = serving,
                onValueChange = { serving = it },
                label = { Text("Serving Size") },
                placeholder = { Text("e.g., 200g, 1 cup, 2 slices") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Calories (kcal)", caloriesStr, { caloriesStr = it }, Modifier.weight(1f))
                NumberField("Protein (g)", proteinStr, { proteinStr = it }, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Carbs (g)", carbsStr, { carbsStr = it }, Modifier.weight(1f))
                NumberField("Fat (g)", fatStr, { fatStr = it }, Modifier.weight(1f))
                NumberField("Fiber (g)", fiberStr, { fiberStr = it }, Modifier.weight(1f))
            }

            if (isAttemptedSubmit && !isValid) {
                Text(
                    "Please fill in valid non-negative numbers for Calories, Protein, Carbs, and Fat.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        isAttemptedSubmit = true
                        if (isValid && calories != null && protein != null && carbs != null && fat != null) {
                            onSave(editingMeal?.id, name.trim(), calories, protein, carbs, fat, fiber, serving.trim(), category)
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (editingMeal != null) "Update Food" else "Add Food")
                }
            }
        }
    }
}

@Composable
private fun BmrTdeePanel(profile: UserProfileEntity) {
    var formula by remember { mutableStateOf(BmrFormula.MIFFLIN_ST_JEOR) }
    val defaultAge = profile.birthYear?.let { LocalDate.now().year - it } ?: 28
    var ageStr by remember(profile.birthYear) { mutableStateOf(defaultAge.toString()) }
    var activityStr by remember { mutableStateOf("1.55") }

    val age = ageStr.toIntOrNull() ?: defaultAge
    val activity = activityStr.toDoubleOrNull() ?: 1.55
    val bmr = NutritionCalculator.bmr(profile.gender, profile.weightKg, profile.heightCm, age, formula)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("BMR & TDEE Calculator")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BmrFormula.entries.forEach {
                    FilterChip(
                        selected = formula == it,
                        onClick = { formula = it },
                        label = { Text(it.name.replace('_', ' ')) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Age (years)", ageStr, { ageStr = it }, Modifier.weight(1f))
                NumberField("Activity Multiplier", activityStr, { activityStr = it }, Modifier.weight(1f))
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "BMR: ${bmr.toInt()} kcal • TDEE: ${NutritionCalculator.tdee(bmr, activity).toInt()} kcal",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BodyFatPanel(profile: UserProfileEntity) {
    var formula by remember { mutableStateOf(BodyFatFormula.US_NAVY) }
    var neckStr by remember { mutableStateOf("38.0") }
    var waistStr by remember { mutableStateOf("82.0") }
    var hipsStr by remember { mutableStateOf("95.0") }
    val defaultAge = profile.birthYear?.let { LocalDate.now().year - it } ?: 28
    var ageStr by remember(profile.birthYear) { mutableStateOf(defaultAge.toString()) }
    var skinfolds by remember { mutableStateOf("10,12,14,16,18,20,22") }

    val neck = neckStr.toDoubleOrNull() ?: 38.0
    val waist = waistStr.toDoubleOrNull() ?: 82.0
    val hips = hipsStr.toDoubleOrNull() ?: 95.0
    val age = ageStr.toIntOrNull() ?: defaultAge

    val bodyFat = when (formula) {
        BodyFatFormula.US_NAVY -> BodyFatCalculator.estimateUsNavy(profile.gender, profile.heightCm, neck, waist, hips)
        BodyFatFormula.JACKSON_POLLOCK_3 -> BodyFatCalculator.estimateJacksonPollock(profile.gender, age, skinfolds.split(',').mapNotNull { it.trim().toDoubleOrNull() }.take(3), formula)
        BodyFatFormula.JACKSON_POLLOCK_7 -> BodyFatCalculator.estimateJacksonPollock(profile.gender, age, skinfolds.split(',').mapNotNull { it.trim().toDoubleOrNull() }.take(7), formula)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Body Fat Estimation")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyFatFormula.entries.forEach {
                    FilterChip(
                        selected = formula == it,
                        onClick = { formula = it },
                        label = { Text(it.name.replace('_', ' ')) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Neck (cm)", neckStr, { neckStr = it }, Modifier.weight(1f))
                NumberField("Waist (cm)", waistStr, { waistStr = it }, Modifier.weight(1f))
                NumberField("Hips (cm)", hipsStr, { hipsStr = it }, Modifier.weight(1f))
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Estimated Body Fat: ${bodyFat.toInt()}%",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
    )
}
