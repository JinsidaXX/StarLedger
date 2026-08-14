package com.starledger.app.feature.allocation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.starledger.app.core.model.AllocationRule
import com.starledger.app.core.model.Category
import com.starledger.app.core.model.EnvelopeType
import com.starledger.app.core.model.Money
import com.starledger.app.core.model.RuleType
import kotlinx.coroutines.launch

@Composable
fun TemplateEditScreen(
    templateId: Long,
    onBack: () -> Unit,
    viewModel: TemplateEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(templateId) { viewModel.load(templateId) }

    var editing by remember { mutableStateOf<AllocationRule?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = state.template?.name ?: "分配模板",
        onBack = onBack,
        actions = {
            TextButton(onClick = { showRenameDialog = true }) {
                Text("重命名", color = AccentBlue)
            }
            TextButton(onClick = { showDeleteDialog = true }) {
                Text("删除", color = RiskRed)
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "分配时按顺序执行：固定金额 → 总收入比例 → 剩余金额比例 → 全部剩余。收入不足时按顺序截断。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(state.rules, key = { it.id }) { rule ->
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = rule }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(rule.color.toColor()),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rule.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                ruleDescription(rule),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        if (!rule.enabled) {
                            Text("停用", style = MaterialTheme.typography.labelSmall, color = RiskRed)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("编辑", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                    }
                }
            }
        }
    }

    if (editing != null) {
        RuleEditDialog(
            rule = editing,
            onDismiss = { editing = null },
            onSave = { rule ->
                scope.launch {
                    viewModel.saveRule(rule)
                    editing = null
                }
            },
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(state.template?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SpaceBackground,
            title = { Text("重命名模板", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模板名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch { viewModel.renameTemplate(newName) }
                            showRenameDialog = false
                        }
                    },
                    enabled = newName.isNotBlank(),
                ) { Text("保存", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消", color = TextSecondary) }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SpaceBackground,
            title = { Text("删除模板", color = TextPrimary) },
            text = { Text("删除「${state.template?.name}」及其所有分配规则？此操作不可恢复。", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (viewModel.deleteTemplate()) onBack()
                    }
                    showDeleteDialog = false
                }) { Text("删除", color = RiskRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = TextSecondary) }
            },
        )
    }
}

private fun ruleDescription(rule: AllocationRule): String = when (rule.ruleType) {
    RuleType.FIXED_AMOUNT -> "固定 ${Money.formatWithSymbol(rule.value)}"
    RuleType.INCOME_PERCENTAGE -> "总收入的 ${rule.percent / 100.0}%"
    RuleType.REMAINING_PERCENTAGE -> "剩余金额的 ${rule.percent / 100.0}%"
    RuleType.REMAINDER -> "全部剩余"
}.let { base ->
    val cat = rule.categoryId?.let { " · 关联分类" } ?: ""
    "$base · ${rule.envelopeType.label}$cat"
}

@Composable
private fun RuleEditDialog(
    rule: AllocationRule?,
    onDismiss: () -> Unit,
    onSave: (AllocationRule) -> Unit,
) {
    val isRemainder = rule?.ruleType == RuleType.REMAINDER
    var valueText by remember(rule) {
        mutableStateOf(if (rule != null && !isRemainder) Money.format(rule.value) else "")
    }
    var envelopeType by remember(rule) { mutableStateOf(rule?.envelopeType ?: EnvelopeType.NECESSARY) }
    var color by remember(rule) { mutableStateOf((rule?.color ?: 0xFF86A8FF).toColor()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceBackground,
        title = { Text("编辑预算：${rule?.name ?: ""}", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isRemainder) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("金额（元）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                } else {
                    Text("剩余金额自动分配到这个预算", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text("类型", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EnvelopeType.entries.forEach { t ->
                        val selected = envelopeType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                .clickable { envelopeType = t }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (selected) AccentBlue else TextSecondary)
                        }
                    }
                }
                Text("颜色", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                HueColorPicker(
                    color = color,
                    onColorChange = { color = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = if (isRemainder) (rule?.value ?: 0) else (Money.parseYuan(valueText) ?: 0)
                    onSave(
                        (rule ?: AllocationRule(templateId = 0, name = "", ruleType = RuleType.FIXED_AMOUNT)).copy(
                            value = value,
                            envelopeType = envelopeType,
                            color = color.toArgbLong(),
                        )
                    )
                },
                enabled = isRemainder || valueText.isNotBlank(),
            ) { Text("保存", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
    )
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

/** 色相取色器：横向彩虹条，点击选色相 */
@Composable
private fun HueColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val saturation = 0.72f
    val value = 0.92f
    val hue = remember(color) { mutableFloatStateOf(color.toHue()) }
    val rainbow = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hue.floatValue, saturation, value)),
            )
            Text(
                "色相 ${hue.floatValue.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.horizontalGradient(rainbow))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                        hue.floatValue = h
                        onColorChange(Color.hsv(h, saturation, value))
                    }
                },
        )
    }
}

/** 从 ARGB 颜色反推色相（0..360） */
private fun Color.toHue(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta <= 0f) return 0f
    val h = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return if (h < 0) h + 360f else h
}
