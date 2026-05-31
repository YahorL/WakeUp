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

    private val _draft = MutableStateFlow(Alarm.default(hour = 6, minute = 30))
    val draft: StateFlow<Alarm> = _draft.asStateFlow()

    val isNew: Boolean = alarmId == null

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

    // ── Design-model challenge helpers (at most one squat + one math, squats-first) ──

    private fun currentSquat(): SquatParams? =
        _draft.value.missions.map { it.params }.filterIsInstance<SquatParams>().firstOrNull()

    private fun currentMath(): MathParams? =
        _draft.value.missions.map { it.params }.filterIsInstance<MathParams>().firstOrNull()

    /** Rebuild the mission list in the canonical wake-up order: squats first, then math. */
    private fun apply(squat: SquatParams?, math: MathParams?) {
        val list = buildList {
            if (squat != null) add(MissionConfig(order = size, type = MissionType.SQUAT, params = squat))
            if (math != null) add(MissionConfig(order = size, type = MissionType.MATH, params = math))
        }
        _draft.value = _draft.value.copy(missions = list)
    }

    fun setSquatsEnabled(enabled: Boolean) = apply(if (enabled) currentSquat() ?: SquatParams(15) else null, currentMath())
    fun setSquatCount(count: Int) = apply(SquatParams(count.coerceIn(1, 50)), currentMath())
    fun setMathEnabled(enabled: Boolean) = apply(currentSquat(), if (enabled) currentMath() ?: MathParams(Difficulty.MEDIUM, 3) else null)
    fun setMathDifficulty(difficulty: Difficulty) = apply(currentSquat(), (currentMath() ?: MathParams(Difficulty.MEDIUM, 3)).copy(difficulty = difficulty))
    fun setMathCount(count: Int) = apply(currentSquat(), (currentMath() ?: MathParams(Difficulty.MEDIUM, 3)).copy(problemCount = count.coerceIn(1, 10)))

    fun save(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.upsert(_draft.value)
        scheduler.schedule(_draft.value.copy(id = id))
        onSaved(id)
    }

    fun delete(onDeleted: () -> Unit = {}) = viewModelScope.launch {
        if (alarmId != null) {
            scheduler.cancel(alarmId)
            repository.delete(alarmId)
        }
        onDeleted()
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
