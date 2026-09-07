# Deep Yield Testing Milestones

Use this checklist on both supported branches where applicable:

- `26.1.x` — Minecraft 26.1.x, NeoForge 26.1.2.106
- `26.2.x` — Minecraft 26.2.x, NeoForge 26.2.0.79

Record the branch, Minecraft version, NeoForge version, config values, and
result for every manual test.

## Build and startup

- [ ] Inspect Git branches and confirm `26.1.x` and `26.2.x` exist.
- [ ] Build `26.1.x` with `bash ./gradlew build`.
- [ ] Build `26.2.x` with `bash ./gradlew build`.
- [ ] Confirm the generated jars contain the correct versions:
  - [ ] `deepyield-1.0.0.jar` on `26.1.x`
  - [ ] `deepyield-2.0.0.jar` on `26.2.x`
- [ ] Start a client test instance.
- [ ] Start a dedicated server if possible.
- [ ] Confirm Deep Yield loads without client-only errors.
- [ ] Confirm the server config is generated and loaded.

## Core eligibility and loot

- [ ] Mine `minecraft:diamond_ore`, which is not a deepslate ore and is not
      in `#deepyield:bonus_ores`; Deep Yield must do nothing.
- [ ] Mine an eligible deepslate ore with the activation roll configured to
      miss; normal loot must be unchanged.
- [ ] Configure `bonusChance = 1.0`,
      `bonusWeightPlus1 = 1`, and all other weights to `0`.
- [ ] Mine normal loot of `1 diamond`; expect `2 diamonds`.
- [ ] Test normal loot of `6 redstone`; expect `12 redstone`.
- [ ] Test normal loot of `2 diamonds` with `+5 copies`; expect `12 diamonds`.
- [ ] Confirm `+1 copy` means one complete additional loot copy, not one item.
- [ ] Confirm a successful activation never produces `+0`.
- [ ] Confirm XP remains exactly vanilla, regardless of item multiplier.
- [ ] Confirm all final drops appear together without a delayed second wave.
- [ ] Confirm no unnecessary one-item-per-entity explosion occurs.
- [ ] Confirm oversized results split into legal stacks, such as `64 + 16`
      for a final quantity of `80`.
- [ ] Confirm item components/data are preserved and incompatible stacks are
      not merged.

## Configuration

- [ ] Test `bonusChance = 0.0`; no eligible block may activate.
- [ ] Test `bonusChance = 1.0`; every eligible allowed block activates.
- [ ] Configure only `bonusWeightPlus1 = 1`; every activation must be exactly
      `x2` total loot.
- [ ] Configure non-100 weights such as `100, 50, 20, 10, 5`; confirm they
      are treated proportionally and do not need to sum to `100`.
- [ ] Configure all bonus weights as zero; confirm the server does not crash,
      a warning is logged, and the documented default table is used.
- [ ] Confirm gameplay reads the actual server config values.
- [ ] Confirm config reload/restart behavior matches NeoForge's normal
      server-config lifecycle.

## Fortune and Silk Touch

- [ ] With `affectFortune = true`, test a Fortune result of `4 diamonds` and
      `+2 copies`; expect `12 diamonds`.
- [ ] With `affectFortune = false`, test a Fortune result of `4 diamonds`;
      expect `4 diamonds` and unchanged vanilla Fortune behavior.
- [ ] With `affectSilkTouch = true`, test a normal result of `1 deepslate
      diamond ore` and `+3 copies`; expect `4 ore blocks`.
- [ ] With `affectSilkTouch = false`, mine with Silk Touch; expect one normal
      deepslate ore block and no Deep Yield activation.
- [ ] Confirm Deep Yield never manually reimplements Fortune.
- [ ] Confirm Silk Touch is never replaced with ordinary ore drops.
- [ ] If an invalid Fortune/Silk Touch combination is available, confirm
      vanilla loot behavior is preserved and disabled Deep Yield settings
      conservatively skip the bonus.

## Tags, modded ores, and blacklist

- [ ] Confirm vanilla deepslate ores are eligible through
      `c:ores_in_ground/deepslate`.
- [ ] Test a modded ore in `c:ores_in_ground/deepslate`; it must work without
      a compile-time dependency on that mod.
- [ ] Add an otherwise ineligible block to `#deepyield:bonus_ores`; confirm it
      becomes eligible.
