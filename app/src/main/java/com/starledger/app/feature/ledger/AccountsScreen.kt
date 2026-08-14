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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.starledger.app.core.design.theme.CategoryPresetColors
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.AccountType
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        title = "账户管理",
        onBack = onBack,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增账户", tint = AccentBlue)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("总资产", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        MoneyText(
                            cents = state.totalAssets,
                            withSymbol = true,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                        )
                        if (state.totalLiabilities > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "负债 ${com.starledger.app.core.model.Money.formatWithSymbol(state.totalLiabilities)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskRed,
                            )
                        }
                    }
                }
            }
            items(state.accounts, key = { it.account.id }) { item ->
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = item.account }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIcon(
                            emoji = accountEmoji(item.account.type),
                            color = item.account.color.toColor(),
                            size = 38,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                            )
                            Text(
                                item.account.type.label + if (item.account.isCredit) " · 负债" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        MoneyText(
                            cents = item.balance,
                            withSymbol = true,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.balance < 0) RiskRed else TextPrimary,
                        )
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        AccountEditDialog(
            account = editing,
            currentBalance = editing?.let { a ->
                state.accounts.firstOrNull { it.account.id == a.id }?.balance ?: 0
            } ?: 0,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { account, targetBalance ->
                scope.launch {
                    if (targetBalance != null) {
                        viewModel.reconcileBalance(account, targetBalance)
                    } else {
                        viewModel.save(account)
                    }
                    creating = false
                    editing = null
                }
            },
            onDelete = { account ->
                scope.launch {
                    if (!viewModel.delete(account)) {
                        errorMessage = "该账户已有账目，无法删除"
                    }
                    editing = null
                }
            },
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = SpaceBackground,
            title = { Text("提示", color = TextPrimary) },
            text = { Text(msg, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun AccountEditDialog(
    account: Account?,
    currentBalance: Long = 0,
    onDismiss: () -> Unit,
    onSave: (Account, Long?) -> Unit,
    onDelete: (Account) -> Unit,
) {
    var name by remember(account) { mutableStateOf(account?.name ?: "") }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.CASH) }
    var initialText by remember(account) { mutableStateOf(if (account == null) "" else com.starledger.app.core.model.Money.format(account.initialBalance)) }
    var reconcileText by remember(account) { mutableStateOf("") }
    var isCredit by remember(account) { mutableStateOf(account?.isCredit ?: false) }
    var includeInTotal by remember(account) { mutableStateOf(account?.includeInTotal ?: true) }
    var color by remember(account) { mutableStateOf((account?.color ?: 0xFF86A8FF).toColor()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text(if (account == null) "新增账户" else "编辑账户", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("账户名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                )
                // 类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.chunked(4).forEach { chunk ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunk.forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (type == t) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                        .clickable { type = t }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        t.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (type == t) AccentBlue else TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
                if (account == null) {
                    OutlinedTextField(
                        value = initialText,
                        onValueChange = { initialText = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("初始余额（元）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors(),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "当前余额",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        MoneyText(
                            cents = currentBalance,
                            withSymbol = true,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentBalance < 0) RiskRed else TextPrimary,
                        )
                    }
                    OutlinedTextField(
                        value = reconcileText,
                        onValueChange = { reconcileText = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("核对余额（元，留空不调整）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors(),
                    )
                }
                // 颜色
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPresetColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == c) Text("✓", color = SpaceBackground)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("负债账户（信用卡）", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isCredit,
                        onCheckedChange = { isCredit = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("计入总资产", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = includeInTotal,
                        onCheckedChange = { includeInTotal = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue),
                    )
                }
                if (account != null) {
                    TextButton(onClick = { onDelete(account) }) {
                        Text("删除账户", color = RiskRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = com.starledger.app.core.model.Money.parseYuan(initialText) ?: 0
                    val target = com.starledger.app.core.model.Money.parseYuan(reconcileText)
                    onSave(
                        (account ?: Account(name = name, type = type)).copy(
                            name = name.ifBlank { type.label },
                            type = type,
                            initialBalance = cents,
                            isCredit = isCredit,
                            includeInTotal = includeInTotal,
                            color = color.toArgbLong(),
                        ),
                        if (account == null) null else target,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("保存", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
}

private fun accountEmoji(type: AccountType): String = when (type) {
    AccountType.CASH -> "💵"
    AccountType.BANK_CARD -> "💳"
    AccountType.WECHAT -> "💬"
    AccountType.ALIPAY -> "🅰️"
    AccountType.CAMPUS_CARD -> "🎓"
    AccountType.CREDIT_CARD -> "💳"
    AccountType.OTHER -> "🏦"
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = SurfaceSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
)
