package com.wakeup.ui.edit

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.*
import com.wakeup.schedule.NoopScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun newAlarmStartsFromDefaults() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        val d = vm.draft.value
        assertEquals(2, d.missions.size)
        assertEquals(MissionType.MATH, d.missions[0].type)
    }

    @Test fun editingFieldsUpdatesDraft() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setTime(6, 15)
        vm.setLabel("Run")
        vm.toggleRepeatDay(0)
        val d = vm.draft.value
        assertEquals(6, d.hour); assertEquals(15, d.minute)
        assertEquals("Run", d.label)
        assertTrue(RepeatDays.isSet(d.repeatDays, 0))
    }

    @Test fun addRemoveAndReorderMissions() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.removeMission(0)
        assertEquals(1, vm.draft.value.missions.size)
        vm.addMission(MissionType.MATH)
        assertEquals(2, vm.draft.value.missions.size)
        assertEquals(listOf(0, 1), vm.draft.value.missions.map { it.order })
    }

    @Test fun saveWritesToRepository() = runTest {
        val repo = FakeAlarmRepository()
        val vm = AlarmEditViewModel(repo, alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setTime(9, 0)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.enabledAlarms().size)
        assertEquals(9, repo.enabledAlarms()[0].hour)
    }
}
