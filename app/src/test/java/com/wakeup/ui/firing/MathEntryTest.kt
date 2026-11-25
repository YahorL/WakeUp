package com.wakeup.ui.firing

import org.junit.Assert.assertEquals
import org.junit.Test

class MathEntryTest {
    private fun type(vararg digits: Int, from: MathEntry = MathEntry(), answer: Int = 42): MathEntry =
        digits.fold(from) { entry, d -> entry.press(MathKey.Digit(d), answer).first }

    @Test fun digitsBuildTheAnswer() = assertEquals(MathEntry("42"), type(4, 2))

    @Test fun typingNeverSubmits() =
        assertEquals(MathOutcome.NONE, MathEntry("4").press(MathKey.Digit(2), answer = 42).second)

    @Test fun aSixthDigitIsIgnored() = assertEquals(MathEntry("12345"), type(1, 2, 3, 4, 5, 6))

    @Test fun backspaceRemovesTheLastDigit() =
        assertEquals(MathEntry("4") to MathOutcome.NONE, MathEntry("42").press(MathKey.Back, 42))

    @Test fun submittingNothingDoesNothing() =
        assertEquals(MathEntry() to MathOutcome.NONE, MathEntry().press(MathKey.Submit, 42))

    @Test fun submittingTheAnswerIsCorrectAndClears() =
        assertEquals(MathEntry() to MathOutcome.CORRECT, MathEntry("42").press(MathKey.Submit, 42))

    @Test fun leadingZerosStillCount() =
        assertEquals(MathOutcome.CORRECT, MathEntry("07").press(MathKey.Submit, 7).second)

    @Test fun submittingAnotherNumberIsWrongAndStaysOnScreen() =
        assertEquals(MathEntry("290", wrong = true) to MathOutcome.WRONG, MathEntry("290").press(MathKey.Submit, 300))

    private val wrong = MathEntry("290", wrong = true)

    @Test fun aDigitAfterAWrongAnswerStartsAFreshEntry() =
        assertEquals(MathEntry("3") to MathOutcome.NONE, wrong.press(MathKey.Digit(3), 300))

    @Test fun backspaceAfterAWrongAnswerClearsIt() =
        assertEquals(MathEntry() to MathOutcome.NONE, wrong.press(MathKey.Back, 300))

    @Test fun submitAfterAWrongAnswerDoesNothing() =
        assertEquals(wrong to MathOutcome.NONE, wrong.press(MathKey.Submit, 300))
}
