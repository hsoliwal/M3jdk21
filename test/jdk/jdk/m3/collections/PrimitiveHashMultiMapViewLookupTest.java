// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Array;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * ValuesView.isEmpty() by head lookup and getBits(index) by walking first: against the chain itself
 * (size() and toArrayBits()) after random adds, entry removals, key removals and clears, for every
 * index from -2 to size + 1, including the exception class and message.
 */
//FLLDCIM: LEETCODIFY — differential proof for the ValuesView lookups
final class PrimitiveHashMultiMapViewLookupTest {

    private static String outcome(PrimitiveHashMultiMap.ValuesView view, int index) {
        try {
            return "value " + view.getBits(index);
        } catch (RuntimeException e) {
            return "threw " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static String expected(PrimitiveHashMultiMap.ValuesView view, PrimitiveKind kind,
            int index) {
        Object values = view.toArrayBits();
        int length = Array.getLength(values);
        if (index < 0 || index >= length) {
            return "threw java.lang.IndexOutOfBoundsException: Index " + index
                    + " out of bounds for length " + length;
        }
        return "value " + kind.readBits(values, index);
    }

    @Test
    void lookupsAgainstTheChain() {
        PrimitiveKind[] kinds = {PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.BYTE,
            PrimitiveKind.DOUBLE};
        for (int seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            PrimitiveKind valueKind = kinds[seed % kinds.length];
            PrimitiveHashMultiMap map = new PrimitiveHashMultiMap(PrimitiveKind.INT, valueKind,
                    1 + seed % 9, seed % 2 == 0);
            for (int step = 0; step < 400; step++) {
                long key = random.nextInt(6);
                long value = random.nextInt(9);
                switch (random.nextInt(8)) {
                    case 0, 1, 2, 3 -> map.addBits(key, value);
                    case 4, 5 -> map.removeEntryBits(key, value);
                    case 6 -> map.valuesViewBits(key).clear();
                    default -> {
                        if (random.nextInt(40) == 0) map.clear();
                    }
                }
                PrimitiveHashMultiMap.ValuesView view = map.valuesViewBits(random.nextInt(7));
                String at = "seed " + seed + " step " + step;
                assertEquals(view.size() == 0, view.isEmpty(), at + " isEmpty");
                for (int index = -2; index <= view.size() + 1; index++) {
                    assertEquals(expected(view, valueKind, index), outcome(view, index),
                            at + " getBits " + index);
                }
            }
        }
    }
}
