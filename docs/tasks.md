# BogatirOrbitalStrike — Tasks & Progress

## Status Legend
- ✅ Done
- 🔄 In Progress
- ⬜ Not Started

---

## Implementation Tasks

| # | Task | Status | File(s) |
|---|------|--------|---------|
| 1 | Plugin skeleton (main class, plugin.yml) | ✅ | `BogatirOrbitalStrike.java`, `plugin.yml` |
| 2 | Custom "Orbital Strike" block (BEACON + PDC) | ✅ | `block/OrbitalStrikeBlock.java` |
| 3 | Block place listener (marks PDC on beacon) | ✅ | `listeners/BlockPlaceListener.java` |
| 4 | Block interact listener (right-click → GUI) | ✅ | `listeners/BlockInteractListener.java` |
| 5 | Inventory GUI (54-slot, all parameters) | ✅ | `gui/OrbitalStrikeGUI.java` |
| 6 | GUI click listener | ✅ | `gui/GUIListener.java` |
| 7 | Chat-based value input system | ✅ | `gui/ChatInputListener.java` |
| 8 | Strike data model | ✅ | `strike/StrikeData.java` |
| 9 | Strike manager (schedules tasks) | ✅ | `strike/StrikeManager.java` |
| 10 | Strike task (full animation sequence) | ✅ | `strike/StrikeTask.java` |
| 11 | Laser renderer (hexagon, center, beam) | ✅ | `laser/LaserRenderer.java` |
| 12 | Fear effect (DARKNESS, NAUSEA, camera shake, title flash) | ✅ | `fear/FearEffectManager.java` |
| 13 | `/orbitalstrike give` command | ✅ | `commands/OrbitalStrikeCommand.java` |
| 14 | `compile.cmd` build script | ✅ | `compile.cmd` |

---

## Feature Checklist

### Core Strike System
- [x] Strike power 1–10 controls explosion radius + fire radius
- [x] Multiple strikes in sequence (count parameter)
- [x] Configurable delay before strike (in ticks)
- [x] Target XYZ coordinates

### Visual Animation Sequence
- [x] Phase 1 — 6 red hexagon dots at sky height (60 ticks)
- [x] Phase 2 — White center point appears (25 ticks)
- [x] Phase 3 — Laser beam descends progressively (20 ticks)
- [x] Phase 4 — Impact explosion + fire spread
- [x] Sound effects at each phase
- [x] Pause between multiple strikes (30 ticks)

### Fear Effect
- [x] Configurable power (1–10) and duration (ticks)
- [x] DARKNESS potion (screen flashing black vignette)
- [x] NAUSEA potion (disorientation)
- [x] Camera shake (yaw/pitch jitter via scheduled teleports)
- [x] Black screen title flash ("⚠ ORBITAL STRIKE ⚠")
- [x] Applied to all players within 100 blocks of impact

### Orbital Strike Block
- [x] Based on BEACON (TileEntity — supports PDC)
- [x] Custom name + lore via ItemMeta PDC
- [x] Right-click opens GUI
- [x] GUI prevents vanilla beacon menu
- [x] Give command: `/orbitalstrike give [player]`

### GUI
- [x] Target X, Y, Z inputs
- [x] Delay (ticks)
- [x] Strike Power (1–10)
- [x] Strike Count
- [x] Fear Power (0–10)
- [x] Fear Duration (ticks)
- [x] ACTIVATE button
- [x] Chat-based numeric input (type value in chat after clicking slot)

---

## Known Limitations / Future Work

- Camera shake uses `player.teleport()` instead of ProtocolLib packets — good enough without extra dependencies
- No persistent storage: if server restarts, active strikes are lost
- Block breaking is handled by `world.createExplosion()` — highly optimized by Paper
- Particles visible to all players within default Paper view distance (~128 blocks)

---

## Build Notes

### Paper Paperclip bootstrapper (important!)
Paper 26.x downloaded from papermc.io is a **Paperclip** jar — a bootstrapper that patches the Mojang server at first run.  
The actual `org.bukkit.*` classes live in `cache/patched_26.1.2.jar`, NOT in the downloaded `paper.jar`.

`compile.cmd` handles this automatically:
1. Looks for `cache/patched_*.jar`
2. If not found → runs `java -jar libs/paper.jar --patchOnly` to generate it
3. Uses the patched jar for `javac -cp`

If auto-patch fails: run `java -jar libs/paper.jar --nogui` once (starts server, stop it with `stop`), then re-run `compile.cmd`.
