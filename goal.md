# Goal

You are working inside an IntelliJ IDEA Minecraft mod project generated
with the official NeoForge Mod Generator.

The project uses NeoGradle.

Your task is to implement a new standalone Minecraft mod called:

Deep Yield

==================================================
PROJECT IDENTITY
==================================================

Mod name:
Deep Yield

Mod ID:
deepyield

Java package:
dk.acidglow.deepyield

Initial mod version:
1.0.0

Mod loader:
NeoForge

Build system:
NeoGradle

The project must support these Minecraft release lines:

- Minecraft 26.1.x
- Minecraft 26.2.x

==================================================
IMPORTANT VERSION STRATEGY
==================================================

Each Minecraft release line MUST live on its own Git branch.

Required Git branches:

26.1.x
26.2.x

DO NOT implement multiple Minecraft versions as:

- separate folders on the same branch
- Gradle subprojects for each Minecraft version
- common/ + versions/ directories
- runtime version detection

Instead, each Git branch contains one normal standalone NeoForge project
for its Minecraft release line.

Conceptually:

Git repository
├── branch: 26.1.x
│   └── Minecraft 26.1.x + compatible NeoForge
│
└── branch: 26.2.x
    └── Minecraft 26.2.x + compatible NeoForge

Keep the source layout and implementation reasonably similar between
branches so bug fixes can later be cherry-picked or manually ported.

However:

Correct version-specific implementation is more important than forcing
the code to be identical.

==================================================
GIT WORKFLOW
==================================================

Before changing anything:

1. Inspect the Git repository.
2. Determine the current branch.
3. Inspect the working tree.
4. Preserve existing user changes.
5. Do not discard work.
6. Do not use destructive Git commands such as:

   git reset --hard

   unless explicitly instructed by the project owner.

The current generated project is expected to target Minecraft 26.2.

Use/create:

26.2.x

as the primary implementation branch.

Implement Deep Yield there first.

Build and verify it.

Then create/adapt:

26.1.x

for Minecraft 26.1.x and the appropriate NeoForge version.

Do not merely change a Minecraft version string and assume compatibility.

Inspect and adapt the APIs for each branch.

Both branches must build independently.

If the repository has no initial commit or branch creation is otherwise
blocked:

- do not fake successful Git operations
- explain the actual blocker
- preserve the requested branch-oriented architecture

==================================================
ORIGINALITY REQUIREMENT
==================================================

Deep Yield is an independently implemented Minecraft mod.

It may be inspired by the broad gameplay concept that deepslate ores can
occasionally provide increased rewards.

DO NOT:

- copy source code from Deepslate Dubble
- decompile Deepslate Dubble
- download its JAR for reverse engineering
- copy its assets
- copy its configuration
- copy its metadata
- reproduce its implementation with renamed classes
- copy implementation details from another mod

Implement Deep Yield independently from scratch.

==================================================
CORE GAMEPLAY IDEA
==================================================

Deep Yield affects eligible deepslate ores.

When a player mines an eligible ore:

1. Minecraft calculates the complete normal item loot.
2. Fortune or Silk Touch are handled normally by Minecraft.
3. Deep Yield checks eligibility and configuration.
4. Deep Yield rolls its configured activation chance.
5. If Deep Yield misses, the player receives normal vanilla/modded loot.
6. If Deep Yield hits, Deep Yield performs a weighted bonus-copy roll.
7. The final normal loot result is duplicated according to that roll.
8. XP remains unchanged.
9. All final item drops are spawned together as one clean block-break
   operation.

==================================================
DEEP YIELD ACTIVATION CHANCE
==================================================

Config option:

bonusChance

Type:
double

Default:
0.20

Minimum:
0.0

Maximum:
1.0

Meaning:

0.00 = 0%
0.10 = 10%
0.20 = 20%
0.50 = 50%
1.00 = 100%

Example:

bonusChance = 0.20

means each eligible block break has a 20% chance to activate Deep Yield.

If the activation roll fails:

Deep Yield does nothing.

The normal Minecraft loot remains unchanged.

==================================================
WEIGHTED BONUS ROLL
==================================================

