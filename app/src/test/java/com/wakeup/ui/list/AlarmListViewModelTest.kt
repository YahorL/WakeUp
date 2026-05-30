package com.wakeup.ui.list

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.Alarm
import com.wakeup.schedule.NoopScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun togglePersistsEnabledState() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0, enabled = true))
        val vm = AlarmListViewModel(repo, NoopScheduler())
        vm.setEnabled(id, false)
        advanceUntilIdle()
        assertFalse(repo.getAlarm(id)!!.enabled)
    }

    @Test fun deleteRemovesAlarm() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0))
        val vm = AlarmListViewModel(repo, NoopScheduler())
        vm.delete(id)
        advanceUntilIdle()
        assertNull(repo.getAlarm(id))
    }

    @Test fun exposesAlarmsFromRepository() = runTest {
        val repo = FakeAlarmRepository()
        repo.upsert(Alarm(hour = 6, minute = 0))
        val vm = AlarmListViewModel(repo, NoopScheduler())
        advanceUntilIdle()
        assertEquals(1, vm.alarms.first().size)
    }
}
