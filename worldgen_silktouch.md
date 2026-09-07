# Worldgen_silktouch

==================================================
NATURAL / WORLD-GENERATED ORES ONLY
==================================================

Deep Yield must only affect eligible ore blocks that are considered
naturally generated.

Player-placed or otherwise placed eligible ore blocks must NEVER activate
Deep Yield.

This requirement exists to prevent farming/duplication loops involving
Silk Touch.

Example exploit that MUST be prevented:

1. Player mines one naturally generated deepslate diamond ore with
   Silk Touch.
2. Deep Yield activates and produces 2 ore blocks.
3. Player places those ore blocks.
4. Player mines them again.

The newly placed blocks MUST NOT be eligible for Deep Yield.

Their normal Minecraft loot behavior must remain unchanged.

==================================================
IMPORTANT PROVENANCE DESIGN
==================================================

Do NOT assume that vanilla Minecraft ore blocks contain a built-in
"world generated" flag.

Instead, track placed eligible ore blocks persistently.

Preferred conceptual model:

- naturally generated eligible ores are unmarked
- any eligible ore that is placed after generation is marked as PLACED
- Deep Yield only activates on eligible blocks that are NOT marked as
  placed

Conceptually:

deepYieldEligible =
    oreCandidate
    AND not blacklisted
    AND not markedAsPlaced
    AND enchantment configuration allows it

==================================================
PLACEMENT TRACKING
==================================================

When an eligible block is placed into the world through a normal block
placement operation:

mark that BLOCK POSITION as player/externally placed.

This must work for:

- vanilla deepslate ores
- modded ores in c:ores_in_ground/deepslate
- blocks explicitly opted in through #deepyield:bonus_ores

At minimum, support normal player block placement.

Where NeoForge provides reliable placement events for other entities or
mechanisms, prefer marking those placements as non-natural as well.

Deep Yield should be conservative:

if an eligible ore was deliberately placed into the world after world
generation, it should normally NOT qualify for Deep Yield.

==================================================
PERSISTENT STORAGE
==================================================

Placed-ore information must persist across:

- server restart
- world save/load
- chunk unload/reload

Do NOT keep placement information only in an in-memory HashSet.

Use an appropriate current-version NeoForge/Minecraft persistent
per-chunk or world data mechanism.

Prefer per-chunk storage if available and appropriate so the system does
not require one giant global list of every placed ore position in the
world.

Only store positions that actually need tracking.

Do NOT store every naturally generated ore.

This is important for memory usage and save-file size.

==================================================
WHY TRACK PLACED ORES INSTEAD OF NATURAL ORES
==================================================

Do NOT create a database containing the location of every ore generated
by world generation.

That would be unnecessarily expensive and difficult to make compatible
with modded world generation.

Instead:

natural ore = default state

placed ore = explicitly tracked exception

This keeps storage proportional to the number of placed eligible ores,
not the number of ores generated in the world.

==================================================
BLOCK BREAK CLEANUP
==================================================

When a tracked placed ore is permanently removed:

its stored placement marker should eventually be removed as well.

However:

do NOT remove the marker before Deep Yield determines whether the block
was placed.

Processing order must preserve the information needed for the current
block break.

Conceptually:

1. Ore break begins.
2. Check whether position is marked as placed.
3. If placed:
      Deep Yield is skipped.
4. Normal loot processing occurs.
5. After block removal is confirmed:
      clean up the stored marker for that position.

Do not allow marker cleanup timing to make a placed ore appear natural
during the same break.

==================================================
SILK TOUCH ANTI-FARMING RULE
==================================================

Silk Touch may still be affected by Deep Yield when:

affectSilkTouch = true

BUT ONLY when the original ore is naturally generated.

Example:

Naturally generated deepslate diamond ore.

Normal Silk Touch result:
1 deepslate diamond ore

Deep Yield:
+1 copy

Final:
2 deepslate diamond ores

Both dropped ore items are ordinary items.

If the player later places either one:

the newly placed block position must be marked as placed.

Mining that placed block:

must NEVER trigger Deep Yield.

It should only produce its normal Minecraft loot.

==================================================
FORTUNE AND NATURAL ORES
==================================================

The same natural-only rule applies to Fortune.

Naturally generated eligible ore:
Deep Yield may activate.

Placed eligible ore:
Deep Yield must not activate.

Fortune itself must continue working normally on placed blocks.

Example:

Placed deepslate diamond ore
+
Fortune III

Minecraft determines the normal Fortune result.

Deep Yield:
SKIPPED

==================================================
VEIN MINER + NATURAL ORE TRACKING
==================================================

Vein mining must respect the same provenance rules on a per-block basis.

For every ore harvested in a vein:

check that individual block position.

Example vein:

Block 1 = natural
Block 2 = natural
Block 3 = player placed
Block 4 = natural

Expected:

Block 1 -> may roll Deep Yield
Block 2 -> may roll Deep Yield
Block 3 -> NEVER rolls Deep Yield
Block 4 -> may roll Deep Yield

Do not assume the entire vein has the same provenance.

==================================================
PISTONS / BLOCK MOVEMENT
==================================================

