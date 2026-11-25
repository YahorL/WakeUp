package com.wakeup.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.schedule.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    /** Alarms that are ringing or snoozed; the list shows them as locked. */
    val activeAlarms: StateFlow<Set<Long>> = MutableStateFlow(emptySet()),
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
        val alarm = repository.getAlarm(id) ?: return@launch
        // For an alarm that is ringing or snoozed the scheduler ignores both calls (see
        // ActiveAlarmGuard): the toggle then only decides whether it rings on future days.
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(Graph.repository, Graph.scheduler, Graph.ringState.activeIds) as T
        }
    }
}
