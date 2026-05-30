package com.wakeup.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.schedule.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
        val alarm = repository.getAlarm(id) ?: return@launch
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
    }

    fun delete(id: Long) = viewModelScope.launch {
        scheduler.cancel(id)
        repository.delete(id)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(Graph.repository, Graph.scheduler) as T
        }
    }
}