If Deep Yield successfully activates, perform a SECOND random roll.

This second roll chooses the number of ADDITIONAL COPIES of the complete
normal loot result.

Version 1.0.0 should support:

+1 copy
+2 copies
+3 copies
+4 copies
+5 copies

Suggested default weights:

+1 copy = 50
+2 copies = 25
+3 copies = 15
+4 copies = 7
+5 copies = 3

These should be configurable.

Suggested config names:

bonusWeightPlus1 = 50
bonusWeightPlus2 = 25
bonusWeightPlus3 = 15
bonusWeightPlus4 = 7
bonusWeightPlus5 = 3

These are WEIGHTS, not required percentages.

The implementation must sum the configured weights and select one result
proportionally.

For example:

50
25
15
7
3

sum to 100, which happens to correspond directly to percentages.

But values such as:

100
50
20
10
5

must also be valid.

Do not require the numbers to sum to 100.

==================================================
SUCCESSFUL HIT GUARANTEE
==================================================

A successful Deep Yield activation MUST ALWAYS increase the resulting
item drops.

There must be NO +0 result.

The smallest possible weighted result is:

+1 additional copy

Therefore:

the MINIMUM successful Deep Yield result is always:

x2 total item drops

Examples:

Normal loot:
1 diamond

Deep Yield:
+1 copy

Final:
2 diamonds

--------------------------------------------------

Normal loot:
6 redstone

Deep Yield:
+1 copy

Final:
12 redstone

--------------------------------------------------

Normal Fortune result:
7 diamonds

Deep Yield:
+1 copy

Final:
14 diamonds

--------------------------------------------------

Normal Silk Touch result:
1 deepslate diamond ore

Deep Yield:
+1 copy

Final:
2 deepslate diamond ore

==================================================
BONUS COPY SEMANTICS
==================================================

The weighted result describes additional COPIES OF THE COMPLETE NORMAL
LOOT RESULT.

It does NOT mean individual items are simply added.

Mapping:

+1 copy = x2 total
+2 copies = x3 total
+3 copies = x4 total
+4 copies = x5 total
+5 copies = x6 total

Formula:

finalLoot =
    normalLoot × (1 + bonusCopies)

Example:

Normal loot:
8 redstone

Weighted Deep Yield result:
+3 copies

Final:

8 original
+ 24 bonus
= 32 redstone

==================================================
INVALID WEIGHT CONFIGURATION
==================================================

All bonus weights must be non-negative integers.

At least one bonus weight must be greater than zero.

If configuration is invalid:

- do not crash the server unnecessarily
- log a clear warning/error
- use a safe fallback behavior

Prefer falling back to the default weight table if all configured weights
are zero or otherwise unusable.

Do not silently produce a successful Deep Yield hit with no reward.

==================================================
ORE ELIGIBILITY
==================================================

Use NeoForge's standardized common block tag for deepslate ores as the
PRIMARY automatic eligibility mechanism.

Expected common tag:

c:ores_in_ground/deepslate

Use the current-version equivalent of the NeoForge Java tag constant,
where available.

For example, the API may expose something equivalent to:

Tags.Blocks.ORES_IN_GROUND_DEEPSLATE

BUT:

verify the exact API independently for both:

26.1.x
26.2.x

Do not blindly assume the Java constant name is identical.

==================================================
MODDED DEEPSLATE ORES
==================================================

Deep Yield MUST work automatically with modded deepslate ores that are
correctly placed in the NeoForge common tag:

c:ores_in_ground/deepslate

Example:

some_mod:deepslate_tin_ore

If that block is tagged as:

c:ores_in_ground/deepslate

Deep Yield should automatically affect it.

No Java integration specific to that mod should be required.

Do not add compile-time dependencies on ore mods.

Do not maintain a hard-coded list of modded ores.

==================================================
DEEP YIELD COMPATIBILITY TAG
==================================================

Also create/support:

#deepyield:bonus_ores

This is an explicit opt-in compatibility tag.

Its purpose is to allow:

- modpack authors
- datapack authors
- mod authors

