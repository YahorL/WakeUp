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
    private val activeAlarmId: () -> Long = { -1L },
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
        val alarm = repository.getAlarm(id) ?: return@launch
        if (enabled) {
            scheduler.schedule(alarm)
        } else if (id != activeAlarmId()) {
            scheduler.cancel(id)
        }
        // If this alarm is mid-ring/snooze, keep its pending snooze fire — you can't escape the
        // wake-up challenge by toggling it off; it'll ring again after the snooze interval.
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(Graph.repository, Graph.scheduler, { Graph.ringState.activeId() }) as T
        }
    }
}
