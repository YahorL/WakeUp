package com.wakeup.di

import android.content.Context
import androidx.room.Room
import com.wakeup.data.AlarmRepository
import com.wakeup.data.AppDatabase
import com.wakeup.data.RoomAlarmRepository

object Graph {
    lateinit var repository: AlarmRepository
        private set

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val db = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "wakeup.db",
        ).build()
        repository = RoomAlarmRepository(db.alarmDao())
        initialized = true
    }
}