to make additional blocks eligible for Deep Yield.

A block is a candidate if:

block ∈ c:ores_in_ground/deepslate

OR

block ∈ #deepyield:bonus_ores

Do not use block registry-name guessing such as:

name contains "deepslate"

unless explicitly requested later.

Tags must be the compatibility mechanism.

==================================================
ORE BLACKLIST
==================================================

Create a server config option:

oreBlacklist

Type:

list of Minecraft block resource locations

Default:

empty list

Example:

oreBlacklist = [
    "minecraft:deepslate_diamond_ore",
    "minecraft:deepslate_emerald_ore",
    "some_mod:deepslate_uranium_ore"
]

The blacklist must support:

- vanilla blocks
- modded blocks

==================================================
BLACKLIST PRIORITY
==================================================

The blacklist ALWAYS wins.

Conceptually:

candidate =
    block is in c:ores_in_ground/deepslate
    OR
    block is in #deepyield:bonus_ores

eligible =
    candidate
    AND
    block is NOT blacklisted

Example:

minecraft:deepslate_diamond_ore

may be in:

c:ores_in_ground/deepslate

but if it is also in:

oreBlacklist

then Deep Yield MUST NOT affect it.

The same applies if a block belongs to:

#deepyield:bonus_ores

Blacklisting must override every opt-in tag.

==================================================
BLACKLIST VALIDATION
==================================================

Resolve configured block IDs using Minecraft's registry system.

Do not maintain a manually hard-coded registry mapping.

Invalid block IDs must not crash the game.

Blocks from missing optional mods must also be handled safely.

For example, if the config still contains:

some_mod:deepslate_tin_ore

but that mod is no longer installed:

Deep Yield should continue loading safely.

An appropriate warning may be logged.

Do not spam the log.

==================================================
FORTUNE CONFIGURATION
==================================================

Create config option:

affectFortune

Type:
boolean

Default:
true

==================================================
FORTUNE ENABLED
==================================================

If:

affectFortune = true

then Deep Yield may activate when the tool has Fortune.

Minecraft must calculate Fortune normally first.

Deep Yield then operates on the resulting item loot.

Example:

Fortune result:
3 diamonds

Deep Yield activates.

Weighted result:
+1 copy

Final:
6 diamonds

--------------------------------------------------

Example:

Fortune result:
4 diamonds

Weighted result:
+3 copies

Final:
16 diamonds

Do NOT manually reproduce Minecraft's Fortune algorithm.

==================================================
FORTUNE DISABLED
==================================================

If:

affectFortune = false

and the tool has Fortune:

Deep Yield must NOT activate for that block break.

Fortune itself MUST continue working normally.

Example:

Fortune result:
3 diamonds

affectFortune = false

Final:
3 diamonds

Do not remove Fortune.

Do not calculate what the block would have dropped without Fortune.

Simply skip Deep Yield.

==================================================
SILK TOUCH CONFIGURATION
==================================================

Create config option:

affectSilkTouch

Type:
boolean

Default:
true

==================================================
SILK TOUCH ENABLED
==================================================

If:

affectSilkTouch = true

Deep Yield may affect Silk Touch loot.

Minecraft calculates Silk Touch normally first.

Deep Yield then copies the resulting final item loot.

Example:

Silk Touch result:
1 minecraft:deepslate_diamond_ore

Deep Yield activates.

Weighted result:
+1 copy

Final:
2 minecraft:deepslate_diamond_ore

--------------------------------------------------

Weighted result:
+4 copies

Final:
5 minecraft:deepslate_diamond_ore

Do NOT special-case Silk Touch to disable Deep Yield when the config
allows it.

==================================================
SILK TOUCH DISABLED
==================================================

If:

affectSilkTouch = false

and the tool uses Silk Touch:

Deep Yield must NOT activate.

Silk Touch itself continues behaving normally.

Example:

Normal Silk Touch result:
1 deepslate diamond ore

Final:
1 deepslate diamond ore

Do not replace the Silk Touch block with ordinary ore resources.

==================================================
FORTUNE + SILK TOUCH CHECK PRIORITY
==================================================