- [ ] Confirm blocks are not detected merely by registry-name text such as
      `deepslate`.
- [ ] Add `minecraft:deepslate_diamond_ore` to `oreBlacklist`; confirm it
      never receives Deep Yield.
- [ ] Add a modded ore to `oreBlacklist`; confirm it is skipped.
- [ ] Confirm blacklist entries override both compatibility tags.
- [ ] Add an invalid registry ID; confirm the server remains safe and does not
      crash.
- [ ] Keep a valid ID from an absent optional mod in the blacklist; confirm it
      is handled safely without log spam.

## Vein-miner compatibility

- [ ] Install a compatible server-side vein-mining mod, especially Ore Vein
      Miner by quillphen, without adding it as a required dependency.
- [ ] Set `bonusChance = 1.0`, `bonusWeightPlus1 = 1`, and all other weights
      to `0`.
- [ ] Vein-mine eight eligible deepslate ores; confirm each block receives an
      independent roll and result.
- [ ] Confirm one successful block never multiplies the entire vein.
- [ ] Test vein mining with no enchantment.
- [ ] Test vein mining with Fortune.
- [ ] Test vein mining with Silk Touch.
- [ ] Set `affectFortune = false`; confirm Fortune remains vanilla while
      Deep Yield skips each Fortune block.
- [ ] Set `affectSilkTouch = false`; confirm Silk Touch remains vanilla while
      Deep Yield skips each Silk Touch block.
- [ ] Confirm secondary blocks with reliable player attribution are treated as
      player-originated.
- [ ] Confirm explosions, machines, commands, and unrelated programmatic
      removal do not qualify merely because vein mining is supported.
- [ ] Confirm no block is processed twice through multiple related hooks.
- [ ] Confirm no secondary block is skipped.
- [ ] Confirm no delayed second wave of drops occurs.
- [ ] Confirm client/server drops remain synchronized.
- [ ] Confirm XP is not multiplied.
- [ ] If the vein-miner implementation bypasses the standard block-drops
      pipeline, document that limitation instead of claiming compatibility.

## Natural-worldgen and placed-ore provenance

- [ ] Set `bonusChance = 1.0`, `bonusWeightPlus1 = 1`, all other weights to
      `0`, and `affectSilkTouch = true`.
- [ ] Mine one naturally generated deepslate diamond ore with Silk Touch;
      expect two deepslate diamond ore items.
- [ ] Place both dropped ore blocks.
- [ ] Mine both placed blocks; Deep Yield must not activate and normal
      Minecraft loot must remain unchanged.
- [ ] Place an eligible deepslate ore, save the world, restart the server,
      and mine it; it must still be recognized as placed.
- [ ] Place a modded ore tagged with `c:ores_in_ground/deepslate`; Deep Yield
      must not activate when it is mined.
- [ ] Mine the same modded ore from natural world generation; Deep Yield may
      activate normally.
- [ ] Confirm a naturally generated but blacklisted ore is skipped.
- [ ] Confirm a placed ore with Fortune still receives normal Fortune loot,
      but never Deep Yield.
- [ ] Test a vein containing natural, natural, placed, and natural ores;
      only the natural blocks may roll Deep Yield.
- [ ] Move a tracked placed ore with pistons; confirm its non-natural marker
      moves with it, or record the documented API limitation.
- [ ] Test placement by a fake player or reliable placement mechanism when
      available; confirm the ore is marked non-natural.
- [ ] Confirm direct unsupported block writes are documented and are not
      falsely claimed to be tracked.
- [ ] Install Deep Yield into a world containing ores placed before
      installation; document that those historical placements cannot be
      reconstructed reliably.
- [ ] Confirm markers are checked before the current break is processed and
      removed only after the block has actually changed.
- [ ] Confirm chunk unload/reload preserves placed-ore markers.
- [ ] Confirm server restart/save-load preserves placed-ore markers.
- [ ] Confirm only placed eligible positions are stored, not every natural
      ore position.

## Final verification

- [ ] Repeat the critical tests on `26.1.x`.
- [ ] Repeat the critical tests on `26.2.x`.
- [ ] Confirm both branches remain independently buildable.
- [ ] Confirm no required dependency on Ore Vein Miner or another ore mod was
      introduced.
- [ ] Record any manual test that could not be completed and why.
