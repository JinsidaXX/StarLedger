package com.starledger.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.backup.BackupManager
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.starmap.StarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val coolingDays: Int = 7,
    val showDailyAmount: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
    private val starRepository: StarRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = kotlinx.coroutines.flow.combine(
        settingsStore.settings,
        _message,
    ) { settings, message ->
        SettingsUiState(
            coolingDays = settings.coolingDays,
            showDailyAmount = settings.showDailyAmount,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setCoolingDays(days: Int) {
        viewModelScope.launch { settingsStore.setCoolingDays(days) }
    }

    fun setShowDailyAmount(show: Boolean) {
        viewModelScope.launch { settingsStore.setShowDailyAmount(show) }
    }

    suspend fun exportJson(): String = backupManager.exportJson()

    suspend fun exportCsv(): String = backupManager.exportCsv()

    fun importJson(json: String) {
        viewModelScope.launch {
            try {
                val count = backupManager.importJson(json)
                starRepository.refreshAllStars()
                _message.value = "导入成功，共恢复 $count 条记录"
            } catch (e: Exception) {
                _message.value = "导入失败：${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
