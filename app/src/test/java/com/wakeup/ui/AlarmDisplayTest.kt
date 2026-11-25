package com.wakeup.ui

import com.wakeup.domain.Difficulty
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmDisplayTest {
    @Test fun clock24hPadsTheHour() = assertEquals(ClockText("06:30", null), formatClock(6, 30, is24h = true))
    @Test fun clock12hMidnightIsTwelveAm() = assertEquals(ClockText("12:00", "AM"), formatClock(0, 0, is24h = false))
    @Test fun clock12hNoonIsTwelvePm() = assertEquals(ClockText("12:00", "PM"), formatClock(12, 0, is24h = false))
    @Test fun clock12hMorningHasNoLeadingZero() = assertEquals(ClockText("6:30", "AM"), formatClock(6, 30, is24h = false))
    @Test fun clock12hEvening() = assertEquals(ClockText("5:45", "PM"), formatClock(17, 45, is24h = false))
    @Test fun inlineClockAppendsThePeriod() {
        assertEquals("06:30", formatClock(6, 30, true).inline())
        assertEquals("6:30 AM", formatClock(6, 30, false).inline())
    }

    @Test fun countdownWithHours() = assertEquals("8 h 44 min", formatCountdown(8 * 3_600_000L + 44 * 60_000L))
    @Test fun countdownUnderAnHour() = assertEquals("44 min", formatCountdown(44 * 60_000L + 59_000L))
    @Test fun countdownNeverNegative() = assertEquals("0 min", formatCountdown(-5_000L))

    @Test fun repeatSummaryNamesTheCommonSets() {
        assertEquals("Once", repeatSummary(0))
        assertEquals("Every day", repeatSummary(0b1111111))
        assertEquals("Weekdays", repeatSummary(0b0011111))
        assertEquals("Weekends", repeatSummary(0b1100000))
    }
    @Test fun repeatSummaryListsOtherDaysMondayFirst() = assertEquals("Mon, Wed", repeatSummary(0b0000101))

    @Test fun daysTextUsesRangesForTheCommonSets() {
        assertEquals("Once", daysText(0))
        assertEquals("Every day", daysText(0b1111111))
        assertEquals("Mon – Fri", daysText(0b0011111))
        assertEquals("Sat, Sun", daysText(0b1100000))
        assertEquals("Tue, Thu", daysText(0b0001010))
    }

    @Test fun tickIndexSpansTheRuler() {
        assertEquals(0, tickIndex(5))
        assertEquals(7, tickIndex(15))
        assertEquals(33, tickIndex(50))
    }
    @Test fun tickIndexClampsOutOfRangeReps() {
        assertEquals(0, tickIndex(1))
        assertEquals(33, tickIndex(80))
    }

    private val squats = SquatParams(15)
    private val math = MathParams(Difficulty.MEDIUM, 3)

    @Test fun summaryForBothChallenges() = assertEquals(
        "You'll do 15 squats and solve 3 medium problems to turn this alarm off.",
        challengeSummary(squats, math),
    )
    @Test fun summaryUsesSingulars() = assertEquals(
        "You'll do 1 squat and solve 1 medium problem to turn this alarm off.",
        challengeSummary(SquatParams(1), MathParams(Difficulty.MEDIUM, 1)),
    )
    @Test fun summaryForSquatsOnly() = assertEquals("You'll do 15 squats to turn this alarm off.", challengeSummary(squats, null))
    @Test fun summaryForMathOnly() = assertEquals("You'll solve 3 medium problems to turn this alarm off.", challengeSummary(null, math))
    @Test fun summaryForNoChallenge() =
        assertEquals("No challenge — this alarm stops with a single tap.", challengeSummary(null, null))

    @Test fun recapForBothChallenges() = assertEquals("15 squats and 3 problems done. Good morning.", recapText(squats, math))
    @Test fun recapUsesSingulars() =
        assertEquals("1 squat and 1 problem done. Good morning.", recapText(SquatParams(1), MathParams(Difficulty.EASY, 1)))
    @Test fun recapForOneChallenge() {
        assertEquals("15 squats done. Good morning.", recapText(squats, null))
        assertEquals("3 problems done. Good morning.", recapText(null, math))
    }
    @Test fun recapForNoChallenge() = assertEquals("Good morning.", recapText(null, null))

    @Test fun countsUseSingularForOne() {
        assertEquals("1 squat", squatsText(1))
        assertEquals("15 squats", squatsText(15))
        assertEquals("1 problem", problemsText(1))
        assertEquals("3 problems", problemsText(3))
        assertEquals("1 rep", repsText(1))
        assertEquals("15 reps", repsText(15))
    }
}
