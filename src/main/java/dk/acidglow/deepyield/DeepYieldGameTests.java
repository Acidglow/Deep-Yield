package dk.acidglow.deepyield;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Server-side integration tests for the block-drops event path. */
final class DeepYieldGameTests {
    private static final BlockPos ORE_POSITION = new BlockPos(1, 2, 1);
    private static final BlockPos SUPPORT_POSITION = ORE_POSITION.below();
    private static final double DROP_RADIUS = 2.0D;

    private DeepYieldGameTests() {
    }

    static void register(RegisterGameTestsEvent event) {
        register(event, "natural_ore_bonus", DeepYieldGameTests::naturalOreGetsFinalLootBonus);
        register(event, "survival_placed_ore_is_skipped", DeepYieldGameTests::survivalPlacedOreIsSkipped);
        register(event, "creative_placed_ore_is_allowed", DeepYieldGameTests::creativePlacedOreIsAllowed);
        register(event, "creative_placed_ore_is_skipped_when_disabled",
                DeepYieldGameTests::creativePlacedOreIsSkippedWhenDisabled);
    }

    private static void register(RegisterGameTestsEvent event, String name, Consumer<GameTestHelper> test) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                Holder.direct(new TestEnvironmentDefinition.AllOf()),
                Identifier.withDefaultNamespace("empty"),
                20,
                1,
                true);
        event.registerTest(Identifier.fromNamespaceAndPath(DeepYield.MODID, name), new DirectGameTest(data, test));
    }

    private static void naturalOreGetsFinalLootBonus(GameTestHelper helper) {
        configureDeterministicBonus();
        Player player = miningPlayer(helper, GameType.SURVIVAL);
        helper.setBlock(ORE_POSITION, Blocks.DEEPSLATE_DIAMOND_ORE);
        breakOre(helper, player);
        helper.runAfterDelay(1, () -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, ORE_POSITION, DROP_RADIUS, 2);
            helper.succeed();
        });
    }

    private static void survivalPlacedOreIsSkipped(GameTestHelper helper) {
        configureDeterministicBonus();
        Player player = miningPlayer(helper, GameType.SURVIVAL);
        placeOre(helper, player);
        breakOre(helper, player);
        helper.runAfterDelay(1, () -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, ORE_POSITION, DROP_RADIUS, 1);
            helper.succeed();
        });
    }

    private static void creativePlacedOreIsAllowed(GameTestHelper helper) {
        configureDeterministicBonus();
        DeepYieldConfig.ALLOW_CREATIVE_PLACED_ORES.set(true);
        Player creativePlayer = miningPlayer(helper, GameType.CREATIVE);
        placeOre(helper, creativePlayer);

        Player survivalMiner = miningPlayer(helper, GameType.SURVIVAL);
        breakOre(helper, survivalMiner);
        helper.runAfterDelay(1, () -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, ORE_POSITION, DROP_RADIUS, 2);
            helper.succeed();
        });
    }

    private static void creativePlacedOreIsSkippedWhenDisabled(GameTestHelper helper) {
        configureDeterministicBonus();
        DeepYieldConfig.ALLOW_CREATIVE_PLACED_ORES.set(false);
        Player creativePlayer = miningPlayer(helper, GameType.CREATIVE);
        placeOre(helper, creativePlayer);

        Player survivalMiner = miningPlayer(helper, GameType.SURVIVAL);
        breakOre(helper, survivalMiner);
        helper.runAfterDelay(1, () -> {
            helper.assertItemEntityCountIs(Items.DIAMOND, ORE_POSITION, DROP_RADIUS, 1);
            helper.succeed();
        });
    }

    private static void configureDeterministicBonus() {
        DeepYieldConfig.BONUS_CHANCE.set(1.0D);
        DeepYieldConfig.BONUS_WEIGHT_PLUS_1.set(1);
        DeepYieldConfig.BONUS_WEIGHT_PLUS_2.set(0);
        DeepYieldConfig.BONUS_WEIGHT_PLUS_3.set(0);
        DeepYieldConfig.BONUS_WEIGHT_PLUS_4.set(0);
        DeepYieldConfig.BONUS_WEIGHT_PLUS_5.set(0);
    }

    private static Player miningPlayer(GameTestHelper helper, GameType gameType) {
        Player player = helper.makeMockPlayer(gameType);
        gameType.updatePlayerAbilities(player.getAbilities());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        return player;
    }

    private static void placeOre(GameTestHelper helper, Player player) {
        helper.setBlock(SUPPORT_POSITION, Blocks.STONE);
        ItemStack oreStack = new ItemStack(Blocks.DEEPSLATE_DIAMOND_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND, oreStack);
        helper.placeAt(player, oreStack, SUPPORT_POSITION, net.minecraft.core.Direction.UP);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertBlockPresent(Blocks.DEEPSLATE_DIAMOND_ORE, ORE_POSITION);
    }

    private static void breakOre(GameTestHelper helper, Player player) {
        BlockPos absolutePosition = helper.absolutePos(ORE_POSITION);
        BlockState state = helper.getLevel().getBlockState(absolutePosition);
        helper.getLevel().destroyBlock(absolutePosition, true, player);
        if (!state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            throw helper.assertionException("GameTest did not start with a deepslate diamond ore");
        }
    }

    private static final class DirectGameTest extends GameTestInstance {
        private final Consumer<GameTestHelper> test;

        private DirectGameTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, Consumer<GameTestHelper> test) {
            super(data);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }

        @Override
        public MapCodec<DirectGameTest> codec() {
            return MapCodec.unit(this);
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent typeDescription() {
            return net.minecraft.network.chat.Component.literal("Deep Yield integration");
        }
    }
}
