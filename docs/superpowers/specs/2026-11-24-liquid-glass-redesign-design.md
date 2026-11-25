# Liquid Glass redesign — design

Date: 2026-11-24
Source package: `alarm-liquid-glass-handoff/` (`DESIGN_SPEC.md`, `tokens.json`, `screenshots/`, `reference/*.dc.html`). Where the spec and a reference file disagree, the reference wins. Where the package and this document disagree, this document wins.

## Intent

Restyle the WakeUp app to the dark "Liquid Glass" look in the handoff package. The user asked for the design to be adapted to the app, not the app to the design: where the package assumes behaviour the app doesn't have, the app's behaviour stays and the design is bent to fit.

## Goals

- Every screen uses the new look: Alarms list, New/Edit alarm, the wake-up flow (ring, squats, math, alarm off), Settings, Permissions.
- All motion in `DESIGN_SPEC.md` §5, and all of it off when the system animator scale is 0.
- Real alarms, real sensor reps, real generated problems. No mock data.

## Non-goals

- No changes to scheduling, `AlarmService`, `AlarmReceiver`, `RingState`, `WakeUpCoordinator`, the squat detector, `MathProblemGenerator`, or the Room schema.
- No light theme.
- No true backdrop blur (see Approaches).

## Anti-goals

- The mockup's "tap the ring to count a rep" must not ship. Reps come only from the motion sensor.
- No pink anywhere.
- No bounce on typed math digits.
- Nothing may make a ringing alarm easier to escape than it is today (Back stays swallowed, `onStop` relaunch stays, snooze limits stay).

## Constraints

- Kotlin 2.0.21, Compose BOM 2024.10.01, minSdk 26, CLI-only build via `./gw` from the repo root.
- The public APIs of `AlarmListViewModel` and `AlarmEditViewModel` stay as they are, so the existing unit tests keep passing unchanged. Additions are allowed.
- `MissionHost`'s contract with `AlarmActivity` stays: `alarm`, `snoozesUsed`, `onAllComplete`, `onSnooze`, `onEngaged`. One addition: `onClose` (see Alarm off).
- Disk is tight (about 6 GB free). Avoid heavy new dependencies.

## Approaches

**A. Translucent fills, no blur library (chosen).** Glass surfaces are gradient fills with a hairline border and a top highlight, drawn over orbs that are already soft radial gradients. Blurring a smooth gradient changes almost nothing, so the look holds. Only the Alarm-off overlay blurs real content: `Modifier.blur(30.dp)` on API 31+, dim only below. No new runtime dependency, cheap to draw on the wake-up screen, works on API 26.

**B. Haze library for real backdrop blur.** Truest to the CSS, and cards would blur list rows scrolling beneath them. But almost nothing scrolls under glass in this design, it adds a dependency whose versions must match the Compose/Kotlin pair here, and it costs GPU time on a screen that must come up instantly.

**C. Platform `RenderEffect` per card.** No dependency, but it blurs a layer's own content, not what is behind it, so it doesn't produce backdrop blur without re-drawing the background into every card.

Chosen: A. This was Claude's call; the user handed over the how.

## Decisions on flow (the user's)

- **Keep today's flow**: ring screen with Begin and Snooze → squats → math → alarm off. The design's ringing screens are adapted to it.

## Decisions (Claude's calls — correct any)

### Theme and tokens

- Dark only. `WakeColors` is rebuilt from `tokens.json`: `bg`, text levels, `error`, hairline, white pill, plus the active palette (`orb1`, `orb2`, `orb3`, `fillFrom`, `fillTo`).
- Three palettes, aurora default, chosen in Settings. `SettingsStore` keeps one value, the palette. The old accent, dark and font-pairing settings and their code (`Accent.kt`, `FontPairing`, `PatternBg`) are removed; old preference keys are simply ignored.
- Fonts: Outfit (200, 300, 400, 500, 600) and IBM Plex Mono (400, 500), downloaded from Google Fonts and bundled in `res/font`. The four old families are deleted. All changing numbers use tabular numerals.
- `themes.xml`: window background `#08080C`; light system-bar icons forced on both activities.
- Background (`Screen`): solid bg, three orbs as radial gradients (colour → transparent at 66%), then a tiled 256 px noise PNG at about 7% alpha. `Screen` takes an orb layout parameter because each screen places its orbs differently; positions come from the reference files.
- Glow shadows are drawn as radial gradients or blurred paint behind the shape, since coloured elevation shadows aren't available on API 26.