If a tool somehow contains enchantment combinations that are not normally
obtainable in survival:

respect Minecraft's normal loot behavior first.

For Deep Yield eligibility:

if a relevant enchantment is present and its corresponding Deep Yield
config option is false, Deep Yield should conservatively skip its bonus.

Do not attempt to redesign vanilla enchantment conflict behavior.

==================================================
EXPERIENCE
==================================================

Deep Yield modifies ITEM DROPS only.

Deep Yield MUST NOT multiply:

- XP
- XP orbs
- block XP values
- unrelated block-break side effects

Example:

Vanilla ore XP:
5

Deep Yield result:
x5 item drops

XP must still be:

5

==================================================
NORMAL LOOT FIRST
==================================================

Deep Yield must not try to predict what an ore should drop.

Always prefer this model:

Minecraft / mod calculates normal loot
                ↓
Fortune / Silk Touch included
                ↓
Deep Yield operates on final item result

This is essential for modded ore compatibility.

Example:

some_mod:deepslate_tin_ore

might drop:

some_mod:raw_tin

or:

some_mod:tin_chunk

or something completely different.

Deep Yield must not care what the item is.

It should copy the resulting loot.

==================================================
ATOMIC DROP PROCESSING
==================================================

Deep Yield bonus drops must feel like part of the original block break.

Do NOT:

1. spawn vanilla loot
2. wait
3. spawn Deep Yield bonus loot afterward

Avoid any visible or gameplay delay.

Preferred processing:

1. Block is broken.
2. Server calculates complete normal loot.
3. Deep Yield performs its checks.
4. Deep Yield performs activation RNG.
5. Deep Yield performs weighted bonus RNG.
6. Deep Yield creates the complete final drop result.
7. All resulting drops are spawned together as part of the same
   server-side block-break operation.

Prefer completing all processing in the same server tick.

==================================================
NO DELAYED DROP SYSTEM
==================================================

Do NOT use:

- scheduled tasks
- delayed tasks
- timers
- later-tick bonus spawning
- repeated spawn operations spread over multiple ticks
- background drop queues
- persistent per-block state
- client-side fake items

There should be no visible:

"normal loot first, bonus loot later"

effect.

==================================================
SERVER AUTHORITATIVE LOGIC
==================================================

ALL Deep Yield gameplay decisions must happen on the logical server.

This includes:

- ore eligibility
- blacklist checks
- Fortune config checks
- Silk Touch config checks
- bonusChance roll
- weighted bonus roll
- final item quantities
- stack splitting

The client must not independently perform Deep Yield RNG.

The server result is authoritative.

==================================================
CLIENT / SERVER SYNC
==================================================

Deep Yield must use Minecraft's normal server-authoritative item entity
synchronization whenever possible.

Prefer modifying/finalizing the loot BEFORE item entities are sent to
clients.

The client should receive the final server result normally.

Avoid a situation where:

client sees vanilla drops
then
server corrects/replaces them

Deep Yield should appear clean and synchronized.

Do not add custom network packets solely to synchronize item drops if
Minecraft already synchronizes those entities correctly.

Only add custom networking if genuinely required.

==================================================
ITEM STACK CONSOLIDATION
==================================================

After calculating the final loot quantity, normalize the resulting item
stacks.

Where identical stacks can safely be combined:

combine them up to the item's maximum stack size.

Example:

Normal loot:
16 redstone

Deep Yield:
x4 total

Final quantity:
64

Preferred:

one ItemStack of 64 redstone

not:

four ItemStacks of 16

==================================================
STACK OVERFLOW
==================================================

Never create an ItemStack larger than its legal maximum size.

Example:

Normal result:
40 items

Final Deep Yield quantity:
80

Maximum stack size:
64

Produce:

64
16

Do not create:

80 in one invalid stack

Do not lose items.

==================================================
ITEM COMPONENT / DATA SAFETY
==================================================

When copying or merging ItemStacks:

preserve relevant item data/components.

Only combine stacks when Minecraft considers them compatible.

Do not merge stacks that differ in meaningful components/data.

This is important for compatibility with modded loot.

