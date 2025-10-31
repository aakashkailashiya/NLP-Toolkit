package com.example.exudesimulator.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.exudesimulator.data.AppDatabase
import com.example.exudesimulator.data.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao = AppDatabase.getDatabase(application).historyDao()

    val history = historyDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveText(text: String) {
        viewModelScope.launch {
            historyDao.insert(HistoryItem(text = text))
        }
    }
}
