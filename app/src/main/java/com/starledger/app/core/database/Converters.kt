package com.starledger.app.core.database

import androidx.room.TypeConverter
import com.starledger.app.core.model.AccountType
import com.starledger.app.core.model.CycleStatus
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.RuleType
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.model.TxType

private const val LIST_SEPARATOR = ""

class Converters {

    @TypeConverter
    fun fromTxType(value: TxType): String = value.name

    @TypeConverter
    fun toTxType(value: String): TxType = TxType.valueOf(value)

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCycleStatus(value: CycleStatus): String = value.name

    @TypeConverter
    fun toCycleStatus(value: String): CycleStatus = CycleStatus.valueOf(value)

    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun fromEnvelopeType(value: EnvelopeType): String = value.name

    @TypeConverter
    fun toEnvelopeType(value: String): EnvelopeType = EnvelopeType.valueOf(value)

    @TypeConverter
    fun fromPlanStatus(value: PlanStatus): String = value.name

    @TypeConverter
    fun toPlanStatus(value: String): PlanStatus = PlanStatus.valueOf(value)

    @TypeConverter
    fun fromStarColorState(value: StarColorState): String = value.name

    @TypeConverter
    fun toStarColorState(value: String): StarColorState = StarColorState.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_SEPARATOR)
}
