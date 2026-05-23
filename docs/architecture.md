# BogatirOrbitalStrike — Architecture

## Overview

Minecraft Paper plugin (1.21.x / Java 21) that adds a configurable orbital laser strike system with cinematic visual effects and a Fear status effect.

---

## Package Structure

```
me.bogatir.orbitalstrike/
│
├── BogatirOrbitalStrike.java          ← Main plugin (JavaPlugin)
│
├── commands/
│   └── OrbitalStrikeCommand.java      ← /orbitalstrike give [player]
│
├── block/
│   └── OrbitalStrikeBlock.java        ← Custom BEACON block via PDC
│
├── gui/
│   ├── OrbitalStrikeGUI.java          ← 54-slot inventory GUI builder
│   ├── GUIListener.java               ← InventoryClickEvent handler
│   └── ChatInputListener.java         ← Async chat capture for value input
│
├── strike/
│   ├── StrikeData.java                ← Config data (coords, power, count, etc.)
│   ├── StrikeManager.java             ← Schedules & tracks StrikeTask instances
│   └── StrikeTask.java                ← BukkitRunnable: full animation sequence
│
├── laser/
│   └── LaserRenderer.java             ← Particle effect helpers
│
├── fear/
│   └── FearEffectManager.java         ← Potion + shake + title effects
│
└── listeners/
    ├── BlockInteractListener.java     ← Right-click beacon → open GUI
    └── BlockPlaceListener.java        ← Place custom item → mark PDC on beacon
```

---

## Data Flow

```
Player places Orbital Strike block
    └─→ BlockPlaceListener marks BEACON TileState PDC

Player right-clicks the block
    └─→ BlockInteractListener cancels vanilla event
        └─→ opens OrbitalStrikeGUI for that player

Player clicks a parameter slot in GUI
    └─→ GUIListener cancels click
        └─→ closes inventory
        └─→ registers ChatInputListener callback for that player

Player types value in chat
    └─→ ChatInputListener (AsyncPlayerChatEvent, priority LOWEST)
        └─→ cancels message, stores value in StrikeData
        └─→ re-opens GUI (sync task)

Player clicks ACTIVATE
    └─→ GUIListener.onActivate()
        └─→ StrikeManager.scheduleStrike(player, data.clone())
            └─→ new StrikeTask(plugin, data, player)
            └─→ task.runTaskTimer(plugin, data.getDelayTicks(), 1L)

StrikeTask.run() — called every tick:
    tick % CYCLE:
        [0  … 59]  → Phase 1: spawn hexagon particles (red DUST, every 3 ticks)
        [60 … 84]  → Phase 2: hexagon + white center (END_ROD)
        [85 … 104] → Phase 3: laser beam descends (END_ROD, progress 0→1)
        [105]      → Phase 4: IMPACT
                       └─→ world.createExplosion(power * 2)
                       └─→ spread FIRE in radius
                       └─→ play THUNDER + EXPLODE sounds
                       └─→ FearEffectManager.applyFear(...)
        [106 … 134] → Pause between strikes

    After data.getCount() * CYCLE ticks → cancel()
```

---

## Custom Block Identity

| Aspect | Value |
|--------|-------|
| Base material | `BEACON` (has TileEntity → PersistentDataHolder) |
| PDC namespace | `bogatirorbitalstrike` (plugin name lowercased) |
| PDC key | `orbital_strike_block` |
| Item PDC key | same key on ItemMeta (for detection on place) |

---

## Strike Animation Timing

| Phase | Duration | Ticks |
|-------|----------|-------|
| Hexagon (6 red dots) | 3.0 s | 0–59 |
| White center | 1.25 s | 60–84 |
| Laser descent | 1.0 s | 85–104 |
| Impact | instant | 105 |
| Pause (between strikes) | 1.5 s | 106–134 |
| **Total per strike** | **~6.75 s** | **135 ticks** |

---

## GUI Layout (54 slots)

```
[ 0][ 1][ 2][ 3][ 4][ 5][ 6][ 7][ 8]   ← glass border
[ 9][10][11][12][13][14][15][16][17]
     X    Y    Z  DLY  PWR  CNT
[18][19][20][21][22][23][24][25][26]   ← glass border
[27][28][29][30][31][32][33][34][35]
              FPOWER FDUR
[36][37][38][39][40][41][42][43][44]
                  ACT
[45][46][47][48][49][50][51][52][53]   ← glass border
```

Slot mapping: X=10, Y=12, Z=14, Delay=20, Power=22, Count=24, FearPower=31, FearDuration=33, Activate=40

---

## Fear Effect Components

| Component | Implementation |
|-----------|---------------|
| Screen flash black | `PotionEffectType.DARKNESS` |
| Disorientation | `PotionEffectType.NAUSEA` |
| Camera shake | Scheduled `player.teleport()` with yaw/pitch jitter every N ticks |
| Black title flash | `player.sendTitle("§0§k|||", "§4⚠ ORBITAL STRIKE ⚠", ...)` |
| Shake interval | Decreases with higher fear power |
| Affected radius | 100 blocks from impact |

---

## Build

| Item | Value |
|------|-------|
| Java target | 21 |
| Server API | Paper 26.1.2 |
| API jar location | `libs/paper.jar` |
| Build script | `compile.cmd` |
| Output | `dist/BogatirOrbitalStrike.jar` |

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `orbitalstrike.use` | op | Right-click and use the block |
| `orbitalstrike.give` | op | `/orbitalstrike give` command |
