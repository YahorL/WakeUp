package com.wakeup.di

import android.content.Context
import androidx.room.Room
import com.wakeup.data.AlarmRepository
import com.wakeup.data.AppDatabase
import com.wakeup.data.RoomAlarmRepository
import com.wakeup.alarm.RingState
import com.wakeup.schedule.AlarmManagerScheduler
import com.wakeup.schedule.AlarmScheduler
import com.wakeup.settings.SettingsStore

object Graph {
    lateinit var repository: AlarmRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var ringState: RingState
        private set

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val db = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "wakeup.db",
        ).build()
        repository = RoomAlarmRepository(db.alarmDao())
        scheduler = AlarmManagerScheduler(context.applicationContext)
        settings = SettingsStore(context.applicationContext)
        ringState = RingState(context.applicationContext)
        initialized = true
    }
}
