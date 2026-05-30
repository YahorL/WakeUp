package com.wakeup.mission

import com.wakeup.domain.Difficulty
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MathProblemGeneratorTest {
    private fun gen() = MathProblemGenerator(Random(42)) // seeded → deterministic

    @Test fun easyProblemsAreSmallAndNonNegative() {
        val g = gen()
        repeat(200) {
            val p = g.generate(Difficulty.EASY)
            assertTrue("answer ${p.answer} should be >= 0", p.answer >= 0)
            assertTrue(p.answer <= 18)
            assertTrue(p.text.isNotBlank())
        }
    }

    @Test fun textEvaluatesToAnswerForAllDifficulties() {
        val g = gen()
        for (d in Difficulty.values()) {
            repeat(200) {
                val p = g.generate(d)
                assertEquals(p.answer, MathProblemGeneratorTestEval.eval(p.text))
            }
        }
    }

    @Test fun hardIsMultiStep() {
        val g = gen()
        val hasTwoOperators = (0 until 100).any { g.generate(Difficulty.HARD).text.count { c -> c in "+-*" } >= 2 }
        assertTrue("hard problems should sometimes be multi-step", hasTwoOperators)
    }
}

/** Tiny left-to-right evaluator used only to verify generator output in tests. */
object MathProblemGeneratorTestEval {
    fun eval(expr: String): Int {
        val tokens = expr.replace("×", "*").replace("÷", "/").split(" ").filter { it.isNotBlank() }
        var acc = tokens[0].toInt()
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]; val n = tokens[i + 1].toInt()
            acc = when (op) { "+" -> acc + n; "-" -> acc - n; "*" -> acc * n; else -> error("op $op") }
            i += 2
        }
        return acc
    }
}