==================================================
PERFORMANCE
==================================================

Deep Yield should be lightweight.

Avoid excessive ItemEntity creation.

Example:

Final result:
32 diamonds

Prefer:

1 ItemEntity containing an ItemStack of 32

instead of:

32 ItemEntity objects containing one diamond each

If the total exceeds maximum stack size:

create only the minimum number of legal stacks required.

The implementation should remain efficient when:

- Fortune produces large drops
- Deep Yield rolls +5 copies
- modded ores produce many items
- multiple players mine simultaneously

Do not create:

- ticking managers
- polling systems
- background queues

for ordinary drop processing.

==================================================
SERVER CONFIGURATION
==================================================

Use a NeoForge SERVER configuration appropriate to the current branch.

Required configuration:

bonusChance = 0.20

bonusWeightPlus1 = 50
bonusWeightPlus2 = 25
bonusWeightPlus3 = 15
bonusWeightPlus4 = 7
bonusWeightPlus5 = 3

affectFortune = true
affectSilkTouch = true

oreBlacklist = []

Use appropriate NeoForge configuration syntax.

Do not force this exact textual representation if the current NeoForge
config API writes the file differently.

==================================================
CONFIG SEMANTICS
==================================================

bonusChance:

determines whether Deep Yield activates.

The weighted bonus settings:

determine WHICH bonus result occurs after activation.

These are two separate RNG stages.

Conceptually:

Block breaks
    ↓
Is block eligible?
    ↓
Is block blacklisted?
    ↓
Are enchantment config rules satisfied?
    ↓
Roll bonusChance
    ↓
MISS
    └── normal loot
    ↓
HIT
    ↓
Roll weighted bonus
    ↓
+1 / +2 / +3 / +4 / +5 copies
    ↓
Create complete final loot
    ↓
Spawn together

==================================================
EXAMPLE PROBABILITY MODEL
==================================================

With:

bonusChance = 20%

Weights:

+1 = 50
+2 = 25
+3 = 15
+4 = 7
+5 = 3

The approximate overall probabilities per eligible block break are:

80%:
no Deep Yield bonus

10%:
+1 copy
x2 total

5%:
+2 copies
x3 total

3%:
+3 copies
x4 total

1.4%:
+4 copies
x5 total

0.6%:
+5 copies
x6 total

Do not hard-code these resulting percentages.

Calculate them naturally from bonusChance and the configured weights.

==================================================
CONFIG RELOAD / LIFECYCLE
==================================================

Use the normal NeoForge server-config lifecycle.

Gameplay code must read actual configured values.

Do not hard-code configuration values into gameplay constants.

If the target NeoForge version requires a world/server restart for some
config changes:

document that honestly.

Do not invent a custom config synchronization framework unless required.

==================================================
NEOFORGE API REQUIREMENTS
==================================================

For EACH branch independently inspect the APIs available in:

Minecraft 26.1.x + its NeoForge version

and:

Minecraft 26.2.x + its NeoForge version

Inspect:

- Minecraft source/mappings
- NeoForge source
- NeoForge events
- loot APIs
- block-break hooks
- configuration APIs
- common tags
- registries
- data generation APIs
- item stack APIs
- mod metadata requirements

Do NOT blindly reuse APIs from:

- Minecraft 1.20.x
- Minecraft 1.21.x
- another NeoForge release
- outdated tutorials

==================================================
PREFERRED DROP INTEGRATION
==================================================

Use the cleanest supported NeoForge API that allows Deep Yield to
interact with the final item loot before it is spawned.

Strongly prefer:

an official NeoForge event/hook/loot API

that permits the resulting drop list to be inspected or modified as part
of the block-break process.

Avoid:

- Mixins
- coremods
- bytecode transformers
- replacing vanilla Minecraft classes

unless no suitable supported NeoForge mechanism exists.

If the exact mechanism differs between 26.1.x and 26.2.x:

use the correct implementation for each branch.

==================================================
RNG REQUIREMENTS
==================================================

Use an appropriate server-side Minecraft/Java random source.

Do not use client-side RNG.

One block break should perform:

