package com.wakeup.alarm

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks every alarm whose wake-up is unfinished: it has fired and is either ringing or snoozed.
 * Such an alarm is "active" and locked — it can't be edited, deleted, rescheduled or cancelled
 * until its missions are done, so there is no way around the challenge.
 *
 * Active alarms are persisted (they must survive process death and reboots). Which alarm is
 * *ringing right now* is deliberately in-memory only: after a restart nothing is ringing, and
 * [WakeUpCoordinator.restoreLostWakeUps] rings the interrupted ones again.
 */
class RingState(private val store: Store) {
    /** [snoozeAt] is when the pending snooze fires, or null if the alarm was last seen ringing. */
    data class Entry(val snoozesUsed: Int, val snoozeAt: Long?)

    interface Store {
        fun load(): Map<Long, Entry>
        fun save(entries: Map<Long, Entry>)
    }

    private var entries: Map<Long, Entry> = store.load()
    private val _activeIds = MutableStateFlow(entries.keys)

    /** Ids of all active alarms, for the UI to show them as locked. */
    val activeIds: StateFlow<Set<Long>> = _activeIds.asStateFlow()

    /** The one alarm that is sounding right now, if any. */
    @Volatile
    var ringingId: Long? = null
        private set

    fun isActive(alarmId: Long): Boolean = alarmId in _activeIds.value

    @Synchronized
    fun entries(): Map<Long, Entry> = entries

    /**
     * Claims the single ringing slot for [alarmId]. Returns false if a different alarm is already
     * ringing: only one wake-up runs at a time, and the caller should count this one as done.
     */
    @Synchronized
    fun startRinging(alarmId: Long, snoozesUsed: Int): Boolean {
        val ringing = ringingId
        if (ringing != null && ringing != alarmId) return false
        ringingId = alarmId
        update(entries + (alarmId to Entry(snoozesUsed, snoozeAt = null)))
        return true
    }

    /** The sound stopped but the wake-up isn't finished (a snooze is about to be scheduled). */
    @Synchronized
    fun stopRinging(alarmId: Long) {
        if (ringingId == alarmId) ringingId = null
    }

    @Synchronized
    fun snoozed(alarmId: Long, snoozesUsed: Int, snoozeAt: Long) {
        stopRinging(alarmId)
        update(entries + (alarmId to Entry(snoozesUsed, snoozeAt)))
    }

    /** The wake-up is over: the alarm is unlocked. */
    @Synchronized
    fun finish(alarmId: Long) {
        stopRinging(alarmId)
        update(entries - alarmId)
    }

    private fun update(next: Map<Long, Entry>) {
        entries = next
        store.save(next)
        _activeIds.value = next.keys
    }
}

/** Persists active alarms as "id:snoozesUsed:snoozeAt" strings (snoozeAt 0 = ringing). */
class PrefsRingStore(context: Context) : RingState.Store {
    private val prefs = context.applicationContext.getSharedPreferences("wakeup_ring", Context.MODE_PRIVATE)

    override fun load(): Map<Long, RingState.Entry> =
        prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(::decode).toMap()

    override fun save(entries: Map<Long, RingState.Entry>) {
        val encoded = entries.map { (id, entry) -> "$id:${entry.snoozesUsed}:${entry.snoozeAt ?: 0}" }.toSet()
        prefs.edit().putStringSet(KEY, encoded).remove(LEGACY_KEY).apply()
    }

    private fun decode(text: String): Pair<Long, RingState.Entry>? {
        val parts = text.split(":")
        val id = parts.getOrNull(0)?.toLongOrNull() ?: return null
        val snoozesUsed = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val snoozeAt = parts.getOrNull(2)?.toLongOrNull() ?: return null
        return id to RingState.Entry(snoozesUsed, snoozeAt.takeIf { it > 0 })
    }

    private companion object {
        const val KEY = "active_alarms"
        const val LEGACY_KEY = "active_alarm_id" // single id written by older builds; never read
    }
}
