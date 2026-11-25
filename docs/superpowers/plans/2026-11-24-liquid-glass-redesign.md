# Liquid Glass Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every screen of the WakeUp Android app to the dark "Liquid Glass" design while keeping all alarm behaviour.

**Architecture:** A rebuilt theme (`ui/theme`) supplies tokens, palette, fonts and the orb background. A set of glass components (`ui/components`) is built on one `Modifier.glass` recipe. Screens are rewritten on those components; anything with logic (formatting, next alarm, math entry) is a pure function with unit tests. No blur library: glass is translucent fill + hairline + top highlight over soft orbs.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01), Material3 (text only), JUnit4. Build with `./gw` from `/home/nyx-ai/Projects/Alarm`.

**Spec:** `docs/superpowers/specs/2026-11-24-liquid-glass-redesign-design.md`. Visual source: `alarm-liquid-glass-handoff/` (`DESIGN_SPEC.md`, `tokens.json`, `screenshots/`, `reference/*.dc.html`; reference beats DESIGN_SPEC, the spec above beats both).

## Global Constraints

- Run Gradle only as `./gw <task>` from the repo root. After every task: `./gw testDebugUnitTest` and `./gw assembleDebug` both succeed.
- Do not change: `alarm/` (except the one `AlarmActivity` edit in Task 7), `schedule/`, `mission/`, `data/`, `domain/`, the Room schema.
- `AlarmListViewModel` and `AlarmEditViewModel` public APIs stay; existing tests are not edited.
- Preserve every behaviour the current screens have at HEAD, including the "locked" handling for an alarm that is ringing or snoozed (`AlarmEditViewModel.locked`, `AlarmListViewModel.activeAlarms`). Read each screen at HEAD before rewriting it.
- minSdk 26. No new runtime dependencies.
- Dark only. No pink. No ring tap-to-count. No animation on typed math digits.
- Every animation is skipped when `LocalReduceMotion.current` is true.
- Icon-only buttons have content descriptions; toggles, day pills and segments expose state (DESIGN_SPEC §6).
- Text that the user controls (alarm label) is single-line with ellipsis everywhere it is shown.
- Commit after each task on `feat/liquid-glass`. No attribution lines in commit messages.

## Review Focus

1. 12-hour devices: midnight and noon must read `12:00 AM` / `12:00 PM`. Pinned in Task 2 (`formatClock`).
2. An alarm saved before this change with fewer than 5 squats: opens showing its real value, ruler index clamps. Pinned in Task 2 (`tickIndex`) and Task 5 (stepper range rule).
3. Singular counts: "1 squat", "1 problem" in summaries and recaps. Pinned in Task 2.
4. No enabled alarms, or an empty list: no hero, sub-line "No upcoming alarms". Pinned in Task 2 (`nextAlarm`).
5. Math answers typed with leading zeros (`07` for 7) count as correct; ✓ on empty does nothing. Pinned in Task 2 (`MathEntry`).

## File Structure

```
ui/theme/WakeColors.kt     tokens + Palette enum (rewritten)
ui/theme/WakeFonts.kt      Outfit + IBM Plex Mono (rewritten)
ui/theme/WakeTheme.kt      WakeUpTheme(palette), Wake accessor, LocalReduceMotion
ui/theme/Background.kt     Screen(orbs) — replaces PatternBg.kt
ui/theme/Motion.kt         easings, springs, riseIn, pressScale, breathe
ui/AppTheme.kt             reads palette from settings
ui/AlarmDisplay.kt         pure formatting helpers
schedule/… untouched;  ui/NextAlarm.kt   nextAlarm()
ui/components/Glass.kt     Modifier.glass, GlassRecipe, glow helpers, Hairline, MonoLabel, Chip
ui/components/Buttons.kt   PrimaryButton, WhitePillButton, GlassIconButton
ui/components/Controls.kt  Toggle, ChallengeIconTile, Stepper, SegmentedControl, DayPill
ui/components/Meters.kt    TickRuler, DotBar, ProgressRing
ui/components/TimeWheel.kt
ui/components/Keypad.kt
ui/components/WakeIcon.kt  (extended)
ui/firing/MathEntry.kt     pure reducer
ui/firing/{MissionHost,RingContent,SquatMissionContent,MathMissionContent,AlarmOffOverlay}.kt
ui/list, ui/edit, ui/settings, ui/perms   screens
settings/SettingsStore.kt  palette only
res/font/                  outfit_{200,300,400,500,600}.ttf, ibm_plex_mono_{400,500}.ttf
res/drawable-nodpi/noise.png
```

