package com.starledger.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.database.SettingsStore
import com.starledger.app.core.ledger.SeedDefaultsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val seedDefaultsUseCase: SeedDefaultsUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    val onboardingDone: StateFlow<Boolean> = settingsStore.settings
        .map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _language = MutableStateFlow(settingsStore.getLanguageSync())
    val language: StateFlow<String> = _language

    init {
        viewModelScope.launch {
            seedDefaultsUseCase()
            _ready.value = true
        }
    }

    fun completeOnboarding() {
        kotlinx.coroutines.runBlocking { settingsStore.setOnboardingDone(true) }
    }

    /** 同步写入语言偏好 */
    fun applyLanguage(language: String) {
        settingsStore.setLanguageSync(language)
        _language.value = language
    }
}
