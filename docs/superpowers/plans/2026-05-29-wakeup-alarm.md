# WakeUp Alarm App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Alarmy-style Android alarm that fires reliably over the lock screen and can only be dismissed by completing configurable math and/or squat "missions".

**Architecture:** Single-module Kotlin/Jetpack Compose app, MVVM. Pure-logic core (next-trigger computation, math generation, squat counting, snooze policy) is unit-tested with JUnit. Room persists alarms + ordered missions. `AlarmManager.setAlarmClock()` schedules; a foreground `AlarmService` rings; a full-screen-intent `AlarmActivity` hosts the mission flow and enforces escape-proofing.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room (KSP), kotlinx.serialization, Coroutines/Flow, Navigation-Compose, AlarmManager, SensorManager (accelerometer). minSdk 26, targetSdk = latest stable. Test device: Google Pixel 9.

---

## Conventions

- **Dependency injection:** a tiny hand-rolled `Graph` service locator held by the `Application` (no Hilt — YAGNI for a single-module personal app). ViewModels, the receiver, and the service all read singletons from `Graph`.
- **Weekday encoding:** `repeatDays` is a 7-bit mask. **Bit `i` (0..6) corresponds to `java.time.DayOfWeek.of(i + 1)`** — i.e. bit 0 = MONDAY … bit 6 = SUNDAY. `0` means a one-shot alarm.
- **Tests:** pure JVM logic → `src/test` (JUnit4 + `kotlinx-coroutines-test`). Anything touching Room or Android framework → `src/androidTest` (instrumented, runs on the connected Pixel 9). The squat algorithm is pure and lives in `src/test`.
- **Commit style:** `feat:`, `test:`, `chore:` prefixes; commit after each task's tests are green.
- **Time:** all scheduling math takes an injected `ZonedDateTime`/`Clock` so it is deterministic in tests. Never call `System.currentTimeMillis()` inside testable logic.

## File structure

```
app/src/main/java/com/wakeup/
  WakeUpApp.kt                 # Application + Graph.init()
  di/Graph.kt                  # service locator (repository, scheduler, db)
  domain/
    Difficulty.kt              # enum
    MissionType.kt             # enum
    RepeatDays.kt              # bit-mask helpers
    MissionParams.kt           # sealed serializable params + codec
    Models.kt                  # Alarm, MissionConfig domain models
  data/
    AlarmEntity.kt             # @Entity alarms
    MissionConfigEntity.kt     # @Entity mission_configs + AlarmWithMissions
    AlarmDao.kt                # DAO
    AppDatabase.kt             # RoomDatabase
    AlarmRepository.kt         # interface
    RoomAlarmRepository.kt     # impl + entity<->domain mappers
  schedule/
    NextTrigger.kt             # pure next-fire computation
    SnoozePolicy.kt            # pure snooze rules
    AlarmScheduler.kt          # interface
    AlarmManagerScheduler.kt   # AlarmManager impl
  alarm/
    AlarmContract.kt           # action/extra string constants
    AlarmReceiver.kt           # fire + boot + snooze BroadcastReceiver
    AlarmService.kt            # foreground ringer (sound/vibrate/wakelock/FSI)
    AlarmActivity.kt           # full-screen host + escape-proofing
  mission/
    MathProblem.kt             # data class
    MathProblemGenerator.kt    # pure generator
    SquatCounter.kt            # pure rep-counting state machine
    SquatDetector.kt           # interface
    AccelerometerSquatDetector.kt # SensorManager impl
  ui/
    list/AlarmListScreen.kt    # + AlarmListViewModel
    edit/AlarmEditScreen.kt    # + AlarmEditViewModel + AlarmEditState
    firing/MissionHost.kt      # runs missions in order
    firing/MathMissionContent.kt
    firing/SquatMissionContent.kt
    perms/PermissionsScreen.kt
    theme/...                  # wizard-generated
  MainActivity.kt              # NavHost: list <-> edit, permission gate

app/src/test/java/com/wakeup/      # JVM unit tests (mirrors packages)
app/src/androidTest/java/com/wakeup/  # Room + instrumented tests
```

---

## Phase 0 — Scaffold & dependencies

### Task 1: Create the Compose project and run it on the Pixel 9

**Files:**
- Create: entire Android Studio project at repo root (`app/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`).

