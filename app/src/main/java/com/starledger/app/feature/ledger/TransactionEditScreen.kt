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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.starledger.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import com.starledger.app.core.model.Account
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.ForcedSavingType
import com.starledger.app.core.model.IncomeType
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.model.TxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    transactionId: Long?,
    onDone: () -> Unit,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) {
        viewModel.load(transactionId)
    }
    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(SpaceBackground), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.loading), color = TextSecondary)
        }
        return
    }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showToAccountPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = if (transactionId == null) stringResource(R.string.tx_add_title) else stringResource(R.string.tx_edit_title),
        onBack = onDone,
        actions = {
            if (transactionId != null) {
                TextButton(onClick = { viewModel.delete() }) {
                    Text(stringResource(R.string.delete), color = RiskRed)
                }
            }
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save() },
                enabled = state.canSave,
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
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 类型切换
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(TxType.EXPENSE, TxType.INCOME, TxType.TRANSFER).forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { viewModel.setType(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = 3,
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = AccentBlue.copy(alpha = 0.25f),
                                activeContentColor = AccentBlue,
                                inactiveContainerColor = SurfaceSecondary,
                                inactiveContentColor = TextSecondary,
                            ),
                        ) {
                            Text(stringResource(type.labelResId))
                        }
                    }
                }
            }

            // 收入类型（仅收入）
            if (state.type == TxType.INCOME) {
                item {
                    Text(
                        stringResource(R.string.tx_income_type),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IncomeType.entries.forEach { incomeType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (state.incomeType == incomeType) AccentBlue.copy(alpha = 0.22f)
                                        else SurfaceSecondary
                                    )
                                    .clickable { viewModel.setIncomeType(incomeType) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(incomeType.labelResId),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.incomeType == incomeType) AccentBlue else TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (state.incomeType == incomeType) {
                                    Text("✓", color = AccentBlue)
                                }
                            }
                        }
                    }
                }
            }

            // 金额输入
            item {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { viewModel.setAmountText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.amount_yuan)) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    textStyle = MaterialTheme.typography.headlineMedium,
                )
            }

            // 分类（收入不需要分类，仅保留收入类型）
            if (state.type != TxType.TRANSFER && state.type != TxType.INCOME) {
                item {
                    val visibleCategories = state.categories.filter {
                        val incomeLike = state.type == TxType.REFUND ||
                            state.type == TxType.REIMBURSEMENT
                        if (incomeLike) !it.isExpense else it.isExpense
                    }
                    Text(
                        stringResource(R.string.tx_category),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(180.dp),
                    ) {
                        items(visibleCategories, key = { it.id }) { category ->
                            CategoryCell(
                                category = category,
                                selected = state.categoryId == category.id,
                                onClick = { viewModel.setCategory(category.id) },
                            )
                        }
                    }
                }
            }

            // 账户
            item {
                AccountField(
                    label = stringResource(R.string.tx_account),
                    account = state.accounts.firstOrNull { it.id == state.accountId },
                    onClick = { showAccountPicker = true },
                )
            }

            // 转入账户（转账）
            if (state.type == TxType.TRANSFER) {
                item {
                    AccountField(
                        label = stringResource(R.string.tx_transfer_to),
                        account = state.accounts.firstOrNull { it.id == state.toAccountId },
                        onClick = { showToAccountPicker = true },
                    )
                }
            }

            // 日期
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSecondary)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📅", fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        TimeUtil.formatDateFull(state.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 更多（商户、备注）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSecondary)
                        .clickable { showMore = !showMore }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.tx_more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (showMore) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            if (showMore) {
                item {
                    OutlinedTextField(
                        value = state.merchant,
                        onValueChange = { viewModel.setMerchant(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tx_merchant)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.setNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tx_note)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                }
            }
        }
    }

    // ---- 弹窗 ----
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.notice), color = TextPrimary) },
            text = { Text(state.error ?: "", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(R.string.confirm), color = AccentBlue)
                }
            },
        )
    }
    if (state.showSalaryConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSalaryConfirm() },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.salary_confirm_title), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.salary_confirm_message, state.salaryRunningDays),
                        color = TextSecondary,
                    )
                    // 强制存储设置
                    Text(
                        stringResource(R.string.forced_saving_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForcedSavingType.entries.forEach { type ->
                            val selected = state.forcedSavingType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                    .clickable { viewModel.setForcedSavingType(type) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    stringResource(type.labelResId),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) AccentBlue else TextSecondary,
                                )
                            }
                        }
                    }
                    if (state.forcedSavingType == ForcedSavingType.FIXED_AMOUNT) {
                        OutlinedTextField(
                            value = state.forcedSavingText,
                            onValueChange = { viewModel.setForcedSavingText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.forced_saving_value)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    if (state.forcedSavingType == ForcedSavingType.INCOME_PERCENTAGE) {
                        OutlinedTextField(
                            value = state.forcedSavingText,
                            onValueChange = { viewModel.setForcedSavingText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.forced_saving_percent_value)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSalaryNewCycle() }) {
                    Text(stringResource(R.string.salary_confirm_new_cycle), color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmSalaryAsNormal() }) {
                    Text(stringResource(R.string.salary_confirm_as_normal), color = TextSecondary)
                }
            },
        )
    }
    if (state.showOverBudgetWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelOverBudget() },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.notice), color = TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.over_budget_warning),
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmOverBudget() }) {
                    Text(stringResource(R.string.confirm), color = RiskRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelOverBudget() }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
        )
    }
    if (showAccountPicker) {
        AccountPickerDialog(
            title = stringResource(R.string.tx_choose_account),
            accounts = state.accounts,
            selectedId = state.accountId,
            onSelect = { viewModel.setAccount(it); showAccountPicker = false },
            onDismiss = { showAccountPicker = false },
        )
    }
    if (showToAccountPicker) {
        AccountPickerDialog(
            title = stringResource(R.string.tx_choose_to_account),
            accounts = state.accounts,
            selectedId = state.toAccountId,
            onSelect = { viewModel.setToAccount(it); showToAccountPicker = false },
            onDismiss = { showToAccountPicker = false },
        )
    }
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = state.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
}

@Composable
private fun CategoryCell(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentBlue.copy(alpha = 0.22f) else SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleIcon(
            emoji = category.icon,
            color = category.color.toColor(),
            size = 34,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AccentBlue else TextSecondary,
        )
    }
}

@Composable
private fun AccountField(
    label: String,
    account: Account?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            account?.name ?: stringResource(R.string.tx_please_select),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AccountPickerDialog(
    title: String,
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(account.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIcon(emoji = account.type.name.let { typeIcon(it) }, color = account.color.toColor(), size = 32)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            account.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedId == account.id) {
                            Text("✓", color = AccentBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun typeIcon(type: String): String = when (type) {
    "CASH" -> "💵"
    "BANK_CARD" -> "💳"
    "WECHAT" -> "💬"
    "ALIPAY" -> "🅰️"
    "CAMPUS_CARD" -> "🎓"
    "CREDIT_CARD" -> "💳"
    else -> "🏦"
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = SurfaceSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
)
