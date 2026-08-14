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
import androidx.compose.ui.res.stringResource
import com.starledger.app.R
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
        title = state.cycle?.name ?: stringResource(R.string.star_detail_monthly_star),
        onBack = onBack,
    ) {
        if (state.loading || state.cycle == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.loading), color = TextSecondary)
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
                            stringResource(
                                R.string.star_status,
                                stringResource(state.star?.colorState?.labelResId ?: R.string.star_state_fog),
                            ),
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
                        DataRow(stringResource(R.string.home_title), state.cycle?.totalIncome ?: 0)
                        DataRow(stringResource(R.string.home_expense), state.expense)
                        DataRow(stringResource(R.string.review_surplus), state.surplus, color = if (state.surplus >= 0) PositiveGreen else RiskRed)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.star_record_status), style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(
                                if ((state.star?.observationCompleteness ?: 0f) >= 1f) stringResource(R.string.star_recorded) else stringResource(R.string.star_not_recorded),
                                style = MaterialTheme.typography.labelSmall,
                                color = if ((state.star?.observationCompleteness ?: 0f) >= 1f) PositiveGreen else RiskRed,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.star_alloc_completion), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                        stringResource(R.string.star_rays),
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
                        Text(if (state.cycle?.reviewCompleted == true) stringResource(R.string.star_review_done) else stringResource(R.string.star_complete_review))
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
                        Text(stringResource(R.string.star_review_detail))
                    }
                }
            }
            item {
                Text(
                    if (state.cycle?.markedUnrecorded == true) stringResource(R.string.star_mark_recorded) else stringResource(R.string.star_mark_unrecorded),
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
                stringResource(R.string.envelope_planned_used, Money.format(env.plannedAmount), Money.format(env.actualAmount)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun starDescription(state: StarDetailUiState): String {
    val star = state.star ?: return ""
    return when (star.colorState) {
        StarColorState.BLUE -> stringResource(R.string.star_desc_blue)
        StarColorState.WARM -> stringResource(R.string.star_desc_warm)
        StarColorState.ORANGE -> stringResource(R.string.star_desc_orange)
        StarColorState.RED -> stringResource(R.string.star_desc_red)
        StarColorState.FOG -> stringResource(R.string.star_desc_fog)
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
