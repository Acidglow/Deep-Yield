package dk.acidglow.deepyield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

class DeepYieldRulesTest {
    @Test
    void onlyPlusOneWeightAlwaysSelectsOneAdditionalCopy() {
        int[] weights = {1, 0, 0, 0, 0};

        for (long randomValue : new long[] {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE}) {
            assertEquals(1, DeepYieldRules.chooseAdditionalCopies(weights, randomValue));
        }
    }

    @Test
    void zeroWeightsFallBackToTheDocumentedDefaultTable() {
        int[] zeroWeights = {0, 0, 0, 0, 0};

        for (long randomValue : new long[] {0L, 49L, 50L, 74L, 99L}) {
            assertEquals(
                    DeepYieldRules.chooseAdditionalCopies(DeepYieldRules.DEFAULT_WEIGHTS, randomValue),
                    DeepYieldRules.chooseAdditionalCopies(zeroWeights, randomValue));
        }
    }

    @Test
    void nonHundredWeightsRemainProportional() {
        int[] weights = {100, 50, 20, 10, 5};

        assertEquals(1, DeepYieldRules.chooseAdditionalCopies(weights, 0L));
        assertEquals(1, DeepYieldRules.chooseAdditionalCopies(weights, 99L));
        assertEquals(2, DeepYieldRules.chooseAdditionalCopies(weights, 100L));
        assertEquals(2, DeepYieldRules.chooseAdditionalCopies(weights, 149L));
        assertEquals(3, DeepYieldRules.chooseAdditionalCopies(weights, 150L));
        assertEquals(4, DeepYieldRules.chooseAdditionalCopies(weights, 170L));
        assertEquals(5, DeepYieldRules.chooseAdditionalCopies(weights, 180L));
    }

    @Test
    void weightedSelectionNeverReturnsZeroAdditionalCopies() {
        int[] weights = {50, 25, 15, 7, 3};

        for (long randomValue = -1000L; randomValue < 1000L; randomValue++) {
            assertTrue(DeepYieldRules.chooseAdditionalCopies(weights, randomValue) >= 1);
        }
    }

    @Test
    void activationHonorsProbabilityBoundaries() {
        assertFalse(DeepYieldRules.shouldActivate(0.0D, 0.0D));
        assertFalse(DeepYieldRules.shouldActivate(0.0D, 0.999999D));
        assertTrue(DeepYieldRules.shouldActivate(1.0D, 0.0D));
        assertTrue(DeepYieldRules.shouldActivate(1.0D, 0.999999D));
        assertTrue(DeepYieldRules.shouldActivate(0.25D, 0.249999D));
        assertFalse(DeepYieldRules.shouldActivate(0.25D, 0.25D));
        assertFalse(DeepYieldRules.shouldActivate(0.25D, 0.75D));
    }

    @Test
    void intermediateActivationProbabilityMatchesConfiguredRange() {
        int activated = 0;
        for (int i = 0; i < 1000; i++) {
            activated += DeepYieldRules.shouldActivate(0.25D, (i + 0.5D) / 1000.0D) ? 1 : 0;
        }
        assertEquals(250, activated);
    }

    @Test
    void eligibilityRequiresAConfiguredOreTagAndBlacklistWins() {
        assertFalse(DeepYieldRules.isEligible(false, false, false));
        assertTrue(DeepYieldRules.isEligible(true, false, false));
        assertTrue(DeepYieldRules.isEligible(false, true, false));
        assertTrue(DeepYieldRules.isEligible(true, true, false));
        assertFalse(DeepYieldRules.isEligible(true, false, true));
        assertFalse(DeepYieldRules.isEligible(false, true, true));
    }

    @Test
    void disabledEnchantmentModesSkipOnlyTheAffectedToolType() {
        assertTrue(DeepYieldRules.shouldSkipEnchantedTool(true, false, false, true));
        assertFalse(DeepYieldRules.shouldSkipEnchantedTool(true, false, true, false));
        assertTrue(DeepYieldRules.shouldSkipEnchantedTool(false, true, true, false));
        assertFalse(DeepYieldRules.shouldSkipEnchantedTool(false, true, true, true));
        assertTrue(DeepYieldRules.shouldSkipEnchantedTool(true, true, false, true));
    }

