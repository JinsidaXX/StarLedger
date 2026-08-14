package com.starledger.app.feature.starmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.database.dao.TransactionDao
import com.starledger.app.core.model.MonthlyStar
import com.starledger.app.core.model.TimeUtil
import com.starledger.app.core.starmap.StarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class StarmapUiState(
    val year: Int = YearMonth.now().year,
    val stars: List<MonthlyStar> = emptyList(),
    val yearIncome: Long = 0,
    val yearExpense: Long = 0,
)

@HiltViewModel
class StarmapViewModel @Inject constructor(
    private val starRepository: StarRepository,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val yearFlow = MutableStateFlow(YearMonth.now().year)

    val uiState: StateFlow<StarmapUiState> = yearFlow
        .flatMapLatest { year ->
            kotlinx.coroutines.flow.flow {
                starRepository.refreshAllStars()
                starRepository.observeStarsByYear(year).collect { stars ->
                    val start = TimeUtil.monthStart(year, 1)
                    val end = TimeUtil.monthEnd(year, 12)
                    emit(
                        StarmapUiState(
                            year = year,
                            stars = stars,
                            yearIncome = transactionDao.sumIncome(start, end),
                            yearExpense = transactionDao.sumExpense(start, end),
                        )
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StarmapUiState())

    fun previousYear() {
        yearFlow.value -= 1
    }

    fun nextYear() {
        yearFlow.value += 1
    }

    fun refresh() {
        viewModelScope.launch { starRepository.refreshAllStars() }
    }
}
