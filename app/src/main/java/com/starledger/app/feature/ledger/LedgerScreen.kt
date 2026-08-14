package com.starledger.app.feature.ledger

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.design.components.EmptyState
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.PositiveGreen
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.model.TxType
import java.time.format.DateTimeFormatter

@Composable
fun LedgerScreen(
    onEditTransaction: (Long) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: LedgerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding(),
    ) {
        // 顶栏：月份切换 + 收入支出
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上个月", tint = TextSecondary)
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    state.month.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Row {
                    Text(
                        "收 ${Money.formatWithSymbol(state.income)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RiskRed,
                    )
                    Text("  ", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "支 ${Money.formatWithSymbol(state.expense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PositiveGreen,
                    )
                }
            }
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下个月", tint = TextSecondary)
            }
        }

        // 管理入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ManagementChip("账户", onOpenAccounts)
            ManagementChip("分类", onOpenCategories)
        }

        // 搜索
        OutlinedTextField(
            value = state.searchText,
            onValueChange = { viewModel.setSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索备注、商户", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = SurfaceSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentBlue,
            ),
        )

        // 类型筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.filterType == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("全部") },
                colors = chipColors(),
            )
            listOf(TxType.EXPENSE, TxType.INCOME, TxType.TRANSFER).forEach { type ->
                FilterChip(
                    selected = state.filterType == type,
                    onClick = { viewModel.setFilter(type) },
                    label = { Text(type.label) },
                    colors = chipColors(),
                )
            }
        }

        // 交易列表（按天分组）
        if (state.transactions.isEmpty()) {
            EmptyState(
                emoji = "🌙",
                title = "这个月还没有账目",
                subtitle = "点击下方 ＋ 快速记一笔",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val grouped = state.transactions.groupBy { it.transaction.date / 86_400_000L }
                grouped.keys.sortedDescending().forEach { day ->
                    val txs = grouped[day].orEmpty()
                    item(key = "day_$day") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                TimeUtil.formatDate(day * 86_400_000L),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SurfaceSecondary),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                TimeUtil.dayOfWeek(day * 86_400_000L),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    items(txs, key = { it.transaction.id }) { tx ->
                        SectionCard {
                            LedgerTransactionRow(tx = tx, onClick = { onEditTransaction(tx.transaction.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun LedgerTransactionRow(
    tx: com.starledger.app.core.ledger.TransactionWithDetails,
    onClick: () -> Unit,
) {
    val t = tx.transaction
    val isExpense = t.type == TxType.EXPENSE
    val title = when (t.type) {
        TxType.TRANSFER -> "${tx.account?.name ?: ""} → ${tx.toAccount?.name ?: ""}"
        else -> tx.category?.name ?: t.type.label
    }
    val amountColor = when (t.type) {
        TxType.EXPENSE -> PositiveGreen
        TxType.TRANSFER -> TextPrimary
        else -> RiskRed
    }
    val amountPrefix = when (t.type) {
        TxType.EXPENSE -> "-"
        TxType.TRANSFER -> ""
        else -> "+"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(
            emoji = when (t.type) {
                TxType.TRANSFER -> "⇄"
                TxType.INCOME, TxType.REFUND, TxType.REIMBURSEMENT -> tx.category?.icon ?: "💰"
                else -> tx.category?.icon ?: "💸"
            },
            color = Color(tx.category?.color ?: 0xFF86A8FF),
            size = 38,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(
                listOfNotNull(
                    t.merchant.takeIf { it.isNotEmpty() },
                    tx.account?.name?.takeIf { t.type != TxType.TRANSFER },
                    t.note.takeIf { it.isNotEmpty() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                cents = t.amount,
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
            )
            Text(
                TimeUtil.formatTime(t.date),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AccentBlue.copy(alpha = 0.22f),
    selectedLabelColor = AccentBlue,
    containerColor = SurfaceSecondary,
    labelColor = TextSecondary,
)
