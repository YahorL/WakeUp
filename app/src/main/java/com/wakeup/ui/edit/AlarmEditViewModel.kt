package com.wakeup.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.*
import com.wakeup.schedule.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    private val _draft = MutableStateFlow(Alarm.default(hour = 7, minute = 0))
    val draft: StateFlow<Alarm> = _draft.asStateFlow()

    init {
        if (alarmId != null) viewModelScope.launch {
            repository.getAlarm(alarmId)?.let { _draft.value = it }
        }
    }

    fun setTime(hour: Int, minute: Int) { _draft.value = _draft.value.copy(hour = hour, minute = minute) }
    fun setLabel(label: String) { _draft.value = _draft.value.copy(label = label) }
    fun toggleRepeatDay(dayIndex: Int) {
        _draft.value = _draft.value.copy(repeatDays = RepeatDays.toggle(_draft.value.repeatDays, dayIndex))
    }
    fun setSound(uri: String?) { _draft.value = _draft.value.copy(soundUri = uri) }
    fun setVibrate(v: Boolean) { _draft.value = _draft.value.copy(vibrate = v) }
    fun setGradualVolume(v: Boolean) { _draft.value = _draft.value.copy(gradualVolume = v) }
    fun setSnoozeEnabled(v: Boolean) { _draft.value = _draft.value.copy(snoozeEnabled = v) }
    fun setSnoozeMinutes(m: Int) { _draft.value = _draft.value.copy(snoozeMinutes = m) }
    fun setMaxSnoozes(n: Int) { _draft.value = _draft.value.copy(maxSnoozes = n) }

    fun addMission(type: MissionType) {
        val params: MissionParams = when (type) {
            MissionType.MATH -> MathParams(Difficulty.MEDIUM, 3)
            MissionType.SQUAT -> SquatParams(10)
        }
        val next = _draft.value.missions + MissionConfig(order = _draft.value.missions.size, type = type, params = params)
        _draft.value = _draft.value.copy(missions = reindex(next))
    }

    fun removeMission(index: Int) {
        _draft.value = _draft.value.copy(missions = reindex(_draft.value.missions.filterIndexed { i, _ -> i != index }))
    }

    fun moveMission(from: Int, to: Int) {
        val list = _draft.value.missions.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _draft.value = _draft.value.copy(missions = reindex(list))
    }

    fun updateMissionParams(index: Int, params: MissionParams) {
        val list = _draft.value.missions.toMutableList()
        list[index] = list[index].copy(params = params)
        _draft.value = _draft.value.copy(missions = list)
    }

    fun save(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.upsert(_draft.value)
        scheduler.schedule(_draft.value.copy(id = id))
        onSaved(id)
    }

    private fun reindex(list: List<MissionConfig>): List<MissionConfig> =
        list.mapIndexed { i, m -> m.copy(order = i) }

    companion object {
        fun factory(alarmId: Long?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmEditViewModel(Graph.repository, alarmId, Graph.scheduler) as T
        }
    }
}
