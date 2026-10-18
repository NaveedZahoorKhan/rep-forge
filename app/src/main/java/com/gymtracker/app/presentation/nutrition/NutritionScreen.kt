package com.gymtracker.app.presentation.nutrition

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.BodyFatFormula
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.BodyFatCalculator
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.presentation.components.WeightLogChartCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.gymtracker.app.presentation.components.SectionTitle
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

    fun saveMeal(id: String?, name: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) {
        val dateEpoch = _selectedDateEpochDay.value
        viewModelScope.launch {
            if (id == null) {
                repository.addMeal(
                    MealEntity(
                        dateEpochDay = dateEpoch,
                        name = name.ifBlank { "Meal" },
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                    )
                )
            } else {
                repository.updateMeal(
                    MealEntity(
                        id = id,
                        dateEpochDay = dateEpoch,
                        name = name.ifBlank { "Meal" },
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat,
                        fiberG = fiber,
                    )
                )
            }
        }
    }

    fun deleteMeal(id: String) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun addWater(deltaMl: Int, goalMl: Int) {
        val dateEpoch = _selectedDateEpochDay.value
        viewModelScope.launch {
            repository.addWater(dateEpochDay = dateEpoch, deltaMl = deltaMl, goalMl = goalMl)
        }
    }

    fun addWeight(weightKg: Double) {
        viewModelScope.launch { repository.addWeightLog(WeightLogEntity(weightKg = weightKg)) }
    }

    fun deleteWeightLog(id: String) {
        viewModelScope.launch { repository.deleteWeightLog(id) }
    }
}

@Composable
fun NutritionScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onNavigateBack != null) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text("Nutrition & Hydration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            NutritionContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
fun NutritionContent(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
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
    var editingMeal by remember { mutableStateOf<MealEntity?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nutrition", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                DateSelectorHeader(
                    selectedDateEpochDay = state.selectedDateEpochDay,
                    onPreviousDay = { viewModel.changeDateByDays(-1) },
                    onNextDay = { viewModel.changeDateByDays(1) },
                    onResetToday = { viewModel.resetToToday() },
                )
            }
        }
        item {
            MacroDashboard(
                log = log,
                water = state.water ?: WaterLogEntity(dateEpochDay = state.selectedDateEpochDay, goalMl = profile.waterGoalMl)
            )
        }
        item {
            MealForm(
                editingMeal = editingMeal,
                onSave = { id, name, calories, protein, carbs, fat, fiber ->
                    viewModel.saveMeal(id, name, calories, protein, carbs, fat, fiber)
                    editingMeal = null
                },
                onCancelEdit = { editingMeal = null }
            )
        }
        if (state.meals.isEmpty()) {
            item {
                Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Text(
                        "No meals logged for this date.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.meals, key = { it.id }) { meal ->
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.name, fontWeight = FontWeight.SemiBold)
                            Text("${meal.calories} kcal • P ${meal.proteinG}g • C ${meal.carbsG}g • F ${meal.fatG}g • Fib ${meal.fiberG}g")
                        }
                        Row {
                            IconButton(onClick = { editingMeal = meal }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Meal", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteMeal(meal.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Meal", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Water & Hydration")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { viewModel.addWater(250, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("+250 ml") }
                    Button(onClick = { viewModel.addWater(500, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("+500 ml") }
                    Button(onClick = { viewModel.addWater(-250, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("-250 ml") }
                }
            }
        }
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

    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) "Today, ${selectedDate.format(formatter)}" else selectedDate.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isToday) {
                    OutlinedButton(
                        onClick = onResetToday,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                    ) {
                        Text("Jump to Today", fontSize = 11.sp)
                    }
                }
            }
            IconButton(onClick = onNextDay) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
            }
        }
    }
}