    @Test
    void multiplicationCopiesCompleteStacksAndConsolidatesCompatibleDrops() {
        List<DeepYieldRules.LootDrop<DropKey>> result = DeepYieldRules.multiplyAndConsolidate(
                List.of(new DeepYieldRules.LootDrop<>(new DropKey("diamond", null), 1, 64),
                        new DeepYieldRules.LootDrop<>(new DropKey("diamond", null), 6, 64)),
                2);

        assertEquals(1, result.size());
        assertEquals(14, result.getFirst().count());
    }

    @Test
    void multiplicationSplitsOversizedResultsAtTheLegalStackSize() {
        List<DeepYieldRules.LootDrop<DropKey>> result = DeepYieldRules.multiplyAndConsolidate(
                List.of(new DeepYieldRules.LootDrop<>(new DropKey("diamond", null), 40, 64)),
                2);

        assertEquals(List.of(64, 16), result.stream().map(DeepYieldRules.LootDrop::count).toList());
    }

    @Test
    void incompatibleComponentsAreNotConsolidatedAndArePreserved() {
        DropKey plain = new DropKey("diamond", null);
        DropKey named = new DropKey("diamond", "special");

        List<DeepYieldRules.LootDrop<DropKey>> result = DeepYieldRules.multiplyAndConsolidate(
                List.of(new DeepYieldRules.LootDrop<>(plain, 2, 64),
                        new DeepYieldRules.LootDrop<>(named, 3, 64)),
                2);

        assertEquals(2, result.size());
        assertEquals(4, result.stream().filter(drop -> drop.key().equals(plain)).findFirst().orElseThrow().count());
        var namedResult = result.stream().filter(drop -> drop.key().equals(named)).findFirst().orElseThrow();
        assertEquals(6, namedResult.count());
        assertEquals("special", namedResult.key().component());
    }

    @Test
    void emptyDropsAreIgnoredWithoutCreatingUnrelatedItems() {
        List<DeepYieldRules.LootDrop<DropKey>> result = DeepYieldRules.multiplyAndConsolidate(
                List.of(new DeepYieldRules.LootDrop<>(new DropKey("empty", null), 0, 64),
                        new DeepYieldRules.LootDrop<>(new DropKey("redstone", null), 6, 64)),
                2);

        assertEquals(1, result.size());
        assertEquals(new DropKey("redstone", null), result.getFirst().key());
        assertEquals(12, result.getFirst().count());
    }

    @Test
    void codecRoundTripPreservesPlacedPositions() {
        PlacedOrePositions positions = new PlacedOrePositions();
        positions.add(new net.minecraft.core.BlockPos(1, 2, 3));
        positions.add(new net.minecraft.core.BlockPos(-4, 5, 6));

        var encoded = PlacedOrePositions.CODEC.codec().encodeStart(JsonOps.INSTANCE, positions).getOrThrow();
        PlacedOrePositions decoded = PlacedOrePositions.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.contains(new net.minecraft.core.BlockPos(1, 2, 3)));
        assertTrue(decoded.contains(new net.minecraft.core.BlockPos(-4, 5, 6)));
        assertFalse(decoded.contains(new net.minecraft.core.BlockPos(0, 0, 0)));
    }

    @Test
    void placedPositionRemovalIsIdempotent() {
        PlacedOrePositions positions = new PlacedOrePositions();
        var position = new net.minecraft.core.BlockPos(8, 9, 10);

        assertTrue(positions.add(position));
        assertFalse(positions.add(position));
        assertTrue(positions.remove(position));
        assertFalse(positions.remove(position));
        assertTrue(positions.isEmpty());
    }

    private record DropKey(String item, String component) {
    }
}
