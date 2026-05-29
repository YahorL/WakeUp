package com.wakeup.di

import android.content.Context

/**
 * Tiny hand-rolled service locator held by [com.wakeup.WakeUpApp].
 * Typed singletons (`repository`, `scheduler`) are added in later tasks
 * (data layer / scheduler) once those types exist.
 */
object Graph {
    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        // repository wired in the data-layer task; scheduler wired in the scheduler task.
        initialized = true
    }
}
