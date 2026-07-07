package com.gymtracker.app.data.local

import androidx.room.TypeConverter
import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.BodyFatFormula
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.OneRepMaxFormula
import com.gymtracker.app.data.local.entity.PeriodizationType
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.WeekDay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun fromStringList(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter fun fromMuscleGroup(value: MuscleGroup): String = value.name
    @TypeConverter fun toMuscleGroup(value: String): MuscleGroup = enumValueOf(value)

    @TypeConverter fun fromEquipment(value: Equipment): String = value.name
    @TypeConverter fun toEquipment(value: String): Equipment = enumValueOf(value)

    @TypeConverter fun fromDifficulty(value: Difficulty): String = value.name
    @TypeConverter fun toDifficulty(value: String): Difficulty = enumValueOf(value)

    @TypeConverter fun fromSetType(value: SetType): String = value.name
    @TypeConverter fun toSetType(value: String): SetType = enumValueOf(value)

    @TypeConverter fun fromSessionStatus(value: SessionStatus): String = value.name
    @TypeConverter fun toSessionStatus(value: String): SessionStatus = enumValueOf(value)

    @TypeConverter fun fromUnitSystem(value: UnitSystem): String = value.name
    @TypeConverter fun toUnitSystem(value: String): UnitSystem = enumValueOf(value)

    @TypeConverter fun fromThemeMode(value: ThemeMode): String = value.name
    @TypeConverter fun toThemeMode(value: String): ThemeMode = enumValueOf(value)

    @TypeConverter fun fromGender(value: Gender): String = value.name
    @TypeConverter fun toGender(value: String): Gender = enumValueOf(value)

    @TypeConverter fun fromWeekDay(value: WeekDay): String = value.name
    @TypeConverter fun toWeekDay(value: String): WeekDay = enumValueOf(value)

    @TypeConverter fun fromPeriodizationType(value: PeriodizationType): String = value.name
    @TypeConverter fun toPeriodizationType(value: String): PeriodizationType = enumValueOf(value)

    @TypeConverter fun fromOneRepMaxFormula(value: OneRepMaxFormula): String = value.name
    @TypeConverter fun toOneRepMaxFormula(value: String): OneRepMaxFormula = enumValueOf(value)

    @TypeConverter fun fromBmrFormula(value: BmrFormula): String = value.name
    @TypeConverter fun toBmrFormula(value: String): BmrFormula = enumValueOf(value)

    @TypeConverter fun fromBodyFatFormula(value: BodyFatFormula): String = value.name
    @TypeConverter fun toBodyFatFormula(value: String): BodyFatFormula = enumValueOf(value)
}