---

### Task 1: Theme foundation

**Files:** rewrite `ui/theme/WakeColors.kt`, `WakeFonts.kt`, `WakeTheme.kt`; create `ui/theme/Background.kt`, `ui/theme/Motion.kt`; delete `ui/theme/PatternBg.kt`, `ui/theme/Accent.kt`; modify `ui/AppTheme.kt`, `settings/SettingsStore.kt`, `ui/settings/SettingsScreen.kt` (minimum to compile), `res/values/themes.xml`, `res/values/colors.xml`; add fonts and `noise.png`.

**Produces:**
- `enum class Palette(val label: String, val orb1: Color, val orb2: Color, val orb3: Color, val fillFrom: Color, val fillTo: Color) { AURORA, EMBER, LAGOON }` with values from `tokens.json`.
- `data class WakeColors(bg, text, textSecondary /* .62 */, textTertiary /* .50 */, textMuted /* .72 */, onLight, error, hairline, whitePill, palette: Palette)` plus `val fill: Brush` (135° `fillFrom → fillTo`).
- **Temporary aliases** on `WakeColors` so untouched screens still compile until Task 8: `accent`(=orb2), `onAccent`, `accentSoft`, `bg2`, `card`, `textDim`, `textMute`, `hair`, `hairStrong`, `selectedFill`, `switchOffTrack`, `keyBg`. Mark each `@Deprecated`.
- `data class WakeFonts(val display: FontFamily /* Outfit */, val mono: FontFamily)` with temporary aliases `ui`, `num` (=display), `numWeight` (=ExtraLight).
- `WakeUpTheme(palette: Palette, content)`, `Wake.colors`, `Wake.fonts`, `val LocalReduceMotion: ProvidableCompositionLocal<Boolean>` (true when `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, read once per composition of the theme).
- `data class Orb(val color: Color, val center: Offset /* fraction of screen, may be <0 or >1 */, val diameter: Dp, val alpha: Float)`; `enum class OrbLayout { LIST, EDITOR, RINGING, MATH }` with positions read from the four reference files; `@Composable fun Screen(layout: OrbLayout = OrbLayout.LIST, modifier, content: BoxScope.() -> Unit)` drawing bg → orbs (radial gradient colour → transparent at 0.66) → tiled `noise.png` at alpha 0.07 → content.
- `Motion.kt`: `val EaseOutExpo = CubicBezierEasing(.2f,.8f,.2f,1f)`, `val SpringEase = CubicBezierEasing(.3f,1.2f,.5f,1f)`, `val SpringSoftEase = CubicBezierEasing(.34f,1.35f,.64f,1f)`, `fun <T> wakeSpring() = spring<T>(dampingRatio = 0.65f, stiffness = 160f)`, `fun Modifier.riseIn(delayMs: Int): Modifier` (opacity 0→1, translateY 16dp→0, scale .98→1, 650 ms EaseOutExpo; no-op under reduce motion), `fun Modifier.pressScale(interactionSource): Modifier` (0.96, 150 ms), `fun Modifier.pressable(onClick, enabled = true, onClickLabel = null)` = no-ripple clickable + pressScale.
- `AppSettings(val palette: Palette = Palette.AURORA)`; `SettingsStore.setPalette(p)`; key `"palette"`; old keys unread.

- [ ] Download Outfit static weights 200–600 and IBM Plex Mono 400/500 from the Google Fonts GitHub repo (`https://github.com/google/fonts/raw/main/ofl/...`; if Outfit is only a variable font there, use one `outfit_variable.ttf` and declare each weight with `FontVariation.Settings(FontVariation.weight(n))`). Delete the nine old font files.
- [ ] Generate `res/drawable-nodpi/noise.png` (256×256 grayscale uniform noise, opaque) with a throwaway Python script kept in the scratchpad.
- [ ] Write the files above. `themes.xml`/`colors.xml`: window background `#08080C`. `AppTheme`: light system-bar icons always off (`isAppearanceLight* = false`).
- [ ] `SettingsScreen`: reduce to a compiling stub that lists the three palettes (full styling in Task 8).
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass theme tokens, fonts and orb background`.

### Task 2: Pure helpers (TDD)

**Files:** modify `ui/AlarmDisplay.kt`; create `ui/NextAlarm.kt`, `ui/firing/MathEntry.kt`; tests `test/.../ui/AlarmDisplayTest.kt`, `ui/NextAlarmTest.kt`, `ui/firing/MathEntryTest.kt`.

**Produces (all in package `com.wakeup.ui` unless noted):**
- `data class ClockText(val time: String, val period: String?)`; `fun formatClock(hour: Int, minute: Int, is24h: Boolean): ClockText` — 24h: `"06:30"`, null; 12h: `"6:30"`, `"AM"`.
- `fun ClockText.inline(): String` → `"06:30"` or `"6:30 AM"`.
- `fun formatCountdown(millis: Long): String` → `"8 h 44 min"`, `"44 min"`, `"0 min"` (replaces the old format; callers add "in "/"Rings in ").
- `fun repeatSummary(mask: Int): String` → `"Once"`, `"Every day"`, `"Weekdays"`, `"Weekends"`, else short names Monday-first joined by `", "` (`"Mon, Wed"`).
- `fun daysText(mask: Int): String` → `"Once"`, `"Every day"`, `"Mon – Fri"`, `"Sat, Sun"`, else same list as above.
- `fun challengeSummary(squat: SquatParams?, math: MathParams?): String` → the four editor sentences from the spec; singular-aware. None: `"No challenge — this alarm stops with a single tap."`
- `fun recapText(squat: SquatParams?, math: MathParams?): String` → `"15 squats and 3 problems done. Good morning."`, single-challenge variants, none: `"Good morning."`
- `fun tickIndex(reps: Int): Int` = `round((reps−5)/45×33)` clamped to 0..33.
- `data class NextAlarm(val alarm: Alarm, val triggerAtMillis: Long)`; `fun nextAlarm(alarms: List<Alarm>, now: ZonedDateTime): NextAlarm?` — enabled alarms only, earliest `NextTrigger.compute`.
- Package `com.wakeup.ui.firing`: 
  ```kotlin
  data class MathEntry(val text: String = "", val wrong: Boolean = false)
  sealed interface MathKey { data class Digit(val d: Int): MathKey; data object Back: MathKey; data object Submit: MathKey }
  enum class MathOutcome { NONE, CORRECT, WRONG }
  fun MathEntry.press(key: MathKey, answer: Int): Pair<MathEntry, MathOutcome>
  ```
  Rules: digit appends (max 5 chars); Back drops last; Submit on empty → unchanged/NONE; Submit equal (`text.toInt() == answer`) → `MathEntry()`/CORRECT; Submit unequal → `copy(wrong = true)`/WRONG. While `wrong`: Digit → `MathEntry(d.toString())`; Back → `MathEntry()`; Submit → unchanged/NONE.
- Keep `DAY_CHIPS`, `Difficulty.label`. Remove `hour12`, `periodLabel`, `time12`, `missionSummary` only when their last caller is gone (Task 7).

- [ ] Write the three test files. Required assertions include: `formatClock(0,0,false)==ClockText("12:00","AM")`, `formatClock(12,0,false).period=="PM"`, `formatClock(6,30,true)==ClockText("06:30",null)`; `formatCountdown(8*3600_000L+44*60_000)=="8 h 44 min"`; `repeatSummary(0b0011111)=="Weekdays"`, `repeatSummary(0b1100000)=="Weekends"`, `repeatSummary(0b1111111)=="Every day"`, `repeatSummary(0)=="Once"`, `repeatSummary(0b0000101)=="Mon, Wed"`; `daysText(0b0011111)=="Mon – Fri"`; `tickIndex(5)==0`, `tickIndex(50)==33`, `tickIndex(1)==0`, `tickIndex(15)==7`; `challengeSummary(SquatParams(1), MathParams(MEDIUM,1))=="You'll do 1 squat and solve 1 medium problem to turn this alarm off."`; `nextAlarm(emptyList(), now)==null`, all-disabled → null, picks the earlier of two enabled; MathEntry: `"07"`+Submit vs 7 → CORRECT, empty Submit → NONE, wrong then Digit(3) → `MathEntry("3")`, wrong then Back → `MathEntry()`, sixth digit ignored.
- [ ] `./gw testDebugUnitTest` → new tests FAIL (unresolved references).
- [ ] Implement.
- [ ] `./gw testDebugUnitTest assembleDebug` → pass (update the two `formatCountdown` call sites' wording as needed).
- [ ] Commit: `feat: display helpers, next-alarm lookup and math entry reducer`.

### Task 3: Shared components

**Files:** create `ui/components/Glass.kt`, `Buttons.kt`, `Controls.kt`, `Meters.kt`, `TimeWheel.kt`, `Keypad.kt`; extend `WakeIcon.kt`. Leave the old `Components.kt` in place until Task 8.

**Produces** (sizes, colours, motion from DESIGN_SPEC §2.2, §3, §5 and `tokens.json`):
- `enum class GlassRecipe { HERO, CARD, CONTROL, TILE, CHIP }`; `fun Modifier.glass(recipe: GlassRecipe, shape: Shape): Modifier` — clip, gradient fill at 160° (fills ×1.5 opacity vs tokens since there is no blur, capped so cards stay translucent), 1 dp border, 1 dp top inner highlight drawn as a vertical gradient stroke.
- `fun Modifier.glow(color: Color, radius: Dp, offsetY: Dp = 0.dp, alpha: Float = 1f): Modifier` — soft colour halo behind the element, radial-gradient based (API 26 safe).
- `Hairline(modifier)`, `MonoLabel(text, modifier, color = textSecondary)` (11 sp / 500 / 0.12 em / uppercase), `Chip(text, modifier, dot: Boolean = true, mono: Boolean = false)` with breathing dot, `NumText(text, fontSize, modifier, color, weight = ExtraLight, letterSpacing, lineHeight, textAlign)` in the new file (old one removed in Task 8; name the new one `GlassNum` until then if they clash, then rename).
- `PrimaryButton(label, onClick, modifier, enabled = true, leading: (@Composable () -> Unit)? = null)`; `WhitePillButton(label, onClick, modifier, trailing = null)`; `GlassIconButton(icon: WakeIcon, contentDescription: String, onClick, modifier)`.
- `Toggle(checked, onCheckedChange, contentDescription: String, modifier, enabled = true)` — 64×44 hit area, liquid knob keyframes, glow ring on turn-on.
- `ChallengeIconTile(icon, active: Boolean, size: Dp = 48.dp)`; `Stepper(value, range: IntRange, onChange, label: String /* for a11y */, modifier)` with value bump; `SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)` with sliding pill; `DayPill(letter, selected, onClick, contentDescription)`.
- `TickRuler(reps: Int, modifier)` (uses `tickIndex`), `DotBar(filled: Int, total: Int = 10, modifier)`, `ProgressRing(count: Int, target: Int, modifier, size: Dp = 272.dp, center: @Composable () -> Unit)` — no click handler; rep squeeze/ripple keyed on `count`, bounce/burst when `count >= target`, alive glow loop.
- `TimeWheel(hour: Int, minute: Int, is24h: Boolean, onChange: (hour: Int, minute: Int) -> Unit, modifier)` — hour and minute columns as looping `LazyColumn`s with `rememberSnapFlingBehavior`; rows 58 dp neighbours / 92 dp centre; selection band behind; tap a neighbour to scroll to it; third AM/PM column (two items, non-looping) when `!is24h`; `onChange` fires when a column settles, always in 24-hour terms.
- `Keypad(onKey: (MathKey) -> Unit, modifier)` — 1–9, ⌫ (dim), 0, ✓ (fill + glow); key entrance stagger 200 + i×30 ms.
- `WakeIcon` additions: `Sliders`, `MathOps`, `Lock`, `ArrowRight`, `Bell`. Take path data from the SVGs in the reference files (Bell: draw a simple 24×24 stroke bell).

- [ ] Write the components.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass shared components`.

