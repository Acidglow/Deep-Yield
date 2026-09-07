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
        markPlaced(level, event.getPos(), placementProvenance(event.getEntity()));
    }

    private static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isOreCandidate(event.getNewState())) {
            return;
        }
        markPlaced(level, event.getPos(), PlacedOrePositions.Provenance.SURVIVAL_PLACED);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!PROCESSED_EVENTS.add(event)) {
            return;
        }
        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        PlacedOrePositions.Provenance provenance = placedProvenance(level, event.getPos());
        if (provenance != null) {
            queueCleanup(level, event.getPos(), state);
        }
        if (!(event.getBreaker() instanceof Player) || !isEligible(state) || !allowsDeepYield(provenance)) {
            return;
        }

        ItemStack tool = event.getTool();
        boolean fortune = hasEnchantment(level, tool, Enchantments.FORTUNE);
        boolean silkTouch = hasEnchantment(level, tool, Enchantments.SILK_TOUCH);
        if (DeepYieldRules.shouldSkipEnchantedTool(
                fortune,
                silkTouch,
                DeepYieldConfig.AFFECT_FORTUNE.get(),
                DeepYieldConfig.AFFECT_SILK_TOUCH.get())) {
            return;
        }

        double chance = DeepYieldConfig.BONUS_CHANCE.get();
        RandomSource random = level.getRandom();
        double roll = chance > 0.0D && chance < 1.0D ? random.nextDouble() : 0.0D;
        if (!DeepYieldRules.shouldActivate(chance, roll)) {
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
            PlacedOrePositions.Provenance provenance = placedProvenance(level, source);
            if (provenance != null) {
                moves.add(new PistonMove(source.immutable(), movedPosition(source, event), provenance));
            }
        }
        for (BlockPos source : resolver.getToDestroy()) {
            PlacedOrePositions.Provenance provenance = placedProvenance(level, source);
            if (provenance != null) {
                moves.add(new PistonMove(source.immutable(), null, provenance));
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
                markPlaced(level, move.destination(), move.provenance());
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

    private static void markPlaced(ServerLevel level, BlockPos pos, PlacedOrePositions.Provenance provenance) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedOrePositions placed = chunk.getData(PLACED_ORES);
        if (placed.mark(pos, provenance)) {
            chunk.markUnsaved();
        }
    }

    private static PlacedOrePositions.Provenance placedProvenance(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedOrePositions placed = chunk.getExistingDataOrNull(PLACED_ORES);
        return placed == null ? null : placed.provenance(pos);
    }

    private static PlacedOrePositions.Provenance placementProvenance(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Player player && player.isCreative()
                ? PlacedOrePositions.Provenance.CREATIVE_PLACED
                : PlacedOrePositions.Provenance.SURVIVAL_PLACED;
    }

    private static boolean allowsDeepYield(PlacedOrePositions.Provenance provenance) {
        return provenance == null
                || provenance == PlacedOrePositions.Provenance.CREATIVE_PLACED
                && DeepYieldConfig.ALLOW_CREATIVE_PLACED_ORES.get();
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
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        boolean blacklisted = false;
        for (String configuredId : DeepYieldConfig.ORE_BLACKLIST.get()) {
            Identifier blacklistId = Identifier.tryParse(configuredId);
            if (blacklistId != null && blacklistId.equals(id)) {
                blacklisted = true;
                break;
            }
        }
        return DeepYieldRules.isEligible(
                state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE),
                state.is(BONUS_ORES),
                blacklisted);
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
        if (total <= 0L && !warnedInvalidWeights) {
            warnedInvalidWeights = true;
            DeepYield.LOGGER.warn("Deep Yield bonus weights are unusable; using the default weight table.");
        }
        return DeepYieldRules.chooseAdditionalCopies(weights, random.nextLong());
    }

    private static void multiplyAndConsolidate(List<ItemEntity> drops, ServerLevel level, int multiplier) {
        if (drops.isEmpty()) {
            return;
        }

        List<DeepYieldRules.LootDrop<ItemStackKey>> multipliedDrops = DeepYieldRules.multiplyAndConsolidate(
                drops.stream()
                        .map(entity -> new DeepYieldRules.LootDrop<>(
                                new ItemStackKey(entity.getItem()),
                                entity.getItem().getCount(),
                                entity.getItem().getMaxStackSize()))
                        .toList(),
                multiplier);
        if (multipliedDrops.isEmpty()) {
            return;
        }

        Map<ItemStackKey, ItemEntity> templates = new HashMap<>();
        for (ItemEntity entity : drops) {
            templates.putIfAbsent(new ItemStackKey(entity.getItem()), entity);
        }
        Map<ItemStackKey, Boolean> usedTemplates = new HashMap<>();
        List<ItemEntity> rebuilt = new ArrayList<>(multipliedDrops.size());
        for (DeepYieldRules.LootDrop<ItemStackKey> multipliedDrop : multipliedDrops) {
            ItemEntity template = templates.get(multipliedDrop.key());
            if (template == null) {
                continue;
            }
            ItemStack result = template.getItem().copyWithCount(multipliedDrop.count());
            ItemStackKey key = multipliedDrop.key();
            boolean reuse = usedTemplates.putIfAbsent(key, Boolean.TRUE) == null;
            ItemEntity entity = reuse
                    ? template
                    : new ItemEntity(level, template.getX(), template.getY(), template.getZ(), result,
                            template.getDeltaMovement().x, template.getDeltaMovement().y, template.getDeltaMovement().z);
            entity.setItem(result);
            rebuilt.add(entity);
        }
        drops.clear();
        drops.addAll(rebuilt);
    }

    private record ItemStackKey(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentMap components) {
        private ItemStackKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponents());
        }
    }

    private record BlockPosKey(BlockPos pos) {
    }

    private record PistonKey(ServerLevel level, BlockPos pos, net.minecraft.core.Direction direction,
                             PistonEvent.PistonMoveType moveType) {
    }

    private record PistonMove(BlockPos source, BlockPos destination, PlacedOrePositions.Provenance provenance) {
    }
}
