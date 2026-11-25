# Alarm app — Liquid Glass redesign spec

The design is a dark "liquid glass" restyle of the existing alarm app (Alarms list + New alarm editor), plus two new ringing screens (squat counter and math challenge). Visual references: rondesignlab.com cases (Veri, Credit Pros, Orion, Clause OS).

- Screenshots for every screen and key state: `screenshots/` (rendered at 2x, 390 pt wide).
- Exact source of each screen: `reference/*.dc.html` (HTML + inline styles + a small logic class). When this spec and the reference disagree, **the reference wins**.
- Tokens in machine-readable form: `tokens.json`.

All sizes are in **pt / dp / CSS px at 1x**. The design frame is 390 × 844 (iPhone 14/15 size). Layouts must stretch to other widths. Fixed widths only apply where noted.

---

## 1. Look and feel in one paragraph

The background is near-black (`#08080C`). Three large, heavily blurred color glows ("orbs") sit behind the content, so the frosted cards on top pick up color. Cards are translucent white (6–13% opacity) with a backdrop blur, a 1 px hairline border at about 13% white, and a brighter 1 px highlight along the top inner edge, which is what reads as "glass". Big numbers (clock times, counters) are very thin (weight 200) and large. Small section labels are uppercase monospace with wide letter-spacing. A soft film grain sits over the glows. Accent color only appears on "on" states (switches, selected days, primary buttons, progress), as a violet→blue gradient.

---

## 2. Tokens

### 2.1 Color

| Token | Value | Use |
|---|---|---|
| `bg` | `#08080C` | Screen background |
| `text.primary` | `#FFFFFF` | Main text |
| `text.secondary` | `rgba(255,255,255,0.62)` | Sub-labels, section labels, meta |
| `text.tertiary` | `rgba(255,255,255,0.50)` | Ruler numbers, tiny captions |
| `text.muted` | `rgba(255,255,255,0.72)` | Row labels inside cards ("Reps required"), unselected segmented items |
| `text.onLight` | `#0B0B10` | Text on white pills (selected segment, "Back to alarms") |
| `error` | `#FFB4B4` | Wrong-answer text |

**Palettes.** The default is **aurora**. It is worth building `ember` and `lagoon` too as theme options, but aurora is the one to ship first.

| Palette | orb1 | orb2 (main glow / accent) | orb3 | fill gradient (135°) |
|---|---|---|---|---|
| **aurora** (default) | `#7B5CFF` violet | `#4C8DFF` blue | `#FF9F43` amber | `#6D28D9 → #2563EB` |
| ember | `#FF6A2B` | `#FFB020` | `#8A4DFF` | `#C2410C → #B45309` |
| lagoon | `#2EE6C5` | `#3D7BFF` | `#9B5CFF` | `#0F766E → #1D4ED8` |

- **orb colors** are bright and are only used blurred, as glows or glow shadows, or for tiny indicator dots and the ruler's active tick.
- **fill gradient** is darker, so white text on it passes 4.5:1. Use it for every solid accent surface: primary buttons, the "on" switch track, selected day pills, active challenge icon tiles, progress fills, and the ✓ key.
- **No pink anywhere.** The user explicitly rejected it.

### 2.2 Glass recipes