### Shared components (`ui/components`)

`GlassCard` (hero / card / control / tile / chip recipes as one `Modifier.glass(recipe, radius)`), `GlassIconButton`, `PrimaryButton`, `WhitePillButton`, `Chip` with breathing dot, `Toggle`, `ChallengeIconTile`, `Stepper`, `TickRuler`, `DotBar`, `SegmentedControl`, `DayPill`, `TimeWheel`, `ProgressRing`, `Keypad`, `MonoLabel`, and a `riseIn(delayMs)` modifier for staggered entrances. Sizes, colours and motion as in `DESIGN_SPEC.md` §3 and §5. `WakeIcon` gains: sliders, math (+ − × =), lock, arrow-right, check-draw.

The old components they replace (`AccentButton`, `WakeSwitch`, `SegmentChip`, `Badge`, `IconChip`, `NumPad`, `cardSurface`, `SectionLabel`) are deleted once nothing uses them.

### Motion and haptics

- One `LocalReduceMotion` CompositionLocal, true when `Settings.Global.ANIMATOR_DURATION_SCALE` is 0. Every entrance, spring, ripple, breathe and alive loop checks it; state changes stay instant.
- Easing and spring values from `tokens.json`. Press scale 0.96 on every button.
- Haptics as in §5: light on each rep, success on target reached and alarm off, error on a wrong answer.

### Clock format

Follow the device setting (`DateFormat.is24HourFormat`). In 12-hour mode: times show as `6:30` with a small AM/PM beside them in secondary colour (about 30% of the numeral size, baseline-aligned) on the hero, rows and ring clock; the header sub-line reads "next at 6:30 AM"; the wheel gets a third AM/PM column. One formatting helper in `AlarmDisplay.kt`, unit-tested for both modes.

### Alarms list

As §4.1, with:

- "Next alarm" comes from a new pure function `nextAlarm(alarms, now)` returning the alarm and its trigger time (built on `NextTrigger.compute`), unit-tested. The hero and the header sub-line both use it. The hero is hidden when it returns null.
- Repeat summary ("Weekdays", "Weekends", "Every day", "Once", "Mon, Wed") and the row's days text ("Mon – Fri", "Sat, Sun") are pure functions, unit-tested.
- Countdown text format becomes "in 8 h 44 min".
- Hero tiles show only enabled challenges; with none, one tile reads "No challenge" / "Single tap".
- Rows keep today's behaviour: tap the row to edit, tap the toggle to enable or disable.

### New / Edit alarm

As §4.2, with:

- **TimeWheel replaces the Material time-picker dialog.** It is a real snapping, looping scroll wheel (two `LazyColumn`s with snap fling), not tap-to-step; tapping a neighbour also scrolls to it. It calls the existing `setTime(hour, minute)`.
- "Rings in X h Y min" uses `NextTrigger.compute` on the draft, refreshed each minute.
- Squat stepper range is 5–50. An existing alarm stored below 5 opens showing its real value with minus disabled; it is only rewritten if the user changes it. The tick index is clamped to 0–33.
- "Save alarm" sits at the end of the scrolling content, as drawn, not in a fixed footer.
- The title reads "Edit alarm" for an existing alarm. Delete behaves as it does today.
- Summary text covers all four cases (both, squats only, math only, none).

### Wake-up flow

`MissionHost` keeps its phases (Ring → Mission(i) → Done) and gains a "squats complete, waiting" state.

**Ring screen (adapted; not in the package).** Top chip with breathing dot and the alarm label, clock, full date, as on `03-ringing-squats.png`. In the centre, a smaller glass disc (about 200) with the alive glow and a bell icon. Below, the glass steps panel listing the enabled challenges as upcoming rows. Then the primary button "Begin" (or "Stop" when the alarm has no challenges), and under it a "Snooze N min" text button, shown only when `SnoozePolicy.canSnooze` allows. Snooze appears only here, as today.

