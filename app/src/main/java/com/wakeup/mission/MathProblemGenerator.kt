package com.wakeup.mission

import com.wakeup.domain.Difficulty
import kotlin.random.Random

class MathProblemGenerator(private val random: Random = Random.Default) {

    fun generate(difficulty: Difficulty): MathProblem = when (difficulty) {
        Difficulty.EASY -> easy()
        Difficulty.MEDIUM -> medium()
        Difficulty.HARD -> hard()
    }

    private fun easy(): MathProblem {
        val a = random.nextInt(0, 10)
        val b = random.nextInt(0, 10)
        return if (random.nextBoolean() || a < b) {
            MathProblem("$a + $b", a + b)
        } else {
            MathProblem("$a - $b", a - b)
        }
    }

    private fun medium(): MathProblem {
        return when (random.nextInt(3)) {
            0 -> { val a = random.nextInt(10, 100); val b = random.nextInt(10, 100); MathProblem("$a + $b", a + b) }
            1 -> { val a = random.nextInt(10, 100); val b = random.nextInt(0, a + 1); MathProblem("$a - $b", a - b) }
            else -> { val a = random.nextInt(2, 13); val b = random.nextInt(2, 13); MathProblem("$a * $b", a * b) }
        }
    }

    private fun hard(): MathProblem {
        val a = random.nextInt(2, 13)
        val b = random.nextInt(2, 13)
        val c = random.nextInt(1, 50)
        return if (random.nextBoolean()) {
            MathProblem("$a * $b + $c", a * b + c)
        } else {
            MathProblem("$a * $b - $c", a * b - c)
        }
    }
}