at most one Deep Yield activation roll

and, if successful:

exactly one weighted bonus selection.

Do not recursively trigger Deep Yield from its own bonus items.

==================================================
DATA GENERATION
==================================================

Support:

#deepyield:bonus_ores

using the appropriate data/tag system.

If NeoForge data generation is appropriate for the targeted version,
use it.

Do not duplicate vanilla ores into this custom tag merely because they
already exist in:

c:ores_in_ground/deepslate

unless there is a real reason.

The common NeoForge tag should remain the primary automatic source of
vanilla and properly tagged modded ores.

==================================================
MAIN MOD PACKAGE
==================================================

Use:

dk.acidglow.deepyield

as the root Java package.

Use a clean main mod entry point following current NeoForge conventions.

Use:

MOD_ID = "deepyield"

where appropriate.

Preserve valid generated MDK structure instead of unnecessarily rewriting
the project.

==================================================
CODE ORGANIZATION
==================================================

Keep the project understandable.

Suggested conceptual package layout:

dk.acidglow.deepyield
├── DeepYield
├── config/
├── gameplay/
└── data/

Only create packages when they contain meaningful code.

Avoid unnecessary abstractions and classes such as:

DeepYieldManager
DeepYieldFactory
DeepYieldController
DeepYieldService
DeepYieldHelperFactory

unless they genuinely solve a problem.

==================================================
LOGGING
==================================================

Use the logger appropriate to NeoForge.

One concise initialization message is acceptable.

Do not log every mined block.

Warnings for genuinely invalid config entries are acceptable.

Avoid console spam.

==================================================
README
==================================================

Create or update README.md.

Include:

# Deep Yield

Deep Yield gives deepslate ores a configurable chance to provide multiple
copies of their normal loot.

Explain that:

- vanilla deepslate ores are supported
- correctly tagged modded deepslate ores are supported
- Fortune can optionally be affected
- Silk Touch can optionally be affected
- ores can be blacklisted
- Deep Yield uses weighted bonus rewards
- a successful Deep Yield activation always means at least x2 total loot
- XP is never multiplied
- all final drops are calculated server-side and spawned together

==================================================
README: WEIGHTED BONUS EXPLANATION
==================================================

Explain the two-stage RNG clearly.

Example:

bonusChance = 20%

If Deep Yield activates, it then chooses:

+1
+2
+3
+4
or
+5

additional copies using configured weights.

Make clear:

+1 copy = x2 total loot

and NOT:

+1 individual item

==================================================
README: ORE COMPATIBILITY
==================================================

Document:

c:ores_in_ground/deepslate

as the main automatic compatibility mechanism where available.

Also document:

#deepyield:bonus_ores

as a manual opt-in compatibility tag.

Explain how datapacks/modpacks can add unsupported ores.

==================================================
README: BLACKLIST
==================================================

Document examples such as:

minecraft:deepslate_diamond_ore
minecraft:deepslate_emerald_ore
some_mod:deepslate_uranium_ore

Explain that blacklist entries override all eligibility tags.

==================================================
README: GIT BRANCHES
==================================================

Document the version model:

Minecraft 26.1.x:
branch 26.1.x

Minecraft 26.2.x:
branch 26.2.x

Do not claim support for Minecraft versions that have not actually been
implemented and built.

==================================================
LICENSE
==================================================

If the project already contains a selected license:

preserve it.

Do not copy another Minecraft mod's license merely because its gameplay
idea inspired Deep Yield.

If no license is selected:

leave the final licensing choice to the project owner.

==================================================
TEST REQUIREMENTS
==================================================

Verify at minimum the following.

==================================================
TEST 1 - NON-DEEPSLATE ORE
==================================================

Block:

minecraft:diamond_ore

Not explicitly added to #deepyield:bonus_ores.

Expected:

Deep Yield does nothing.

==================================================
TEST 2 - ACTIVATION MISS
==================================================

Eligible deepslate ore.

Deep Yield activation roll fails.

Expected:

normal loot only.

==================================================
TEST 3 - MINIMUM HIT
==================================================

Normal drop:

1 diamond

Deep Yield activates.

