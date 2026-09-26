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
    @TypeConverter fun toMuscleGroup(value: String): MuscleGroup =
        MuscleGroup.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MuscleGroup.FULL_BODY

    @TypeConverter fun fromEquipment(value: Equipment): String = value.name
    @TypeConverter fun toEquipment(value: String): Equipment =
        Equipment.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Equipment.BARBELL

    @TypeConverter fun fromDifficulty(value: Difficulty): String = value.name
    @TypeConverter fun toDifficulty(value: String): Difficulty =
        Difficulty.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Difficulty.BEGINNER

    @TypeConverter fun fromSetType(value: SetType): String = value.name
    @TypeConverter fun toSetType(value: String): SetType =
        SetType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SetType.NORMAL

    @TypeConverter fun fromSessionStatus(value: SessionStatus): String = value.name
    @TypeConverter fun toSessionStatus(value: String): SessionStatus =
        SessionStatus.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SessionStatus.ACTIVE

    @TypeConverter fun fromUnitSystem(value: UnitSystem): String = value.name
    @TypeConverter fun toUnitSystem(value: String): UnitSystem =
        UnitSystem.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UnitSystem.METRIC

    @TypeConverter fun fromThemeMode(value: ThemeMode): String = value.name
    @TypeConverter fun toThemeMode(value: String): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ThemeMode.SYSTEM

    @TypeConverter fun fromGender(value: Gender): String = value.name
    @TypeConverter fun toGender(value: String): Gender =
        Gender.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Gender.MALE

    @TypeConverter fun fromWeekDay(value: WeekDay?): String? = value?.name
    @TypeConverter fun toWeekDay(value: String?): WeekDay? =
        if (value.isNullOrBlank()) null else WeekDay.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

    @TypeConverter fun fromPeriodizationType(value: PeriodizationType): String = value.name
    @TypeConverter fun toPeriodizationType(value: String): PeriodizationType =
        PeriodizationType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PeriodizationType.NONE

    @TypeConverter fun fromOneRepMaxFormula(value: OneRepMaxFormula): String = value.name
    @TypeConverter fun toOneRepMaxFormula(value: String): OneRepMaxFormula =
        OneRepMaxFormula.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OneRepMaxFormula.EPLEY

    @TypeConverter fun fromBmrFormula(value: BmrFormula): String = value.name
    @TypeConverter fun toBmrFormula(value: String): BmrFormula =
        BmrFormula.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BmrFormula.MIFFLIN_ST_JEOR

    @TypeConverter fun fromBodyFatFormula(value: BodyFatFormula): String = value.name
    @TypeConverter fun toBodyFatFormula(value: String): BodyFatFormula =
        BodyFatFormula.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BodyFatFormula.US_NAVY
}