### Task 4: Alarms list

**Files:** rewrite `ui/list/AlarmListScreen.kt`.

**Consumes:** Task 1–3 APIs; `nextAlarm`, `formatClock`, `formatCountdown`, `repeatSummary`, `daysText`.

- [ ] Rewrite per DESIGN_SPEC §4.1, reference `Main.dc.html`, and spec section "Alarms list": header ("{n} active · next at {clock}" / "No upcoming alarms"), hero card (hidden when `nextAlarm` is null; tiles for enabled challenges, or one "No challenge / Single tap" tile), "ALL ALARMS" + "{on} / {total} on" (bumps on change), `AlarmRow`s, bottom `PrimaryButton("New alarm")` 30 dp above the nav inset over a 150 dp transparent→bg fade; list content padded so the last row clears the button. `is24h` from `DateFormat.is24HourFormat(LocalContext.current)`. 30 s tick as today. Row a11y label for the toggle: "Turn off {label} alarm at {time}" / "Turn on …". Entrance stagger per §5. Carry over the locked-alarm handling present at HEAD.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass alarms list`.
- [ ] **Device checkpoint:** ask the user to pair the Pixel (see Verification). If paired: install, screenshot, compare with `screenshots/01-alarms.png` and `01b`, fix differences. If not yet paired, continue and do all comparisons in Task 9.

### Task 5: New / Edit alarm

**Files:** rewrite `ui/edit/AlarmEditScreen.kt`.

- [ ] Rewrite per DESIGN_SPEC §4.2, reference `Editor.dc.html`, and spec section "New / Edit alarm": top bar (back / "New alarm" or "Edit alarm" / delete), `TimeWheel` hero card with "Rings in {countdown}" chip (from `NextTrigger.compute` on the draft, recomputed on change and each minute), LABEL field (56 dp glass, placeholder "Label"), REPEAT with `repeatSummary` on the right and seven `DayPill`s from `DAY_CHIPS`, two challenge cards with animated expand/collapse, summary via `challengeSummary`, `PrimaryButton("Save alarm")` as the last item of the scroll content. Remove the Material `TimePickerDialog`.
- [ ] Squat stepper: `range = minOf(5, squat.squatCount)..50` so a stored value below 5 is shown and can only go up; math stepper `1..10`.
- [ ] Carry over the locked handling at HEAD (save/delete disabled or hidden exactly as today).
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass alarm editor with inline time wheel`.

