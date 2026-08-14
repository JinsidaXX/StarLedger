package com.starledger.app.core.starmap

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.StarColorState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 恒星渲染所需的可视化数据 */
data class StarVisual(
    val colorState: StarColorState = StarColorState.BLUE,
    val brightness: Float = 0.5f,
    val rays: List<StarRayData> = emptyList(),
    val surplusAmount: Long = 0,
    val surplusHandled: Boolean = false,
    val activePlanCount: Int = 0,
    val allocationDone: Boolean = false,
    val reviewDone: Boolean = false,
    val hasTransactions: Boolean = false,
    val markedUnrecorded: Boolean = false,
) {
    companion object {
        fun from(computed: StarComputed): StarVisual = StarVisual(
            colorState = computed.colorState,
            brightness = computed.brightness,
            rays = computed.rays,
            surplusAmount = computed.surplusAmount,
            surplusHandled = computed.surplusHandled,
            activePlanCount = computed.activePlanCount,
            allocationDone = computed.allocationCompletion >= 1f,
            reviewDone = computed.reviewCompleted,
            hasTransactions = computed.hasTransactions,
            markedUnrecorded = computed.markedUnrecorded,
        )

        fun from(star: MonthlyStar): StarVisual = StarVisual(
            colorState = star.colorState,
            brightness = star.brightness,
            rays = StarEngine.raysFromJson(star.snapshotData),
            surplusAmount = star.surplusAmount,
            surplusHandled = false,
            activePlanCount = 0,
            allocationDone = star.allocationCompletion >= 1f,
            reviewDone = star.reviewCompleted,
            hasTransactions = star.observationCompleteness > 0f,
            markedUnrecorded = false,
        )

        /** 未记录月份：星雾占位 */
        fun unrecorded(): StarVisual = StarVisual(
            colorState = StarColorState.FOG,
            brightness = 0.15f,
            markedUnrecorded = true,
        )
    }
}

private val WarmColor = Color(0xFFF3B95F)
private val OrangeColor = Color(0xFFFF8C42)
private val RedColor = Color(0xFFFF6B7A)
private val FogColor = Color(0xFF5A6B8C)
private val GoldColor = Color(0xFFF6D477)
private val GreenColor = Color(0xFF58D6A9)
private val BlueWhite = Color(0xFF86A8FF)
private val CoreWhite = Color(0xFFF4F7FF)
private val CometColor = Color(0xFFA78BFA)

/** 星芒颜色：按预算偏差 */
fun rayColor(ratio: Float, baseColor: Color): Color = when {
    ratio > 1.5f -> RedColor
    ratio > 1.2f -> OrangeColor
    ratio > 1.1f -> WarmColor
    else -> baseColor
}

/** 星核颜色 */
fun coreColor(state: StarColorState): Color = when (state) {
    StarColorState.BLUE -> BlueWhite
    StarColorState.WARM -> WarmColor
    StarColorState.ORANGE -> OrangeColor
    StarColorState.RED -> RedColor
    StarColorState.FOG -> FogColor
}

/**
 * 恒星 Canvas。
 * 星核 = 整体状态；星芒 = 各分类预算与支出；星环 = 结余；
 * 星雾 = 未记录；彗星 = 大额消费计划；光点 = 完成分配/复盘。
 */
@Composable
fun StarCanvas(
    visual: StarVisual,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
    labelStyle: TextStyle = TextStyle(fontSize = 11.sp),
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val r = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawStar(
            visual = visual,
            center = center,
            radius = r,
            textMeasurer = textMeasurer,
            showLabels = showLabels,
            labelStyle = labelStyle,
        )
    }
}

