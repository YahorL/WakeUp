package com.wakeup.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakeup.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAlarmRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: AlarmRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        repo = RoomAlarmRepository(db.alarmDao())
    }

    @After fun tearDown() = db.close()

    @Test fun upsertThenReadPreservesMissionsAndOrder() = runBlocking {
        val alarm = Alarm(
            hour = 7, minute = 30, label = "Gym", repeatDays = RepeatDays.toggle(RepeatDays.NONE, 0),
            missions = listOf(MathParams(Difficulty.HARD, 2), SquatParams(15)),
        )
        val id = repo.upsert(alarm)
        val loaded = repo.getAlarm(id)!!

        assertEquals(7, loaded.hour)
        assertEquals("Gym", loaded.label)
        assertEquals(listOf(MathParams(Difficulty.HARD, 2), SquatParams(15)), loaded.missions)
    }

    @Test fun upsertReplacesMissions() = runBlocking {
        val id = repo.upsert(Alarm(hour = 6, minute = 0, missions = listOf(SquatParams(5))))
        val updated = repo.getAlarm(id)!!.copy(missions = listOf(MathParams(Difficulty.EASY, 1)))
        repo.upsert(updated)
        val loaded = repo.getAlarm(id)!!
        assertEquals(listOf(MathParams(Difficulty.EASY, 1)), loaded.missions)
    }

    @Test fun observeAndEnabledFilter() = runBlocking {
        repo.upsert(Alarm(hour = 5, minute = 0, enabled = true))
        repo.upsert(Alarm(hour = 6, minute = 0, enabled = false))
        assertEquals(2, repo.observeAlarms().first().size)
        assertEquals(1, repo.enabledAlarms().size)
    }
}
