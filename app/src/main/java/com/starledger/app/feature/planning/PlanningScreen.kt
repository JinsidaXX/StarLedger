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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.starledger.app.core.allocation.SurplusMode
import com.starledger.app.core.design.components.MoneyText
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
import com.starledger.app.core.model.BudgetEnvelope
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.PlanStatus
import com.starledger.app.core.model.TimeUtil

@Composable
fun PlanningScreen(
    onPlanDetail: (Long) -> Unit,
    onPlanEdit: (Long?) -> Unit,
    onTemplateEdit: (Long) -> Unit,
    onOwnedItems: () -> Unit,
    viewModel: PlanningViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showApplyDialog by remember { mutableStateOf(false) }
    var showSurplusDialog by remember { mutableStateOf(false) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var editingEnvelope by remember { mutableStateOf<BudgetEnvelope?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("规划", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        }

        // 本期收入
        item {
            SectionCard {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("本期收入", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            MoneyText(
                                cents = state.cycle?.totalIncome ?: 0,
                                withSymbol = true,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                "自动汇总自记账收入",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("已分配", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${Money.format(state.cycle?.totalAllocated ?: 0)} / ${Money.format(state.cycle?.totalIncome ?: 0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SimpleProgress(
                        progress = if ((state.cycle?.totalIncome ?: 0) > 0)
                            (state.cycle?.totalAllocated ?: 0).toFloat() / (state.cycle?.totalIncome ?: 1)
                        else 0f,
                        color = AccentBlue,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showApplyDialog = true },
                        enabled = (state.cycle?.totalIncome ?: 0) > 0 && state.templates.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = SpaceBackground,
                            disabledContainerColor = SurfaceSecondary,
                            disabledContentColor = TextSecondary,
                        ),
                    ) {
                        Text(if (state.envelopes.isEmpty()) "按模板分配" else "重新分配")
                    }
                }
            }
        }

        // 预算信封
        if (state.envelopes.isNotEmpty()) {
            item {
                Text(
                    "预算信封",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            items(state.envelopes, key = { "env_${it.id}" }) { envelope ->
                EnvelopeCard(
                    envelope = envelope,
                    onClick = { editingEnvelope = envelope },
                )
            }
        }

        // 大额消费计划
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "大额消费计划",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onPlanEdit(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceSecondary,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text("规划一笔消费")
                }
            }
        }
        if (state.plans.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "有想买的东西吗？先记下来，冷静几天再决定。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(state.plans, key = { "plan_${it.id}" }) { plan ->
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlanDetail(plan.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StarPurple.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("☄️")
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(plan.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(planStatusText(plan), style = MaterialTheme.typography.labelSmall, color = planStatusColor(plan))
                        }
                        MoneyText(plan.estimatedAmount, withSymbol = true, color = TextSecondary)
                    }
                }
            }
        }

        // 我的物品
        item {
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOwnedItems)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🎒", fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "我的物品",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text("长期管理 ›", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                }
            }
        }

        // 模板管理
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "分配模板",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showCreateTemplateDialog = true }) {
                    Text("新建模板", color = AccentBlue)
                }
            }
        }
        items(state.templates, key = { "tpl_${it.id}" }) { template ->
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTemplateEdit(template.id) }
                        .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (template.isDefault) {
                        Text("默认", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        TextButton(onClick = { viewModel.setDefaultTemplate(template) }) {
                            Text("设为默认", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                        }
                    }
                    TextButton(onClick = { onTemplateEdit(template.id) }) {
                        Text("编辑", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                    }
                }
            }
        }

        // 结余处理（周期已结束）
        if (state.cycleEnded && (state.cycle?.totalIncome ?: 0) > 0 && !(state.cycle?.surplusHandled ?: true)) {
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("本期已结束", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "结余 ${Money.formatWithSymbol(state.surplus)}，可以结转下期或归入缓冲。",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showSurplusDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurplusGold,
                                contentColor = SpaceBackground,
                            ),
                        ) {
                            Text("处理结余")
                        }
                    }
                }
            }
        }
    }

    // ---- 弹窗 ----
    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateTemplateDialog = false },
            onConfirm = { name ->
                viewModel.createTemplate(name)
                showCreateTemplateDialog = false
            },
        )
    }
    if (showApplyDialog) {
        val defaultTemplate = state.templates.firstOrNull { it.isDefault } ?: state.templates.firstOrNull()
        if (defaultTemplate != null) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                containerColor = SpaceBackground,
                title = { Text("按模板分配", color = TextPrimary) },
                text = {
                    Text(
                        "使用「${defaultTemplate.name}」把本期收入 ${Money.formatWithSymbol(state.cycle?.totalIncome ?: 0)} 分配到各分类。重新分配会替换现有预算。",
                        color = TextPrimary,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.applyTemplate(defaultTemplate)
                        showApplyDialog = false
                    }) { Text("分配", color = AccentBlue) }
                },
                dismissButton = {
                    TextButton(onClick = { showApplyDialog = false }) { Text("取消", color = TextSecondary) }
                },
            )
        }
    }
    if (showSurplusDialog) {
        AlertDialog(
            onDismissRequest = { showSurplusDialog = false },
            containerColor = SpaceBackground,
            title = { Text("处理结余", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("结余 ${Money.formatWithSymbol(state.surplus)}", color = TextPrimary)
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
                            Text(mode.label, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurplusDialog = false }) { Text("取消", color = TextSecondary) }
            },
        )
    }
    editingEnvelope?.let { envelope ->
        EnvelopeEditDialog(
            envelope = envelope,
            onDismiss = { editingEnvelope = null },
            onConfirm = { newPlanned ->
                viewModel.adjustEnvelope(envelope, newPlanned)
                editingEnvelope = null
            },
        )
    }
}

