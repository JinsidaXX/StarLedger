package com.starledger.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starledger.app.core.database.dao.OwnedItemDao
import com.starledger.app.core.model.OwnedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnedItemsViewModel @Inject constructor(
    private val ownedItemDao: OwnedItemDao,
) : ViewModel() {

    val items: StateFlow<List<OwnedItem>> = ownedItemDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(item: OwnedItem) {
        viewModelScope.launch { ownedItemDao.deleteById(item.id) }
    }
}