This task is manual (uses Android Studio's wizard, which always emits a current, self-consistent Gradle/Kotlin/Compose setup — more reliable than hand-pinning versions).

- [ ] **Step 1: Generate the project**

In Android Studio: **New Project → Empty Activity** (the Compose template). Set:
- Name: `WakeUp`
- Package name: `com.wakeup`
- Save location: `/home/nyx-ai/Projects/Alarm`
- Language: Kotlin, Minimum SDK: **API 26 (Android 8.0)**
- Build configuration language: Kotlin DSL (`build.gradle.kts`).

Let the wizard place files into the existing repo folder (it will create `app/`, Gradle wrapper, and `gradle/libs.versions.toml`).

- [ ] **Step 2: Confirm it builds**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. (First run downloads Gradle/AGP — that's fine.)

- [ ] **Step 3: Run on the Pixel 9**

Enable Developer Options + USB debugging on the phone, plug it in (or pair over Wi-Fi). Then:

Run: `./gradlew installDebug` then launch the app from the launcher, **or** press Run ▶ in Android Studio with the device selected.
Expected: the default "Hello Android" Compose screen appears on the phone.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: scaffold Compose project (minSdk 26)"
```

---

### Task 2: Add dependencies (Room, serialization, coroutines, navigation, lifecycle, test libs)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add versions/libraries to the version catalog**

In `gradle/libs.versions.toml`, under `[versions]` add (accept Android Studio's "update available" suggestions for newer stable versions when prompted):

```toml
ksp = "2.0.21-1.0.28"
room = "2.6.1"
kotlinxSerialization = "1.7.3"
coroutines = "1.9.0"
navigationCompose = "2.8.4"
lifecycleCompose = "2.8.7"
```

Under `[libraries]` add:

```toml
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleCompose" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

Under `[plugins]` add:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

(`kotlin` version.ref already exists from the wizard. If the catalog uses a different Kotlin version, set `ksp` to the matching `<kotlin>-<ksp>` pairing Studio suggests.)

- [ ] **Step 2: Apply plugins and add dependencies in `app/build.gradle.kts`**

In the `plugins { }` block add:

```kotlin
alias(libs.plugins.ksp)
alias(libs.plugins.kotlin.serialization)
```

In `dependencies { }` add:

```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.kotlinx.serialization.json)
implementation(libs.coroutines.android)
implementation(libs.navigation.compose)
implementation(libs.lifecycle.runtime.compose)
implementation(libs.lifecycle.viewmodel.compose)

testImplementation(libs.coroutines.test)
androidTestImplementation(libs.room.testing)
```

- [ ] **Step 3: Sync and build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` (Gradle resolves the new dependencies).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: add Room, serialization, coroutines, navigation deps"
```

---

### Task 3: Manifest permissions, Application class, and Graph stub

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/wakeup/WakeUpApp.kt`
- Create: `app/src/main/java/com/wakeup/di/Graph.kt`

- [ ] **Step 1: Declare permissions and the Application class**

In `AndroidManifest.xml`, add these `<uses-permission>` lines **above** the `<application>` tag:

```xml
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

On the `<application>` tag add `android:name=".WakeUpApp"`.

- [ ] **Step 2: Create the Graph service locator**

```kotlin
package com.wakeup.di

import android.content.Context
import com.wakeup.data.AlarmRepository
import com.wakeup.schedule.AlarmScheduler

object Graph {
    lateinit var repository: AlarmRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        // repository and scheduler are wired in Tasks 7 and 10.
        // Throw a clear error until then so partial states are obvious.
        initialized = true
    }
}
```

- [ ] **Step 3: Create the Application**

```kotlin
package com.wakeup

import android.app.Application
import com.wakeup.di.Graph

class WakeUpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/wakeup/WakeUpApp.kt app/src/main/java/com/wakeup/di/Graph.kt
git commit -m "feat: declare alarm permissions and add Application/Graph"
```

---

## Phase 1 — Domain types & pure helpers (TDD)

### Task 4: Enums and `RepeatDays` bit-mask helpers

**Files:**
- Create: `app/src/main/java/com/wakeup/domain/Difficulty.kt`
- Create: `app/src/main/java/com/wakeup/domain/MissionType.kt`
- Create: `app/src/main/java/com/wakeup/domain/RepeatDays.kt`
- Test: `app/src/test/java/com/wakeup/domain/RepeatDaysTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.domain

import org.junit.Assert.*
import org.junit.Test

class RepeatDaysTest {
    @Test fun noneIsOneShot() {
        assertTrue(RepeatDays.isOneShot(RepeatDays.NONE))
        assertEquals(emptyList<Int>(), RepeatDays.activeDays(RepeatDays.NONE))
    }

    @Test fun toggleSetsAndClearsBit() {
        var mask = RepeatDays.NONE
        mask = RepeatDays.toggle(mask, 0) // Monday
        assertTrue(RepeatDays.isSet(mask, 0))
        assertFalse(RepeatDays.isSet(mask, 1))
        mask = RepeatDays.toggle(mask, 0)
        assertFalse(RepeatDays.isSet(mask, 0))
    }

    @Test fun activeDaysReturnsSortedIndices() {
        var mask = RepeatDays.NONE
        mask = RepeatDays.toggle(mask, 6) // Sunday
        mask = RepeatDays.toggle(mask, 0) // Monday
        mask = RepeatDays.toggle(mask, 4) // Friday
        assertEquals(listOf(0, 4, 6), RepeatDays.activeDays(mask))
        assertFalse(RepeatDays.isOneShot(mask))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.domain.RepeatDaysTest"`
Expected: FAIL — `RepeatDays` unresolved.

- [ ] **Step 3: Write the implementations**

`Difficulty.kt`:
```kotlin
package com.wakeup.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty { EASY, MEDIUM, HARD }
```

`MissionType.kt`:
```kotlin
package com.wakeup.domain

enum class MissionType { MATH, SQUAT }
```

`RepeatDays.kt`:
```kotlin
package com.wakeup.domain

/** 7-bit weekday mask. Bit i (0..6) == DayOfWeek.of(i + 1): bit 0 = Monday .. bit 6 = Sunday. */
object RepeatDays {
    const val NONE = 0

    fun isSet(mask: Int, dayIndex: Int): Boolean = (mask shr dayIndex) and 1 == 1

    fun toggle(mask: Int, dayIndex: Int): Int = mask xor (1 shl dayIndex)

    fun isOneShot(mask: Int): Boolean = mask == NONE

    fun activeDays(mask: Int): List<Int> = (0..6).filter { isSet(mask, it) }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.domain.RepeatDaysTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/domain/ app/src/test/java/com/wakeup/domain/RepeatDaysTest.kt
git commit -m "feat: add Difficulty/MissionType enums and RepeatDays helpers (TDD)"
```

---

### Task 5: Mission params (sealed, serializable) + JSON codec

**Files:**
- Create: `app/src/main/java/com/wakeup/domain/MissionParams.kt`
- Test: `app/src/test/java/com/wakeup/domain/MissionParamsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MissionParamsTest {
    @Test fun mathParamsRoundTrip() {
        val original: MissionParams = MathParams(Difficulty.HARD, problemCount = 3)
        val json = MissionParamsCodec.encode(original)
        assertEquals(original, MissionParamsCodec.decode(json))
    }

    @Test fun squatParamsRoundTrip() {
        val original: MissionParams = SquatParams(squatCount = 12)
        val json = MissionParamsCodec.encode(original)
        assertEquals(original, MissionParamsCodec.decode(json))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.domain.MissionParamsTest"`
Expected: FAIL — `MissionParams`/`MissionParamsCodec` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.wakeup.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface MissionParams

@Serializable
@SerialName("math")
data class MathParams(val difficulty: Difficulty, val problemCount: Int) : MissionParams

@Serializable
@SerialName("squat")
data class SquatParams(val squatCount: Int) : MissionParams

object MissionParamsCodec {
    private val json = Json { encodeDefaults = true }
    fun encode(params: MissionParams): String = json.encodeToString(params)
    fun decode(text: String): MissionParams = json.decodeFromString(text)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.domain.MissionParamsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/domain/MissionParams.kt app/src/test/java/com/wakeup/domain/MissionParamsTest.kt
git commit -m "feat: add serializable MissionParams + JSON codec (TDD)"
```

---

### Task 6: Domain models (`Alarm`, `MissionConfig`)

**Files:**
- Create: `app/src/main/java/com/wakeup/domain/Models.kt`

(No dedicated test — these are plain data holders exercised by later tests.)

- [ ] **Step 1: Write the models**

```kotlin
package com.wakeup.domain

data class MissionConfig(
    val id: Long = 0,
    val order: Int,
    val type: MissionType,
    val params: MissionParams,
)

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Int = RepeatDays.NONE,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    val rampSeconds: Int = 30,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val maxSnoozes: Int = 1,
    val missions: List<MissionConfig> = emptyList(),
) {
    companion object {
        /** Defaults from the spec: medium math x3, then 10 squats. */
        fun default(hour: Int, minute: Int): Alarm = Alarm(
            hour = hour, minute = minute,
            missions = listOf(
                MissionConfig(order = 0, type = MissionType.MATH, params = MathParams(Difficulty.MEDIUM, 3)),
                MissionConfig(order = 1, type = MissionType.SQUAT, params = SquatParams(10)),
            ),
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/wakeup/domain/Models.kt
git commit -m "feat: add Alarm/MissionConfig domain models with defaults"
```

---

### Task 7: `NextTrigger` — next-fire computation (TDD, the trickiest logic)

**Files:**
- Create: `app/src/main/java/com/wakeup/schedule/NextTrigger.kt`
- Test: `app/src/test/java/com/wakeup/schedule/NextTriggerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.schedule

import com.wakeup.domain.RepeatDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerTest {
    private val zone = ZoneId.of("America/New_York")
    // Wednesday 2026-05-27, 08:00 local.
    private fun now() = ZonedDateTime.of(2026, 5, 27, 8, 0, 0, 0, zone)

    private fun expected(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun oneShotLaterToday() {
        val result = NextTrigger.compute(now(), hour = 9, minute = 0, repeatDays = RepeatDays.NONE)
        assertEquals(expected(2026, 5, 27, 9, 0), result)
    }

    @Test fun oneShotTimeAlreadyPassedRollsToTomorrow() {
        val result = NextTrigger.compute(now(), hour = 7, minute = 0, repeatDays = RepeatDays.NONE)
        assertEquals(expected(2026, 5, 28, 7, 0), result)
    }

    @Test fun repeatingPicksNextMatchingWeekday() {
        // Repeat Mon(0) & Fri(4). Now is Wednesday; next match is Friday 2026-05-29.
        val mask = RepeatDays.toggle(RepeatDays.toggle(RepeatDays.NONE, 0), 4)
        val result = NextTrigger.compute(now(), hour = 6, minute = 30, repeatDays = mask)
        assertEquals(expected(2026, 5, 29, 6, 30), result)
    }

    @Test fun repeatingTodayButTimePassedSkipsToNextWeek() {
        // Repeat Wednesday(2) only, at 07:00 — already passed today, so next Wednesday.
        val mask = RepeatDays.toggle(RepeatDays.NONE, 2)
        val result = NextTrigger.compute(now(), hour = 7, minute = 0, repeatDays = mask)
        assertEquals(expected(2026, 6, 3, 7, 0), result)
    }

    @Test fun repeatingTodayTimeStillAheadFiresToday() {
        val mask = RepeatDays.toggle(RepeatDays.NONE, 2) // Wednesday
        val result = NextTrigger.compute(now(), hour = 23, minute = 0, repeatDays = mask)
        assertEquals(expected(2026, 5, 27, 23, 0), result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.schedule.NextTriggerTest"`
Expected: FAIL — `NextTrigger` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.wakeup.schedule

import com.wakeup.domain.RepeatDays
import java.time.ZonedDateTime

object NextTrigger {
    /**
     * Epoch millis of the next time an alarm with the given time-of-day and repeat mask fires,
     * strictly after [now]. One-shot (mask == 0) fires today if the time is still ahead, else tomorrow.
     */
    fun compute(now: ZonedDateTime, hour: Int, minute: Int, repeatDays: Int): Long {
        val todayAtTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (RepeatDays.isOneShot(repeatDays)) {
            val fire = if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
            return fire.toInstant().toEpochMilli()
        }

        // Scan 0..7 days ahead for a matching weekday whose candidate time is after now.
        for (offset in 0..7) {
            val candidate = todayAtTime.plusDays(offset.toLong())
            val dayIndex = candidate.dayOfWeek.value - 1 // MONDAY(1)->0 .. SUNDAY(7)->6
            if (RepeatDays.isSet(repeatDays, dayIndex) && candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        error("Unreachable: a repeating alarm must match within 7 days")
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.schedule.NextTriggerTest"`
Expected: PASS (all 5).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/schedule/NextTrigger.kt app/src/test/java/com/wakeup/schedule/NextTriggerTest.kt
git commit -m "feat: add NextTrigger next-fire computation (TDD)"
```

---

### Task 8: `SnoozePolicy` (TDD)

**Files:**
- Create: `app/src/main/java/com/wakeup/schedule/SnoozePolicy.kt`
- Test: `app/src/test/java/com/wakeup/schedule/SnoozePolicyTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.schedule

import org.junit.Assert.*
import org.junit.Test

class SnoozePolicyTest {
    @Test fun disabledNeverSnoozes() {
        assertFalse(SnoozePolicy.canSnooze(snoozeEnabled = false, maxSnoozes = 3, snoozesUsed = 0))
    }

    @Test fun unlimitedWhenMaxIsZero() {
        assertTrue(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 0, snoozesUsed = 99))
    }

    @Test fun blockedOnceMaxReached() {
        assertTrue(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 2, snoozesUsed = 1))
        assertFalse(SnoozePolicy.canSnooze(snoozeEnabled = true, maxSnoozes = 2, snoozesUsed = 2))
    }

    @Test fun nextSnoozeTimeAddsMinutes() {
        assertEquals(1_000_000L + 5 * 60_000L, SnoozePolicy.nextSnoozeTime(now = 1_000_000L, snoozeMinutes = 5))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.schedule.SnoozePolicyTest"`
Expected: FAIL — `SnoozePolicy` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.wakeup.schedule

object SnoozePolicy {
    /** maxSnoozes == 0 means unlimited. Only meaningful when snoozeEnabled. */
    fun canSnooze(snoozeEnabled: Boolean, maxSnoozes: Int, snoozesUsed: Int): Boolean {
        if (!snoozeEnabled) return false
        if (maxSnoozes == 0) return true
        return snoozesUsed < maxSnoozes
    }

    fun nextSnoozeTime(now: Long, snoozeMinutes: Int): Long = now + snoozeMinutes * 60_000L
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.schedule.SnoozePolicyTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/schedule/SnoozePolicy.kt app/src/test/java/com/wakeup/schedule/SnoozePolicyTest.kt
git commit -m "feat: add SnoozePolicy rules (TDD)"
```

---

### Task 9: `MathProblemGenerator` (TDD)

**Files:**
- Create: `app/src/main/java/com/wakeup/mission/MathProblem.kt`
- Create: `app/src/main/java/com/wakeup/mission/MathProblemGenerator.kt`
- Test: `app/src/test/java/com/wakeup/mission/MathProblemGeneratorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
            assertTrue(p.answer <= 18) // max single-digit sum 9+9
            assertTrue(p.text.isNotBlank())
        }
    }

    @Test fun textEvaluatesToAnswerForAllDifficulties() {
        val g = gen()
        for (d in Difficulty.values()) {
            repeat(200) {
                val p = g.generate(d)
                // The stored answer must be internally consistent: re-evaluating equals answer.
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.mission.MathProblemGeneratorTest"`
Expected: FAIL — `MathProblem`/`MathProblemGenerator` unresolved.

- [ ] **Step 3: Write the implementation**

`MathProblem.kt`:
```kotlin
package com.wakeup.mission

/** [text] uses spaces between tokens, e.g. "12 + 7" or "6 * 4 + 3". */
data class MathProblem(val text: String, val answer: Int)
```

`MathProblemGenerator.kt`:
```kotlin
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
            MathProblem("$a - $b", a - b) // a >= b guaranteed by the branch
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.mission.MathProblemGeneratorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/mission/MathProblem.kt app/src/main/java/com/wakeup/mission/MathProblemGenerator.kt app/src/test/java/com/wakeup/mission/MathProblemGeneratorTest.kt
git commit -m "feat: add MathProblemGenerator with difficulty tiers (TDD)"
```

---

### Task 10: `SquatCounter` rep-detection state machine (TDD)

**Files:**
- Create: `app/src/main/java/com/wakeup/mission/SquatCounter.kt`
- Test: `app/src/test/java/com/wakeup/mission/SquatCounterTest.kt`

The counter is a pure state machine over a stream of `(magnitude, timestampMs)` samples (magnitude = √(x²+y²+z²) from the accelerometer, ~9.81 at rest). A rep = a dip below `downThreshold` followed by a rise above `upThreshold`, debounced by `minRepIntervalMs`. The SensorManager wiring (Task 19) just forwards samples here.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.mission

import org.junit.Assert.assertEquals
import org.junit.Test

class SquatCounterTest {
    private fun counter() = SquatCounter(downThreshold = 7f, upThreshold = 11f, minRepIntervalMs = 500)

    /** Emit one down-up cycle starting at [startMs]; returns ms after the cycle. */
    private fun cycle(c: SquatCounter, startMs: Long): Long {
        c.onSample(9.8f, startMs)        // rest
        c.onSample(6.0f, startMs + 100)  // dip (below down)
        c.onSample(9.8f, startMs + 200)  // mid
        c.onSample(12.0f, startMs + 300) // peak (above up) → rep completes here
        return startMs + 300
    }

    @Test fun countsThreeWellSpacedReps() {
        val c = counter()
        var t = 0L
        repeat(3) { t = cycle(c, t + 600) }
        assertEquals(3, c.count)
    }

    @Test fun ignoresRepsFasterThanDebounce() {
        val c = counter()
        cycle(c, 0)        // rep 1 completes ~300ms
        cycle(c, 350)      // starts 50ms later → within 500ms debounce → ignored
        assertEquals(1, c.count)
    }

    @Test fun jitterWithoutFullDipDoesNotCount() {
        val c = counter()
        c.onSample(9.8f, 0)
        c.onSample(8.0f, 100)  // never drops below downThreshold(7)
        c.onSample(12.0f, 200) // peak without a registered dip
        assertEquals(0, c.count)
    }

    @Test fun resetClearsCount() {
        val c = counter()
        cycle(c, 0)
        c.reset()
        assertEquals(0, c.count)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.mission.SquatCounterTest"`
Expected: FAIL — `SquatCounter` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.wakeup.mission

/**
 * Counts squat reps from accelerometer-magnitude samples.
 * A rep = magnitude dips below [downThreshold] then rises above [upThreshold],
 * with at least [minRepIntervalMs] between completed reps.
 */
class SquatCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val minRepIntervalMs: Long,
) {
    private var sawDip = false
    private var lastRepMs = Long.MIN_VALUE

    var count: Int = 0
        private set

    /** @return true if this sample completed a rep. */
    fun onSample(magnitude: Float, timestampMs: Long): Boolean {
        if (magnitude < downThreshold) {
            sawDip = true
        } else if (magnitude > upThreshold && sawDip) {
            sawDip = false
            if (timestampMs - lastRepMs >= minRepIntervalMs) {
                lastRepMs = timestampMs
                count++
                return true
            }
        }
        return false
    }

    fun reset() {
        count = 0
        sawDip = false
        lastRepMs = Long.MIN_VALUE
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.mission.SquatCounterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/mission/SquatCounter.kt app/src/test/java/com/wakeup/mission/SquatCounterTest.kt
git commit -m "feat: add SquatCounter rep-detection state machine (TDD)"
```

---

## Phase 2 — Persistence

### Task 11: Room entities, converters, DAO, database

**Files:**
- Create: `app/src/main/java/com/wakeup/data/AlarmEntity.kt`
- Create: `app/src/main/java/com/wakeup/data/MissionConfigEntity.kt`
- Create: `app/src/main/java/com/wakeup/data/AlarmDao.kt`
- Create: `app/src/main/java/com/wakeup/data/AppDatabase.kt`

- [ ] **Step 1: Write the entities**

`AlarmEntity.kt`:
```kotlin
package com.wakeup.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val repeatDays: Int,
    val soundUri: String?,
    val vibrate: Boolean,
    val gradualVolume: Boolean,
    val rampSeconds: Int,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val maxSnoozes: Int,
)
```

`MissionConfigEntity.kt`:
```kotlin
package com.wakeup.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "mission_configs",
    foreignKeys = [ForeignKey(
        entity = AlarmEntity::class,
        parentColumns = ["id"],
        childColumns = ["alarmId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("alarmId")],
)
data class MissionConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val order: Int,
    val type: String,   // MissionType.name
    val params: String, // MissionParamsCodec JSON
)

data class AlarmWithMissions(
    @Embedded val alarm: AlarmEntity,
    @Relation(parentColumn = "id", entityColumn = "alarmId")
    val missions: List<MissionConfigEntity>,
)
```

- [ ] **Step 2: Write the DAO and database**

`AlarmDao.kt`:
```kotlin
package com.wakeup.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Transaction
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAlarmsWithMissions(): Flow<List<AlarmWithMissions>>

    @Transaction
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmWithMissions(id: Long): AlarmWithMissions?

    @Transaction
    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabledAlarms(): List<AlarmWithMissions>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    @Query("DELETE FROM mission_configs WHERE alarmId = :alarmId")
    suspend fun deleteMissionsFor(alarmId: Long)

    @Insert
    suspend fun insertMissions(missions: List<MissionConfigEntity>)
}
```

`AppDatabase.kt`:
```kotlin
package com.wakeup.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class, MissionConfigEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
```

(No Room `TypeConverter` is required for v1: `MissionType` is stored as its `String` name and mission params as JSON, so there is no `Converters.kt`.)

- [ ] **Step 3: Build**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` (KSP generates Room code).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/wakeup/data/
git commit -m "feat: add Room entities, DAO, and database"
```

---

### Task 12: Repository interface + Room implementation with mappers (instrumented test)

**Files:**
- Create: `app/src/main/java/com/wakeup/data/AlarmRepository.kt`
- Create: `app/src/main/java/com/wakeup/data/RoomAlarmRepository.kt`
- Test: `app/src/androidTest/java/com/wakeup/data/RoomAlarmRepositoryTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
package com.wakeup.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakeup.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAlarmRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: AlarmRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        repo = RoomAlarmRepository(db.alarmDao())
    }

    @After fun tearDown() = db.close()

    @Test fun upsertThenReadPreservesMissionsAndOrder() = runBlocking {
        val alarm = Alarm(
            hour = 7, minute = 30, label = "Gym", repeatDays = RepeatDays.toggle(RepeatDays.NONE, 0),
            missions = listOf(
                MissionConfig(order = 0, type = MissionType.MATH, params = MathParams(Difficulty.HARD, 2)),
                MissionConfig(order = 1, type = MissionType.SQUAT, params = SquatParams(15)),
            ),
        )
        val id = repo.upsert(alarm)
        val loaded = repo.getAlarm(id)!!

        assertEquals(7, loaded.hour)
        assertEquals("Gym", loaded.label)
        assertEquals(2, loaded.missions.size)
        assertEquals(MissionType.MATH, loaded.missions[0].type)
        assertEquals(MathParams(Difficulty.HARD, 2), loaded.missions[0].params)
        assertEquals(SquatParams(15), loaded.missions[1].params)
    }

    @Test fun upsertReplacesMissions() = runBlocking {
        val id = repo.upsert(Alarm(hour = 6, minute = 0,
            missions = listOf(MissionConfig(order = 0, type = MissionType.SQUAT, params = SquatParams(5)))))
        val updated = repo.getAlarm(id)!!.copy(
            missions = listOf(MissionConfig(order = 0, type = MissionType.MATH, params = MathParams(Difficulty.EASY, 1))))
        repo.upsert(updated)
        val loaded = repo.getAlarm(id)!!
        assertEquals(1, loaded.missions.size)
        assertEquals(MissionType.MATH, loaded.missions[0].type)
    }

    @Test fun observeAndEnabledFilter() = runBlocking {
        repo.upsert(Alarm(hour = 5, minute = 0, enabled = true))
        repo.upsert(Alarm(hour = 6, minute = 0, enabled = false))
        assertEquals(2, repo.observeAlarms().first().size)
        assertEquals(1, repo.enabledAlarms().size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "com.wakeup.data.RoomAlarmRepositoryTest"` (Pixel 9 connected)
Expected: FAIL — `AlarmRepository`/`RoomAlarmRepository` unresolved.

- [ ] **Step 3: Write the interface**

`AlarmRepository.kt`:
```kotlin
package com.wakeup.data

import com.wakeup.domain.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun observeAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: Long): Alarm?
    suspend fun enabledAlarms(): List<Alarm>
    /** Inserts or updates the alarm (and replaces its missions). Returns the alarm id. */
    suspend fun upsert(alarm: Alarm): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun delete(id: Long)
}
```

- [ ] **Step 4: Write the implementation with mappers**

`RoomAlarmRepository.kt`:
```kotlin
package com.wakeup.data

import com.wakeup.domain.Alarm
import com.wakeup.domain.MissionConfig
import com.wakeup.domain.MissionParamsCodec
import com.wakeup.domain.MissionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {

    override fun observeAlarms(): Flow<List<Alarm>> =
        dao.observeAlarmsWithMissions().map { list -> list.map { it.toDomain() } }

    override suspend fun getAlarm(id: Long): Alarm? = dao.getAlarmWithMissions(id)?.toDomain()

    override suspend fun enabledAlarms(): List<Alarm> = dao.getEnabledAlarms().map { it.toDomain() }

    override suspend fun upsert(alarm: Alarm): Long {
        val id = dao.insertAlarm(alarm.toEntity()) // REPLACE handles updates by id
        dao.deleteMissionsFor(id)
        dao.insertMissions(alarm.missions.map { it.toEntity(id) })
        return id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun delete(id: Long) = dao.deleteAlarm(id)
}

private fun AlarmWithMissions.toDomain(): Alarm = Alarm(
    id = alarm.id, hour = alarm.hour, minute = alarm.minute, label = alarm.label,
    enabled = alarm.enabled, repeatDays = alarm.repeatDays, soundUri = alarm.soundUri,
    vibrate = alarm.vibrate, gradualVolume = alarm.gradualVolume, rampSeconds = alarm.rampSeconds,
    snoozeEnabled = alarm.snoozeEnabled, snoozeMinutes = alarm.snoozeMinutes, maxSnoozes = alarm.maxSnoozes,
    missions = missions.sortedBy { it.order }.map {
        MissionConfig(it.id, it.order, MissionType.valueOf(it.type), MissionParamsCodec.decode(it.params))
    },
)

private fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id, hour = hour, minute = minute, label = label, enabled = enabled, repeatDays = repeatDays,
    soundUri = soundUri, vibrate = vibrate, gradualVolume = gradualVolume, rampSeconds = rampSeconds,
    snoozeEnabled = snoozeEnabled, snoozeMinutes = snoozeMinutes, maxSnoozes = maxSnoozes,
)

private fun MissionConfig.toEntity(alarmId: Long): MissionConfigEntity = MissionConfigEntity(
    id = 0, alarmId = alarmId, order = order, type = type.name, params = MissionParamsCodec.encode(params),
)
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "com.wakeup.data.RoomAlarmRepositoryTest"`
Expected: PASS.

- [ ] **Step 6: Wire repository into Graph and run on device**

In `Graph.kt`, replace the body of `init` with:

```kotlin
fun init(context: Context) {
    if (initialized) return
    val db = androidx.room.Room.databaseBuilder(
        context.applicationContext, com.wakeup.data.AppDatabase::class.java, "wakeup.db"
    ).build()
    repository = com.wakeup.data.RoomAlarmRepository(db.alarmDao())
    // scheduler wired in Task 16
    initialized = true
}
```

(Leave `scheduler` unset for now; nothing reads it yet.)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/wakeup/data/AlarmRepository.kt app/src/main/java/com/wakeup/data/RoomAlarmRepository.kt app/src/androidTest/java/com/wakeup/data/RoomAlarmRepositoryTest.kt app/src/main/java/com/wakeup/di/Graph.kt
git commit -m "feat: add AlarmRepository (Room impl) + mappers; wire into Graph"
```

---

## Phase 3 — Alarm list & edit UI

### Task 13: `AlarmListViewModel` (TDD with a fake repository)

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/list/AlarmListViewModel.kt`
- Test: `app/src/test/java/com/wakeup/ui/list/AlarmListViewModelTest.kt`
- Create (test helper): `app/src/test/java/com/wakeup/data/FakeAlarmRepository.kt`

- [ ] **Step 1: Write the fake repository (test source set)**

```kotlin
package com.wakeup.data

import com.wakeup.domain.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmRepository : AlarmRepository {
    private val items = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override fun observeAlarms() = items.map { it.sortedWith(compareBy({ a -> a.hour }, { a -> a.minute })) }
    override suspend fun getAlarm(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun enabledAlarms() = items.value.filter { it.enabled }
    override suspend fun upsert(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) nextId++ else alarm.id
        val saved = alarm.copy(id = id)
        items.value = items.value.filter { it.id != id } + saved
        return id
    }
    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }
    override suspend fun delete(id: Long) { items.value = items.value.filter { it.id != id } }
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.wakeup.ui.list

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun togglePersistsEnabledState() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0, enabled = true))
        val vm = AlarmListViewModel(repo)

        vm.setEnabled(id, false)
        advanceUntilIdle()

        assertFalse(repo.getAlarm(id)!!.enabled)
    }

    @Test fun deleteRemovesAlarm() = runTest {
        val repo = FakeAlarmRepository()
        val id = repo.upsert(Alarm(hour = 7, minute = 0))
        val vm = AlarmListViewModel(repo)

        vm.delete(id)
        advanceUntilIdle()

        assertNull(repo.getAlarm(id))
    }

    @Test fun exposesAlarmsFromRepository() = runTest {
        val repo = FakeAlarmRepository()
        repo.upsert(Alarm(hour = 6, minute = 0))
        val vm = AlarmListViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, vm.alarms.first().size)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.ui.list.AlarmListViewModelTest"`
Expected: FAIL — `AlarmListViewModel` unresolved.

- [ ] **Step 4: Write the ViewModel**

```kotlin
package com.wakeup.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(private val repository: AlarmRepository) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(Graph.repository) as T
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.ui.list.AlarmListViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/list/AlarmListViewModel.kt app/src/test/java/com/wakeup/ui/list/AlarmListViewModelTest.kt app/src/test/java/com/wakeup/data/FakeAlarmRepository.kt
git commit -m "feat: add AlarmListViewModel (TDD with fake repository)"
```

---

### Task 14: `AlarmEditViewModel` + `AlarmEditState` (TDD)

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/edit/AlarmEditViewModel.kt`
- Test: `app/src/test/java/com/wakeup/ui/edit/AlarmEditViewModelTest.kt`

The edit screen holds a draft `Alarm` in state and mutates it. The ViewModel takes a nullable `alarmId` (null = new alarm seeded from `Alarm.default`).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.wakeup.ui.edit

import com.wakeup.data.FakeAlarmRepository
import com.wakeup.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun newAlarmStartsFromDefaults() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null)
        advanceUntilIdle()
        val d = vm.draft.value
        assertEquals(2, d.missions.size) // medium math x3 + 10 squats
        assertEquals(MissionType.MATH, d.missions[0].type)
    }

    @Test fun editingFieldsUpdatesDraft() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null)
        advanceUntilIdle()
        vm.setTime(6, 15)
        vm.setLabel("Run")
        vm.toggleRepeatDay(0)
        val d = vm.draft.value
        assertEquals(6, d.hour); assertEquals(15, d.minute)
        assertEquals("Run", d.label)
        assertTrue(RepeatDays.isSet(d.repeatDays, 0))
    }

    @Test fun addRemoveAndReorderMissions() = runTest {
        val vm = AlarmEditViewModel(FakeAlarmRepository(), alarmId = null)
        advanceUntilIdle()
        vm.removeMission(0)
        assertEquals(1, vm.draft.value.missions.size)
        vm.addMission(MissionType.MATH)
        assertEquals(2, vm.draft.value.missions.size)
        // orders must be contiguous 0..n-1
        assertEquals(listOf(0, 1), vm.draft.value.missions.map { it.order })
    }

    @Test fun saveWritesToRepository() = runTest {
        val repo = FakeAlarmRepository()
        val vm = AlarmEditViewModel(repo, alarmId = null)
        advanceUntilIdle()
        vm.setTime(9, 0)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.enabledAlarms().size)
        assertEquals(9, repo.enabledAlarms()[0].hour)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.ui.edit.AlarmEditViewModelTest"`
Expected: FAIL — `AlarmEditViewModel` unresolved.

- [ ] **Step 3: Write the ViewModel**

```kotlin
package com.wakeup.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeup.data.AlarmRepository
import com.wakeup.di.Graph
import com.wakeup.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
) : ViewModel() {

    private val _draft = MutableStateFlow(Alarm.default(hour = 7, minute = 0))
    val draft: StateFlow<Alarm> = _draft.asStateFlow()

    init {
        if (alarmId != null) viewModelScope.launch {
            repository.getAlarm(alarmId)?.let { _draft.value = it }
        }
    }

    fun setTime(hour: Int, minute: Int) { _draft.value = _draft.value.copy(hour = hour, minute = minute) }
    fun setLabel(label: String) { _draft.value = _draft.value.copy(label = label) }
    fun toggleRepeatDay(dayIndex: Int) {
        _draft.value = _draft.value.copy(repeatDays = RepeatDays.toggle(_draft.value.repeatDays, dayIndex))
    }
    fun setSound(uri: String?) { _draft.value = _draft.value.copy(soundUri = uri) }
    fun setVibrate(v: Boolean) { _draft.value = _draft.value.copy(vibrate = v) }
    fun setGradualVolume(v: Boolean) { _draft.value = _draft.value.copy(gradualVolume = v) }
    fun setSnoozeEnabled(v: Boolean) { _draft.value = _draft.value.copy(snoozeEnabled = v) }
    fun setSnoozeMinutes(m: Int) { _draft.value = _draft.value.copy(snoozeMinutes = m) }
    fun setMaxSnoozes(n: Int) { _draft.value = _draft.value.copy(maxSnoozes = n) }

    fun addMission(type: MissionType) {
        val params: MissionParams = when (type) {
            MissionType.MATH -> MathParams(Difficulty.MEDIUM, 3)
            MissionType.SQUAT -> SquatParams(10)
        }
        val next = _draft.value.missions + MissionConfig(order = _draft.value.missions.size, type = type, params = params)
        _draft.value = _draft.value.copy(missions = reindex(next))
    }

    fun removeMission(index: Int) {
        _draft.value = _draft.value.copy(missions = reindex(_draft.value.missions.filterIndexed { i, _ -> i != index }))
    }

    fun moveMission(from: Int, to: Int) {
        val list = _draft.value.missions.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _draft.value = _draft.value.copy(missions = reindex(list))
    }

    fun updateMissionParams(index: Int, params: MissionParams) {
        val list = _draft.value.missions.toMutableList()
        list[index] = list[index].copy(params = params)
        _draft.value = _draft.value.copy(missions = list)
    }

    fun save(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.upsert(_draft.value)
        onSaved(id)
    }

    private fun reindex(list: List<MissionConfig>): List<MissionConfig> =
        list.mapIndexed { i, m -> m.copy(order = i) }

    companion object {
        fun factory(alarmId: Long?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmEditViewModel(Graph.repository, alarmId) as T
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.wakeup.ui.edit.AlarmEditViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/edit/AlarmEditViewModel.kt app/src/test/java/com/wakeup/ui/edit/AlarmEditViewModelTest.kt
git commit -m "feat: add AlarmEditViewModel with mission editing (TDD)"
```

---

### Task 15: List & edit Compose screens + navigation

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/list/AlarmListScreen.kt`
- Create: `app/src/main/java/com/wakeup/ui/edit/AlarmEditScreen.kt`
- Modify: `app/src/main/java/com/wakeup/MainActivity.kt`

This task is UI; verification is visual on the Pixel 9 (no unit test).

- [ ] **Step 1: Write the list screen**

```kotlin
package com.wakeup.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.Alarm
import com.wakeup.domain.RepeatDays

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    viewModel: AlarmListViewModel = viewModel(factory = AlarmListViewModel.Factory),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("WakeUp") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) { Icon(Icons.Default.Add, contentDescription = "Add alarm") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    onToggle = { viewModel.setEnabled(alarm.id, it) },
                    onClick = { onEditAlarm(alarm.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
            val repeat = if (RepeatDays.isOneShot(alarm.repeatDays)) "Once"
                else RepeatDays.activeDays(alarm.repeatDays).joinToString(" ") { DAY_LABELS[it] }
            Text(listOf(alarm.label, repeat).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium)
            Text("${alarm.missions.size} mission(s)", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = alarm.enabled, onCheckedChange = onToggle)
    }
}
```

- [ ] **Step 2: Write the edit screen**

```kotlin
package com.wakeup.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.domain.*

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.factory(alarmId)),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "New alarm" else "Edit alarm") },
                actions = { TextButton(onClick = { viewModel.save { onDone() } }) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("%02d:%02d".format(draft.hour, draft.minute), style = MaterialTheme.typography.displayMedium)
            TextButton(onClick = { showTimePicker = true }) { Text("Change time") }

            OutlinedTextField(
                value = draft.label, onValueChange = viewModel::setLabel,
                label = { Text("Label") }, modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DAY_LABELS.forEachIndexed { i, label ->
                    FilterChip(
                        selected = RepeatDays.isSet(draft.repeatDays, i),
                        onClick = { viewModel.toggleRepeatDay(i) },
                        label = { Text(label.take(1)) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ToggleRow("Vibrate", draft.vibrate, viewModel::setVibrate)
            ToggleRow("Gradual volume", draft.gradualVolume, viewModel::setGradualVolume)
            ToggleRow("Snooze allowed", draft.snoozeEnabled, viewModel::setSnoozeEnabled)

            Spacer(Modifier.height(16.dp))
            Text("Missions", style = MaterialTheme.typography.titleMedium)
            draft.missions.forEachIndexed { index, mission ->
                MissionEditor(
                    mission = mission,
                    onParamsChange = { viewModel.updateMissionParams(index, it) },
                    onRemove = { viewModel.removeMission(index) },
                    onMoveUp = { viewModel.moveMission(index, index - 1) },
                    onMoveDown = { viewModel.moveMission(index, index + 1) },
                )
            }
            Row {
                Button(onClick = { viewModel.addMission(MissionType.MATH) }) { Text("+ Math") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.addMission(MissionType.SQUAT) }) { Text("+ Squats") }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = draft.hour, initialMinute = draft.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { viewModel.setTime(state.hour, state.minute); showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MissionEditor(
    mission: MissionConfig,
    onParamsChange: (MissionParams) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mission.type.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onMoveUp) { Text("↑") }
                TextButton(onClick = onMoveDown) { Text("↓") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
            }
            when (val p = mission.params) {
                is MathParams -> {
                    Text("Difficulty")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Difficulty.values().forEach { d ->
                            FilterChip(selected = p.difficulty == d, onClick = { onParamsChange(p.copy(difficulty = d)) },
                                label = { Text(d.name) })
                        }
                    }
                    Stepper("Problems", p.problemCount, 1..10) { onParamsChange(p.copy(problemCount = it)) }
                }
                is SquatParams -> Stepper("Squats", p.squatCount, 1..50) { onParamsChange(SquatParams(it)) }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value", Modifier.weight(1f))
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("-") }
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
    }
}
```

- [ ] **Step 3: Wire navigation in `MainActivity`**

Replace `MainActivity.kt` body with:

```kotlin
package com.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wakeup.ui.edit.AlarmEditScreen
import com.wakeup.ui.list.AlarmListScreen
import com.wakeup.ui.theme.WakeUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeUpTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        AlarmListScreen(
                            onAddAlarm = { nav.navigate("edit") },
                            onEditAlarm = { id -> nav.navigate("edit?alarmId=$id") },
                        )
                    }
                    composable("edit") { AlarmEditScreen(alarmId = null, onDone = { nav.popBackStack() }) }
                    composable("edit?alarmId={alarmId}") { entry ->
                        val id = entry.arguments?.getString("alarmId")?.toLongOrNull()
                        AlarmEditScreen(alarmId = id, onDone = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
```

(Use the wizard's actual theme name if it differs from `WakeUpTheme`.)

- [ ] **Step 4: Build, install, and verify on the Pixel 9**

Run: `./gradlew installDebug`
Manual check on phone: add an alarm via the FAB, set a time, toggle repeat days, add/remove a math and squat mission, Save. Confirm it appears in the list, the toggle persists across app restarts (Room), and tapping it re-opens with the saved values.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/list/AlarmListScreen.kt app/src/main/java/com/wakeup/ui/edit/AlarmEditScreen.kt app/src/main/java/com/wakeup/MainActivity.kt
git commit -m "feat: add alarm list & edit screens with navigation"
```

---

## Phase 4 — Scheduling & firing engine

### Task 16: `AlarmScheduler` + `AlarmManagerScheduler`, wire into Graph and save flow

**Files:**
- Create: `app/src/main/java/com/wakeup/schedule/AlarmScheduler.kt`
- Create: `app/src/main/java/com/wakeup/schedule/AlarmManagerScheduler.kt`
- Create: `app/src/main/java/com/wakeup/alarm/AlarmContract.kt`
- Modify: `app/src/main/java/com/wakeup/di/Graph.kt`
- Modify: `app/src/main/java/com/wakeup/ui/edit/AlarmEditViewModel.kt`
- Modify: `app/src/main/java/com/wakeup/ui/list/AlarmListViewModel.kt`

- [ ] **Step 1: Define the contract constants**

```kotlin
package com.wakeup.alarm

object AlarmContract {
    const val ACTION_FIRE = "com.wakeup.action.FIRE"
    const val ACTION_SNOOZE = "com.wakeup.action.SNOOZE"
    const val ACTION_DISMISS = "com.wakeup.action.DISMISS"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZES_USED = "snoozes_used"
    const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
    const val NOTIFICATION_ID = 1001
}
```

- [ ] **Step 2: Define the scheduler interface**

```kotlin
package com.wakeup.schedule

import com.wakeup.domain.Alarm

interface AlarmScheduler {
    /** Schedules the alarm's next occurrence. No-op (and cancels) if disabled. */
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: Long)
    /** Schedules a one-off snooze fire for [alarmId] at [triggerAtMillis] carrying [snoozesUsed]. */
    fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int)
}
```

- [ ] **Step 3: Implement with AlarmManager**

```kotlin
package com.wakeup.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wakeup.alarm.AlarmContract
import com.wakeup.alarm.AlarmReceiver
import com.wakeup.domain.Alarm
import java.time.ZonedDateTime

class AlarmManagerScheduler(private val context: Context) : AlarmScheduler {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.enabled) { cancel(alarm.id); return }
        val triggerAt = NextTrigger.compute(ZonedDateTime.now(), alarm.hour, alarm.minute, alarm.repeatDays)
        setExact(alarm.id, triggerAt, snoozesUsed = 0)
    }

    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) =
        setExact(alarmId, triggerAtMillis, snoozesUsed)

    override fun cancel(alarmId: Long) {
        am.cancel(firePendingIntent(alarmId, snoozesUsed = 0, mutableUpdate = false))
    }

    private fun setExact(alarmId: Long, triggerAt: Long, snoozesUsed: Int) {
        val show = AlarmManager.AlarmClockInfo(triggerAt, null)
        am.setAlarmClock(show, firePendingIntent(alarmId, snoozesUsed, mutableUpdate = true))
    }

    private fun firePendingIntent(alarmId: Long, snoozesUsed: Int, mutableUpdate: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, alarmId.toInt(), intent, flags)
    }
}
```

- [ ] **Step 4: Wire scheduler into Graph**

In `Graph.kt` `init`, after creating the repository add:

```kotlin
scheduler = com.wakeup.schedule.AlarmManagerScheduler(context.applicationContext)
```

- [ ] **Step 5: Inject the scheduler into both ViewModels and schedule on save / toggle / delete**

The `AlarmScheduler` interface now exists, so add it as a **constructor dependency** to both ViewModels (no `Graph` access inside methods — that keeps them unit-testable with a fake scheduler).

In `AlarmListViewModel`, change the class header and methods, and update the `Factory`:

```kotlin
class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
        val alarm = repository.getAlarm(id) ?: return@launch
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
    }

    fun delete(id: Long) = viewModelScope.launch {
        scheduler.cancel(id)
        repository.delete(id)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(Graph.repository, Graph.scheduler) as T
        }
    }
}
```

Add `import com.wakeup.schedule.AlarmScheduler` to `AlarmListViewModel`.

In `AlarmEditViewModel`, add `scheduler` as the third constructor parameter, schedule in `save`, and update the `factory`:

```kotlin
class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    // ... existing _draft, draft, init, and all set/add/remove/move/update methods unchanged ...

    fun save(onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.upsert(_draft.value)
        scheduler.schedule(_draft.value.copy(id = id))
        onSaved(id)
    }

    companion object {
        fun factory(alarmId: Long?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmEditViewModel(Graph.repository, alarmId, Graph.scheduler) as T
        }
    }
}
```

Add `import com.wakeup.schedule.AlarmScheduler` to `AlarmEditViewModel`.

- [ ] **Step 6: Add a no-op scheduler and update the Task 13/14 tests**

Create `app/src/test/java/com/wakeup/schedule/NoopScheduler.kt`:
```kotlin
package com.wakeup.schedule

import com.wakeup.domain.Alarm

class NoopScheduler : AlarmScheduler {
    override fun schedule(alarm: Alarm) {}
    override fun cancel(alarmId: Long) {}
    override fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long, snoozesUsed: Int) {}
}
```

Update the existing constructions in the Task 13 & 14 tests to pass it:
- `AlarmListViewModelTest`: `AlarmListViewModel(repo, NoopScheduler())` (add `import com.wakeup.schedule.NoopScheduler`).
- `AlarmEditViewModelTest`: `AlarmEditViewModel(FakeAlarmRepository(), alarmId = null, scheduler = NoopScheduler())` (and likewise for the `repo`-based cases).

- [ ] **Step 7: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all suites green with the injected scheduler).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/wakeup/schedule/AlarmScheduler.kt app/src/main/java/com/wakeup/schedule/AlarmManagerScheduler.kt app/src/main/java/com/wakeup/alarm/AlarmContract.kt app/src/main/java/com/wakeup/di/Graph.kt app/src/main/java/com/wakeup/ui/edit/AlarmEditViewModel.kt app/src/main/java/com/wakeup/ui/list/AlarmListViewModel.kt app/src/test/java/com/wakeup/schedule/NoopScheduler.kt app/src/test/java/com/wakeup/ui/
git commit -m "feat: add AlarmManager scheduler; schedule on save/toggle/delete"
```

---

### Task 17: `AlarmReceiver` (fire, boot re-arm, snooze)

**Files:**
- Create: `app/src/main/java/com/wakeup/alarm/AlarmReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the receiver**

```kotlin
package com.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeup.di.Graph
import com.wakeup.schedule.SnoozePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.init(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rearmAll(context)
            AlarmContract.ACTION_SNOOZE -> handleSnooze(context, intent)
            else -> startRinging(context, intent) // ACTION_FIRE
        }
    }

    private fun startRinging(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        val snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
        if (alarmId < 0) return
        val svc = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        val snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
        if (alarmId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarm = Graph.repository.getAlarm(alarmId) ?: return@launch
                val nextUsed = snoozesUsed + 1
                val triggerAt = SnoozePolicy.nextSnoozeTime(System.currentTimeMillis(), alarm.snoozeMinutes)
                Graph.scheduler.scheduleSnooze(alarmId, triggerAt, nextUsed)
            } finally { pending.finish() }
        }
    }

    private fun rearmAll(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Graph.repository.enabledAlarms().forEach { Graph.scheduler.schedule(it) }
            } finally { pending.finish() }
        }
    }
}
```

- [ ] **Step 2: Register the receiver in the manifest**

Inside `<application>` add:

```xml
<receiver android:name=".alarm.AlarmReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

(The FIRE/SNOOZE intents are explicit, so they need no intent-filter; BOOT_COMPLETED does.)

- [ ] **Step 3: Build**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL initially — `AlarmService` is referenced but not yet created. That's expected; Task 18 creates it. To keep this task self-contained, temporarily comment the `Intent(context, AlarmService::class.java)` line is **not** needed — instead implement Task 18 before building. Proceed to Task 18, then build at the end of Task 18.

- [ ] **Step 4: Commit (after Task 18 builds)**

Defer the commit to the end of Task 18 so the project compiles.

---

### Task 18: `AlarmService` (foreground ringer)

**Files:**
- Create: `app/src/main/java/com/wakeup/alarm/AlarmService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the service**

```kotlin
package com.wakeup.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.wakeup.R
import com.wakeup.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L) ?: -1L
        val snoozesUsed = intent?.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0) ?: 0
        if (alarmId < 0) { stopSelf(); return START_NOT_STICKY }

        acquireWakeLock()
        startForeground(AlarmContract.NOTIFICATION_ID, buildNotification(alarmId, snoozesUsed))
        scope.launch { ringFor(alarmId) }
        return START_STICKY
    }

    private suspend fun ringFor(alarmId: Long) {
        val alarm = Graph.repository.getAlarm(alarmId)
        val soundUri = alarm?.soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            isLooping = true
            setDataSource(this@AlarmService, soundUri)
            prepare()
            if (alarm?.gradualVolume == true) setVolume(0f, 0f)
            start()
        }
        if (alarm?.vibrate != false) startVibration()
        if (alarm?.gradualVolume == true) rampVolume(alarm.rampSeconds)
    }

    private suspend fun rampVolume(seconds: Int) {
        val steps = 20
        for (i in 1..steps) {
            val v = i / steps.toFloat()
            player?.setVolume(v, v)
            delay((seconds * 1000L) / steps)
        }
    }

    private fun startVibration() {
        vibrator = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also {
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION") it.vibrate(pattern, 0)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeup:alarm").apply { acquire(10 * 60 * 1000L) }
    }

    private fun buildNotification(alarmId: Long, snoozesUsed: Int): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(AlarmContract.NOTIFICATION_CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setBypassDnd(true); setSound(null, null) }
            )
        }
        val fullScreen = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        val fsPending = PendingIntent.getActivity(
            this, alarmId.toInt(), fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, AlarmContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fsPending, true)
            .build()
    }

    override fun onDestroy() {
        player?.run { stop(); release() }; player = null
        vibrator?.cancel(); vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release(); wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun stop(context: Context) = context.stopService(Intent(context, AlarmService::class.java))
    }
}
```

- [ ] **Step 2: Declare the service in the manifest**

Inside `<application>` add:

```xml
<service
    android:name=".alarm.AlarmService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

- [ ] **Step 3: Build**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL — `AlarmActivity` is referenced but not yet created. Created in Task 19; build at the end of Task 19.

- [ ] **Step 4: Commit (after Task 19 builds)**

Defer commit to the end of Task 19.

---

## Phase 5 — Firing Activity & missions

### Task 19: `AlarmActivity` shell — show over lock screen, host placeholder

**Files:**
- Create: `app/src/main/java/com/wakeup/alarm/AlarmActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the activity (mission host wired in Task 22)**

```kotlin
package com.wakeup.alarm

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.wakeup.ui.theme.WakeUpTheme

class AlarmActivity : ComponentActivity() {
    private var alarmId: Long = -1L
    private var snoozesUsed: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)

        setContent {
            WakeUpTheme {
                Text("Alarm $alarmId firing") // replaced by MissionHost in Task 22
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
```

- [ ] **Step 2: Declare the activity in the manifest**

Inside `<application>` add:

```xml
<activity
    android:name=".alarm.AlarmActivity"
    android:exported="false"
    android:launchMode="singleInstance"
    android:excludeFromRecents="true"
    android:showOnLockScreen="true"
    android:turnScreenOn="true" />
```

- [ ] **Step 3: Build the whole project**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` (Receiver + Service + Activity now all resolve).

- [ ] **Step 4: On-device smoke test**

Set an alarm for ~2 minutes out (via the UI), lock the phone. Expected: at the trigger time the screen turns on, the alarm sound plays (ramping up), and a full-screen "Alarm N firing" screen appears over the lock screen. Kill the app from recents first to confirm it still fires (setAlarmClock is independent of the process).

Tear-down for now: since dismissal isn't wired yet, stop the sound via: `adb shell am force-stop com.wakeup` (temporary, only until Task 22).

- [ ] **Step 5: Commit (covers Tasks 17–19)**

```bash
git add app/src/main/java/com/wakeup/alarm/ app/src/main/AndroidManifest.xml
git commit -m "feat: add AlarmReceiver, foreground AlarmService, and full-screen AlarmActivity"
```

---

### Task 20: `MathMissionContent` Composable

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/firing/MathMissionContent.kt`

UI-only; verified via the integrated flow in Task 23. Logic (generation/validation) is already tested in Task 9.

- [ ] **Step 1: Write the composable**

```kotlin
package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wakeup.domain.MathParams
import com.wakeup.mission.MathProblem
import com.wakeup.mission.MathProblemGenerator

@Composable
fun MathMissionContent(params: MathParams, onComplete: () -> Unit) {
    val generator = remember { MathProblemGenerator() }
    var solved by remember { mutableStateOf(0) }
    var problem by remember { mutableStateOf<MathProblem>(generator.generate(params.difficulty)) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Solve to wake up", style = MaterialTheme.typography.titleLarge)
        Text("${solved} / ${params.problemCount}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Text("${problem.text} = ?", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' }; error = false },
            isError = error,
            label = { Text(if (error) "Wrong — try again" else "Answer") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                submit(input, problem, onCorrect = {
                    solved++
                    if (solved >= params.problemCount) onComplete()
                    else { problem = generator.generate(params.difficulty); input = "" }
                }, onWrong = { error = true; input = "" })
            }),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            submit(input, problem, onCorrect = {
                solved++
                if (solved >= params.problemCount) onComplete()
                else { problem = generator.generate(params.difficulty); input = "" }
            }, onWrong = { error = true; input = "" })
        }) { Text("Submit") }
    }
}

private inline fun submit(input: String, problem: MathProblem, onCorrect: () -> Unit, onWrong: () -> Unit) {
    val value = input.toIntOrNull()
    if (value != null && value == problem.answer) onCorrect() else onWrong()
}
```

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/firing/MathMissionContent.kt
git commit -m "feat: add math mission Compose UI"
```

---

### Task 21: `SquatDetector` interface, accelerometer impl, and `SquatMissionContent`

**Files:**
- Create: `app/src/main/java/com/wakeup/mission/SquatDetector.kt`
- Create: `app/src/main/java/com/wakeup/mission/AccelerometerSquatDetector.kt`
- Create: `app/src/main/java/com/wakeup/ui/firing/SquatMissionContent.kt`

- [ ] **Step 1: Write the detector interface**

```kotlin
package com.wakeup.mission

import kotlinx.coroutines.flow.Flow

/** Emits the running rep count as the user squats. Implementations own their sensor lifecycle. */
interface SquatDetector {
    /** Cold flow: collecting registers the sensor; cancelling unregisters it. Emits total reps. */
    fun repCounts(): Flow<Int>
}
```

- [ ] **Step 2: Write the accelerometer implementation**

```kotlin
package com.wakeup.mission

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class AccelerometerSquatDetector(context: Context) : SquatDetector {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Tuned on-device in Task 25; rest magnitude ~9.81.
    private val counter = SquatCounter(downThreshold = 7.5f, upThreshold = 11.5f, minRepIntervalMs = 600)

    override fun repCounts(): Flow<Int> = callbackFlow {
        counter.reset()
        trySend(0)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val magnitude = sqrt(x * x + y * y + z * z)
                val tsMs = event.timestamp / 1_000_000L // ns → ms
                if (counter.onSample(magnitude, tsMs)) trySend(counter.count)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
```

- [ ] **Step 3: Write the squat mission UI**

```kotlin
package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.domain.SquatParams
import com.wakeup.mission.AccelerometerSquatDetector
import com.wakeup.mission.SquatDetector

@Composable
fun SquatMissionContent(
    params: SquatParams,
    onComplete: () -> Unit,
    detectorOverride: SquatDetector? = null, // for previews/tests
) {
    val context = LocalContext.current
    val detector = remember(detectorOverride) { detectorOverride ?: AccelerometerSquatDetector(context) }
    val reps by detector.repCounts().collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(reps) { if (reps >= params.squatCount) onComplete() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Do squats to wake up", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Text("$reps / ${params.squatCount}", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (reps.toFloat() / params.squatCount).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text("Hold the phone to your chest and squat.", style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/mission/SquatDetector.kt app/src/main/java/com/wakeup/mission/AccelerometerSquatDetector.kt app/src/main/java/com/wakeup/ui/firing/SquatMissionContent.kt
git commit -m "feat: add SquatDetector (accelerometer) and squat mission UI"
```

---

### Task 22: `MissionHost` — run missions in order, snooze, and dismiss

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/firing/MissionHost.kt`
- Modify: `app/src/main/java/com/wakeup/alarm/AlarmActivity.kt`

- [ ] **Step 1: Write the MissionHost composable**

```kotlin
package com.wakeup.ui.firing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakeup.domain.Alarm
import com.wakeup.domain.MathParams
import com.wakeup.domain.SquatParams
import com.wakeup.schedule.SnoozePolicy

@Composable
fun MissionHost(
    alarm: Alarm,
    snoozesUsed: Int,
    onAllComplete: () -> Unit,
    onSnooze: () -> Unit,
) {
    var index by remember { mutableStateOf(0) }
    val canSnooze = SnoozePolicy.canSnooze(alarm.snoozeEnabled, alarm.maxSnoozes, snoozesUsed)

    Scaffold(bottomBar = {
        if (canSnooze) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = onSnooze) { Text("Snooze ${alarm.snoozeMinutes} min") }
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            if (alarm.missions.isEmpty()) {
                // No missions configured: a single dismiss button.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onAllComplete) { Text("Dismiss") }
                }
                return@Box
            }
            val mission = alarm.missions[index]
            val advance = {
                if (index + 1 >= alarm.missions.size) onAllComplete() else index++
            }
            when (val p = mission.params) {
                is MathParams -> MathMissionContent(params = p, onComplete = advance)
                is SquatParams -> SquatMissionContent(params = p, onComplete = advance)
            }
        }
    }
}
```

- [ ] **Step 2: Wire MissionHost into AlarmActivity with load + dismiss/snooze actions**

Replace `AlarmActivity.kt` with:

```kotlin
package com.wakeup.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.wakeup.di.Graph
import com.wakeup.domain.Alarm
import com.wakeup.schedule.SnoozePolicy
import com.wakeup.ui.firing.MissionHost
import com.wakeup.ui.theme.WakeUpTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private var alarmId: Long = -1L
    private var snoozesUsed: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        showWhenLockedAndTurnScreenOn()
        alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)

        lifecycleScope.launch {
            val alarm = Graph.repository.getAlarm(alarmId) ?: Alarm(hour = 0, minute = 0)
            setContent {
                WakeUpTheme {
                    MissionHost(
                        alarm = alarm,
                        snoozesUsed = snoozesUsed,
                        onAllComplete = { dismiss(alarm) },
                        onSnooze = { snooze() },
                    )
                }
            }
        }
    }

    private fun dismiss(alarm: Alarm) {
        AlarmService.stop(this)
        // Re-arm next occurrence for repeating alarms; one-shot just stops.
        Graph.scheduler.schedule(alarm)
        finish()
    }

    private fun snooze() {
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_SNOOZE
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        }
        sendBroadcast(snoozeIntent)
        AlarmService.stop(this)
        finish()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
```

Note on repeating re-arm: `Graph.scheduler.schedule(alarm)` recomputes the next trigger from `now`, which for a repeating alarm is the next matching weekday and for a one-shot is tomorrow. For a true one-shot we don't want it to re-arm; handle that in Step 3.

- [ ] **Step 3: Don't re-arm one-shot alarms; disable them after firing**

Change `dismiss` to:

```kotlin
private fun dismiss(alarm: Alarm) {
    AlarmService.stop(this)
    lifecycleScope.launch {
        if (com.wakeup.domain.RepeatDays.isOneShot(alarm.repeatDays)) {
            Graph.repository.setEnabled(alarm.id, false)
            Graph.scheduler.cancel(alarm.id)
        } else {
            Graph.scheduler.schedule(alarm)
        }
        finish()
    }
}
```

- [ ] **Step 4: Build, install, full end-to-end on the Pixel 9**

Run: `./gradlew installDebug`
Manual: set an alarm 1–2 min out with math×2 then squats×5. Lock phone. At fire: screen wakes, sound ramps, full-screen mission appears. Solve math, then squat (hold phone to chest) to reach the count. Confirm sound stops and screen dismisses. Set a one-shot and confirm it disables after firing; set a repeating alarm and confirm it re-arms for the next day.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/firing/MissionHost.kt app/src/main/java/com/wakeup/alarm/AlarmActivity.kt
git commit -m "feat: run missions in order with snooze/dismiss; one-shot disables after firing"
```

---

## Phase 6 — Escape-proofing, permissions, reliability

### Task 23: Escape-proofing in `AlarmActivity`

**Files:**
- Modify: `app/src/main/java/com/wakeup/alarm/AlarmActivity.kt`

- [ ] **Step 1: Disable back and relaunch on background**

In `AlarmActivity.onCreate`, after `super.onCreate(...)`, register a back-press callback that does nothing:

```kotlin
onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
    override fun handleOnBackPressed() { /* swallow: missions must be completed */ }
})
```

Add a field and lifecycle guard so leaving the Activity (home/recents) relaunches it while the service is still ringing:

```kotlin
private var completed = false
```

Set `completed = true` at the start of both `dismiss(...)` and `snooze()` (before `finish()`). Then override `onStop`:

```kotlin
override fun onStop() {
    super.onStop()
    if (!completed && !isFinishing) {
        // Brought to background without completing — relaunch ourselves over the lock screen.
        startActivity(Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmContract.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmContract.EXTRA_SNOOZES_USED, snoozesUsed)
        })
    }
}
```

- [ ] **Step 2: Build, install, verify**

Run: `./gradlew installDebug`
Manual: while an alarm is firing, press Home and Recents. Expected: the alarm screen returns to the foreground within a moment and the sound keeps playing. Completing missions or snoozing still exits cleanly (no relaunch loop).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/wakeup/alarm/AlarmActivity.kt
git commit -m "feat: escape-proof firing screen (swallow back, relaunch on background)"
```

---

### Task 24: Permission onboarding screen

**Files:**
- Create: `app/src/main/java/com/wakeup/ui/perms/PermissionsScreen.kt`
- Modify: `app/src/main/java/com/wakeup/MainActivity.kt`

- [ ] **Step 1: Write the permissions screen**

```kotlin
package com.wakeup.ui.perms

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/** Returns true when all alarm-critical permissions are satisfied. */
fun alarmPermissionsGranted(context: Context): Boolean {
    val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true
    val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    else true
    return notif && exact
}

@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val granted = remember(refresh) { alarmPermissionsGranted(context) }

    LaunchedEffect(granted) { if (granted) onAllGranted() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Permissions needed", style = MaterialTheme.typography.headlineSmall)
        Text("WakeUp needs these to ring reliably over the lock screen.")

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else refresh++
        }) { Text("Allow notifications") }

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")))
            }
        }) { Text("Allow exact alarms") }

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")))
        }) { Text("Disable battery optimization") }

        TextButton(onClick = { refresh++ }) { Text("I've granted these — continue") }
    }
}
```

- [ ] **Step 2: Gate the app on permissions in `MainActivity`**

Add a `permissions` start destination check. Replace the `NavHost` `startDestination` logic:

```kotlin
import com.wakeup.ui.perms.PermissionsScreen
import com.wakeup.ui.perms.alarmPermissionsGranted
```

and set:

```kotlin
val start = if (alarmPermissionsGranted(this)) "list" else "permissions"
NavHost(navController = nav, startDestination = start) {
    composable("permissions") {
        PermissionsScreen(onAllGranted = { nav.navigate("list") { popUpTo("permissions") { inclusive = true } } })
    }
    // ... existing list and edit composables ...
}
```

- [ ] **Step 3: Build, install, verify**

Run: `./gradlew installDebug`
Manual: on first launch (fresh install / cleared data), the permissions screen appears; granting notifications + exact alarms advances to the list. Verify `adb shell pm clear com.wakeup` resets it.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/wakeup/ui/perms/PermissionsScreen.kt app/src/main/java/com/wakeup/MainActivity.kt
git commit -m "feat: add permission onboarding and gate app entry on it"
```

---

### Task 25: Boot re-arm verification + final on-device E2E and squat tuning

**Files:**
- Modify (only if tuning needed): `app/src/main/java/com/wakeup/mission/AccelerometerSquatDetector.kt`

No new code unless tuning is required; this task validates reliability end to end.

- [ ] **Step 1: Verify boot re-arming**

Set a repeating alarm. Reboot the Pixel 9. After boot completes (don't open the app), confirm via `adb shell dumpsys alarm | grep com.wakeup` that an alarm is registered. Then let it fire to confirm.

- [ ] **Step 2: Tune squat thresholds on-device**

Run a squat mission and count manually vs. the app. If reps over/under-count, adjust `downThreshold` / `upThreshold` / `minRepIntervalMs` in `AccelerometerSquatDetector` (rest magnitude is ~9.81; deeper/faster squats produce larger excursions). Re-run `SquatCounterTest` after any threshold-shape change to keep the state machine correct. Commit any change:

```bash
git add app/src/main/java/com/wakeup/mission/AccelerometerSquatDetector.kt
git commit -m "tune: squat detection thresholds for device"
```

- [ ] **Step 3: Full acceptance pass**

Walk the full checklist on the Pixel 9:
- Create math-only, squat-only, and math+squat alarms.
- Fire with the app killed (recents-swiped) → still rings over lock screen.
- Snooze respects `maxSnoozes` (button disappears once exhausted).
- Back button / Home / Recents cannot escape a firing alarm.
- One-shot disables after firing; repeating re-arms for the next matching day.
- Gradual volume ramps; vibration toggles correctly; custom sound (if you wire the ringtone picker — optional) or default plays.

- [ ] **Step 4: Run the full unit + instrumented suite**

Run: `./gradlew testDebugUnitTest connectedDebugAndroidTest`
Expected: all green.

- [ ] **Step 5: Final commit / tag**

```bash
git add -A
git commit -m "test: verify boot re-arm and full alarm acceptance on device" || echo "nothing to commit"
git tag v0.1.0
```

---

## Notes & deferrals

- **Sound picker:** the edit screen currently keeps `soundUri` from defaults. Wiring Android's `RingtoneManager.ACTION_RINGTONE_PICKER` into `AlarmEditScreen` is a small, optional follow-up (the service already honors `soundUri`).
- **Camera squat detection:** intentionally deferred. The `SquatDetector` interface is the seam — add a `PoseSquatDetector` and a setting to choose it, with no changes elsewhere.
- **Permission version drift:** `USE_FULL_SCREEN_INTENT`, exact-alarm, and foreground-service rules shift across Android releases. If the full-screen intent doesn't surface on the Pixel 9's Android version, check Settings → Apps → WakeUp → "Full screen intents" and the foreground-service-type requirements for that API level.
- **`ic_launcher_foreground`** is used as the notification small icon for simplicity; swap for a dedicated monochrome alarm icon if desired.
```
