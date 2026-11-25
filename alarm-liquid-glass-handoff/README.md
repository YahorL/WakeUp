# Alarm app — Liquid Glass redesign: handoff for Claude Code

## How to use

1. Unzip this folder into the root of your app repo (or anywhere Claude Code can read it, e.g. `design/liquid-glass/`).
2. Open Claude Code in the repo and paste the prompt below. Adjust the folder path if you put it somewhere else.

## Prompt to paste into Claude Code

```
I'm redesigning my alarm app's UI. The full design handoff is in design/liquid-glass/:
- DESIGN_SPEC.md — tokens, components, screens, motion (read this fully first)
- tokens.json — colors, glass recipes, type, radii, motion curves
- screenshots/ — rendered target for every screen and key state (look at all of them)
- reference/*.dc.html — the exact source of the mockup (HTML/CSS + a small logic class); use it to check any measurement the spec doesn't cover. These files need a design-tool runtime and won't open standalone — read them as source.

What to do:
1. Explore the codebase first: stack, navigation, where the Alarms list and New/Edit alarm screens live, how squats (motion sensor) and math challenges are implemented, and what runs when an alarm rings.
2. Propose a short plan before editing: which files change, which new shared components you'll create (GlassCard, Toggle, Stepper, SegmentedControl, DayPill, TimeWheel, TickRuler, ProgressRing, Keypad), and which libraries you need for blur, gradients, SVG and animation on this stack. Wait for my OK.
3. Implement in this order: theme tokens → shared components → Alarms list → New/Edit alarm → Ringing (squats) → Ringing (math) → motion pass → reduce-motion pass.
4. Restyle only. Keep all existing behaviour and data: alarm scheduling, the motion-sensor squat counting, math problem generation, persistence. The alarms in the screenshots (Weekend 08:30, Gym 17:45) are mock data — render the user's real alarms. The "tap the ring to count a rep" in the mockup is a prototype affordance — do NOT ship it; reps come from the motion sensor.
5. If the ringing flow screens don't exist yet in this app, build them from the spec and wire them to the existing challenge logic; ask me if something is unclear instead of inventing behaviour.
6. Bundle the fonts Outfit (200–600) and IBM Plex Mono (400, 500). Use the real OS status bar.
7. After each screen, run the app on a simulator/emulator, take a screenshot and compare it side by side with the matching file in screenshots/. Fix differences in spacing, sizes, colors and blur before moving on.
```

## Decisions to confirm with Claude Code (or tell it your preference up front)

- **After squats are done:** the mockup shows a "Continue to math" button. Auto-advancing after about 1 s is an alternative.
- **Palettes:** ship only **aurora** (violet/blue), or also offer ember (orange, close to the current brand) and lagoon (teal) as a theme setting.
- **Time picker:** the mockup's wheel is tap-to-step. A native scroll wheel or picker with the same styling is recommended.
- **Clock format:** the design uses 24-hour time, like the current app. Follow the device setting if you support 12-hour.

## Contents

| Path | What |
|---|---|
| `DESIGN_SPEC.md` | The full spec |
| `tokens.json` | Design tokens |
| `screenshots/01-alarms.png` | Alarms list |
| `screenshots/01b-alarms-empty.png` | All alarms off (hero still shown in the mockup; hide it when there is no upcoming alarm) |
| `screenshots/02-new-alarm-full.png` | New alarm, full scroll height |
| `screenshots/02b-new-alarm-math-off.png` | Math challenge collapsed |
| `screenshots/03-ringing-squats.png` | Ringing, squat counter in progress |
| `screenshots/03b-ringing-squats-done.png` | Squats complete, unlock math |
| `screenshots/04-ringing-math.png` | Math problem with keypad |
| `screenshots/04b-ringing-math-wrong.png` | Wrong answer state |
| `screenshots/04c-alarm-off.png` | Success overlay |
| `reference/*.dc.html` | Exact mockup source per screen |

Screenshots are static: they don't show motion. All animations are described in DESIGN_SPEC.md §5, and the live, clickable prototype is the Claude design canvas this package was exported from.
