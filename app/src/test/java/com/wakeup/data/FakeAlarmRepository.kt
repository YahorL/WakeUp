package com.wakeup.data

import com.wakeup.domain.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmRepository : AlarmRepository {
    private val items = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override fun observeAlarms() = items.map { it.sortedWith(compareBy({ a -> a.hour }, { a -> a.minute })) }
    override suspend fun getAlarm(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun enabledAlarms() = items.value.filter { it.enabled }
    override suspend fun upsert(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) nextId++ else alarm.id
        val saved = alarm.copy(id = id)
        items.value = items.value.filter { it.id != id } + saved
        return id
    }
    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }
    override suspend fun delete(id: Long) { items.value = items.value.filter { it.id != id } }
}
