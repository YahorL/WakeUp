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
