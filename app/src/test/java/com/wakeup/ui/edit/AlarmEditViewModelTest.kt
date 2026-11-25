package com.wakeup.ui.edit

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.*
import com.wakeup.schedule.NoopScheduler
import com.wakeup.schedule.RecordingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
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
        assertEquals(listOf(SquatParams.DEFAULT, MathParams.DEFAULT), d.missions)
    }

    @Test fun squatAndMathHelpersMaintainSquatFirstOrder() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setSquatsEnabled(false)
        assertEquals(listOf(MathParams.DEFAULT), vm.draft.value.missions)
        vm.setSquatsEnabled(true)
        assertEquals(listOf(SquatParams.DEFAULT, MathParams.DEFAULT), vm.draft.value.missions)
        vm.setMathEnabled(false)
        assertEquals(listOf(SquatParams.DEFAULT), vm.draft.value.missions)
    }

    @Test fun editingFieldsUpdatesDraft() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setTime(6, 15)
        vm.setLabel("Run")
        vm.toggleRepeatDay(6) // Sunday — off in the weekday default
        val d = vm.draft.value
        assertEquals(6, d.hour); assertEquals(15, d.minute)
        assertEquals("Run", d.label)
        assertTrue(RepeatDays.isSet(d.repeatDays, 6))
    }

    @Test fun challengeSettingsAreClampedAndKeepTheOtherMission() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setSquatCount(99)
        vm.setMathCount(0)
        vm.setMathDifficulty(Difficulty.HARD)
        assertEquals(listOf(SquatParams(50), MathParams(Difficulty.HARD, 1)), vm.draft.value.missions)
    }

    @Test fun upToFiftyMathProblemsCanBeRequired() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setMathCount(50)
        assertEquals(50, vm.draft.value.math?.problemCount)
        vm.setMathCount(51)
        assertEquals(50, vm.draft.value.math?.problemCount)
    }

    @Test fun alarmThatIsRingingOrSnoozedCannotBeSavedOrDeleted() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0, label = "Original"))
        val scheduler = RecordingScheduler()
        val vm = AlarmEditViewModel(repo, alarmId = id, scheduler = scheduler, activeAlarms = MutableStateFlow(setOf(id)))
        advanceUntilIdle()
        assertTrue(vm.locked.value)

        vm.setLabel("Edited")
        vm.setSquatsEnabled(false)
        var finished = false
        vm.save { finished = true }
        vm.delete { finished = true }
        advanceUntilIdle()

        assertEquals("Original", repo.getAlarm(id)!!.label)
        assertTrue(scheduler.scheduled.isEmpty() && scheduler.cancelled.isEmpty())
        assertFalse("the editor must stay open", finished)
    }

    @Test fun alarmUnlocksOnceItsWakeUpIsFinished() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0))
        val active = MutableStateFlow(setOf(id))
        val vm = AlarmEditViewModel(repo, alarmId = id, scheduler = NoopScheduler(), activeAlarms = active)
        advanceUntilIdle()
        active.value = emptySet()
        advanceUntilIdle()
        assertFalse(vm.locked.value)
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

    @Test fun hourAndMinuteCanBeSetIndependently() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())
        advanceUntilIdle()
        vm.setTime(6, 30)
        // Two wheels settling back to back must not overwrite each other's value.
        vm.setHour(7)
        vm.setMinute(45)
        assertEquals(7 to 45, vm.draft.value.hour to vm.draft.value.minute)
    }
}
