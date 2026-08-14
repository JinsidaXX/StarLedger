package com.starledger.app.feature.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.starledger.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.design.components.EmptyState
import com.starledger.app.core.design.components.ListRow
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.components.SimpleProgress
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.PositiveGreen
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.design.theme.WarningYellow
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.model.TxType
import com.starledger.app.core.starmap.StarCanvas

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onPlanDetail: (Long) -> Unit,
    onPlanEdit: (Long?) -> Unit,
    onStarDetail: (Long) -> Unit,
    onReview: (Long) -> Unit,
    onGoPlanning: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cycle = state.cycle
    var categoriesExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 顶栏：本期 + 设置
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    if (cycle != null) {
                        Text(
                            TimeUtil.formatRange(cycle.startDate, cycle.endDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Row {
                        Text(
                            "${stringResource(R.string.income_short)} ${Money.formatWithSymbol(state.income)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskRed,
                        )
                        Text("  ", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${stringResource(R.string.expense_short)} ${Money.formatWithSymbol(state.expense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PositiveGreen,
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title), tint = TextSecondary)
                }
            }
        }

        // 本期恒星
        item {
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = cycle != null) { cycle?.let { onStarDetail(it.id) } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StarCanvas(
                            visual = state.starVisual,
                            modifier = Modifier.size(88.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            cycle?.name ?: stringResource(R.string.home_month_star),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                cycle == null -> stringResource(R.string.home_no_records_yet)
                                state.starVisual.colorState == com.starledger.app.core.model.StarColorState.FOG ->
                                    stringResource(R.string.home_no_records_yet)
                                state.expense == 0L -> stringResource(R.string.home_record_first)
                                else -> stringResource(R.string.home_expense_amount, Money.formatWithSymbol(state.expense))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // 可放心使用金额
        item {
            SectionCard {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.home_safe_to_spend), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    MoneyText(
                        cents = state.safeToSpend,
                        withSymbol = true,
                        style = MaterialTheme.typography.displayMedium,
                        color = if (state.safeToSpend < 0) RiskRed else TextPrimary,
                    )
                    if (state.showDailyAmount && state.daysLeft > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.home_daily_ref, state.daysLeft, Money.formatWithSymbol(state.dailyReference)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    if (cycle != null && cycle.totalIncome == 0L) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onGoPlanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = SpaceBackground,
                            ),
                        ) {
                            Text(stringResource(R.string.home_record_income))
                        }
                    }
                }
            }
        }

        // 总资产
        item {
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccounts)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_total_assets), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        MoneyText(
                            cents = state.totalAssets,
                            withSymbol = true,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                    }
                    Text(stringResource(R.string.home_manage), style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                }
            }
        }

        // 已分配进度
        if (cycle != null && cycle.totalIncome > 0) {
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_allocated), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${Money.format(state.allocated)} / ${Money.format(cycle.totalIncome)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        SimpleProgress(
                            progress = if (cycle.totalIncome > 0)
                                state.allocated.toFloat() / cycle.totalIncome else 0f,
                            color = AccentBlue,
                        )
                    }
                }
            }
        }

        // 各分类剩余（可折叠）
        if (state.envelopes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoriesExpanded = !categoriesExpanded }
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_categories_left),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (categoriesExpanded) stringResource(R.string.home_collapse) else stringResource(R.string.home_expand),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                    )
                }
            }
            if (categoriesExpanded) {
                items(state.envelopes, key = { "env_${it.id}" }) { envelope ->
                    EnvelopeRow(envelope)
                }
            }
        }

        // 进行中的消费计划
        if (state.activePlans.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_active_plans),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            items(state.activePlans, key = { "plan_${it.id}" }) { plan ->
                val days = TimeUtil.daysFromToday(plan.earliestDecisionDate)
                SectionCard {
                    ListRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        leading = { CircleIcon("☄️", com.starledger.app.core.design.theme.StarPurple) },
                        title = plan.name,
                        subtitle = when {
                            days > 0 -> stringResource(R.string.home_days_to_decide, days)
                            days <= 0 -> stringResource(R.string.home_can_decide)
                            else -> ""
                        },
                        trailing = {
                            MoneyText(
                                cents = plan.estimatedAmount,
                                withSymbol = true,
                                color = TextSecondary,
                            )
                        },
                        onClick = { onPlanDetail(plan.id) },
                    )
                }
            }
        }

        // 今日账目
        item {
            Text(
                stringResource(R.string.home_today),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        if (state.recent.isEmpty()) {
            item {
                SectionCard {
                    EmptyState(
                        emoji = "✍️",
                        title = stringResource(R.string.home_no_records),
                        subtitle = stringResource(R.string.home_add_hint),
                    )
                }
            }
        } else {
            items(state.recent, key = { "tx_${it.transaction.id}" }) { tx ->
                SectionCard {
                    TransactionRow(
                        tx = tx,
                        onClick = { onEditTransaction(tx.transaction.id) },
                    )
                }
            }
        }

        // 月末复盘入口（周期已结束且有数据时）
        if (cycle != null &&
            cycle.endDate < System.currentTimeMillis() &&
            (cycle.totalIncome > 0 || state.expense > 0)
        ) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.home_month_ended), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                stringResource(
                                    R.string.home_month_summary,
                                    Money.formatWithSymbol(cycle.totalIncome),
                                    Money.formatWithSymbol(state.expense),
                                    Money.formatWithSymbol(cycle.totalIncome - state.expense),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Button(
                            onClick = { onReview(cycle.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSecondary,
                                contentColor = TextPrimary,
                            ),
                        ) {
                            Text(stringResource(R.string.home_view_review))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvelopeRow(envelope: BudgetEnvelope) {
    val remaining = envelope.remainingAmount
    val ratio = if (envelope.plannedAmount > 0)
        envelope.actualAmount.toFloat() / envelope.plannedAmount else 0f
    val color = when {
        ratio > 1.5f -> RiskRed
        ratio > 1.2f -> com.starledger.app.core.design.theme.StarPurple
        ratio > 1.1f -> WarningYellow
        else -> envelope.color.toColor()
    }
    SectionCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    envelope.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.envelope_planned, Money.format(envelope.plannedAmount)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(8.dp))
                MoneyText(
                    cents = remaining,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remaining < 0) RiskRed else TextPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            SimpleProgress(progress = ratio, color = color, height = 4)
        }
    }
}

@Composable
private fun TransactionRow(
    tx: com.starledger.app.core.ledger.TransactionWithDetails,
    onClick: () -> Unit,
) {
    val t = tx.transaction
    val isExpense = t.type == TxType.EXPENSE
    ListRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        leading = {
            CircleIcon(
                emoji = tx.category?.icon ?: if (isExpense) "💸" else "💰",
                color = androidx.compose.ui.graphics.Color(tx.category?.color ?: 0xFF86A8FF),
                size = 36,
            )
        },
        title = tx.category?.name ?: stringResource(t.type.labelResId),
        subtitle = listOfNotNull(
            t.merchant.takeIf { it.isNotEmpty() },
            tx.account?.name,
        ).joinToString(" · ").ifEmpty { TimeUtil.formatDateFull(t.date) },
        trailing = {
            MoneyText(
                cents = t.amount,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isExpense) PositiveGreen else RiskRed,
                signed = !isExpense,
            )
        },
        onClick = onClick,
    )
}