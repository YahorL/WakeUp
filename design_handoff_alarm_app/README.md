# Handoff: Squat &amp; Math Alarm App

## Overview
A calm, monochrome **alarm app for Android** whose defining feature is the **dismissal challenge**: when an alarm fires, the user can't silence it until they complete a configurable number of **squats** (counted by the phone's motion sensor) and solve a configurable number of **math problems**. The goal is to force the user to actually wake up — physically and mentally — before the alarm goes quiet.

This handoff covers three areas of the app:
1. **Home** — the alarm list
2. **Settings** — appearance (accent color, dark mode, typeface)
3. **Set / edit alarm** — including the "challenge builder" (squats + math config)
4. **The wake-up flow (Direction A)** — the dismissal experience, screen by screen: ringing → squats → math → done

## About the Design Files
The files in `reference/` are **design references created in HTML/React (JSX via in-browser Babel)** — prototypes that show the intended look, layout, and behavior. **They are not production code to copy directly.** They were built inside a design-prototyping harness (a pan/zoom "design canvas", a fake Android device bezel, and a "tweaks" panel) that is **not** part of the product — ignore `frames/design-canvas.jsx`, `frames/tweaks-panel.jsx`, the `DesignCanvas`/`DCSection`/`DCArtboard` wrappers, and the `AndroidDevice` bezel. They exist only to present the work.

Your task is to **recreate these designs in the target codebase's environment** using its established patterns and libraries. If this will be a native Android app, build with Jetpack Compose (or the team's existing UI stack). If it's React Native / Flutter / etc., use that. If no codebase exists yet, choose the most appropriate framework for a production Android alarm app. The motion-sensor squat counting and real alarm scheduling are **platform features** that must be implemented natively (see *Interactions & Behavior*); the HTML prototype only simulates them with taps.

## Fidelity
**High-fidelity.** Colors, typography, spacing, radii, and interactions are intended as final. Recreate the UI faithfully. All exact values are listed under *Design Tokens*. The one deliberate abstraction: squat reps are triggered by tapping a pad in the prototype — in production this is driven by the accelerometer.

---

## Design Tokens