@Composable
private fun MacroDashboard(log: NutritionLogEntity, water: WaterLogEntity) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${log.calories} / ${log.calorieGoal} kcal", fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(progress = { (log.calories.toFloat() / log.calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Protein ${log.proteinG}/${log.proteinGoalG}g  Carbs ${log.carbsG}/${log.carbsGoalG}g  Fat ${log.fatG}/${log.fatGoalG}g  Fiber ${log.fiberG}/${log.fiberGoalG}g")
            Text("Water ${water.milliliters}/${water.goalMl} ml")
            LinearProgressIndicator(progress = { (water.milliliters.toFloat() / water.goalMl.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MealForm(
    editingMeal: MealEntity?,
    onSave: (id: String?, name: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit,
    onCancelEdit: () -> Unit,
) {
    var name by remember(editingMeal) { mutableStateOf(editingMeal?.name.orEmpty()) }
    var caloriesStr by remember(editingMeal) { mutableStateOf(editingMeal?.calories?.toString().orEmpty()) }
    var proteinStr by remember(editingMeal) { mutableStateOf(editingMeal?.proteinG?.toString().orEmpty()) }
    var carbsStr by remember(editingMeal) { mutableStateOf(editingMeal?.carbsG?.toString().orEmpty()) }
    var fatStr by remember(editingMeal) { mutableStateOf(editingMeal?.fatG?.toString().orEmpty()) }
    var fiberStr by remember(editingMeal) { mutableStateOf(editingMeal?.fiberG?.toString().orEmpty()) }

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

    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(if (editingMeal != null) "Edit Meal" else "Log Meal")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Meal Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Kcal", caloriesStr, { caloriesStr = it }, Modifier.weight(1f))
                NumberField("Protein (g)", proteinStr, { proteinStr = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Carbs (g)", carbsStr, { carbsStr = it }, Modifier.weight(1f))
                NumberField("Fat (g)", fatStr, { fatStr = it }, Modifier.weight(1f))
                NumberField("Fiber (g)", fiberStr, { fiberStr = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (editingMeal != null) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
                Button(
                    onClick = {
                        if (isValid && calories != null && protein != null && carbs != null && fat != null) {
                            onSave(editingMeal?.id, name.trim(), calories, protein, carbs, fat, fiber)
                            if (editingMeal == null) {
                                name = ""
                                caloriesStr = ""
                                proteinStr = ""
                                carbsStr = ""
                                fatStr = ""
                                fiberStr = ""
                            }
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editingMeal != null) "Update Meal" else "Add Meal")
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

    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("BMR / TDEE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BmrFormula.entries.forEach {
                    FilterChip(selected = formula == it, onClick = { formula = it }, label = { Text(it.name.replace('_', ' ')) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Age", ageStr, { ageStr = it }, Modifier.weight(1f))
                NumberField("Activity Multiplier", activityStr, { activityStr = it }, Modifier.weight(1f))
            }
            Text("BMR ${bmr.toInt()} kcal - TDEE ${NutritionCalculator.tdee(bmr, activity).toInt()} kcal")
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
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Body fat estimate")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyFatFormula.entries.forEach {
                    FilterChip(selected = formula == it, onClick = { formula = it }, label = { Text(it.name.replace('_', ' ')) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Neck (cm)", neckStr, { neckStr = it }, Modifier.weight(1f))
                NumberField("Waist (cm)", waistStr, { waistStr = it }, Modifier.weight(1f))
                NumberField("Hips (cm)", hipsStr, { hipsStr = it }, Modifier.weight(1f))
            }
            NumberField("Skinfolds mm", skinfolds, { skinfolds = it }, Modifier.fillMaxWidth())
            NumberField("Age", ageStr, { ageStr = it }, Modifier.fillMaxWidth())
            Text("Estimated ${bodyFat.toInt()}%")
        }
    }
}

@Composable
private fun WeightForm(onAdd: (Double) -> Unit) {
    var weight by remember { mutableDoubleStateOf(75.0) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField("Weight kg", weight.toString(), { weight = it.toDoubleOrNull() ?: weight }, Modifier.weight(1f))
        Button(onClick = { onAdd(weight) }) { Text("Log") }
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
