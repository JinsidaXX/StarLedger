package com.starledger.app.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.starledger.app.R
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
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.model.Money
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSettleConfirm by remember { mutableStateOf(false) }
    var settleResult by remember { mutableStateOf<Long?>(null) }

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

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 偏好
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_cooling), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
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
                                        if (days == 0) stringResource(R.string.plan_cooling_off) else stringResource(R.string.plan_cooling_days, days),
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
                                Text(stringResource(R.string.settings_daily_amount), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(stringResource(R.string.settings_daily_hint), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Switch(
                                checked = state.showDailyAmount,
                                onCheckedChange = { viewModel.setShowDailyAmount(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue),
                            )
                        }
                        // 语言选择
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(languageLabel(state.language), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            TextButton(onClick = { showLanguageDialog = true }) {
                                Text(stringResource(R.string.edit), color = AccentBlue)
                            }
                        }
                    }
                }
            }

            // 财务周期
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_cycle_mode), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CycleMode.entries.forEach { mode ->
                                val selected = state.cycleMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (selected) AccentBlue.copy(alpha = 0.25f) else SurfaceSecondary)
                                        .clickable { viewModel.setCycleMode(mode) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        stringResource(mode.labelResId),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) AccentBlue else TextSecondary,
                                    )
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.settings_cycle_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        if (state.runningCycleDays != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.settings_running_cycle, state.runningCycleDays!!),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                    )
                                }
                                TextButton(onClick = { showSettleConfirm = true }) {
                                    Text(stringResource(R.string.settings_settle_cycle), color = RiskRed)
                                }
                            }
                        }
                    }
                }
            }

            // 备份与恢复
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        SettingRow(stringResource(R.string.settings_export_json)) {
                            exportJsonLauncher.launch("StarLedger-backup-${LocalDate.now()}.json")
                        }
                        SettingRow(stringResource(R.string.settings_import_json), danger = true) {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                        SettingRow(stringResource(R.string.settings_export_csv)) {
                            exportCsvLauncher.launch("StarLedger-${LocalDate.now()}.csv")
                        }
                    }
                }
            }

            // 隐私说明
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            stringResource(R.string.settings_privacy_desc),
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
                        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${stringResource(R.string.app_name)} v0.2.0", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "${stringResource(R.string.onboarding_slogan)}\n${stringResource(R.string.onboarding_license)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }

    // 手动结束并结算确认
    if (showSettleConfirm) {
        AlertDialog(
            onDismissRequest = { showSettleConfirm = false },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.settings_settle_title), color = TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.settings_settle_confirm),
                    color = TextPrimary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.manualSettleCurrentCycle { surplus ->
                        settleResult = surplus
                        showSettleConfirm = false
                    }
                }) { Text(stringResource(R.string.confirm), color = RiskRed) }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirm = false }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
            },
        )
    }

    // 结算结果提示
    settleResult?.let { surplus ->
        AlertDialog(
            onDismissRequest = { settleResult = null },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.settings_settle_done), color = TextPrimary) },
            text = {
                Text(
                    if (surplus > 0) stringResource(R.string.settings_settle_result, Money.formatWithSymbol(surplus))
                    else stringResource(R.string.settings_settle_result_zero),
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { settleResult = null }) { Text(stringResource(R.string.confirm), color = AccentBlue) }
            },
        )
    }

    // 导入确认
    showImportConfirm?.let { content ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.settings_import_confirm_title), color = TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.settings_import_confirm),
                    color = TextPrimary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importJson(content)
                    showImportConfirm = null
                }) { Text(stringResource(R.string.settings_import), color = RiskRed) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = SpaceBackground,
            title = { Text(stringResource(R.string.settings_language), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "zh", "en").forEach { lang ->
                        val selected = state.language == lang
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AccentBlue.copy(alpha = 0.2f) else SurfaceSecondary)
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                    com.starledger.app.di.applyAppLanguage(context)
                                    (context as? android.app.Activity)?.recreate()
                                }
                                .padding(14.dp),
                        ) {
                            Text(
                                languageLabel(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) AccentBlue else TextPrimary,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
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

@Composable
private fun languageLabel(language: String): String = when (language) {
    "zh" -> stringResource(R.string.language_zh)
    "en" -> stringResource(R.string.language_en)
    else -> stringResource(R.string.language_system)
}
