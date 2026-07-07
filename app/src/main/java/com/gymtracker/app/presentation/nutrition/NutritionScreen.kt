package com.gymtracker.app.presentation.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.gymtracker.app.presentation.components.MultiLineChartCard
import com.gymtracker.app.presentation.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val profile: UserProfileEntity? = null,
    val log: NutritionLogEntity? = null,
    val meals: List<MealEntity> = emptyList(),
    val water: WaterLogEntity? = null,
    val weights: List<WeightLogEntity> = emptyList(),
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    private val today = LocalDate.now().toEpochDay()
    val state: StateFlow<NutritionUiState> = combine(
        repository.observeUserProfile(),
        repository.observeNutritionLog(today),
        repository.observeMeals(today),
        repository.observeWater(today),
        repository.observeWeightLogs(),
    ) { profile, log, meals, water, weights ->
        NutritionUiState(profile, log, meals, water, weights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    fun addMeal(name: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) {
        viewModelScope.launch {
            repository.addMeal(
                MealEntity(
                    dateEpochDay = today,
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

    fun addWater(deltaMl: Int, goalMl: Int) {
        val current = state.value.water ?: WaterLogEntity(dateEpochDay = today, goalMl = goalMl)
        viewModelScope.launch {
            repository.upsertWater(current.copy(milliliters = (current.milliliters + deltaMl).coerceAtLeast(0), goalMl = goalMl))
        }
    }

    fun addWeight(weightKg: Double) {
        viewModelScope.launch { repository.addWeightLog(WeightLogEntity(weightKg = weightKg)) }
    }
}

@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NutritionContent(state = state, viewModel = viewModel)
}

@Composable
fun NutritionContent(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
) {
    val profile = state.profile ?: UserProfileEntity()
    val log = state.log ?: NutritionLogEntity(
        dateEpochDay = LocalDate.now().toEpochDay(),
        calorieGoal = profile.calorieGoal,
        proteinGoalG = profile.proteinGoalG,
        carbsGoalG = profile.carbsGoalG,
        fatGoalG = profile.fatGoalG,
        fiberGoalG = profile.fiberGoalG,
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Nutrition", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            MacroDashboard(log = log, water = state.water ?: WaterLogEntity(dateEpochDay = LocalDate.now().toEpochDay(), goalMl = profile.waterGoalMl))
        }
        item { MealForm(onAdd = viewModel::addMeal) }
        items(state.meals, key = { it.id }) { meal ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(meal.name, fontWeight = FontWeight.SemiBold)
                    Text("${meal.calories} kcal - P ${meal.proteinG} C ${meal.carbsG} F ${meal.fatG} Fiber ${meal.fiberG}")
                }
            }
        }
        item {
            SectionTitle("Water")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.addWater(250, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("+250 ml") }
                Button(onClick = { viewModel.addWater(500, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("+500 ml") }
                Button(onClick = { viewModel.addWater(-250, profile.waterGoalMl) }, modifier = Modifier.weight(1f)) { Text("-250 ml") }
            }
        }
        item { BmrTdeePanel(profile) }
        item { BodyFatPanel(profile.gender, profile.heightCm) }
        item {
            SectionTitle("Weight log")
            WeightForm(onAdd = viewModel::addWeight)
            val chronological = state.weights.sortedBy { it.loggedAt }
            MultiLineChartCard(listOf(chronological.map { it.weightKg }, chronological.mapNotNull { it.movingAverageKg }))
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
private fun MealForm(onAdd: (String, Int, Double, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("Chicken rice bowl") }
    var calories by remember { mutableIntStateOf(650) }
    var protein by remember { mutableDoubleStateOf(45.0) }
    var carbs by remember { mutableDoubleStateOf(75.0) }
    var fat by remember { mutableDoubleStateOf(18.0) }
    var fiber by remember { mutableDoubleStateOf(8.0) }
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Meal logging")
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Meal") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Kcal", calories.toString(), { calories = it.toIntOrNull() ?: calories }, Modifier.weight(1f))
                NumberField("Protein", protein.toString(), { protein = it.toDoubleOrNull() ?: protein }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Carbs", carbs.toString(), { carbs = it.toDoubleOrNull() ?: carbs }, Modifier.weight(1f))
                NumberField("Fat", fat.toString(), { fat = it.toDoubleOrNull() ?: fat }, Modifier.weight(1f))
                NumberField("Fiber", fiber.toString(), { fiber = it.toDoubleOrNull() ?: fiber }, Modifier.weight(1f))
            }
            Button(onClick = { onAdd(name, calories, protein, carbs, fat, fiber) }, modifier = Modifier.fillMaxWidth()) { Text("Add meal") }
        }
    }
}

@Composable
private fun BmrTdeePanel(profile: UserProfileEntity) {
    var formula by remember { mutableStateOf(BmrFormula.MIFFLIN_ST_JEOR) }
    var age by remember { mutableIntStateOf(30) }
    var activity by remember { mutableDoubleStateOf(1.55) }
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
                NumberField("Age", age.toString(), { age = it.toIntOrNull() ?: age }, Modifier.weight(1f))
                NumberField("Activity", activity.toString(), { activity = it.toDoubleOrNull() ?: activity }, Modifier.weight(1f))
            }
            Text("BMR ${bmr.toInt()} kcal - TDEE ${NutritionCalculator.tdee(bmr, activity).toInt()} kcal")
        }
    }
}

@Composable
private fun BodyFatPanel(gender: Gender, heightCm: Double) {
    var formula by remember { mutableStateOf(BodyFatFormula.US_NAVY) }
    var neck by remember { mutableDoubleStateOf(38.0) }
    var waist by remember { mutableDoubleStateOf(82.0) }
    var hips by remember { mutableDoubleStateOf(95.0) }
    var age by remember { mutableIntStateOf(30) }
    var skinfolds by remember { mutableStateOf("10,12,14,16,18,20,22") }
    val bodyFat = when (formula) {
        BodyFatFormula.US_NAVY -> BodyFatCalculator.estimateUsNavy(gender, heightCm, neck, waist, hips)
        BodyFatFormula.JACKSON_POLLOCK_3 -> BodyFatCalculator.estimateJacksonPollock(gender, age, skinfolds.split(',').mapNotNull { it.trim().toDoubleOrNull() }.take(3), formula)
        BodyFatFormula.JACKSON_POLLOCK_7 -> BodyFatCalculator.estimateJacksonPollock(gender, age, skinfolds.split(',').mapNotNull { it.trim().toDoubleOrNull() }.take(7), formula)
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
                NumberField("Neck", neck.toString(), { neck = it.toDoubleOrNull() ?: neck }, Modifier.weight(1f))
                NumberField("Waist", waist.toString(), { waist = it.toDoubleOrNull() ?: waist }, Modifier.weight(1f))
                NumberField("Hips", hips.toString(), { hips = it.toDoubleOrNull() ?: hips }, Modifier.weight(1f))
            }
            NumberField("Skinfolds mm", skinfolds, { skinfolds = it }, Modifier.fillMaxWidth())
            NumberField("Age", age.toString(), { age = it.toIntOrNull() ?: age }, Modifier.fillMaxWidth())
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
