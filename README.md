# Deep Yield

Deep Yield gives naturally generated deepslate ores a configurable chance to
drop extra copies of their normal loot. It works on a server and needs no
client-side configuration.

## What happens when an ore is mined

For each eligible ore block, Deep Yield first lets Minecraft calculate normal
loot, including Fortune or Silk Touch. It then makes one independent bonus
roll. On a successful roll, it adds complete extra copies of that result.

With the default configuration, an eligible ore has a 20% chance to activate.
The bonus can range from one to five extra copies. XP and other block-break
effects stay unchanged.

For example, a successful `+1` bonus means:

| Tool result before Deep Yield | Final result |
| --- | --- |
| 1 diamond | 2 diamonds |
| 4 redstone from Fortune | 8 redstone |
| 1 deepslate diamond ore from Silk Touch | 2 deepslate diamond ores |

## Placed ore and repeat-bonus protection

Deep Yield records eligible ores that are placed after the mod is installed.
This prevents a player from repeatedly mining, placing, and re-mining the
same ore for more Deep Yield bonuses.

| Ore origin | Can Deep Yield activate? | Fortune and Silk Touch |
| --- | --- | --- |
| Naturally generated | Yes | Work normally, then Deep Yield may add copies |
| Placed in Survival | No | Work as normal Minecraft loot |
| Placed in Creative | Yes by default | Work normally, then Deep Yield may add copies |

Creative placement is intended for testing and server administration. Set
`allowCreativePlacedOres = false` to make Creative-placed ores behave like
Survival placements.

Example: mining a natural ore with Silk Touch may produce extra ore blocks.
If those blocks are placed in Survival and then mined with Fortune, Fortune
works normally, but Deep Yield does not roll a second time.

## Server setup

Install the matching Deep Yield jar in the server's `mods` folder and in each
player's matching NeoForge client. The server creates its configuration at
`config/deepyield-server.toml`.

The default configuration is:

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
allowCreativePlacedOres = true
oreBlacklist = []
```

`bonusChance` is the activation probability for each eligible block. The five
`bonusWeightPlus` settings control how often each extra-copy result occurs;
they are weights, so they do not need to add up to 100. `+1` means two copies
total, `+2` means three copies total, and so on.

For a deterministic server test, use:

```toml
bonusChance = 1.0
bonusWeightPlus1 = 1
bonusWeightPlus2 = 0
bonusWeightPlus3 = 0
bonusWeightPlus4 = 0
bonusWeightPlus5 = 0
```

Every eligible allowed ore then drops exactly twice its normal loot. Restart
or reload the server according to NeoForge's normal server-config lifecycle
after changing the file.

## Eligible ores and blacklist

Vanilla deepslate ores work automatically. Modded ores work when they use the
`c:ores_in_ground/deepslate` block tag. Datapacks and modpacks can opt in
other blocks with `#deepyield:bonus_ores`.

Use `oreBlacklist` to exclude any eligible ore. Blacklist entries always win:

```toml
oreBlacklist = [
  "minecraft:deepslate_diamond_ore",
  "some_mod:deepslate_uranium_ore"
]
```

Invalid or currently unavailable block IDs are ignored safely.

## Vein mining

Each harvested ore block gets its own Deep Yield roll. A successful block
never multiplies the whole vein.

Deep Yield works with vein-mining mods that use Minecraft's normal drop path.
It also supports Ore Vein Miner without bundling it or requiring it as a
dependency: its secondary command-based breaks use Deep Yield's optional
`before_remove` and `after_remove` hooks. Fortune, Silk Touch, blacklist
rules, and placed-ore protection still apply separately to every block.

## Supported branches

| Git branch | Minecraft | NeoForge | Deep Yield version |
| --- | --- | --- | --- |
| `26.1.x` | 26.1.x | 26.1.2.106 | 1.0.0 |
| `26.2.x` | 26.2.x | 26.2.0.79 | 2.0.0 |

## Advanced server notes

Placed-ore markers are saved with the chunk, survive server restarts, and
move with ores pushed by pistons. Ores placed before Deep Yield was installed
cannot be identified reliably and are treated as natural. Some machines or
custom block writes may bypass normal placement events; Deep Yield does not
guess their origin.

For local Ore Vein Miner testing, place its matching jar in `libs 26.1.x/` or
`libs 26.2.x/` on the corresponding branch. These local folders are ignored
by Git and are not included in the released Deep Yield jar.

## License

Deep Yield is available under the [MIT License](LICENSE).