Weighted result:

+1 copy

Expected:

2 diamonds

==================================================
TEST 4 - REDSTONE MINIMUM HIT
==================================================

Normal resulting loot:

6 redstone

Deep Yield activates.

Weighted result:

+1 copy

Expected:

12 redstone

This confirms that "+1" means one complete extra COPY of the loot, not
one extra item.

==================================================
TEST 5 - HIGH BONUS
==================================================

Normal loot:

2 diamonds

Weighted result:

+5 copies

Expected:

12 diamonds

because:

2 original
+
5 × 2 bonus
=
12

==================================================
TEST 6 - FORTUNE ENABLED
==================================================

affectFortune = true

Fortune result:

4 diamonds

Deep Yield weighted result:

+2 copies

Expected:

12 diamonds

==================================================
TEST 7 - FORTUNE DISABLED
==================================================

affectFortune = false

Fortune result:

4 diamonds

Expected:

4 diamonds

Deep Yield must not activate.

Vanilla Fortune remains unchanged.

==================================================
TEST 8 - SILK TOUCH ENABLED
==================================================

affectSilkTouch = true

Normal Silk Touch result:

1 deepslate diamond ore

Deep Yield:

+3 copies

Expected:

4 deepslate diamond ore blocks

==================================================
TEST 9 - SILK TOUCH DISABLED
==================================================

affectSilkTouch = false

Normal Silk Touch result:

1 deepslate diamond ore

Expected:

1 deepslate diamond ore

Deep Yield must not activate.

==================================================
TEST 10 - XP
==================================================

An ore normally grants XP.

Deep Yield activates with a large bonus.

Expected:

item drops increase.

XP remains exactly the normal Minecraft amount.

==================================================
TEST 11 - VANILLA BLACKLIST
==================================================

oreBlacklist contains:

minecraft:deepslate_diamond_ore

Expected:

Deep Yield never affects deepslate diamond ore.

==================================================
TEST 12 - MODDED BLACKLIST
==================================================

A modded deepslate ore is correctly tagged in:

c:ores_in_ground/deepslate

but is also blacklisted.

Expected:

Deep Yield does nothing.

==================================================
TEST 13 - MODDED COMMON TAG
==================================================

A modded ore belongs to:

c:ores_in_ground/deepslate

Expected:

Deep Yield affects it automatically.

No mod-specific Java integration is required.

==================================================
TEST 14 - CUSTOM TAG
==================================================

A block not automatically detected is added to:

#deepyield:bonus_ores

Expected:

Deep Yield can affect it.

==================================================
TEST 15 - BLACKLIST OVERRIDES CUSTOM TAG
==================================================

A block belongs to:

#deepyield:bonus_ores

and:

oreBlacklist

Expected:

Deep Yield does nothing.

==================================================
TEST 16 - STACK SPLITTING
==================================================

Final quantity:

80

Maximum stack size:

64

Expected final logical stacks:

64
16

No invalid oversized ItemStack.

==================================================
TEST 17 - ATOMIC DROPS
==================================================

Deep Yield activates.

Expected:

normal loot and Deep Yield bonus appear together.

There must be no intentionally delayed second wave of bonus item drops.

==================================================
TEST 18 - CLIENT / SERVER SYNC
==================================================

Test on a dedicated server if possible.

Expected:

server determines the Deep Yield result.

Client receives the correct final item entities.

No client-side duplicate RNG.

No temporary incorrect vanilla amount followed by correction.

==================================================
TEST 19 - CHANCE ZERO
==================================================

bonusChance = 0.0

Expected:

Deep Yield never activates.

==================================================
TEST 20 - CHANCE ONE
==================================================

bonusChance = 1.0

Expected:

every eligible allowed block break activates Deep Yield.

The weighted roll still determines the bonus tier.

==================================================
TEST 21 - WEIGHT TABLE
==================================================

Configure:

bonusWeightPlus1 = 1
bonusWeightPlus2 = 0
bonusWeightPlus3 = 0
bonusWeightPlus4 = 0
bonusWeightPlus5 = 0

bonusChance = 1.0