fun DrawScope.drawStar(
    visual: StarVisual,
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer? = null,
    showLabels: Boolean = false,
    labelStyle: TextStyle = TextStyle(fontSize = 11.sp),
) {
    val state = visual.colorState
    val coreR = radius * 0.24f
    val innerR = coreR * 1.15f
    val maxRayR = radius * 0.94f
    val ringR = radius * 0.52f
    val overallAlpha = (0.5f + 0.5f * visual.brightness.coerceIn(0f, 1f))

    val isFog = state == StarColorState.FOG || visual.markedUnrecorded

    // ---- 背景深空微光（细节模式） ----
    if (showLabels) {
        val specks = listOf(
            0.35f to 1.1f, 1.9f to 1.4f, 2.9f to 1.2f, 4.2f to 1.5f, 5.1f to 1.1f,
        )
        specks.forEach { (angle, dist) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius * 0.008f,
                center = polar(center, radius * dist * 0.42f, angle),
            )
        }
    }

    // ---- 星雾 ----
    if (isFog) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(FogColor.copy(alpha = 0.02f), FogColor.copy(alpha = 0.45f)),
                center = center,
                radius = maxRayR,
            ),
            radius = maxRayR,
            center = center,
        )
    }

    // ---- 星环（结余） ----
    if (visual.surplusAmount > 0) {
        val ringAlpha = if (visual.surplusHandled) 0.9f else 0.28f
        drawCircle(
            color = GoldColor.copy(alpha = ringAlpha * overallAlpha),
            radius = ringR,
            center = center,
            style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round),
        )
    }

    // ---- 星芒 ----
    val rays = visual.rays.take(StarEngine.MAX_RAYS)
    val maxPlanned = (rays.maxOfOrNull { it.planned } ?: 1L).coerceAtLeast(1)
    val count = rays.size
    if (count > 0 && !visual.markedUnrecorded) {
        rays.forEachIndexed { i, ray ->
            val angle = (-PI / 2 + i * 2 * PI / count).toFloat()
            val plannedWeight = ray.planned.toFloat() / maxPlanned
            val spendProgress = ray.ratio.coerceIn(0f, 1.3f)
            val lengthFactor = 0.42f + 0.58f * plannedWeight
            val tipR = innerR + (maxRayR - innerR) * lengthFactor * (0.45f + 0.55f * (spendProgress / 1.3f))
            val halfSpread = (PI / count) * 0.38f
            val base = ray.color.toColor()
            val c = if (isFog) FogColor else rayColor(ray.ratio, base)
            val alpha = if (ray.ratio <= 0f && !visual.hasTransactions) 0.18f
            else (0.35f + 0.65f * min(ray.ratio, 1f))
            val tip = Offset(
                center.x + tipR * cos(angle).toFloat(),
                center.y + tipR * sin(angle).toFloat(),
            )
            val path = Path().apply {
                moveTo(
                    center.x + innerR * cos(angle - halfSpread).toFloat(),
                    center.y + innerR * sin(angle - halfSpread).toFloat(),
                )
                lineTo(tip.x, tip.y)
                lineTo(
                    center.x + innerR * cos(angle + halfSpread).toFloat(),
                    center.y + innerR * sin(angle + halfSpread).toFloat(),
                )
                close()
            }
            // 星芒从核心向外渐隐，形成柔和辉光
            drawPath(
                path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        c.copy(alpha = alpha * overallAlpha),
                        c.copy(alpha = alpha * overallAlpha * 0.08f),
                    ),
                    start = center,
                    end = tip,
                ),
            )

            if (showLabels && textMeasurer != null) {
                val labelR = min(tipR + radius * 0.08f, radius * 1.18f)
                val labelPos = polar(center, labelR, angle)
                val label = if (ray.remainingAvailable()) {
                    "${ray.name} 余${Money.format(ray.planned - ray.actual)}"
                } else {
                    "${ray.name} +${Money.format(ray.actual - ray.planned)}"
                }
                val measured = textMeasurer.measure(label, labelStyle.copy(color = c.copy(alpha = 0.9f)))
                val topLeft = Offset(
                    (labelPos.x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    (labelPos.y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height),
                )
                drawText(measured, topLeft = topLeft)
            }
        }
    }

    // ---- 星核（含外层辉光） ----
    val core = if (isFog) FogColor.copy(alpha = 0.55f) else coreColor(state).copy(alpha = overallAlpha)
    if (!isFog) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor(state).copy(alpha = 0.5f * overallAlpha),
                    Color.Transparent,
                ),
                center = center,
                radius = coreR * 2.6f,
            ),
            radius = coreR * 2.6f,
            center = center,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                (if (isFog) Color(0xFF3A4A6B) else CoreWhite).copy(alpha = overallAlpha),
                core,
            ),
            center = center,
            radius = coreR * 1.6f,
        ),
        radius = coreR * 1.25f,
        center = center,
    )
    if (!isFog && visual.hasTransactions) {
        drawCircle(
            color = CoreWhite.copy(alpha = 0.9f * overallAlpha),
            radius = coreR * 0.35f,
            center = center,
        )
    }

    // ---- 彗星（进行中的大额消费计划） ----
    if (!visual.markedUnrecorded && visual.activePlanCount > 0) {
        val cometCount = min(visual.activePlanCount, 3)
        repeat(cometCount) { k ->
            val angle = 0.7f + k * 2.0f
            val dist = ringR + (maxRayR - ringR) * (0.35f + 0.25f * k)
            val head = polar(center, dist, angle)
            val tailStart = polar(center, dist - radius * 0.2f, angle)
            drawLine(
                color = CometColor.copy(alpha = 0.8f * overallAlpha),
                start = tailStart,
                end = head,
                strokeWidth = radius * 0.02f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = CoreWhite.copy(alpha = 0.95f * overallAlpha),
                radius = radius * 0.028f,
                center = head,
            )
        }
    }

    // ---- 光点（完成分配 / 复盘） ----
    if (!visual.markedUnrecorded) {
        if (visual.allocationDone) {
            drawCircle(
                color = GreenColor.copy(alpha = 0.9f * overallAlpha),
                radius = radius * 0.022f,
                center = polar(center, ringR, 2.6f),
            )
        }
        if (visual.reviewDone) {
            drawCircle(
                color = GoldColor.copy(alpha = 0.95f * overallAlpha),
                radius = radius * 0.022f,
                center = polar(center, ringR, 3.4f),
            )
        }
    }
}

private fun polar(center: Offset, radius: Float, angle: Float): Offset =
    Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))

private fun StarRayData.remainingAvailable(): Boolean = actual <= planned