**Squats.** As §4.3. The ring is display-only: no click handler. `onEngaged` fires on each rep, as today.
- Reaching the target: burst animation, success haptic, the sensor stops being collected, and the locked row becomes the white "Continue to math →" button. The app waits for the tap. The sound auto-pause is not extended while waiting, so the alarm starts ringing again after the usual 10 s if the user stalls.
- If squats is the last or only challenge there is no button: after the burst (about 0.9 s) it goes straight to Alarm off.

**Math.** As §4.4. The header shows the alarm label and time, "Problem i of n", the difficulty chip, and the segmented progress bar (one segment for squats if enabled, one per problem).
- **Input changes to match the design:** digits build the answer (up to 5), ⌫ deletes, and **✓ submits**. Today's auto-check on digit count and the CLR key are removed. ✓ on an empty answer does nothing.
- Wrong answer: shake, error haptic, the answer turns error-coloured with "Not quite — try again", and it stays until the next key press. While it is showing: a digit clears it and becomes the first digit of the new entry; ⌫ clears it to empty; ✓ does nothing. (The reference instead appends to the wrong answer; this is deliberately different, so a half-awake user never has to delete a wrong answer by hand.)
- Correct answer: next problem, or Alarm off after the last one.
- `onEngaged` fires on every key, as today.

**Alarm off.** The overlay from `04c-alarm-off.png` over the last screen (blurred on API 31+, dimmed otherwise), with the recap line built from what the alarm required.
- The alarm is silenced and marked finished the moment the last challenge completes (today's `dismiss()` path), never on the button.
- The white pill reads **"Done"** rather than "Back to alarms", because this screen usually sits over the lock screen where opening the list would demand an unlock. It closes the screen. If untouched, the screen closes itself after 5 s (today it is 2.2 s, extended so the entrance animation can finish).
- With no challenges ("Stop"), the same overlay shows with the line "Good morning."

### Settings and Permissions

Not in the package; restyled with the same parts. Settings: glass top bar, a "PALETTE" section with three selectable swatch cards (aurora, ember, lagoon). Permissions: same content and logic as today in glass cards with the primary button.

## Order of work

1. Theme tokens, fonts, background, reduce-motion local.
2. Shared components.
3. Alarms list (with the pure helpers and their tests).
4. New/Edit alarm.
5. Ring screen and squats.
6. Math and Alarm off.
7. Settings and Permissions; delete dead components and fonts.
8. Motion pass, then reduce-motion pass.

`./gw testDebugUnitTest` and `./gw assembleDebug` must pass after every step.

## Verification

- Unit tests: existing suites unchanged and green; new tests for `nextAlarm`, repeat summary, days text, time formatting (12/24), countdown text, tick index, and the math-entry state logic (extracted into a small pure reducer so ✓/wrong/clear can be tested without Compose).
- Visual: there is no phone connected and no emulator on this machine. At the end of step 3 the user pairs the Pixel over wireless ADB; from then on each screen is installed, screenshotted with `adb exec-out screencap`, and compared with its file in `screenshots/`. No screen is called a visual match before that comparison is done.
- On-device checks for the wake-up flow: a test alarm fires over the lock screen, Begin and Snooze work, reps count from the sensor only, Continue appears, ✓ submits, the alarm goes silent on the last answer, Back cannot escape.

## Git

The refactor on `refactor/review-cleanup` is committed (head `93236cc`) and the tree is clean apart from this document. With the user's go-ahead: branch `feat/liquid-glass` from that head, commit this document there, and commit after each step of the work. Nothing is committed without that go-ahead.

## Left to the builder

- Exact orb positions and any measurement not listed here: read them from `reference/*.dc.html`.
- How to generate the noise tile.
- Internal structure of each component and how animations are composed.
- Layout squeeze on short screens (the ring and squat screens should shrink the disc before anything scrolls).
