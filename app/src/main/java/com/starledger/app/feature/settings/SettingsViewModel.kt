package com.starledger.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.R
import com.starledger.app.core.backup.BackupManager
import com.starledger.app.core.cycle.CycleService
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.model.CycleMode
import com.starledger.app.core.starmap.StarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val coolingDays: Int = 7,
    val showDailyAmount: Boolean = false,
    val language: String = "system",
    val cycleMode: CycleMode = CycleMode.CALENDAR_MONTH,
    val runningCycleDays: Long? = null,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
    private val starRepository: StarRepository,
    private val cycleService: CycleService,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _language = MutableStateFlow(settingsStore.getLanguageSync())
    private val _runningCycleDays = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.settings,
        _message,
        _language,
        _runningCycleDays,
    ) { settings, message, language, runningDays ->
        SettingsUiState(
            coolingDays = settings.coolingDays,
            showDailyAmount = settings.showDailyAmount,
            language = language,
            cycleMode = settings.cycleMode,
            runningCycleDays = runningDays,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        refreshRunningCycle()
    }

    fun refreshRunningCycle() {
        viewModelScope.launch {
            val running = cycleService.getRunningCycle()
            _runningCycleDays.value = running?.let { cycleService.runningDays(it) }
        }
    }

    fun setCoolingDays(days: Int) {
        viewModelScope.launch { settingsStore.setCoolingDays(days) }
    }

    fun setShowDailyAmount(show: Boolean) {
        viewModelScope.launch { settingsStore.setShowDailyAmount(show) }
    }

    fun setLanguage(language: String) {
        settingsStore.setLanguageSync(language)
        _language.value = language
    }

    fun setCycleMode(mode: CycleMode) {
        viewModelScope.launch {
            settingsStore.setCycleMode(mode)
            refreshRunningCycle()
        }
    }

    /** 手动结束并结算当前运行周期，返回结算金额（用于提示） */
    fun manualSettleCurrentCycle(onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            val settlement = cycleService.manuallySettleCurrent()
            onResult(settlement?.surplus)
            refreshRunningCycle()
        }
    }

    suspend fun exportJson(): String = backupManager.exportJson()

    suspend fun exportCsv(): String = backupManager.exportCsv()

    fun importJson(json: String) {
        viewModelScope.launch {
            try {
                val count = backupManager.importJson(json)
                starRepository.refreshAllStars()
                _message.value = context.getString(R.string.settings_import_success, count)
            } catch (e: Exception) {
                _message.value = context.getString(R.string.settings_import_fail, e.message ?: "")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
