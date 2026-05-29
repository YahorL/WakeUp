# WakeUp — Alarmy-style Alarm App (Design)

**Date:** 2026-05-29
**Status:** Approved (design phase)
**Platform:** Native Android (Kotlin, Jetpack Compose)
**Distribution:** Personal use (sideload)

## 1. Goal

An alarm app in the style of Alarmy. Alarms are difficult to dismiss: each alarm
can require one or more "missions" — solving math problems and/or doing squats —
that must be completed before the alarm stops ringing. The alarm fires reliably
even when the phone is locked or the app has been killed, and is hard to escape
without completing its missions.

### Non-goals (v1)

- Camera-based squat detection (designed for, not built — see §6).
- Cloud sync / accounts / multi-device.
- Play Store publication (build cleanly, but no store paperwork now).
- Mission types beyond math and squats (e.g. barcode scan, photo, typing).

## 2. Platform & constraints

- **Native Android / Kotlin / Jetpack Compose**, single app module, MVVM.
- **minSdk 26** (Android 8.0) — notification channels, modern APIs.
- **targetSdk**: latest stable available at implementation time (verify exact
  permission behavior against it — Android 12/13/14/15 changed exact-alarm,
  notification, full-screen-intent, and foreground-service rules).
- Personal sideload: runtime permissions handled in-app; no store policy work.
- End-to-end testing requires a **real device or emulator** (alarm firing,
  full-screen intent, real sensors cannot be unit-tested).

## 3. Architecture

Single module, organized by responsibility:

- `data/` — Room entities, DAOs, database, `AlarmRepository`.
- `alarm/` — `AlarmScheduler`, `AlarmReceiver`, `AlarmService`, `AlarmActivity`
  (the firing engine).
- `mission/` — `Mission` model, `MathMission`, `SquatMission`,
  `SquatDetector` interface + `AccelerometerSquatDetector`.
- `ui/` — Compose screens (list, edit, firing) + ViewModels + theme.
- `permissions/` — onboarding / permission gate.

The "engine" pieces are canonical for a reliable Android alarm and have no good
alternatives, so they are fixed (not design options):

- **Scheduling:** `AlarmManager.setAlarmClock()` (exact, survives Doze, shows the
  system alarm icon) + `BroadcastReceiver`, re-armed on reboot.
- **Ringing:** a **foreground service** owns sound (`MediaPlayer` with gradual
  volume ramp), vibration, and wake lock; runs until missions complete.
- **Lock-screen takeover:** **full-screen-intent notification** launching a
  full-screen Activity with `setShowWhenLocked` / `setTurnScreenOn`.
- **Squats:** `SensorManager` accelerometer with peak-detection, behind a
  `SquatDetector` interface (camera impl can drop in later).
- **Persistence:** `Room` for alarms + mission configs.

## 4. Data model (Room)

### `Alarm`
| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK, auto) | |
| `hour`, `minute` | Int | 24h time-of-day |
| `label` | String | user label, may be empty |
| `enabled` | Boolean | |
| `repeatDays` | Int | 7-bit weekday mask; `0` = one-shot |
| `soundUri` | String? | system ringtone URI; null = default |
| `vibrate` | Boolean | |
| `gradualVolume` | Boolean | ramp volume up |
| `rampSeconds` | Int | ramp duration when `gradualVolume` |
| `snoozeEnabled` | Boolean | |
| `snoozeMinutes` | Int | snooze length |
| `maxSnoozes` | Int | cap on snoozes (only when `snoozeEnabled`); `0` = unlimited |

### `MissionConfig` (one-to-many with `Alarm`, ordered)
| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK, auto) | |
| `alarmId` | Long (FK) | |
| `order` | Int | execution order within the alarm |
| `type` | Enum | `MATH` \| `SQUAT` |
| `params` | String (JSON) | type-specific params via TypeConverter |

- **Math params:** `difficulty` (EASY \| MEDIUM \| HARD), `problemCount`.
- **Squat params:** `squatCount`.

Storing params as JSON behind a TypeConverter keeps the schema stable as new
mission types are added.

### Defaults (new alarm)
Medium math ×3, then 10 squats; one 5-minute snooze; gradual volume on (30s);
vibrate on; default ringtone; one-shot (no repeat).

## 5. Alarm firing flow

1. `AlarmScheduler.schedule(alarm)` computes the next trigger from time +
   `repeatDays` and registers it with `setAlarmClock()` → PendingIntent to
   `AlarmReceiver`.