### Task 6: Ring screen and squats

**Files:** rewrite `ui/firing/MissionHost.kt`, `SquatMissionContent.kt`; create `ui/firing/RingContent.kt`, `ui/firing/StepsPanel.kt`.

**Produces:** `MissionHost(alarm, snoozesUsed, onAllComplete, onSnooze, onEngaged, onClose: () -> Unit = {})`; `StepsPanel(alarm, squatReps: Int?, squatsDone: Boolean, onContinue: (() -> Unit)?)`; `RingHeader(alarm)` (chip, clock via `formatClock`, date `EEEE, MMMM d`).

- [ ] `MissionHost`: `Screen(OrbLayout.RINGING)` for ring/squats, `OrbLayout.MATH` for math. Phases unchanged (Ring → Mission(i) → Done). `LaunchedEffect(phase)` still calls `onAllComplete()` on Done. Done renders the Alarm-off overlay (Task 7; until then keep a minimal glass "Alarm off" text).
- [ ] `RingContent`: `RingHeader`, a 200 dp glass disc with alive glow and `WakeIcon.Bell`, `StepsPanel` with upcoming rows, `PrimaryButton("Begin")` (or "Stop" when `alarm.missions.isEmpty()`), and a "Snooze {n} min" text button only when `canSnooze`. Same callbacks as today.
- [ ] `SquatMissionContent`: `RingHeader`, `ProgressRing(reps, target)` with "of {target} squats", hint "Hold your phone — the motion sensor is counting", `StepsPanel`. Keep `remember { AccelerometerSquatDetector(context).repCounts() }`. On each rep: `onEngaged()` + light haptic. On reaching target: success haptic, stop collecting (clamp display to target), then if a later mission exists show "Continue to math →" (`onContinue` → `onComplete`), else call `onComplete()` after 900 ms. The disc shrinks on short screens before anything scrolls.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass ring and squat screens`.

### Task 7: Math and Alarm off

**Files:** rewrite `ui/firing/MathMissionContent.kt`; create `ui/firing/AlarmOffOverlay.kt`; modify `ui/firing/MissionHost.kt`, `alarm/AlarmActivity.kt`.

- [ ] `MathMissionContent(alarm, params, onComplete, onEngaged)`: header ("{LABEL} · {clock}" mono, "Problem i of n", difficulty chip), segmented progress (squat segment if `alarm.squat != null`, one per problem; done = fill, current = half white, future = 16% white; captions "SQUATS ✓" / "MATH"), problem card ("SOLVE TO SILENCE", expression 46/300 with × and −, gradient hairline, "= {answer}" 60/200 with blinking caret, "?" at 35% when empty, error colour + "Not quite — try again" when `entry.wrong`), `Keypad` at the bottom. State is a `MathEntry`; every key calls `onEngaged()` then `entry.press(key, problem.answer)`. WRONG → shake (−10, 9, −6, 3, 0 over 450 ms) + error haptic. CORRECT → next problem or `onComplete()`. No animation on digits.
- [ ] `AlarmOffOverlay(recap: String, onDone: () -> Unit)`: dim `rgba(8,8,12,.55)`, 120 dp fill circle with drawn check, "Alarm off" 34/300, recap, `WhitePillButton("Done")`; entrance timings from §5; success haptic. `MissionHost` keeps the last mission screen composed underneath in Done and applies `Modifier.blur(30.dp)` to it on API 31+.
- [ ] `AlarmActivity`: pass `onClose = { finish() }`; change the linger `delay(2200)` to `delay(5000)`. `dismiss()` is otherwise untouched, so the sound still stops the moment the last challenge completes.
- [ ] Delete `hour12`, `periodLabel`, `time12`, `missionSummary` if unused.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass math screen and alarm-off overlay`.

