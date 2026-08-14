package com.starledger.app.feature.starmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.components.SimpleProgress
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.PositiveGreen
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.StarPurple
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.SurplusGold
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.design.theme.WarningYellow
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.StarColorState
import com.starledger.app.core.starmap.StarCanvas

@Composable
fun StarDetailScreen(
    cycleId: Long,
    onBack: () -> Unit,
    onReview: (Long) -> Unit,
    viewModel: StarDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(cycleId) { viewModel.load(cycleId) }

    ScreenScaffold(
        title = state.cycle?.name ?: "月度恒星",
        onBack = onBack,
    ) {
        if (state.loading || state.cycle == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 大星图
            item {
                SectionCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StarCanvas(
                            visual = state.visual,
                            modifier = Modifier.size(300.dp),
                            showLabels = true,
                        )
                    }
                }
            }

            // 状态说明
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "恒星状态：${state.star?.colorState?.label ?: "—"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = stateColor(state.star?.colorState),
                        )
                        Text(
                            starDescription(state),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 周期数据
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DataRow("本期收入", state.cycle?.totalIncome ?: 0)
                        DataRow("支出", state.expense)
                        DataRow("结余", state.surplus, color = if (state.surplus >= 0) PositiveGreen else RiskRed)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("记账状态", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(
                                if ((state.star?.observationCompleteness ?: 0f) >= 1f) "已记账" else "未记录",
                                style = MaterialTheme.typography.labelSmall,
                                color = if ((state.star?.observationCompleteness ?: 0f) >= 1f) PositiveGreen else RiskRed,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("分配完成度", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        SimpleProgress(
                            progress = state.star?.allocationCompletion ?: 0f,
                            color = PositiveGreen,
                        )
                    }
                }
            }

            // 星芒明细（各分类预算）
            if (state.envelopes.isNotEmpty()) {
                item {
                    Text(
                        "星芒 · 各分类预算",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(state.envelopes, key = { it.id }) { env ->
                    RayDetailRow(env)
                }
            }

            // 操作
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.completeReview() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.cycle?.reviewCompleted == true) PositiveGreen else AccentBlue,
                            contentColor = SpaceBackground,
                        ),
                    ) {
                        Text(if (state.cycle?.reviewCompleted == true) "已完成复盘 ✓" else "完成复盘")
                    }
                    Button(
                        onClick = { onReview(cycleId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSecondary,
                            contentColor = TextPrimary,
                        ),
                    ) {
                        Text("复盘详情")
                    }
                }
            }
            item {
                Text(
                    if (state.cycle?.markedUnrecorded == true) "本周期已标记为未记录，点击恢复" else "这个月没有记录？可以标记为「未记录」",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable { viewModel.markUnrecorded() }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: Long, color: Color = TextPrimary) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        MoneyText(cents = value, withSymbol = true, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun RayDetailRow(env: com.starledger.app.core.model.BudgetEnvelope) {
    val ratio = if (env.plannedAmount > 0) env.actualAmount.toFloat() / env.plannedAmount else 0f
    val color = when {
        ratio > 1.5f -> RiskRed
        ratio > 1.2f -> StarPurple
        ratio > 1.1f -> WarningYellow
        else -> env.color.toColor()
    }
    SectionCard {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                )
                Spacer(Modifier.width(8.dp))
                Text(env.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                MoneyText(
                    cents = env.remainingAmount,
                    color = if (env.remainingAmount < 0) RiskRed else TextPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            SimpleProgress(progress = ratio, color = color, height = 4)
            Spacer(Modifier.height(4.dp))
            Text(
                "计划 ${Money.format(env.plannedAmount)} · 已用 ${Money.format(env.actualAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

private fun starDescription(state: StarDetailUiState): String {
    val star = state.star ?: return ""
    return when (star.colorState) {
        StarColorState.BLUE -> "各分类大致符合计划，一切都在轨道上。"
        StarColorState.WARM -> "有分类轻微超出计划，本期需要稍微留意。"
        StarColorState.ORANGE -> "部分分类明显超出计划，可以考虑下个周期重新安排。"
        StarColorState.RED -> "本期支出明显超过可用资金，建议先查看哪些分类需要关注。"
        StarColorState.FOG -> "本月还没有记录，先记下第一笔账吧。"
    }
}

@Composable
private fun stateColor(state: StarColorState?): Color = when (state) {
    StarColorState.BLUE -> AccentBlue
    StarColorState.WARM -> WarningYellow
    StarColorState.ORANGE -> StarPurple
    StarColorState.RED -> RiskRed
    StarColorState.FOG -> com.starledger.app.core.design.theme.FogBlue
    null -> TextSecondary
}
