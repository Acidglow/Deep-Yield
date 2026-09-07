package dk.acidglow.deepyield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class DeepYieldGameplay {
    private static final TagKey<Block> BONUS_ORES = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            Identifier.fromNamespaceAndPath(DeepYield.MODID, "bonus_ores"));
    private static final int[] DEFAULT_WEIGHTS = {50, 25, 15, 7, 3};
    private static boolean warnedInvalidWeights;

    private DeepYieldGameplay() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onBlockDrops);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player)) {
            return;
        }

        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        if (!isEligible(state)) {
            return;
        }

        ItemStack tool = event.getTool();
        boolean fortune = hasEnchantment(level, tool, Enchantments.FORTUNE);
        boolean silkTouch = hasEnchantment(level, tool, Enchantments.SILK_TOUCH);
        if ((fortune && !DeepYieldConfig.AFFECT_FORTUNE.get())
                || (silkTouch && !DeepYieldConfig.AFFECT_SILK_TOUCH.get())) {
            return;
        }

        double chance = DeepYieldConfig.BONUS_CHANCE.get();
        RandomSource random = level.getRandom();
        if (chance <= 0.0D || (chance < 1.0D && random.nextDouble() >= chance)) {
            return;
        }

        int additionalCopies = chooseAdditionalCopies(random);
        multiplyAndConsolidate(event.getDrops(), level, additionalCopies + 1);
    }

    private static boolean isEligible(BlockState state) {
        Block block = state.getBlock();
        if (!state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE) && !state.is(BONUS_ORES)) {
            return false;
        }

        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return true;
        }
        for (String configuredId : DeepYieldConfig.ORE_BLACKLIST.get()) {
            Identifier blacklistId = Identifier.tryParse(configuredId);
            if (blacklistId != null && blacklistId.equals(id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasEnchantment(ServerLevel level, ItemStack tool, ResourceKey<Enchantment> key) {
        if (tool.isEmpty()) {
            return false;
        }
        Registry<Enchantment> enchantments = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        Holder<Enchantment> holder = enchantments.get(key).orElse(null);
        return holder != null && tool.getEnchantmentLevel(holder) > 0;
    }

    private static int chooseAdditionalCopies(RandomSource random) {
        int[] weights = {
                DeepYieldConfig.BONUS_WEIGHT_PLUS_1.get(),
                DeepYieldConfig.BONUS_WEIGHT_PLUS_2.get(),
                DeepYieldConfig.BONUS_WEIGHT_PLUS_3.get(),
                DeepYieldConfig.BONUS_WEIGHT_PLUS_4.get(),
                DeepYieldConfig.BONUS_WEIGHT_PLUS_5.get()
        };
        long total = 0L;
        for (int weight : weights) {
            total += weight;
        }
        if (total <= 0L) {
            if (!warnedInvalidWeights) {
                warnedInvalidWeights = true;
                DeepYield.LOGGER.warn("Deep Yield bonus weights are unusable; using the default weight table.");
            }
            weights = DEFAULT_WEIGHTS;
            total = 100L;
        }

        long roll = Math.floorMod(random.nextLong(), total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0L) {
                return i + 1;
            }
        }
        return 1;
    }

    private static void multiplyAndConsolidate(List<ItemEntity> drops, ServerLevel level, int multiplier) {
        if (drops.isEmpty()) {
            return;
        }

        Map<ItemStackKey, ConsolidatedDrop> consolidated = new HashMap<>();
        for (ItemEntity entity : drops) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            long amount = (long) stack.getCount() * multiplier;
            ConsolidatedDrop existing = consolidated.get(new ItemStackKey(stack));
            if (existing == null) {
                consolidated.put(new ItemStackKey(stack), new ConsolidatedDrop(entity, stack.copy(), amount));
            } else {
                existing.amount += amount;
            }
        }

        if (consolidated.isEmpty()) {
            return;
        }

        List<ItemEntity> rebuilt = new ArrayList<>(consolidated.size());
        for (ConsolidatedDrop drop : consolidated.values()) {
            ItemEntity template = drop.template;
            long remaining = drop.amount;
            boolean first = true;
            while (remaining > 0L) {
                int count = (int) Math.min(remaining, drop.stack.getMaxStackSize());
                ItemStack result = drop.stack.copyWithCount(count);
                ItemEntity entity;
                if (first) {
                    entity = template;
                    entity.setItem(result);
                    first = false;
                } else {
                    entity = new ItemEntity(level, template.getX(), template.getY(), template.getZ(), result,
                            template.getDeltaMovement().x, template.getDeltaMovement().y, template.getDeltaMovement().z);
                }
                rebuilt.add(entity);
                remaining -= count;
            }
        }
        drops.clear();
        drops.addAll(rebuilt);
    }

    private record ItemStackKey(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentMap components) {
        private ItemStackKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponents());
        }
    }

    private static final class ConsolidatedDrop {
        private final ItemEntity template;
        private final ItemStack stack;
        private long amount;

        private ConsolidatedDrop(ItemEntity template, ItemStack stack, long amount) {
            this.template = template;
            this.stack = stack;
            this.amount = amount;
        }
    }
}