Expected:

every eligible block receives exactly:

+1 copy
=
x2 total

==================================================
TEST 22 - INVALID WEIGHTS
==================================================

Configure all weights to zero.

Expected:

server does not crash.

Deep Yield uses clearly documented safe fallback behavior and logs an
appropriate warning.

==================================================
BUILD REQUIREMENTS
==================================================

Do not stop after editing code.

For:

26.2.x

run the Gradle build.

Use the project's Gradle wrapper.

For example:

./gradlew build

or the operating-system equivalent.

Resolve compilation and resource errors.

Run data generation if necessary.

Continue until the branch builds successfully or a genuine external
blocker is identified.

Then perform the same verification independently on:

26.1.x

Do not assume that because 26.2.x builds, 26.1.x also works.

==================================================
DEDICATED SERVER CHECK
==================================================

Where reasonably possible, verify that the mod can initialize on a
dedicated NeoForge server.

Gameplay logic must not reference client-only classes.

Do not claim server-only client-install compatibility unless it has been
verified against the target NeoForge version's mod-loading requirements.

==================================================
CODE QUALITY
==================================================

Use modern Java appropriate for the targeted Minecraft version.

Follow current NeoForge conventions.

Avoid deprecated APIs.

Do not suppress warnings merely to hide issues.

Avoid unnecessary dependencies.

Write comments explaining WHY non-obvious compatibility logic exists.

Do not write comments that simply restate obvious Java code.

==================================================
DO NOT DO THESE THINGS
==================================================

Do NOT:

- copy Deepslate Dubble
- decompile other mods
- implement multiple Minecraft versions in folders on one branch
- create Minecraft-version Gradle submodules
- replace NeoGradle
- switch to Fabric
- switch to legacy Forge
- add Architectury unless explicitly requested later
- use Mixins unless genuinely necessary
- manually reimplement Fortune
- disable Silk Touch when affectSilkTouch=true
- add +1 individual item instead of +1 complete loot copy
- allow a successful Deep Yield hit to produce +0
- multiply XP
- hard-code only vanilla ores
- rely on registry-name "deepslate" guessing
- spawn delayed bonus drops
- perform Deep Yield RNG on the client
- spawn one ItemEntity per individual resource unnecessarily
- create invalid oversized ItemStacks
- claim a branch works if it does not build
- leave known compilation errors unresolved

==================================================
FINAL VERIFICATION
==================================================

Before finishing, verify that Git contains:

26.1.x
26.2.x

Verify:

26.1.x targets Minecraft 26.1.x and compatible NeoForge.

Verify:

26.2.x targets Minecraft 26.2.x and compatible NeoForge.

Verify both independently.

==================================================
FINAL REPORT
==================================================

When finished, provide a concise but complete report containing:

1. Git branches created or used.
2. Exact Minecraft version targeted by 26.1.x.
3. Exact NeoForge version used by 26.1.x.
4. Exact Minecraft version targeted by 26.2.x.
5. Exact NeoForge version used by 26.2.x.
6. Files created/modified on 26.1.x.
7. Files created/modified on 26.2.x.
8. Exact NeoForge mechanism used to intercept/modify block drops.
9. How c:ores_in_ground/deepslate is used.
10. How #deepyield:bonus_ores is used.
11. How oreBlacklist is implemented.
12. How bonusChance works.
13. How the weighted bonus selection works.
14. How the minimum x2 guarantee is enforced.
15. How Fortune is handled.
16. How Silk Touch is handled.
17. How XP is kept unchanged.
18. How final ItemStacks are consolidated and split.
19. How delayed bonus spawning is avoided.
20. How server/client synchronization works.
21. Whether clients are required to have Deep Yield installed.
22. Exact Gradle build command used for 26.1.x.
23. 26.1.x build result.
24. Exact Gradle build command used for 26.2.x.
25. 26.2.x build result.
26. Any remaining manual in-game testing requirements.

MOST IMPORTANT:

Actually modify the project.

Actually create/use the requested Git branches.

Actually implement the mod.

Actually build both branches.

Do not merely provide example code or explain how it could be done.
