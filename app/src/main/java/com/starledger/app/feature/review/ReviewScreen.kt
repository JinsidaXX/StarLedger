package com.starledger.app.feature.review

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.starledger.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.allocation.SurplusMode
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
import com.starledger.app.core.design.theme.WarningYellow
import com.starledger.app.core.model.Money
import com.starledger.app.core.starmap.StarCanvas

@Composable
fun ReviewScreen(
    cycleId: Long,
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSurplusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cycleId) { viewModel.load(cycleId) }

    ScreenScaffold(
        title = stringResource(R.string.review_title),
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
            // 本期总结（默认只展示这些，可关闭）
            item {
                SectionCard {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.review_ended), style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.review_total_income), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                MoneyText(
                                    cents = state.income,
                                    withSymbol = true,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PositiveGreen,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.review_total_expense), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                MoneyText(
                                    cents = state.expense,
                                    withSymbol = true,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.review_surplus), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                MoneyText(
                                    cents = state.surplus,
                                    withSymbol = true,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (state.surplus >= 0) SurplusGold else RiskRed,
                                )
                            }
                        }
                    }
                }
            }

            // 星图预览
            item {
                SectionCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StarCanvas(visual = state.visual, modifier = Modifier.size(160.dp))
                    }
                }
            }

            // 预算偏差
            if (state.envelopes.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.review_budget_deviation),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(state.envelopes, key = { it.id }) { env ->
                    val ratio = if (env.plannedAmount > 0) env.actualAmount.toFloat() / env.plannedAmount else 0f
                    val color = when {
                        ratio > 1.5f -> RiskRed
                        ratio > 1.2f -> StarPurple
                        ratio > 1.1f -> WarningYellow
                        else -> PositiveGreen
                    }
                    SectionCard {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(env.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                MoneyText(
                                    cents = env.actualAmount - env.plannedAmount,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color,
                                    signed = true,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            SimpleProgress(progress = ratio, color = color, height = 4)
                        }
                    }
                }
            }

            // 结余处理
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.review_handle_surplus), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    if (state.cycle?.surplusHandled == true)
                                        stringResource(R.string.review_surplus_handled, Money.formatWithSymbol(state.surplus))
                                    else
                                        stringResource(R.string.review_surplus_amount, Money.formatWithSymbol(state.surplus)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            if (state.cycle?.surplusHandled != true && state.surplus > 0) {
                                Button(
                                    onClick = { showSurplusDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SurplusGold,
                                        contentColor = SpaceBackground,
                                    ),
                                ) {
                                    Text(stringResource(R.string.review_handle_surplus))
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.review_skip_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 完成复盘
            item {
                Button(
                    onClick = { viewModel.completeReview() },
                    enabled = state.cycle?.reviewCompleted != true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = SpaceBackground,
                        disabledContainerColor = SurfaceSecondary,
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    Text(
                        if (state.cycle?.reviewCompleted == true) stringResource(R.string.review_completed_star) else stringResource(R.string.review_complete_star),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    if (showSurplusDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSurplusDialog = false },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.review_handle_surplus), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.review_surplus_amount, Money.formatWithSymbol(state.surplus)), color = TextPrimary)
                    SurplusMode.entries.forEach { mode ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceSecondary)
                                .clickable {
                                    viewModel.handleSurplus(mode)
                                    showSurplusDialog = false
                                }
                                .padding(14.dp),
                        ) {
                            Text(stringResource(mode.labelResId), color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showSurplusDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
        )
    }
}
