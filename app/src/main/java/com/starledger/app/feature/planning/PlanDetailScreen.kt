package com.starledger.app.feature.planning

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.design.components.MoneyText
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.PositiveGreen
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.StarPurple
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.TimeUtil

@Composable
fun PlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PlanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(planId) { viewModel.load(planId) }
    LaunchedEffect(state.closed) { if (state.closed) onBack() }

    var showBuyDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "消费计划",
        onBack = onBack,
        actions = {
            TextButton(onClick = { onEdit(planId) }) {
                Text("编辑", color = AccentBlue)
            }
        },
    ) {
        if (state.loading || state.plan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            return@ScreenScaffold
        }
        val plan = state.plan!!
        val canDecide = plan.status == PlanStatus.READY ||
            (plan.status == PlanStatus.COOLING && state.daysLeft <= 0)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 计划卡片
            item {
                SectionCard {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("☄️", fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp))
                        Spacer(Modifier.height(8.dp))
                        Text(plan.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        MoneyText(
                            cents = plan.estimatedAmount,
                            withSymbol = true,
                            style = MaterialTheme.typography.displayMedium,
                            color = AccentBlue,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "创建于 ${TimeUtil.formatDateFull(plan.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 冷静期状态
            item {
                when {
                    plan.status == PlanStatus.COOLING && state.daysLeft > 0 -> {
                        SectionCard {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("🌙", fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "距离可以决定还有 ${state.daysLeft} 天",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StarPurple,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "冷静期内不着急做决定",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                    plan.status == PlanStatus.COOLING || plan.status == PlanStatus.READY -> {
                        SectionCard {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "冷静期结束，可以决定了",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PositiveGreen,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "想清楚了再行动",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                    plan.status == PlanStatus.PURCHASED -> {
                        SectionCard {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "已购买 ✅",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PositiveGreen,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "这笔消费已记入账本并关联到该计划。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                    else -> {
                        SectionCard {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    plan.status.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // 理由与替代方案
            if (plan.reason.isNotBlank() || plan.alternative.isNotBlank() || plan.note.isNotBlank()) {
                item {
                    SectionCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (plan.reason.isNotBlank()) {
                                Text("为什么想买", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(plan.reason, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            if (plan.alternative.isNotBlank()) {
                                Text("替代方案", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(plan.alternative, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            if (plan.note.isNotBlank()) {
                                Text("备注", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(plan.note, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 决定操作
            if (canDecide) {
                item {
                    Button(
                        onClick = { showBuyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = SpaceBackground,
                        ),
                    ) {
                        Text("购买", style = MaterialTheme.typography.titleMedium)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.cancel() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskRed),
                    ) {
                        Text("放弃")
                    }
                }
                item {
                    Text(
                        "暂不决定也没关系，计划会保留在这里。",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // 已购后的处理
            if (plan.status == PlanStatus.PURCHASED) {
                item {
                    Button(
                        onClick = { viewModel.convertToItem() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = SpaceBackground,
                        ),
                    ) {
                        Text("转为物品长期管理", style = MaterialTheme.typography.titleMedium)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.deletePlan() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskRed),
                    ) {
                        Text("从计划中删除")
                    }
                }
            }

            // 冷静期中的提前操作
            if (plan.status == PlanStatus.COOLING && state.daysLeft > 0) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.cancel() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskRed),
                    ) {
                        Text("放弃这个计划")
                    }
                }
            }
        }
    }

    // 购买弹窗：选择账户与分类
    if (showBuyDialog) {
        BuyDialog(
            accounts = state.accounts,
            envelopes = state.envelopes,
            plan = state.plan,
            onDismiss = { showBuyDialog = false },
            onConfirm = { accountId, categoryId, amount ->
                viewModel.buy(accountId, categoryId, amount)
                showBuyDialog = false
            },
        )
    }
}

@Composable
private fun BuyDialog(
    accounts: List<com.starledger.app.core.model.Account>,
    envelopes: List<com.starledger.app.core.model.BudgetEnvelope>,
    plan: com.starledger.app.core.model.PlannedPurchase?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?, Long?) -> Unit,
) {
    var selectedAccount by remember { mutableStateOf<Long?>(null) }
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var amountText by remember(plan) { mutableStateOf(com.starledger.app.core.model.Money.format(plan?.estimatedAmount ?: 0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text("确认购买", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("实际金额（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                    ),
                )
                Text("从哪个账户支付？", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedAccount == account.id) AccentBlue.copy(alpha = 0.2f) else SurfaceSecondary)
                                .clickable { selectedAccount = account.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(account.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                            if (selectedAccount == account.id) Text("✓", color = AccentBlue)
                        }
                    }
                }
                if (envelopes.isNotEmpty()) {
                    Text("记入哪个预算？（可选）", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        envelopes.filter { it.categoryId != null }.take(8).forEach { env ->
                            val selected = selectedCategory == env.categoryId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) AccentBlue.copy(alpha = 0.2f) else SurfaceSecondary)
                                    .clickable { selectedCategory = env.categoryId }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(env.name, style = MaterialTheme.typography.labelSmall, color = if (selected) AccentBlue else TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedAccount?.let { acc ->
                        val amount = com.starledger.app.core.model.Money.parseYuan(amountText)
                        onConfirm(acc, selectedCategory, amount)
                    }
                },
                enabled = selectedAccount != null && com.starledger.app.core.model.Money.parseYuan(amountText) != null,
            ) { Text("确认购买", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
}
