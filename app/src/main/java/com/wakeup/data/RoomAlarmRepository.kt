package com.wakeup.data

import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.MissionParams
import com.wakeup.domain.MissionParamsCodec
import com.wakeup.domain.SquatParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {

    override fun observeAlarms(): Flow<List<Alarm>> =
        dao.observeAlarmsWithMissions().map { list -> list.map { it.toDomain() } }

    override suspend fun getAlarm(id: Long): Alarm? = dao.getAlarmWithMissions(id)?.toDomain()

    override suspend fun enabledAlarms(): List<Alarm> = dao.getEnabledAlarms().map { it.toDomain() }

    override suspend fun upsert(alarm: Alarm): Long {
        val id = dao.insertAlarm(alarm.toEntity()) // REPLACE handles updates by id
        dao.deleteMissionsFor(id)
        dao.insertMissions(alarm.missions.mapIndexed { order, params -> params.toEntity(id, order) })
        return id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun delete(id: Long) = dao.deleteAlarm(id)
}

private fun AlarmWithMissions.toDomain(): Alarm = Alarm(
    id = alarm.id, hour = alarm.hour, minute = alarm.minute, label = alarm.label,
    enabled = alarm.enabled, repeatDays = alarm.repeatDays, soundUri = alarm.soundUri,
    vibrate = alarm.vibrate, gradualVolume = alarm.gradualVolume, rampSeconds = alarm.rampSeconds,
    snoozeEnabled = alarm.snoozeEnabled, snoozeMinutes = alarm.snoozeMinutes, maxSnoozes = alarm.maxSnoozes,
    missions = missions.sortedBy { it.order }.map { MissionParamsCodec.decode(it.params) },
)

private fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id, hour = hour, minute = minute, label = label, enabled = enabled, repeatDays = repeatDays,
    soundUri = soundUri, vibrate = vibrate, gradualVolume = gradualVolume, rampSeconds = rampSeconds,
    snoozeEnabled = snoozeEnabled, snoozeMinutes = snoozeMinutes, maxSnoozes = maxSnoozes,
)

private fun MissionParams.toEntity(alarmId: Long, order: Int): MissionConfigEntity = MissionConfigEntity(
    alarmId = alarmId,
    order = order,
    type = when (this) {
        is MathParams -> "MATH"
        is SquatParams -> "SQUAT"
    },
    params = MissionParamsCodec.encode(this),
)