### Task 8: Settings, Permissions, cleanup

**Files:** rewrite `ui/settings/SettingsScreen.kt`, `ui/perms/PermissionsScreen.kt`; delete old `ui/components/Components.kt` contents that are unused; remove the deprecated aliases from `WakeColors`/`WakeFonts`.

- [ ] Settings: glass top bar (back, "Settings"), `MonoLabel("PALETTE")`, three selectable glass cards each showing the palette's three orb colours and fill gradient, name, and a selected state; calls `Graph.settings.setPalette`.
- [ ] Permissions: same checks, copy and intents as HEAD, in glass cards with `PrimaryButton`.
- [ ] Remove dead components and all `@Deprecated` aliases; fix any remaining callers. `grep -rn "accentSoft\|cardSurface\|WakeSwitch\|AccentButton\|fonts.ui" app/src/main` → no matches.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- [ ] Commit: `feat: Liquid Glass settings and permissions; remove old design system`.

### Task 9: Motion audit and device verification

- [ ] Walk DESIGN_SPEC §5 row by row and confirm each animation exists; add any missing. Confirm every one is gated on `LocalReduceMotion`.
- [ ] `./gw testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; commit `feat: motion and reduce-motion pass`.
- [ ] With the Pixel paired: `source ~/.wakeup-env && adb install -r app/build/outputs/apk/debug/app-debug.apk`; for each screen `adb exec-out screencap -p > <scratchpad>/<name>.png`, compare against `alarm-liquid-glass-handoff/screenshots/`, fix spacing/size/colour differences, re-run, commit fixes.
- [ ] On device: set a test alarm one minute ahead; confirm it fires over the lock screen, Begin and Snooze work, reps come only from the sensor, Continue appears, ✓ submits, sound stops on the last answer, Back cannot escape, "Done" closes.
- [ ] Report what was and was not verified on the device.
