package com.wakeup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Transaction
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAlarmsWithMissions(): Flow<List<AlarmWithMissions>>

    @Transaction
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmWithMissions(id: Long): AlarmWithMissions?

    @Transaction
    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabledAlarms(): List<AlarmWithMissions>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    @Query("DELETE FROM mission_configs WHERE alarmId = :alarmId")
    suspend fun deleteMissionsFor(alarmId: Long)

    @Insert
    suspend fun insertMissions(missions: List<MissionConfigEntity>)
}