| Recipe | Fill | Border | Blur | Shadow | Radius |
|---|---|---|---|---|---|
| `glass.hero` (big feature cards) | linear 160°: `rgba(255,255,255,.13)` 0% → `.035` 60% → `.06` 100% | 1 px `rgba(255,255,255,.14)` | 28, saturate 160% | inset top highlight `0 1 0 rgba(255,255,255,.20)` + drop `0 30 60 -30 rgba(0,0,0,.7)` | 30–32 |
| `glass.card` (challenge cards, list rows) | linear 160°: `rgba(255,255,255,.11)` → `.03` | 1 px `rgba(255,255,255,.12–.13)` | 22–26, saturate 150% | inset top highlight `.14–.16` | 24–28 |
| `glass.control` (circle icon buttons, input) | linear 160°: `rgba(255,255,255,.14)` → `.04` | 1 px `rgba(255,255,255,.14)` | 20 | inset top highlight `.18` | 999 / 20 for input |
| `glass.tile` (small inner tiles, steppers' tray, segmented tray) | flat `rgba(255,255,255,.06)` | 1 px `rgba(255,255,255,.10)` | none | none | 18 / 999 |
| `glass.chip` (pills like "in 8 h 44 min") | flat `rgba(255,255,255,.08–.10)` | 1 px `rgba(255,255,255,.12–.14)` | 20 (optional) | none | 999 |

**Platform notes.** If true backdrop blur is expensive or unavailable (e.g. Android below 12), fall back to the fill at about 1.5× opacity with no blur. The hairline border and the top highlight carry most of the glass look.

### 2.3 Background layers (every screen, back to front)

1. Solid `bg`.
2. **Orb 1**: circle about 420–460 in diameter, radial gradient `orb1` → transparent at 66%, extra blur 10, opacity `0.85`. It sits partly off-screen at the top-left.
3. **Orb 2**: circle about 360–380 using `orb2`, partly off the right edge at about y 60–150, opacity `0.85`.
4. **Orb 3**: circle about 440–460 using `orb3`, partly off the bottom edge, opacity `0.60`.
5. **Grain**: fractal noise (baseFrequency 0.85, 3 octaves, desaturated), full-screen, opacity 0.22, blend mode *overlay*. On native, use a tiled noise PNG of about 256 px at about 6–8% opacity if overlay blending is not available.
6. Content.

Each screen places its orbs a little differently (see the reference files). The Ringing screen puts orb 2 centered behind the counter ring.

### 2.4 Typography

| Role | Font | Size / weight | Extras |
|---|---|---|---|
| Hero clock (Alarms card) | Outfit | 96 / 200 | letter-spacing −0.05em, line-height 0.95, tabular numerals |
| Ringing clock | Outfit | 104 / 200 | same |
| Editor wheel (selected) | Outfit | 88 / 200 | neighbours 40 / 200 at 32% white |
| Counter in ring | Outfit | 96 / 200 | |
| Math expression / answer | Outfit | 46 / 300, answer 60 / 200 | |
| List row time | Outfit | 38 / 200 | −0.04em |
| Screen title "Alarms" | Outfit | 36 / 400 | −0.03em |
| Card title ("Squats") | Outfit | 18 / 500 | |
| Body / row label | Outfit | 15 / 500 or 400 | |
| Secondary | Outfit | 12–14 / 400 | `text.secondary` |
| Section label ("TO TURN IT OFF") | IBM Plex Mono | 11 / 500 | UPPERCASE, letter-spacing 0.12em, `text.secondary` |
| Stepper value | Outfit | 30 / 300 | tabular numerals |
| Keypad digits | Outfit | 30 / 300 | |

Both fonts are free on Google Fonts and need to be bundled with the app. Use tabular (monospaced) numerals for every number that changes.

### 2.5 Spacing, radius, sizes

- Screen side padding 20. Top content starts about 52 below the safe-area top. Use the **real** system status bar; none is drawn in the mockup.
- Vertical rhythm: 10 / 12 / 14 / 16 / 20 / 24 / 28.
- Radii: 999 (pills, circles), 30–32 (hero cards), 24–28 (cards, rows), 20–22 (input, keys), 16 (icon tiles), 12 (small tiles inside the hero).
- Minimum touch target is 44 × 44. The switch's visible track is 54 × 32, but its hit area is 64 × 44.
- Primary button: height 60, full width (20 inset), radius 999, `fill` gradient, 1 px `rgba(255,255,255,.22)` border, inset top highlight `.35`, glow shadow `0 20 44 -14 orb2`. Label 17 / 500 white.

---

## 3. Components

| Component | Spec |
|---|---|
| **GlassCard** | Uses `glass.card` or `glass.hero` from 2.2. Padding 18–22. |
| **IconButton** (back, delete, settings) | 44 circle with `glass.control`, a 20 stroke icon (1.6–1.8 stroke) in white. |
| **Toggle** | Track 54 × 32 with radius 999, 3 padding and a 1 px `rgba(255,255,255,.16)` border. **On:** track uses the `fill` gradient with glow shadow `0 0 18 -4 orb2` and a top highlight. **Off:** track is `rgba(255,255,255,.10)`. Knob is a 24 white circle with shadow `0 2 8 rgba(0,0,0,.35)` that moves 22 to the right when on. Animated (see §5). |
| **ChallengeIconTile** | 48 × 48, radius 16. On: `fill` with glow `0 10 24 -10 orb2`. Off: `rgba(255,255,255,.10)`. White 22 icon. |
| **Stepper** | A tray (`glass.tile`, radius 999, 4 padding) holding a − button, the value, and a + button. Buttons are 44 circles with `rgba(255,255,255,.08)` fill and a `.14` border. The value is 30 / 300, at least 56 wide, centered. |
| **TickRuler** (squat reps) | 34 ticks evenly spaced across the full width, 2 wide with rounded ends, aligned to the bottom of a 30-tall row. Every 11th tick and the last one are 20 tall; the others are 12. Ticks before the current value are `rgba(255,255,255,.85)`, ticks after are `.18`, and the current tick is 3 wide, 30 tall, `orb2` colored with a glow. Index = `round((reps−5)/45 × 33)`. Labels 5 / 20 / 35 / 50 sit below in 11 pt mono. |
| **DotBar** (math count) | 10 bars in a grid with 6 gap, 6 tall, radius 999. The first *n* use `fill`; the rest use `rgba(255,255,255,.14)`. |
| **SegmentedControl** (Easy / Medium / Hard) | A `glass.tile` tray with radius 999, 4 padding and 4 gap; each item is 44 tall. A **sliding** white pill (`rgba(255,255,255,.94)`, shadow `0 6 18 -8 rgba(0,0,0,.6)`) sits behind the selected item, whose text turns `#0B0B10`. Unselected text is `text.muted`. |
| **DayPill** | 46 circle with 15 / 500 text. Selected: `fill` gradient, `.25` border, and glow `0 8 20 -8 orb2`. Unselected: `rgba(255,255,255,.06)` fill, `.14` border, `text.muted` letter. |
| **TimeWheel** | Two columns (hours and minutes) about 130 wide each, separated by a ":" at 72 / 200 and 70% white. Each column shows the previous value, the current value and the next value (58-tall rows for neighbours, 92 tall for the current). A glass "selection band" 92 tall sits behind the middle row. Tapping a neighbour steps the value, but implement it as a real scroll wheel or picker with snapping if the platform has one. Under it is the chip "Rings in X h Y min" with a glowing dot. |
| **AlarmRow** | `glass.card`, radius 24, padding 12 / 8 / 12 / 18. It contains the time (38 / 200), the label (15 / 500), and the days text plus 18 × 18 challenge mini-badges (radius 6, `rgba(255,255,255,.12)`) using a down-arrow for squats and a plus for math. The toggle sits on the right. When off, the time and label block drops to 45% opacity. |
| **ProgressRing** (squats) | A 272 glass disc (radial highlight at the top, `.16` border, blur 30). Inside it are an SVG track ring (r 122, stroke 10, `rgba(255,255,255,.10)`), a progress ring (same radius, round cap, `orb1 → orb2` gradient, drop-shadow glow, starting at 12 o'clock), and an inner dotted ring (r 104, 1.5 stroke, dash 2 / 7, `.22`). The center shows the count (96 / 200) over "of 15 squats". |
| **Keypad** | A 3-column grid with 10 gap; keys are 66 tall with radius 22 and `glass.card` styling. The layout is 1–9, then ⌫ (dimmer: `.04` fill and `.08` border), 0, and ✓ (`fill` gradient with glow). |

---

## 4. Screens

### 4.1 Alarms (`screenshots/01-alarms.png`, `01b-alarms-empty.png`)

1. **Header row:** "Alarms" (36 / 400) with the sub-line "{n} active · next at HH:MM" below it, and the settings IconButton on the right. When no alarms are on, the sub-line reads "No upcoming alarms", matching the existing copy.
2. **Next-alarm hero card** (`glass.hero`, radius 30, padding 20, gap 14):
   - Top row: the mono label "NEXT ALARM" over "{label} · {repeat summary}", and on the right a chip with a glowing `orb2` dot and "in X h Y min".
   - The hero clock.
   - A two-column grid of small `glass.tile` tiles (radius 18, padding 12), each with a 36 icon tile, a title and a sub-line. Squats tile: "{n} squats" / "Motion sensor", with a `fill` icon. Math tile: "{n} problems" / "{difficulty}", with a neutral icon. Show only the challenges that are enabled.
   - Hide the card when there is no upcoming alarm.
3. **Section header:** "ALL ALARMS" on the left and "{on} / {total} on" on the right, both in mono.
4. **List of AlarmRows** with 10 gap. Tapping a row opens the editor; tapping the toggle enables or disables the alarm.
5. **"New alarm" primary button** fixed 30 above the bottom safe area, with a + icon. A gradient fade from transparent to `bg` (150 tall) sits behind it so the list scrolls under it.

### 4.2 New alarm / Edit alarm (`02-new-alarm-full.png`, `02b-new-alarm-math-off.png`)

This is a scrolling screen; the screenshot shows the full scroll height.

1. **Top bar:** back IconButton, the title "New alarm" (18 / 500, centered), and a delete IconButton.
2. **TimeWheel card** (`glass.hero`, radius 32) with the "Rings in…" chip.
3. **LABEL:** a mono label over a 56-tall glass text field (radius 20, 17 pt text, default "Wake up").
4. **REPEAT:** a mono label with the summary on the right ("Weekdays" / "Weekends" / "Every day" / "Once" / "Mon, Wed"), over a row of 7 DayPills (S M T W T F S).
5. **TO TURN IT OFF:** a mono label over two challenge cards:
   - **Squats card:** a header row with ChallengeIconTile, "Squats" / "Counted by motion sensor", and a Toggle. When on, an expandable section appears below a 1 px `rgba(255,255,255,.10)` divider. It holds the row "Reps required" with a Stepper (range 5–50) and the TickRuler.
   - **Math card:** a header with "Math problems" / "Solve to silence" and a Toggle. The expandable section holds the SegmentedControl (Easy / Medium / Hard), the row "How many" with a Stepper (range 1–10), and the DotBar.
6. **Summary text** (14, `text.secondary`, centered), for example "You'll do 15 squats and solve 3 medium problems to turn this alarm off." With no challenges it reads "No challenge — this alarm stops with a single tap."
7. **"Save alarm" primary button** at the end of the content (pushed to the bottom).

### 4.3 Ringing — squats (`03-ringing-squats.png`, `03b-ringing-squats-done.png`) — new screen

- A top chip (mono, uppercase) with a glowing dot and the alarm label, then the clock (104 / 200), then the full date ("Wednesday, November 25", 15, secondary).
- The ProgressRing, centered, which counts reps from the **existing motion-sensor logic**. In the mockup, tapping the ring simulates a rep; that is a prototype affordance only and **must not ship**.
- The hint "Hold your phone — the motion sensor is counting".
- A bottom glass panel (radius 28, padding 8) listing the steps:
  - Active step: "Squats" with "{n} / {target}" and a 4-tall progress bar filled with `fill`.
  - Locked step: a lock icon with "Math problems" / "{count} {difficulty} · unlocks after squats".
  - When squats are complete, the locked row is replaced by a white button "Continue to math →". Alternatively, auto-advance after a short delay; see "Decisions to confirm" in README.md.
- If only one challenge is enabled, show only that step. If none are enabled, use a plain "Stop" button.

### 4.4 Ringing — math (`04-ringing-math.png`, `04b-…-wrong.png`, `04c-alarm-off.png`) — new screen

- Header: the mono line "{label} · {time}", "Problem {i} of {n}" (22 / 400), and a difficulty chip on the right.
- A segmented progress bar: one segment for squats (if enabled) plus one per problem. Done segments use `fill`, the current segment is half white, and future ones are `rgba(255,255,255,.16)`. Tiny mono captions sit below it.
- A problem card (`glass.hero`): "SOLVE TO SILENCE", the expression (46 / 300), a gradient hairline, then "= {answer}" with a blinking `orb2` caret. The answer shows "?" at 35% white when empty. Wrong answers show the answer in `error` color with "Not quite — try again".
- The Keypad, anchored to the bottom.
- On the last correct answer, an "Alarm off" overlay appears: the screen blurs (30) and dims (`rgba(8,8,12,.55)`), a 120 `fill` circle shows a check, then "Alarm off" (34 / 300), a recap line, and a white "Back to alarms" pill.

---

## 5. Motion

Default easing curves:

- **ease-out-expo-ish** — `cubic-bezier(.2,.8,.2,1)`, for entrances and panels.
- **spring** — `cubic-bezier(.3,1.2,.5,1)` or `(.34,1.35,.64,1)`, for knobs, pops and slides. On native, a spring with damping ratio of about 0.6–0.7 and a response of about 0.45–0.55 s.

All motion must be disabled or reduced when the OS "Reduce Motion" setting is on.

| Where | Trigger | Animation |
|---|---|---|
| **Every screen** | Screen appears | Staggered "rise in": opacity 0 → 1, translateY 16 → 0, scale .98 → 1, over 0.6–0.7 s ease-out-expo. Delays run top to bottom: header 0 ms, hero 80–90 ms, sections +60–80 ms each, list rows 240 + i×80 ms, primary button about 420–450 ms, keypad keys 200 + i×30 ms. |
| Every button | Press | Scale to 0.96 while pressed, 150 ms. |
| Status dots (chips) | Always | "Breathe": scale 1 → 1.6 → 1 and opacity 1 → .55 → 1 over 2.4 s, ease-in-out, infinite. |
| **Toggle** (list rows + challenge cards) | Tap | Liquid knob, 0.55 s spring. Turning on, the knob goes x 0 → 9 (scaleX 1.42, stretched) → 24 (scaleX .92, squash and overshoot) → 22 (scaleX 1). Turning off mirrors this. The track color crossfades over 0.35 s. **When turning on**, a glow ring expands from the track: box-shadow spread 0 → 14 in `orb2`, opacity .75 → 0, 0.7 s ease-out. |
| Challenge icon tile | Toggle | On: pop (scale .8 + rotate −8° → 1.12 + 3° → 1), 0.6 s spring. Off: dip (1 → .86 → 1), 0.35 s. |
| Challenge settings panel | Toggle | Height expands or collapses 0.45 s ease-out-expo. On open, the content fades in and moves from translateY −12 / scale .97 to rest (opacity delayed 100 ms). On close it fades out over 0.2 s. |
| TickRuler | Panel opens | Ticks grow from the bottom (scaleY 0 → 1) with a stagger of 120 + i×14 ms, 0.5 s spring. When reps change, the tick height changes with a 0.25 s spring. |
| DotBar | Panel opens | Bars grow from the left (scaleX 0 → 1) with a stagger of 140 + i×35 ms. |
| Stepper value | + / − | Bump: translateY 8 / scale .9 / opacity .2 → rest, 0.35 s spring. |
| SegmentedControl | Select | The white pill slides to the new item, 0.5 s spring `(.34,1.35,.64,1)`. Text color crossfades over 0.3 s. |
| DayPill | Tap | On: pop (.78 → 1.14 → 1). Off: dip (1 → .84 → 1). 0.5 s spring. The "Weekdays" label bumps. |
| TimeWheel | Value change | The new number rolls in from the direction of travel (translateY ±34 → 0, blur 4 → 0, opacity 0 → 1), 0.4 s ease-out-expo. The "Rings in" chip bumps. |
| List "x / 3 on" | Toggle | Bump. |
| ProgressRing | Idle | "Alive" glow: an outer glow of 0 → 60 blur in `orb2` and back, 2.6 s, infinite. |
| ProgressRing | Each rep | The disc squeezes (1 → .95 → 1, 0.35 s spring), a ripple ring expands (spread 0 → 34, opacity .7 → 0, 0.7 s), the count bumps, and the progress arc animates its length (0.4 s ease). Haptic: light impact. |
| ProgressRing | Target reached | The disc bounces (1 → 1.08 → 1) and a big burst ring expands (spread 0 → 80, 0.9 s). "Continue to math" rises in with a 250 ms delay. Haptic: success. |
| Math answer | Typing | **No animation on typed digits.** The user explicitly rejected a bounce here. The caret blinks (1.05 s, steps). |
| Math answer | Wrong | Horizontal shake (−10, 9, −6, 3, 0) over 0.45 s, then "Not quite" rises in. Haptic: error. |
| Alarm-off overlay | Solved | The overlay fades in over 0.5 s. The check circle pops (scale .4 → 1.12 → 1, 0.7 s spring, 100 ms delay), the check stroke draws itself (0.5 s, 550 ms delay), then the title, the line and the button rise in at 450 / 550 / 650 ms. The last progress segment fills from the left (0.6 s). Haptic: success. |

---

## 6. Accessibility

- Every icon-only button has a label: "Back to alarms", "Delete alarm", "Settings", "Turn off Wake up alarm at 06:30", "Earlier hour", and so on.
- Toggles and day pills expose an on/off (pressed) state. The segmented control exposes which item is selected.
- Text contrast is 4.5:1 or better everywhere (the `fill` gradient was darkened for that reason).
- Reduce Motion: drop the entrances, springs, ripples, breathe and alive effects; keep instant state changes.
- Dynamic Type: the big thin numerals can stay fixed. Body text should scale.
