package com.wakeup.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.domain.Difficulty
import com.wakeup.domain.MathParams
import com.wakeup.domain.RepeatDays
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
    private val scheduler: AlarmScheduler,
    activeAlarms: StateFlow<Set<Long>> = MutableStateFlow(emptySet()),
) : ViewModel() {

    private val _draft = MutableStateFlow(Alarm.default(hour = 6, minute = 30))
    val draft: StateFlow<Alarm> = _draft.asStateFlow()

    val isNew: Boolean = alarmId == null

    /** True while this alarm is ringing or snoozed: it can't be changed until the wake-up is done. */
    val locked: StateFlow<Boolean> = activeAlarms
        .map { alarmId in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, alarmId in activeAlarms.value)

    init {
        if (alarmId != null) viewModelScope.launch {
            repository.getAlarm(alarmId)?.let { _draft.value = it }
        }
    }

    fun setTime(hour: Int, minute: Int) = _draft.update { it.copy(hour = hour, minute = minute) }

    fun setHour(hour: Int) = _draft.update { it.copy(hour = hour) }

    fun setMinute(minute: Int) = _draft.update { it.copy(minute = minute) }

    fun setLabel(label: String) = _draft.update { it.copy(label = label) }

    fun toggleRepeatDay(dayIndex: Int) =
        _draft.update { it.copy(repeatDays = RepeatDays.toggle(it.repeatDays, dayIndex)) }

    fun setSquatsEnabled(enabled: Boolean) =
        setMissions(squat = if (enabled) squat ?: SquatParams.DEFAULT else null)

    fun setSquatCount(count: Int) = setMissions(squat = SquatParams(count.coerceIn(SquatParams.COUNT_RANGE)))

    fun setMathEnabled(enabled: Boolean) =
        setMissions(math = if (enabled) math ?: MathParams.DEFAULT else null)

    fun setMathDifficulty(difficulty: Difficulty) =
        setMissions(math = (math ?: MathParams.DEFAULT).copy(difficulty = difficulty))

    fun setMathCount(count: Int) =
        setMissions(math = (math ?: MathParams.DEFAULT).copy(problemCount = count.coerceIn(MathParams.COUNT_RANGE)))

    private val squat: SquatParams? get() = _draft.value.squat
    private val math: MathParams? get() = _draft.value.math

    /** The wake-up flow always runs squats first, then math. */
    private fun setMissions(squat: SquatParams? = this.squat, math: MathParams? = this.math) =
        _draft.update { it.copy(missions = listOfNotNull(squat, math)) }

    fun save(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (locked.value) return@launch
        val id = repository.upsert(_draft.value)
        scheduler.schedule(_draft.value.copy(id = id))
        onSaved(id)
    }

    fun delete(onDeleted: () -> Unit = {}) = viewModelScope.launch {
        if (locked.value) return@launch
        if (alarmId != null) {
            scheduler.cancel(alarmId)
            repository.delete(alarmId)
        }
        onDeleted()
    }

    companion object {
        fun factory(alarmId: Long?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmEditViewModel(Graph.repository, alarmId, Graph.scheduler, Graph.ringState.activeIds) as T
        }
    }
}