@Composable
private fun EnvelopeCard(
    envelope: BudgetEnvelope,
    onClick: () -> Unit,
) {
    val ratio = if (envelope.plannedAmount > 0)
        envelope.actualAmount.toFloat() / envelope.plannedAmount else 0f
    val color = when {
        ratio > 1.5f -> RiskRed
        ratio > 1.2f -> StarPurple
        ratio > 1.1f -> WarningYellow
        else -> envelope.color.toColor()
    }
    SectionCard {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    envelope.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    envelope.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(8.dp))
                MoneyText(
                    cents = envelope.remainingAmount,
                    color = if (envelope.remainingAmount < 0) RiskRed else TextPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            SimpleProgress(progress = ratio, color = color, height = 4)
            Spacer(Modifier.height(4.dp))
            Text(
                "计划 ${Money.format(envelope.plannedAmount)} · 已用 ${Money.format(envelope.actualAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text("新建模板", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模板名称") },
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
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("创建", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
}

@Composable
private fun EnvelopeEditDialog(
    envelope: BudgetEnvelope,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(Money.format(envelope.plannedAmount)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text("调整预算：${envelope.name}", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("计划金额（元）") },
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
                Spacer(Modifier.height(6.dp))
                Text(
                    "已使用 ${Money.formatWithSymbol(envelope.actualAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { Money.parseYuan(text)?.let(onConfirm) },
                enabled = Money.parseYuan(text) != null,
            ) { Text("保存", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
}

private fun planStatusText(plan: com.starledger.app.core.model.PlannedPurchase): String = when (plan.status) {
    PlanStatus.COOLING -> {
        val days = TimeUtil.daysFromToday(plan.earliestDecisionDate)
        if (days > 0) "冷静期 · 距可决定还有 $days 天" else "冷静期结束，可以决定"
    }
    PlanStatus.READY -> "可以决定了"
    PlanStatus.POSTPONED -> "已延期"
    PlanStatus.CANCELED -> "已放弃"
    PlanStatus.REPLACED -> "已更换方案"
    PlanStatus.PURCHASED -> "已购买"
    PlanStatus.DRAFT -> "草稿"
}

private fun planStatusColor(plan: com.starledger.app.core.model.PlannedPurchase): Color = when (plan.status) {
    PlanStatus.COOLING, PlanStatus.POSTPONED, PlanStatus.READY -> StarPurple
    PlanStatus.PURCHASED -> PositiveGreen
    PlanStatus.CANCELED, PlanStatus.REPLACED -> TextSecondary
    PlanStatus.DRAFT -> WarningYellow
}
