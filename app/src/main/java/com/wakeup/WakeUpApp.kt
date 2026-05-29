package com.wakeup

import android.app.Application
import com.wakeup.di.Graph

class WakeUpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
