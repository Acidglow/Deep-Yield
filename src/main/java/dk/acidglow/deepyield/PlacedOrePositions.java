package dk.acidglow.deepyield;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

public final class PlacedOrePositions {
    public static final MapCodec<PlacedOrePositions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("positions", java.util.List.of())
                    .forGetter(value -> value.survivalPositions.stream().toList()),
            Codec.LONG.listOf().optionalFieldOf("creative_positions", java.util.List.of())
                    .forGetter(value -> value.creativePositions.stream().toList()))
            .apply(instance, PlacedOrePositions::new));

    public enum Provenance {
        SURVIVAL_PLACED,
        CREATIVE_PLACED
    }

    private final Set<Long> survivalPositions;
    private final Set<Long> creativePositions;

    public PlacedOrePositions() {
        this(Set.of(), Set.of());
    }

    private PlacedOrePositions(Collection<Long> survivalPositions, Collection<Long> creativePositions) {
        this.survivalPositions = new HashSet<>(survivalPositions);
        this.creativePositions = new HashSet<>(creativePositions);
    }

    public boolean contains(BlockPos pos) {
        return provenance(pos) != null;
    }

    public boolean add(BlockPos pos) {
        return mark(pos, Provenance.SURVIVAL_PLACED);
    }

    public boolean mark(BlockPos pos, Provenance provenance) {
        long packedPosition = pos.asLong();
        if (provenance == provenance(pos)) {
            return false;
        }
        survivalPositions.remove(packedPosition);
        creativePositions.remove(packedPosition);
        return positionsFor(provenance).add(packedPosition);
    }

    public Provenance provenance(BlockPos pos) {
        long packedPosition = pos.asLong();
        if (survivalPositions.contains(packedPosition)) {
            return Provenance.SURVIVAL_PLACED;
        }
        return creativePositions.contains(packedPosition) ? Provenance.CREATIVE_PLACED : null;
    }

    public boolean remove(BlockPos pos) {
        long packedPosition = pos.asLong();
        return survivalPositions.remove(packedPosition) | creativePositions.remove(packedPosition);
    }

    public boolean isEmpty() {
        return survivalPositions.isEmpty() && creativePositions.isEmpty();
    }

    private Set<Long> positionsFor(Provenance provenance) {
        return provenance == Provenance.CREATIVE_PLACED ? creativePositions : survivalPositions;
    }
}
