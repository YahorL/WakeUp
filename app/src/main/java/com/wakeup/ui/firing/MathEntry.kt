package com.wakeup.ui.firing

/** What is typed into the math answer field. [wrong] marks a submitted answer that was incorrect. */
data class MathEntry(val text: String = "", val wrong: Boolean = false)

sealed interface MathKey {
    data class Digit(val d: Int) : MathKey
    data object Back : MathKey
    data object Submit : MathKey
}

enum class MathOutcome { NONE, CORRECT, WRONG }

private const val MAX_DIGITS = 5

/**
 * Applies one keypad press. Only [MathKey.Submit] checks the answer. A wrong answer stays on
 * screen until the next press: a digit starts a fresh entry, backspace clears it, and submitting
 * again does nothing — nobody half-awake should have to delete a wrong answer by hand.
 */
fun MathEntry.press(key: MathKey, answer: Int): Pair<MathEntry, MathOutcome> {
    if (wrong) {
        return when (key) {
            is MathKey.Digit -> MathEntry(key.d.toString())
            MathKey.Back -> MathEntry()
            MathKey.Submit -> this
        } to MathOutcome.NONE
    }
    return when (key) {
        is MathKey.Digit -> copy(text = (text + key.d).take(MAX_DIGITS)) to MathOutcome.NONE
        MathKey.Back -> copy(text = text.dropLast(1)) to MathOutcome.NONE
        MathKey.Submit -> when (text.toIntOrNull()) {
            null -> this to MathOutcome.NONE
            answer -> MathEntry() to MathOutcome.CORRECT
            else -> copy(wrong = true) to MathOutcome.WRONG
        }
    }
}
