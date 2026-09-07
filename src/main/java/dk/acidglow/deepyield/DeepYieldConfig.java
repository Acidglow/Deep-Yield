package dk.acidglow.deepyield;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DeepYieldConfig {
    private static final int DEFAULT_PLUS_1 = 50;
    private static final int DEFAULT_PLUS_2 = 25;
    private static final int DEFAULT_PLUS_3 = 15;
    private static final int DEFAULT_PLUS_4 = 7;
    private static final int DEFAULT_PLUS_5 = 3;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BONUS_CHANCE;
    public static final ModConfigSpec.IntValue BONUS_WEIGHT_PLUS_1;
    public static final ModConfigSpec.IntValue BONUS_WEIGHT_PLUS_2;
    public static final ModConfigSpec.IntValue BONUS_WEIGHT_PLUS_3;
    public static final ModConfigSpec.IntValue BONUS_WEIGHT_PLUS_4;
    public static final ModConfigSpec.IntValue BONUS_WEIGHT_PLUS_5;
    public static final ModConfigSpec.BooleanValue AFFECT_FORTUNE;
    public static final ModConfigSpec.BooleanValue AFFECT_SILK_TOUCH;
    public static final ModConfigSpec.BooleanValue ALLOW_CREATIVE_PLACED_ORES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ORE_BLACKLIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Deep Yield server gameplay settings").push("deep_yield");

        BONUS_CHANCE = builder
                .comment("Chance for an eligible block break to activate Deep Yield.")
                .defineInRange("bonusChance", 0.20D, 0.0D, 1.0D);
        BONUS_WEIGHT_PLUS_1 = builder
                .comment("Weight for one additional copy (x2 total).")
                .defineInRange("bonusWeightPlus1", DEFAULT_PLUS_1, 0, Integer.MAX_VALUE);
        BONUS_WEIGHT_PLUS_2 = builder
                .comment("Weight for two additional copies (x3 total).")
                .defineInRange("bonusWeightPlus2", DEFAULT_PLUS_2, 0, Integer.MAX_VALUE);
        BONUS_WEIGHT_PLUS_3 = builder
                .comment("Weight for three additional copies (x4 total).")
                .defineInRange("bonusWeightPlus3", DEFAULT_PLUS_3, 0, Integer.MAX_VALUE);
        BONUS_WEIGHT_PLUS_4 = builder
                .comment("Weight for four additional copies (x5 total).")
                .defineInRange("bonusWeightPlus4", DEFAULT_PLUS_4, 0, Integer.MAX_VALUE);
        BONUS_WEIGHT_PLUS_5 = builder
                .comment("Weight for five additional copies (x6 total).")
                .defineInRange("bonusWeightPlus5", DEFAULT_PLUS_5, 0, Integer.MAX_VALUE);
        AFFECT_FORTUNE = builder
                .comment("If false, Deep Yield skips tools containing Fortune.")
                .define("affectFortune", true);
        AFFECT_SILK_TOUCH = builder
                .comment("If false, Deep Yield skips tools containing Silk Touch.")
                .define("affectSilkTouch", true);
        ALLOW_CREATIVE_PLACED_ORES = builder
                .comment("If true, ores placed while a player is in Creative mode may receive Deep Yield.")
                .define("allowCreativePlacedOres", true);
        ORE_BLACKLIST = builder
                .comment("Block IDs excluded from Deep Yield, even when eligible by tag.")
                .defineList("oreBlacklist", List.of(), () -> "", value -> value instanceof String string
                        && net.minecraft.resources.Identifier.tryParse(string) != null);

        builder.pop();
        SPEC = builder.build();
    }

    private DeepYieldConfig() {
    }
}
