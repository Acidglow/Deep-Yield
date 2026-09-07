package dk.acidglow.deepyield;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DeepYieldRules {
    static final int[] DEFAULT_WEIGHTS = {50, 25, 15, 7, 3};

    private DeepYieldRules() {
    }

    static boolean shouldActivate(double chance, double roll) {
        if (chance <= 0.0D) {
            return false;
        }
        return chance >= 1.0D || roll < chance;
    }

    static int chooseAdditionalCopies(int[] configuredWeights, long randomValue) {
        long total = 0L;
        for (int weight : configuredWeights) {
            total += weight;
        }
        int[] weights = total > 0L ? configuredWeights : DEFAULT_WEIGHTS;
        if (total <= 0L) {
            total = 100L;
        }

        long roll = Math.floorMod(randomValue, total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0L) {
                return i + 1;
            }
        }
        return 1;
    }

    static boolean isEligible(boolean deepslateOre, boolean bonusOre, boolean blacklisted) {
        return (deepslateOre || bonusOre) && !blacklisted;
    }

    static boolean shouldSkipEnchantedTool(
            boolean fortune, boolean silkTouch, boolean affectFortune, boolean affectSilkTouch) {
        return (fortune && !affectFortune) || (silkTouch && !affectSilkTouch);
    }

    static <K> List<LootDrop<K>> multiplyAndConsolidate(List<LootDrop<K>> drops, int multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be positive");
        }

        Map<K, Long> totals = new LinkedHashMap<>();
        Map<K, Integer> maximumStackSizes = new LinkedHashMap<>();
        for (LootDrop<K> drop : drops) {
            if (drop.count() <= 0) {
                continue;
            }
            totals.merge(drop.key(), (long) drop.count() * multiplier, Long::sum);
            maximumStackSizes.putIfAbsent(drop.key(), drop.maximumStackSize());
        }

        List<LootDrop<K>> result = new ArrayList<>();
        for (Map.Entry<K, Long> entry : totals.entrySet()) {
            int maximumStackSize = maximumStackSizes.get(entry.getKey());
            long remaining = entry.getValue();
            while (remaining > 0L) {
                int count = (int) Math.min(remaining, maximumStackSize);
                result.add(new LootDrop<>(entry.getKey(), count, maximumStackSize));
                remaining -= count;
            }
        }
        return result;
    }

    record LootDrop<K>(K key, int count, int maximumStackSize) {
    }
}