2. `AlarmReceiver` fires → starts `AlarmService` (foreground, type
   `mediaPlayback`).
3. `AlarmService` acquires a wake lock, plays the sound with the gradual ramp,
   vibrates, and posts a **full-screen-intent** notification.
4. The full-screen intent launches `AlarmActivity` over the lock screen
   (`setShowWhenLocked` + `setTurnScreenOn`), hosting the mission flow.
5. On all missions complete → Activity stops the Service, then the Scheduler
   arms the next occurrence (repeating alarms) or disables the alarm (one-shot).
6. `BOOT_COMPLETED` → `AlarmReceiver` re-arms all enabled alarms.

## 6. Escape-proofing (strong)

- `AlarmActivity` disables the back gesture/button.
- A lifecycle guard relaunches `AlarmActivity` if it is backgrounded before
  completion (home button, recents).
- `AlarmService` keeps the sound playing regardless of the Activity state and
  stops **only** on genuine mission completion.
- **Snooze is the only sanctioned exit**, and only when the alarm allows it and
  `maxSnoozes` is not exhausted. After max snoozes, snooze is disabled and
  missions must be completed.

## 7. Mission framework

- `MissionHost` Composable runs the alarm's missions in configured order,
  advancing only when each reports success.
- **MathMission** — generates problems per difficulty:
  - EASY: single-digit `+` / `−` (non-negative results).
  - MEDIUM: two-digit `+` / `−` and single×two-digit `×`.
  - HARD: multi-step (e.g. `a × b + c`).
  Requires `problemCount` correct answers in a row; a wrong answer regenerates
  the current problem (no progress lost beyond that problem).
- **SquatMission** — `SquatDetector` interface; `AccelerometerSquatDetector`
  does peak/valley detection on vertical acceleration with debouncing, counting
  reps toward `squatCount` and showing live progress. Camera-based detection can
  be added later behind the same interface with no other changes.

## 8. Screens (Compose)

- **Alarm list (home):** each alarm shows time, label, repeat days, enable
  toggle, and next-fire time; FAB to add a new alarm.
- **Alarm edit:** time picker; label; repeat-day chips; sound picker (system
  ringtone picker); vibrate + volume-ramp toggles; **missions editor**
  (add / remove / reorder math & squat, set params); snooze rules.
- **Alarm firing:** full-screen mission flow (§7).
- **Permission onboarding:** requests notifications, exact alarm, full-screen
  intent; prompts for battery-optimization exemption (important for reliability).

## 9. Permissions (manifest)

- `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM` (alarm-clock app; normally
  auto-granted on recent Android, runtime fallback for Android 12–13).
- `POST_NOTIFICATIONS` (Android 13+ runtime).
- `USE_FULL_SCREEN_INTENT` (Android 14+).
- `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- Accelerometer requires no permission.

Exact behavior is version-sensitive; verify against the chosen `targetSdk`
during planning/implementation.

## 10. Error handling

- Exact-alarm permission denied → prompt; fall back to inexact alarm with a
  visible reliability warning.
- Invalid/missing `soundUri` → fall back to default ringtone.
- Service killed → `START_STICKY`; the `setAlarmClock` registration means the
  alarm still fires independently.
- Squat sensor noise → tuned thresholds + debounce; reject implausibly fast reps.
- Reboot → re-arm all enabled alarms.

## 11. Testing strategy

Pure-Kotlin logic is unit-tested (TDD):

- **Next-trigger-time computation** from `repeatDays` (today-vs-tomorrow,
  weekday wrap, one-shot) — the trickiest logic.
- Math generation/validation per difficulty (result ranges, operand bounds).
- Squat peak-detection fed **synthetic accelerometer sample arrays**.
- Snooze scheduling and `maxSnoozes` enforcement.
- Mission ordering.

`AlarmRepository` is tested with an in-memory Room database. Framework pieces
(Service, full-screen intent, real sensors) are validated via instrumented tests
and manual on-device testing.

## 12. Build order (high level)

1. Project scaffold (Compose, Room, Gradle, manifest, permissions).
2. Data layer + repository (+ tests).
3. Alarm list & edit UI with scheduling logic (+ next-trigger tests).
4. Alarm firing engine (receiver, service, full-screen activity).
5. Mission framework + math mission (+ tests).
6. Squat mission + accelerometer detector (+ tests).
7. Escape-proofing + permission onboarding + reboot re-arming.
8. End-to-end testing on device; threshold tuning.