### Color system
The app is **monochrome + one accent**. Almost everything is a neutral; the accent appears on the primary CTA, active toggles/selections, the alarm time glyph, badges, and the background glow. There is a **light** and **dark** theme (dark is the default — it's 6am).

**Neutrals**
| Token | Light | Dark | Usage |
|---|---|---|---|
| `bg` | `#fafaf9` | `#0e0f10` | screen background |
| `bg2` | `#f1f1ef` | `#161718` | secondary surfaces |
| `card` | `#ffffff` | `#1a1b1c` | cards, inputs, panels |
| `cardSunk` | `#f4f4f2` | `#121314` | recessed fills |
| `text` | `#181a1b` | `#f2f2f0` | primary text |
| `textDim` | `#74787b` | `#8d9194` | secondary text |
| `textMute` | `#a3a7aa` | `#5e6265` | tertiary / placeholders |
| `hair` | `rgba(0,0,0,0.085)` | `rgba(255,255,255,0.10)` | hairline borders, dividers |
| `hairStrong` | `rgba(0,0,0,0.16)` | `rgba(255,255,255,0.18)` | stronger borders |
| App backdrop (outside phone) | `#e9e9e6` | `#0a0b0c` | n/a in product |

**Accent** — user-selectable. Defined in OKLCH. `onAccent` text/icons are always `#ffffff`.
| Name | Value |
|---|---|
| Amber (default-ish) | `oklch(0.70 0.14 55)` |
| Green | `oklch(0.66 0.15 150)` |
| Ocean | `oklch(0.64 0.15 248)` |
| Violet | `oklch(0.62 0.16 300)` |
| **Neon blue** (current pick) | `oklch(0.70 0.22 250)` |

**Accent-derived**
- `accentSoft` = `color-mix(in oklch, <accent> 14%, transparent)` in light, `22%` in dark. Used for soft icon-chip backgrounds, badge fills, the squat press-pad fill.
- Background glow = `color-mix(in oklch, <accent> 14%, transparent)` light / `24%` dark.
- Pattern dots = `color-mix(in oklch, <accent> 30%, transparent)` light / `52%` dark.

**Error / wrong-answer**: `#e5484d`.

### Background pattern
Every screen sits on a two-layer backdrop behind its content (the screen's own background is transparent so this shows through):
1. **Accent glow**: `radial-gradient(130% 95% at 50% -16%, <glow>, transparent 62%)` — a soft pool of accent bleeding down from the top edge.
2. **Dot grid**: `radial-gradient(<dot> 1.1px, transparent 1.3px)` tiled at `background-size: 21px 21px`, offset `-2px -2px`, masked by `radial-gradient(135% 100% at 50% -12%, #000 0%, rgba(0,0,0,0.5) 36%, transparent 72%)` so the dots are densest at the top and **fade to nothing** by ~70% down (the "gradient opacity"). `pointer-events: none`, sits at `z-index: 0`; content sits at `z-index: 1`.

### Typography
Three selectable pairings. **UI** font is for body/labels/buttons; **display** for big headings and the large numerals (`num`).
| Pairing | UI font | Display / numerals | Numeral weight |
|---|---|---|---|
| **Grotesk** (default) | Hanken Grotesk | Space Grotesk | 500 |
| **Serif** | Hanken Grotesk | Instrument Serif | 400 |
| **Mono** | Space Grotesk | JetBrains Mono | 500 |

Large numerals always use `font-variant-numeric: tabular-nums` and tight negative letter-spacing.

**Type scale (observed sizes, px)**: 11, 12, 12.5, 13, 13.5, 14, 14.5, 15, 16, 18, 22, 26, 30, 32, 34 (headings), 42 (alarm time on card), 56–64 (edit time picker), 74 (alarm-ring time), 132 (squat counter). Uppercase kicker labels use `letter-spacing: 1–2px`, `font-weight: 600–700`.

### Spacing, radius, shadow
- **Spacing** is loose and consistent; common gaps: 4, 5, 6, 7, 8, 10, 12, 14, 18, 22 px. Screen horizontal padding ~20–22px.
- **Radius**: pills/toggles = full (`100px`/`50%`); chips & small buttons = 11px; inputs = 14px; cards = 18–22px; CTA buttons = 16–18px; press pad = 20px.
- **Shadow**: almost none. Cards in light mode use `0 1px 2px rgba(0,0,0,0.03)`; dark mode uses no shadow (relies on `hair` borders). Device drop shadow is prototype-only.
- **Hit targets**: icon buttons are 40×40; toggles 50×30; keypad keys 58px tall; CTAs 54–58px tall. Keep ≥44px.

### Iconography
Simple **geometric line icons**, 1.8–2.2px stroke, round caps/joins, 24px grid. Set used: plus, check, clock, chevron (L/R/D), x, settings (concentric circles), repeat, trash, bolt (lightning — represents "math"), sun, arrow-up, arrow-down (represent squat direction), backspace. Use the codebase's icon library equivalents (e.g. Material Symbols, Lucide). No emoji.

---

## Screens / Views

### 1. Home — alarm list
**Purpose**: see all alarms at a glance, toggle them on/off, jump into edit, add new, open settings.

**Layout**: vertical flex, full height.
- **Header** (padding 18/22/12): left = "Alarms" in display font, 34px/weight 500, letter-spacing -0.5; below it a 13.5px `textDim` row — a clock icon (15px, accent) + "Next ring in **7h 04m**" (the duration is `text` weight 600). Right = a 40×40 settings icon button (`textDim`) → opens Settings.
- **List** (flex:1, scrollable, padding 4/16/12, gap 12): one `AlarmCard` per alarm.
- **Footer** (padding 10/16/16): full-width **New alarm** CTA — height 56, radius 18, `accent` bg, `onAccent` text weight 600 16px, centered plus icon + label.

**AlarmCard**: `card` bg, radius 22, `hair` border, padding 16/18. When the alarm is **off**, the whole card content is `opacity: 0.5` (except the toggle).
- Top row (space-between): left is a tap target (→ edit) showing the time — `num` font 42px, weight per pairing, letter-spacing -1, baseline-aligned with a 15px weight-600 `textDim` AM/PM — and below it the label (14px, `text`, weight 500). Right is the on/off **Switch**.
- **Day row** (gap 5, margin-top 14): seven 24px circles labelled S M T W T F S. Active day = filled (`#ececea` light / `#2a2c2d` dark) with `text` color; inactive = transparent with `textMute`.
- **Challenge badges** (gap 7, margin-top 14, wrap): pill badges summarizing the dismissal config — e.g. "↓ 15 squats" and "⚡ Math · Medium ×3". Badges with content use the **solid** style (accentSoft bg, accent text/icon); if an alarm has no challenge, show a neutral "Tap to dismiss" badge.

**Badge component**: inline-flex, gap 5, padding 3/9, radius 100, 12px weight-500, `white-space: nowrap`. Solid = `accentSoft` bg + accent text; neutral = `bg2` + `textDim` + `hair` border.

**Switch component**: 50×30 track, radius full. Off track = `#d9d9d6` light / `#2c2e30` dark. On track = `accent`. Knob = 22px white circle, `0 1px 3px rgba(0,0,0,.3)`, slides with `cubic-bezier(.3,1.3,.5,1)` 0.22s.

**Sample data** (for realistic mock):
```
[
  { h:6, m:30, period:'AM', label:'Weekday wake-up', days:[Mon..Fri], on:true,  squats:15, math:true,  mathLevel:'Medium', mathCount:3 },
  { h:7, m:15, period:'AM', label:'Gym session',     days:[Tue,Thu,Sat], on:true, squats:25, math:false, mathLevel:'Easy',  mathCount:0 },
  { h:9, m:0,  period:'AM', label:'Lazy Sunday',      days:[Sun], on:false, squats:0,  math:true,  mathLevel:'Hard', mathCount:2 },
]
```
(Days are 0=Sun…6=Sat.)

### 2. Settings
**Purpose**: app-wide appearance. Reached from the Home gear icon; back chevron returns to Home.

**Layout**: top bar (back chevron 40×40 + "Settings" in display font 26px/500), then a scrollable body (padding 14/20/20).
- **Accent color** section: uppercase 12px `textMute` label, then a **5-column grid** of circular swatches (gap 12, each `aspect-ratio: 1`). Each swatch is the accent color filled; the **selected** one has a 2px `text` ring (via a 2px border + 3px padding gap) and shows a white check icon centered. Below: a 13px `textDim` caption naming the current accent + "used across alarms, buttons & the wake-up challenge."
- **Appearance** card (`card` bg, `hair` border, radius 18): 
  - **Dark mode** row — accentSoft icon chip (clock icon in dark, sun in light) + title "Dark mode" (15px/600) + subtitle "Gentle on 6am eyes" (12.5px `textDim`) + Switch. Divider below.
  - **Typeface** — title then a 3-up segmented control (Grotesk / Serif / Mono), each button rendered **in its own display font**. Selected = `#ececea`/`#2a2c2d` fill + `text`; unselected = transparent + `textDim` + `hair` border. Height 40, radius 11.
- **About** section: short `textDim` paragraph explaining the motion-sensor + math concept.

Changing any control here updates the **whole app live** (it's global app state, not screen-local).

### 3. Set / edit alarm
**Purpose**: configure one alarm, including the dismissal challenge. Reached by tapping a card (edit) or New alarm (create). Back/Save chevron returns to Home; trash deletes.

**Layout**: top bar (back chevron · centered title "Edit alarm" / "New alarm" · trash icon), scrollable body, pinned Save footer.
- **Time picker**: centered. Hours and minutes each as a column: a chevron-down "increment" button above and below a large numeral (`num` font 64px, letter-spacing -2). A 56px `textMute` colon between them. Minutes step by 5. To the right, a vertical AM/PM pair of 6/12-padded pills (selected = `accent`/`onAccent`).
- **Label**: uppercase section label + a `card` field (radius 14, padding 13/16) showing the label text. (In production make this an editable text input.)
- **Repeat**: section label + a row of seven 38px day circles S–S. Selected = `accent` fill + `onAccent`; unselected = `hair` border + `textDim`. Toggling adds/removes the day.
- **"To turn it off" — challenge builder** (`card`, radius 18, padding 4/18). Two rows divided by a hairline:
  - **Squats** row: accentSoft icon chip (arrow-down) + title "Squats" + subtitle "Counted by motion sensor" + Switch (off sets reps to 0; on restores/defaults to 15). When on, an inset row appears: "Reps required" (13.5 `textDim`) + a **Stepper** (1–50).
  - **Math problems** row: accentSoft chip (bolt) + title "Math problems" + subtitle "Solve to silence" + Switch. When on: a 3-up difficulty segmented control (Easy / Medium / Hard), then "How many" + a **Stepper** (1–10).
  - Titles use `white-space: nowrap` so they never wrap behind the subtitle.
- **Summary line** (only when both squats &amp; math are on): centered 12.5px `textDim` — "You'll do **N squats** and solve **M level problems** to turn this alarm off."
- **Save footer**: hairline top, opaque `bg`, full-width Save CTA (accent, 54px, radius 16).

**Stepper component**: `[ − ]  value  [ + ]` — two 40px circular outline buttons (`hair` border) flanking a 56px-min tabular numeral (26px). Disabled at min/max (`textMute`, opacity 0.5).

### 4. The wake-up flow (Direction A — "minimal counter")
The dismissal experience. It is a single state machine with phases: **ring → squats → math → done**. Calm, sequential, big tabular numerals. A close/repeat icon (top-right) restarts the flow in squats/math. Background pattern shows through (transparent screen bg).

Demo config used in the mock: 10 squats, 2 medium problems, alarm 6:30 AM "Weekday wake-up".

**Phase 1 — Alarm ringing**: centered. A pulsing accent ring around a 64px accentSoft chip with a clock icon (the ring animates `scale(0.9→1.5)` + fade, 2s infinite). Then "ALARM" kicker (accent, letter-spacing 2), the time at 74px (`num`), the label (16px `textDim`). Pinned at the bottom: a primary CTA "**Begin · 10 squats + 2 problems**" (accent, 58px) and a `textDim` "Snooze 5 min" text button.

**Phase 2 — Squats**: header row "SQUATS" kicker (left) + "reps / total" step counter (right, accent). Center: the live rep count at **132px** (`num`), which dips `translateY` as the pad is pressed; "of N squats" beneath. A **segmented progress bar** (one segment per required rep, filled = accent, empty = `hair`). Bottom: the **press pad** — height 92, radius 20, `hairStrong` border (turns accent + accentSoft fill while pressed); shows an arrow-down/up icon + "Press down as you squat" / "Stand up to count", with a sub-line "Motion sensor counts each rep". A fill rises inside the pad with press depth.
> **Production**: replace the press pad with **accelerometer-based rep detection** — detect the down-then-up motion of a squat (the user holds the phone). The pad becomes a status/affordance ("Squat to count — N to go") rather than a tap target. Each detected rep increments the counter; when `reps === total`, advance to math (or done if no math).

**Phase 3 — Math**: header "MATH" kicker + "index / total" step. Center: "{level} · solve to silence" caption, the problem at 56px (`num`), then an answer field (min 130×56, radius 14, `hairStrong` border that turns accent when filling, `#e5484d` + shake animation on wrong). Bottom: a **3×4 numeric keypad** — digits 1–9, then CLR / 0 / backspace. Number keys are `card`/`#1f2122` with `hair` border, radius 16, 58px tall, 26px `num`; CLR and backspace are transparent. Typing the correct answer auto-advances (260ms); a wrong answer of full length flashes red + shakes + clears.

**Phase 4 — Done ("Alarm off")**: centered. A 72px solid-accent circle with a white check, "Alarm off" in display font 32px, an encouraging line ("Nicely done — N squats and M problems before 6:30. Good morning."), and a "Replay flow" outline button (prototype convenience).

**Problem generation** (`reference/app/dismiss-shared.jsx`):
- **Easy**: single-digit add or subtract (result ≥ 0).
- **Medium**: 1-digit × 1-digit multiplication, or two 2-digit addition.
- **Hard**: `a × b − c` (a:11–19, b:3–9, c:2–9).

---

## Interactions & Behavior
- **Navigation**: Home ⇄ Settings, Home ⇄ Edit (back/save returns to Home). The wake-up flow is its own full-screen state machine, launched when an alarm fires.
- **Toggles/selections** animate via the Switch easing (0.22s spring). Difficulty/typeface/AM-PM segmented selections are instant.
- **Pulsing ring** on the ring screen: `@keyframes scale(0.9→1.5)+opacity(0.5→0)`, 2s ease-out infinite.
- **Wrong-answer shake**: `@keyframes` translateX ±7→0 over 0.4s; field text turns `#e5484d`, then clears.
- **Squat counter** dips with press depth (spring back on release). In production, animate to the accelerometer signal instead.
- **Auto-advance**: correct math answer advances after ~240–260ms; final squat advances after ~350ms.
- **Snooze**: prototype is non-functional; in production schedule a +5min re-fire.

### Platform features to implement natively (not in the prototype)
1. **Alarm scheduling** that fires reliably at the set time on the set days, including from background/locked state (Android `AlarmManager` exact alarms + a full-screen intent / foreground service; handle Doze, boot-restore, permissions).
2. **Motion-sensor squat counting** via the accelerometer (rep = controlled down-then-up). Needs a debounce/threshold so steps or jostling don't count, and a visible live count.
3. **Sound/vibration** while ringing; only silenced once the challenge completes.
4. **Editable text input** for the alarm label.

## State Management
**Global app state** (drives every screen live):
- `accent` (one of the OKLCH values), `dark` (bool), `fontPairing` ('Grotesk'|'Serif'|'Mono'). In the prototype these come from a "tweaks" store; in production they're user settings (persist them).
- `alarms[]`: each `{ id, h, m, period, label, days[], on, squats (0=off), math (bool), mathLevel, mathCount }`.

**Edit screen** holds a working copy of one alarm; changes patch the alarm in the list.

**Wake-up flow** local state: `phase`, `reps`, current problem index `qi`, the generated `problem` (`{ text, answer }`), the typed `entry`, and a transient `wrong` flag.

## Assets
No image/font assets to bundle. Fonts are Google Fonts — **Hanken Grotesk**, **Space Grotesk**, **Instrument Serif**, **JetBrains Mono** — use the codebase's font pipeline to load equivalents. Icons are simple geometric line icons; map them to the project's icon set. No logo yet.

## Files
In `reference/` (design references — read for exact values, do not ship):
- `Alarm.html` — entry point; defines the accent list, the `Phone` wrapper that layers the background pattern, the `Brief`, and the canvas composition. **Product-relevant**: the `PatternBg` usage and accent wiring.
- `app/theme.jsx` — **design tokens** (`ACCENT_OPTIONS`, `FONTS`, `makeTheme`), the `PatternBg` background, and shared atoms: `Icon`, `Switch`, `Stepper`, `NumPad`, `Badge`. Start here.
- `app/screens-shared.jsx` — `HomeScreen`, `AlarmCard`, `EditScreen`, `SettingsScreen`, and the `AppShell` navigator + sample alarm data.
- `app/dismiss-a.jsx` — the Direction A wake-up flow state machine (ring → squats → math → done).
- `app/dismiss-shared.jsx` — `genProblem(level)` math generator and the `useSquat` press/rep hook (replace with accelerometer logic in production).
- `frames/*` — **prototype harness only; ignore for implementation.**

Screenshots in `screenshots/`: `app-screens.png` (Home · Settings · Edit) and `wakeup-flow.png` (ringing · squats · math · done), both in dark mode with the neon-blue accent.
