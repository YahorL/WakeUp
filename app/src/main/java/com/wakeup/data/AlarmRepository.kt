package com.wakeup.data

import com.wakeup.domain.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun observeAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: Long): Alarm?
    suspend fun enabledAlarms(): List<Alarm>
    /** Inserts or updates the alarm (and replaces its missions). Returns the alarm id. */
    suspend fun upsert(alarm: Alarm): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun delete(id: Long)
}
