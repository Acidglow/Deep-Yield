# Vein_miner

==================================================
VEIN MINER COMPATIBILITY
==================================================

Deep Yield must be designed to coexist with server-side vein-mining mods,
especially mods that automatically harvest multiple connected ores from
one player-initiated block break.

An important compatibility target is:

Ore Vein Miner by quillphen

Do NOT add a required compile-time dependency on Ore Vein Miner.

Deep Yield must remain fully functional when Ore Vein Miner is absent.

==================================================
PER-BLOCK DEEP YIELD ROLLS
==================================================

Deep Yield's gameplay rule is:

ONE ACTUAL ORE BLOCK HARVEST
=
ONE INDEPENDENT DEEP YIELD ACTIVATION ROLL

This remains true during vein mining.

Example:

A vein-mining operation harvests:

8 eligible deepslate ore blocks

Expected:

Deep Yield performs up to 8 independent activation rolls,
one for each harvested ore block.

Do NOT perform one single Deep Yield roll for the entire vein.

Do NOT multiply the entire vein from one successful roll.

Each block must independently determine whether Deep Yield activates.

==================================================
VEIN MINING EXAMPLE
==================================================

Configuration:

bonusChance = 0.20

Vein size:

8 blocks

Conceptually:

Block 1 -> independent 20% roll
Block 2 -> independent 20% roll
Block 3 -> independent 20% roll
Block 4 -> independent 20% roll
Block 5 -> independent 20% roll
Block 6 -> independent 20% roll
Block 7 -> independent 20% roll
Block 8 -> independent 20% roll

A successful roll on one block must not automatically affect the other
blocks in the vein.

Each successful block then performs its own weighted bonus-copy roll.

==================================================
PLAYER-INITIATED VEIN MINING
==================================================

If requirePlayer = true:

blocks harvested as part of a vein-mining operation directly initiated
by a player should be treated as player-originated mining IF the current
NeoForge / loot context provides a reliable player source.

Do not unnecessarily restrict Deep Yield only to the single physical
block that the player manually held the mouse button on.

However:

do NOT guess player attribution.

Do not make explosions, machines, commands, automation, or unrelated
programmatic block removal qualify merely to improve vein-miner
compatibility.

Use reliable server-side player / loot / break context.

==================================================
LOOT PIPELINE COMPATIBILITY
==================================================

When choosing Deep Yield's NeoForge integration point, investigate how
server-side vein-mining mods harvest their secondary blocks.

Prefer an integration point that observes the normal final loot result
for every harvested ore block, including programmatically harvested
blocks that still use Minecraft's normal harvesting/loot pipeline.

The implementation should work with:

- normal hand-mined blocks
- Fortune
- Silk Touch
- vanilla deepslate ores
- modded deepslate ores
- player-initiated vein mining

Do not special-case Ore Vein Miner by class name unless absolutely
necessary.

Prefer compatibility through standard Minecraft / NeoForge harvesting
and loot behavior.

==================================================
NO DOUBLE PROCESSING
==================================================

Be careful that a vein-mining mod may trigger multiple related hooks for
one secondary block.

Deep Yield must process each actual harvested block at most once.

Do not allow:

BreakEvent
+
loot event
+
manual compatibility handler

to cause the same ore block to receive multiple Deep Yield activation
rolls.

One harvested ore block = at most one Deep Yield roll.

==================================================
VEIN MINER + FORTUNE
==================================================

If Ore Vein Miner preserves Fortune for its secondary blocks:

Deep Yield must use the Fortune-generated loot result independently for
each block.

Example:

Secondary deepslate diamond ore produces:

3 diamonds

Deep Yield weighted result:

+2 copies

Expected result for THAT block:

9 diamonds

Do not apply the same Fortune result or Deep Yield result to the whole
vein.

==================================================
VEIN MINER + SILK TOUCH
==================================================

If the vein-mining mod preserves Silk Touch:

and:

affectSilkTouch = true

each harvested deepslate ore block may independently activate Deep Yield.

Example:

One secondary ore normally produces:

1 deepslate diamond ore

Deep Yield weighted result:

+1 copy

Expected result for that block:

2 deepslate diamond ore

If:

affectSilkTouch = false

the vein miner should still perform normal Silk Touch harvesting,
but Deep Yield must not affect those blocks.

==================================================
ATOMIC VEIN-MINING DROPS
==================================================

Do not intentionally introduce additional delayed item spawning during
vein mining.

For every individual harvested block:

calculate its final Deep Yield-modified loot before spawning whenever
the available API allows it.

If the vein-mining mod itself harvests several blocks within one server
tick, Deep Yield should remain synchronous with that operation.

Do not schedule bonus drops for later ticks.

==================================================
OPTIONAL COMPATIBILITY TEST
==================================================

If Ore Vein Miner is available in the development/test environment,
perform a manual compatibility test.

Suggested test:

1. Install Ore Vein Miner alongside Deep Yield.
2. Set bonusChance = 1.0.
3. Set:
   bonusWeightPlus1 = 1
   all other bonus weights = 0.
4. Find a connected vein of eligible deepslate ores.
5. Activate vein mining.

Expected:

every individual ore produces exactly x2 of its normal item loot.

Repeat with:

- no enchantment
- Fortune
- Silk Touch
- affectFortune=false
- affectSilkTouch=false

Verify:

- no blocks receive Deep Yield twice
- no secondary blocks are accidentally skipped
- no delayed second wave of drops
- no client/server desync
- XP is not multiplied

If the behavior cannot be verified because Ore Vein Miner's exact
harvesting implementation bypasses the chosen standard NeoForge hook,
document this explicitly rather than claiming compatibility.
