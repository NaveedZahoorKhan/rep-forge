package com.gymtracker.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.domain.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    val profile = repository.observeUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.createOrUpdateProfile(profile.copy(onboardingComplete = true))
        }
    }
}

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val storedProfile by viewModel.profile.collectAsStateWithLifecycle()
    var name by remember(storedProfile?.displayName) { mutableStateOf(storedProfile?.displayName ?: "Athlete") }
    var gender by remember(storedProfile?.gender) { mutableStateOf(storedProfile?.gender ?: Gender.OTHER) }
    var unitSystem by remember(storedProfile?.unitSystem) { mutableStateOf(storedProfile?.unitSystem ?: UnitSystem.METRIC) }
    var height by remember(storedProfile?.heightCm) { mutableStateOf((storedProfile?.heightCm ?: 175.0).toString()) }
    var weight by remember(storedProfile?.weightKg) { mutableStateOf((storedProfile?.weightKg ?: 75.0).toString()) }
    var calories by remember(storedProfile?.calorieGoal) { mutableStateOf((storedProfile?.calorieGoal ?: 2200).toString()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GymTracker", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Set the baseline used for calories, units, body stats, and progression targets.")

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach {
                FilterChip(selected = gender == it, onClick = { gender = it }, label = { Text(it.name.lowercase().replaceFirstChar { c -> c.titlecase() }) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitSystem.entries.forEach {
                FilterChip(selected = unitSystem == it, onClick = { unitSystem = it }, label = { Text(if (it == UnitSystem.METRIC) "kg/cm" else "lb/in") })
            }
        }
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height cm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text("Daily calories") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val base = storedProfile ?: UserProfileEntity()
                viewModel.save(
                    base.copy(
                        displayName = name.ifBlank { "Athlete" },
                        gender = gender,
                        unitSystem = unitSystem,
                        heightCm = height.toDoubleOrNull() ?: base.heightCm,
                        weightKg = weight.toDoubleOrNull() ?: base.weightKg,
                        calorieGoal = calories.toIntOrNull() ?: base.calorieGoal,
                    )
                )
                onDone()
            },
        ) {
            Text("Start training")
        }
    }
}
