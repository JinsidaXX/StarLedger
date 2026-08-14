package com.starledger.app.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starledger.app.core.design.components.ScreenScaffold
import com.starledger.app.core.design.components.SectionCard
import com.starledger.app.core.design.theme.AccentBlue
import com.starledger.app.core.design.theme.RiskRed
import com.starledger.app.core.design.theme.SpaceBackground
import com.starledger.app.core.design.theme.SurfaceSecondary
import com.starledger.app.core.design.theme.TextPrimary
import com.starledger.app.core.design.theme.TextSecondary
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showImportConfirm by remember { mutableStateOf<String?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { u ->
            scope.launch {
                val content = viewModel.exportJson()
                runCatching {
                    context.contentResolver.openOutputStream(u)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { u ->
            scope.launch {
                val content = viewModel.exportCsv()
                runCatching {
                    context.contentResolver.openOutputStream(u)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { u ->
            val content = runCatching {
                context.contentResolver.openInputStream(u)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: ""
            }.getOrDefault("")
            if (content.isNotBlank()) {
                showImportConfirm = content
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { viewModel.clearMessage() }
    }

    ScreenScaffold(title = "设置", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 偏好
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("大额消费冷静期", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 7, 14, 30).forEach { days ->
                                val selected = state.coolingDays == days
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (selected) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                        .clickable { viewModel.setCoolingDays(days) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        if (days == 0) "关闭" else "$days 天",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) AccentBlue else TextSecondary,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("首页显示每日可用金额", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text("仅作参考，不打分", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Switch(
                                checked = state.showDailyAmount,
                                onCheckedChange = { viewModel.setShowDailyAmount(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue),
                            )
                        }
                    }
                }
            }

            // 备份与恢复
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("备份与恢复", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        SettingRow("导出 JSON 备份（全部数据）") {
                            exportJsonLauncher.launch("StarLedger-backup-${LocalDate.now()}.json")
                        }
                        SettingRow("导入 JSON 备份（覆盖当前数据）", danger = true) {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                        SettingRow("导出 CSV 账单") {
                            exportCsvLauncher.launch("StarLedger-${LocalDate.now()}.csv")
                        }
                    }
                }
            }

            // 隐私说明
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("数据与隐私", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "· 所有数据只保存在手机本地\n" +
                                "· 无需登录、无需联网\n" +
                                "· 不上传账单、不收集消费行为\n" +
                                "· 无广告、无付费功能",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 关于
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("关于", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("星图账本 StarLedger v0.1.0", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "让每一笔收支，都有自己的轨道。\n免费 · 开源 · GPL-3.0-or-later",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }

    // 导入确认
    showImportConfirm?.let { content ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            containerColor = SpaceBackground,
            title = { Text("确认导入", color = TextPrimary) },
            text = {
                Text(
                    "导入将覆盖当前全部数据，建议先导出一份备份。是否继续？",
                    color = TextPrimary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importJson(content)
                    showImportConfirm = null
                }) { Text("导入", color = RiskRed) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("取消", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) RiskRed else TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text("›", color = TextSecondary)
    }
}
