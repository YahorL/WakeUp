package com.wakeup.alarm

class InMemoryRingStore : RingState.Store {
    var saved: Map<Long, RingState.Entry> = emptyMap()
    override fun load() = saved
    override fun save(entries: Map<Long, RingState.Entry>) { saved = entries }
}
