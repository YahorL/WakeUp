package com.wakeup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /** Inserts or updates [alarm] and replaces its missions, all or nothing. Returns the alarm id. */
    @Transaction
    suspend fun upsertWithMissions(alarm: AlarmEntity, missions: List<MissionConfigEntity>): Long {
        val id = insertAlarm(alarm) // REPLACE handles updates by id
        deleteMissionsFor(id)
        insertMissions(missions.map { it.copy(alarmId = id) })
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    @Query("DELETE FROM mission_configs WHERE alarmId = :alarmId")
    suspend fun deleteMissionsFor(alarmId: Long)

    @Insert
    suspend fun insertMissions(missions: List<MissionConfigEntity>)
}
