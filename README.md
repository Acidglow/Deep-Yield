# Deep Yield

Deep Yield gives eligible deepslate ores a configurable chance to provide
multiple copies of their normal loot.

## Features

- Vanilla deepslate ores are supported automatically.
- Modded ores tagged with `c:ores_in_ground/deepslate` are supported without
  mod-specific integration.
- `#deepyield:bonus_ores` is an explicit opt-in compatibility tag for datapacks
  and modpacks.
- Fortune and Silk Touch can each be enabled or disabled independently.
- `oreBlacklist` entries override every eligibility tag.
- XP and other block-break side effects are never multiplied.
- All decisions and final drops are calculated server-side in the same
  block-drops event.
- Each actual harvested ore block receives its own independent Deep Yield
  roll, including blocks harvested by compatible player-initiated vein mining.
- No Ore Vein Miner dependency is required; compatibility uses the normal
  NeoForge block-drops pipeline.

## Configuration

The server config is generated as `config/deepyield-server.toml`. The
important settings are:

```toml
[deep_yield]
bonusChance = 0.20
bonusWeightPlus1 = 50
bonusWeightPlus2 = 25
bonusWeightPlus3 = 15
bonusWeightPlus4 = 7
bonusWeightPlus5 = 3
affectFortune = true
affectSilkTouch = true
oreBlacklist = []
```

Deep Yield uses two random stages. First, `bonusChance` decides whether a
break activates. On a hit, the configured weights select +1 through +5
additional copies of the complete normal loot result. `+1 copy` means x2
total loot, not one additional individual item. The configured weights do
not need to sum to 100. If all weights are zero, the default table is used
and a warning is logged.

Blacklisted IDs can include vanilla and modded blocks, for example:

```toml
oreBlacklist = [
  "minecraft:deepslate_diamond_ore",
  "minecraft:deepslate_emerald_ore",
  "some_mod:deepslate_uranium_ore"
]
```

Invalid or missing registry IDs are ignored safely. Config changes follow
NeoForge's normal server-config lifecycle; restart/reload behavior is
determined by the current NeoForge configuration system.

## Version branches

The intended version model is one standalone project per Git branch:

- Minecraft 26.1.x: `26.1.x`
- Minecraft 26.2.x: `26.2.x`

The `26.1.x` branch targets Minecraft 26.1.x with NeoForge 26.1.2.106.

## Vein-miner compatibility

Deep Yield observes `BlockDropsEvent`, so a vein-mining mod that harvests
secondary blocks through Minecraft's normal server-side block-breaking and
loot pipeline receives one Deep Yield roll per harvested block. A successful
roll affects only that block's final loot; it never multiplies the entire
vein. Fortune, Silk Touch, blacklist checks, and the custom compatibility
tag are evaluated independently for every event.

The event hook is the only Deep Yield drop integration, preventing separate
break and loot handlers from processing the same event twice. Blocks without
reliable player attribution are left unchanged. No delayed bonus spawning or
Ore Vein Miner-specific dependency is used.
