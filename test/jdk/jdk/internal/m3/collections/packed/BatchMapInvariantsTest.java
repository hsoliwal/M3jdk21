// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * checkInvariants with one sorted copy against the previous pairwise scan (kept below): the same
 * first exception and message for random sorted and pending lanes with overlaps and duplicates.
 */
//FLLDCIM: LEETCODIFY — differential proof for the batch-map invariant check
final class BatchMapInvariantsTest {

    /** The previous pending checks, verbatim apart from reading parameters. */
    static String reference(long[] sorted, int sortedSize, long[] pending, int pendingSize) {
        for (int i = 0; i < pendingSize; i++) {
            if (Arrays.binarySearch(sorted, 0, sortedSize, pending[i]) >= 0) {
                return "threw java.lang.IllegalStateException: pending/sorted overlap";
            }
            for (int j = i + 1; j < pendingSize; j++) {
                if (pending[i] == pending[j]) {
                    return "threw java.lang.IllegalStateException: pending duplicate";
                }
            }
        }
        return "ok";
    }

    static void set(Object owner, String name, Object value) throws ReflectiveOperationException {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(owner, value);
    }

    @Test
    void sameFirstException() throws ReflectiveOperationException {
        for (int seed = 0; seed < 20_000; seed++) {
            Random random = new Random(seed);
            int span = 2 + random.nextInt(40);
            long[] sorted = random.longs(random.nextInt(12), 0, span).sorted().distinct().toArray();
            long[] pending = random.longs(random.nextInt(14), 0, span).toArray();
            int pendingSize = pending.length == 0 ? 0 : random.nextInt(pending.length + 1);
            PackedLongLongBatchSortedMap map = new PackedLongLongBatchSortedMap(4, 4);
            set(map, "sortedKeys", sorted);
            set(map, "sortedValues", new long[sorted.length]);
            set(map, "sortedSize", sorted.length);
            set(map, "pendingKeys", pending);
            set(map, "pendingValues", new long[pending.length]);
            set(map, "pendingSize", pendingSize);
            String actual;
            try {
                map.checkInvariants();
                actual = "ok";
            } catch (IllegalStateException e) {
                actual = "threw " + e.getClass().getName() + ": " + e.getMessage();
            }
            assertEquals(reference(sorted, sorted.length, pending, pendingSize), actual,
                    "seed " + seed);
        }
    }
}