Investigate how the targeted Minecraft / NeoForge versions expose piston
and block-movement events.

A tracked placed ore must not become eligible merely because it is moved
to another position.

If an eligible placed ore moves from:

position A

to:

position B

its non-natural status should move with it where this can be reliably
detected.

Do not silently "launder" a placed ore into a natural ore through piston
movement.

If robust movement provenance cannot be implemented through supported
NeoForge APIs, document the limitation explicitly rather than using an
unsafe guess.

==================================================
AUTOMATED / MODDED BLOCK PLACEMENT
==================================================

Where possible, eligible ore blocks placed by:

- machines
- fake players
- modded placement systems
- structure-like placement systems after world generation

should be considered non-natural.

However:

do not add hard dependencies on individual mods.

Use standard NeoForge/Minecraft placement hooks where available.

The primary anti-exploit requirement is that normal player-placed ores
can never reactivate Deep Yield.

==================================================
EXISTING WORLDS / MOD INSTALLATION LIMITATION
==================================================

Be aware of an important limitation:

If Deep Yield is installed into an existing world, ore blocks that were
manually placed BEFORE Deep Yield was installed may be indistinguishable
from naturally generated ores.

Do not pretend this can be reliably reconstructed after the fact.

Document this limitation clearly.

From the moment Deep Yield begins tracking placements, newly placed
eligible ores must be handled correctly.

==================================================
TEST - SILK TOUCH REPLACEMENT EXPLOIT
==================================================

Set:

bonusChance = 1.0
bonusWeightPlus1 = 1
all other weights = 0
affectSilkTouch = true

1. Find one naturally generated deepslate diamond ore.
2. Mine it with Silk Touch.

Expected:

2 deepslate diamond ore items.

3. Place both ore blocks.
4. Mine those placed blocks.

Expected:

Deep Yield does NOT activate on either block.

They produce only their normal Minecraft loot.

==================================================
TEST - SAVE / RELOAD
==================================================

1. Place an eligible deepslate ore.
2. Save the world.
3. Restart the server.
4. Mine the placed ore.

Expected:

The position is still recognized as placed.

Deep Yield does NOT activate.

==================================================
TEST - PLACED MODDED ORE
==================================================

Take a modded block tagged as:

c:ores_in_ground/deepslate

Place it manually.

Mine it.

Expected:

Deep Yield does NOT activate.

The rule must not apply only to vanilla blocks.

==================================================
TEST - NATURAL MODDED ORE
==================================================

Find the same modded ore naturally generated by its worldgen.

Mine it.

Expected:

Deep Yield may activate normally.

==================================================
TEST - BLACKLIST + NATURAL STATE
==================================================

Naturally generated block
+
blacklisted

Expected:

Deep Yield does nothing.

Blacklist remains higher priority than natural-worldgen eligibility.

==================================================
FINAL ELIGIBILITY ORDER
==================================================

Use a clear decision order similar to:

1. Is block in c:ores_in_ground/deepslate
   OR #deepyield:bonus_ores?

   NO -> skip

2. Is block in oreBlacklist?

   YES -> skip

3. Is block marked as placed/non-natural?

   YES -> skip

4. Does the tool contain Fortune while affectFortune=false?

   YES -> skip

5. Does the tool contain Silk Touch while affectSilkTouch=false?

   YES -> skip

6. Roll bonusChance.

   MISS -> normal loot

7. HIT -> weighted bonus-copy roll.

8. Apply copies to the final normal item loot.

9. Spawn final drops together server-side.

XP remains unchanged.

==================================================
IMPLEMENTATION IN THIS PROJECT
==================================================

Both supported branches use NeoForge's persistent per-chunk data attachments:

- `AttachmentType<PlacedOrePositions>` is registered in
  `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`.
- The attachment is held by `LevelChunk`, serialized with a `MapCodec`, and
  contains only packed `BlockPos` values that were marked as placed.
- `LevelChunk.markUnsaved()` is called after every marker mutation, so markers
  survive save/load, restart, and chunk unload/reload without recording
  naturally generated ores.

Normal and entity-backed placement uses
`BlockEvent.EntityPlaceEvent`; reliable fluid placement uses
`BlockEvent.FluidPlaceBlockEvent`. Both are filtered by the deepslate/common
ore tag or `#deepyield:bonus_ores`, so modded tagged ores are covered without
compile-time dependencies.

`BlockDropsEvent` reads the marker before checking player attribution or
rolling Deep Yield. A marked block is skipped, while its cleanup is queued for
`LevelTickEvent.Post` and performed only after the position's state has
changed. This preserves placed provenance during the current break and leaves
normal Minecraft Fortune/Silk Touch drops untouched.

Piston movement is supported through NeoForge `PistonEvent.Pre`/`Post` and the
event's `PistonStructureResolver`. Tracked positions in the resolver's push
list move with the piston; tracked positions in its destroy list are removed.
If a mod changes blocks directly without firing one of these supported hooks,
there is no general API that can identify that write, so no unsafe global
block-change guess is used.

Installing the mod into an existing world cannot recover eligible blocks
placed before installation: those blocks are indistinguishable from natural
worldgen. New placements are tracked from installation onward.
