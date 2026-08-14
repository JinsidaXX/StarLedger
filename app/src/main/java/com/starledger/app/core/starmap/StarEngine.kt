package com.starledger.app.core.starmap

import com.starledger.app.core.model.BudgetCycle
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.model.TimeUtil
import org.json.JSONArray
import org.json.JSONObject

/** 单条星芒数据（一个信封对应一条星芒） */
data class StarRayData(
    val envelopeId: Long,
    val name: String,
    val planned: Long,
    val actual: Long,
    val color: Long,
    val envelopeType: String,
    /** 实际/计划 */
    val ratio: Float,
)

/** 计算后的恒星完整数据 */
data class StarComputed(
    val cycleId: Long,
    val year: Int,
    val month: Int,
    val brightness: Float,
    val colorState: StarColorState,
    val observationCompleteness: Float,
    val allocationCompletion: Float,
    val budgetStatus: Float,
    val reviewCompleted: Boolean,
    val surplusAmount: Long,
    val surplusHandled: Boolean,
    val markedUnrecorded: Boolean,
    val rays: List<StarRayData>,
    val activePlanCount: Int,
    val totalIncome: Long,
    val totalExpense: Long,
    val hasTransactions: Boolean,
)

/**
 * 星图引擎：由周期数据本地计算恒星。
 * 星辉不只看结余，还综合分配完成度、记录完整度、复盘和结余。
 * 结余对亮度的贡献封顶 20%，防止用户故意设高预算刷亮。
 */
object StarEngine {

    const val MAX_RAYS = 8

    fun compute(
        cycle: BudgetCycle,
        envelopes: List<BudgetEnvelope>,
        expense: Long,
        activePlanCount: Int,
        activeDays: Int,
        hasTransactions: Boolean,
    ): StarComputed {
        val allocationCompletion = if (cycle.totalIncome > 0) {
            (cycle.totalAllocated.toFloat() / cycle.totalIncome).coerceIn(0f, 1f)
        } else 0f

        // 记录了就是好：只要当月有账目，即视为记录完整，不再按记账天数打折
        val observationCompleteness = when {
            cycle.markedUnrecorded -> 0f
            !hasTransactions -> 0f
            else -> 1f
        }

        // 总体预算状态：有分类信封的实际/计划合计
        val trackedEnvelopes = envelopes.filter { it.categoryId != null && it.plannedAmount > 0 }
        val sumPlanned = trackedEnvelopes.sumOf { it.plannedAmount }
        val sumActual = trackedEnvelopes.sumOf { it.actualAmount }
        val budgetStatus = if (sumPlanned > 0) sumActual.toFloat() / sumPlanned else 0f

        // 结余：收入 - 支出
        val surplusAmount = if (cycle.totalIncome > 0) {
            cycle.totalIncome - expense
        } else 0L

        // 结余对亮度的贡献：封顶 20%
        val surplusRatio = if (cycle.totalIncome > 0) {
            (surplusAmount.toFloat() / cycle.totalIncome).coerceIn(0f, 0.2f)
        } else 0f

        val brightness = (
            0.30f +
                allocationCompletion * 0.25f +
                observationCompleteness * 0.25f +
                (if (cycle.reviewCompleted) 0.10f else 0f) +
                surplusRatio / 0.2f * 0.10f
            ).coerceIn(0f, 1f)

        val colorState = when {
            observationCompleteness < 0.3f -> StarColorState.FOG
            budgetStatus > 1.5f -> StarColorState.RED
            budgetStatus > 1.2f -> StarColorState.ORANGE
            budgetStatus > 1.1f -> StarColorState.WARM
            else -> StarColorState.BLUE
        }

        val rays = envelopes
            .sortedBy { it.sortOrder }
            .filter { it.plannedAmount > 0 }
            .take(MAX_RAYS)
            .map { env ->
                StarRayData(
                    envelopeId = env.id,
                    name = env.name,
                    planned = env.plannedAmount,
                    actual = env.actualAmount,
                    color = env.color,
                    envelopeType = env.type.name,
                    ratio = if (env.plannedAmount > 0) env.actualAmount.toFloat() / env.plannedAmount else 0f,
                )
            }

        return StarComputed(
            cycleId = cycle.id,
            year = cycle.year,
            month = cycle.month,
            brightness = brightness,
            colorState = colorState,
            observationCompleteness = observationCompleteness,
            allocationCompletion = allocationCompletion,
            budgetStatus = budgetStatus,
            reviewCompleted = cycle.reviewCompleted,
            surplusAmount = surplusAmount,
            surplusHandled = cycle.surplusHandled,
            markedUnrecorded = cycle.markedUnrecorded,
            rays = rays,
            activePlanCount = activePlanCount,
            totalIncome = cycle.totalIncome,
            totalExpense = expense,
            hasTransactions = hasTransactions,
        )
    }

    fun toEntity(computed: StarComputed): MonthlyStar = MonthlyStar(
        cycleId = computed.cycleId,
        year = computed.year,
        month = computed.month,
        brightness = computed.brightness,
        colorState = computed.colorState,
        observationCompleteness = computed.observationCompleteness,
        allocationCompletion = computed.allocationCompletion,
        budgetStatus = computed.budgetStatus,
        reviewCompleted = computed.reviewCompleted,
        surplusAmount = computed.surplusAmount,
        snapshotData = raysToJson(computed.rays),
        updatedAt = System.currentTimeMillis(),
    )

    fun raysToJson(rays: List<StarRayData>): String {
        val arr = JSONArray()
        rays.forEach { ray ->
            arr.put(
                JSONObject()
                    .put("envelopeId", ray.envelopeId)
                    .put("name", ray.name)
                    .put("planned", ray.planned)
                    .put("actual", ray.actual)
                    .put("color", ray.color)
                    .put("envelopeType", ray.envelopeType)
                    .put("ratio", ray.ratio.toDouble())
            )
        }
        return arr.toString()
    }

    fun raysFromJson(json: String): List<StarRayData> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                StarRayData(
                    envelopeId = obj.getLong("envelopeId"),
                    name = obj.getString("name"),
                    planned = obj.getLong("planned"),
                    actual = obj.getLong("actual"),
                    color = obj.getLong("color"),
                    envelopeType = obj.getString("envelopeType"),
                    ratio = obj.getDouble("ratio").toFloat(),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 记录完整度：只要当月有账目即为完整 */
    fun completenessOf(
        cycle: BudgetCycle,
        activeDays: Int,
        hasTransactions: Boolean,
    ): Float = when {
        cycle.markedUnrecorded -> 0f
        !hasTransactions -> 0f
        else -> 1f
    }

    fun monthLabel(year: Int, month: Int): String = TimeUtil.monthName(year, month)
}
