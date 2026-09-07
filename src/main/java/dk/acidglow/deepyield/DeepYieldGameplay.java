package dk.acidglow.deepyield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class DeepYieldGameplay {
    private static final TagKey<Block> BONUS_ORES = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            Identifier.fromNamespaceAndPath(DeepYield.MODID, "bonus_ores"));
    private static final int[] DEFAULT_WEIGHTS = {50, 25, 15, 7, 3};
    public static final AttachmentType<PlacedOrePositions> PLACED_ORES = AttachmentType
            .builder(PlacedOrePositions::new)
            .serialize(PlacedOrePositions.CODEC)
            .build();
    private static final Set<BlockDropsEvent> PROCESSED_EVENTS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<ServerLevel, Map<BlockPosKey, BlockState>> PENDING_CLEANUP =
            new IdentityHashMap<>();
    private static final Map<PistonKey, List<PistonMove>> PENDING_PISTON_MOVES = new HashMap<>();
    private static boolean warnedInvalidWeights;

    private DeepYieldGameplay() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onFluidPlace);
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onPistonPre);
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onPistonPost);
        NeoForge.EVENT_BUS.addListener(DeepYieldGameplay::onLevelTick);
    }

    public static void registerAttachments(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                helper -> helper.register(Identifier.fromNamespaceAndPath(DeepYield.MODID, "placed_ores"), PLACED_ORES));
    }

    private static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isOreCandidate(event.getPlacedBlock())) {
            return;
        }
        markPlaced(level, event.getPos());
    }

    private static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isOreCandidate(event.getNewState())) {
            return;
        }
        markPlaced(level, event.getPos());
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!PROCESSED_EVENTS.add(event)) {
            return;
        }
        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        boolean placed = isMarkedPlaced(level, event.getPos());
        if (placed) {
            queueCleanup(level, event.getPos(), state);
        }
        if (!(event.getBreaker() instanceof Player) || !isEligible(state) || placed) {
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

    private static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }
        List<PistonMove> moves = new ArrayList<>();
        for (BlockPos source : resolver.getToPush()) {
            if (isMarkedPlaced(level, source)) {
                moves.add(new PistonMove(source.immutable(), movedPosition(source, event)));
            }
        }
        for (BlockPos source : resolver.getToDestroy()) {
            if (isMarkedPlaced(level, source)) {
                moves.add(new PistonMove(source.immutable(), null));
            }
        }
        if (!moves.isEmpty()) {
            PENDING_PISTON_MOVES.put(new PistonKey(level, event.getPos().immutable(), event.getDirection(),
                    event.getPistonMoveType()), moves);
        }
    }

    private static void onPistonPost(PistonEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<PistonMove> moves = PENDING_PISTON_MOVES.remove(
                new PistonKey(level, event.getPos().immutable(), event.getDirection(), event.getPistonMoveType()));
        if (moves == null) {
            return;
        }
        for (PistonMove move : moves) {
            removePlaced(level, move.source());
        }
        for (PistonMove move : moves) {
            if (move.destination() != null) {
                markPlaced(level, move.destination());
            }
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPosKey, BlockState> cleanup = PENDING_CLEANUP.get(level);
        if (cleanup == null) {
            return;
        }
        var iterator = cleanup.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey().pos();
            if (!level.getBlockState(pos).equals(entry.getValue())) {
                removePlaced(level, pos);
                iterator.remove();
            }
        }
        if (cleanup.isEmpty()) {
            PENDING_CLEANUP.remove(level);
        }
    }

    private static BlockPos movedPosition(BlockPos source, PistonEvent event) {
        var direction = event.getPistonMoveType().isExtend
                ? event.getDirection()
                : event.getDirection().getOpposite();
        return source.relative(direction).immutable();
    }

    private static void markPlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedOrePositions placed = chunk.getData(PLACED_ORES);
        if (placed.add(pos)) {
            chunk.markUnsaved();
        }
    }

    private static boolean isMarkedPlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedOrePositions placed = chunk.getExistingDataOrNull(PLACED_ORES);
        return placed != null && placed.contains(pos);
    }

    private static void removePlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedOrePositions placed = chunk.getExistingDataOrNull(PLACED_ORES);
        if (placed == null || !placed.remove(pos)) {
            return;
        }
        if (placed.isEmpty()) {
            chunk.removeData(PLACED_ORES);
        }
        chunk.markUnsaved();
    }

    private static void queueCleanup(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) {
            removePlaced(level, pos);
            return;
        }
        PENDING_CLEANUP.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(new BlockPosKey(pos.immutable()), state);
    }

    private static boolean isOreCandidate(BlockState state) {
        return state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE) || state.is(BONUS_ORES);
    }

    private static boolean isEligible(BlockState state) {
        Block block = state.getBlock();
        if (!isOreCandidate(state)) {
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

    private record BlockPosKey(BlockPos pos) {
    }

    private record PistonKey(ServerLevel level, BlockPos pos, net.minecraft.core.Direction direction,
                             PistonEvent.PistonMoveType moveType) {
    }

    private record PistonMove(BlockPos source, BlockPos destination) {
    }
}
