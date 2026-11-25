package com.wakeup

import android.app.Application
import com.wakeup.di.Graph

class WakeUpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        // A fresh process means nothing is ringing. If a wake-up was cut short (the process was
        // killed or the app force-stopped mid-ring or mid-snooze), ring it again.
        // Must never throw (e.g. exact-alarm access revoked): this runs on every launch.
        runCatching { Graph.wakeUps.restoreLostWakeUps() }
    }
}
