package dk.acidglow.deepyield;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;

public final class PlacedOrePositions {
    public static final MapCodec<PlacedOrePositions> CODEC = Codec.LONG.listOf()
            .xmap(PlacedOrePositions::new, value -> value.positions.stream().toList())
            .fieldOf("positions");

    private final Set<Long> positions;

    public PlacedOrePositions() {
        this.positions = new HashSet<>();
    }

    private PlacedOrePositions(Collection<Long> positions) {
        this.positions = new HashSet<>(positions);
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    public boolean add(BlockPos pos) {
        return positions.add(pos.asLong());
    }

    public boolean remove(BlockPos pos) {
        return positions.remove(pos.asLong());
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }
}
