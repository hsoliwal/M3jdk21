/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** JDK differential and aligned-state proofs for growing/tombstone rehash. */
final class M3LongHashSetStagedRehashTest {
    @Test void randomizedOperationsPreserveKeysAndStateLanes() throws Exception {
        for (int trial = 0; trial < 5; trial++) {
            Random random = new Random(0x705A34L + trial);
            M3LongHashSet actual = new M3LongHashSet();
            Set<Long> model = new HashSet<>();
            for (int step = 0; step < 12000; step++) {
                long key = switch (step % 29) {
                    case 0 -> 0L;
                    case 1 -> Long.MIN_VALUE;
                    case 2 -> Long.MAX_VALUE;
                    default -> random.nextLong() % 919L;
                };
                int op = random.nextInt(12);
                if (op < 5) {
                    assertEquals(model.add(key), actual.add(key));
                } else if (op < 9) {
                    assertEquals(model.remove(key), actual.remove(key));
                } else if (op == 11) {
                    actual.clear();
                    model.clear();
                } else {
                    assertEquals(model.contains(key), actual.contains(key));
                }
                assertEquals(model.size(), actual.size());
                if ((step & 127) == 0) {
                    assertEquals(model, asSet(actual.toArray()));
                    assertAligned(actual);
                }
            }
            assertEquals(model, asSet(actual.toArray()));
            assertAligned(actual);
        }
    }

    @Test void tombstoneRebuildPreservesZeroExtremesAndIteratorEpoch() throws Exception {
        M3LongHashSet set = new M3LongHashSet();
        Set<Long> model = new HashSet<>();
        for (long value : new long[] {0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L, 19L, 37L}) {
            assertEquals(model.add(value), set.add(value));
        }
        for (int i = 0; i < 2000; i++) {
            assertEquals(model.add(100000L + i), set.add(100000L + i));
        }
        var iterator = set.iterator();
        for (int i = 0; i < 1800; i++) {
            assertEquals(model.remove(100000L + i), set.remove(100000L + i));
        }
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertEquals(model, asSet(set.toArray()));
        assertAligned(set);
        for (long x : new long[] {0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L, 19L, 37L}) {
            assertEquals(model.remove(x), set.remove(x));
        }
        for (int i = 1800; i < 2000; i++) {
            assertEquals(model.remove(100000L + i), set.remove(100000L + i));
        }
        assertTrue(set.isEmpty());
        assertAligned(set);
    }

    private static Set<Long> asSet(long[] values) {
        Set<Long> result = new HashSet<>();
        for (long value : values) assertTrue(result.add(value), "duplicate key");
        return result;
    }

    private static void assertAligned(M3LongHashSet set) throws Exception {
        Field keyField = M3LongHashSet.class.getDeclaredField("keys");
        Field stateField = M3LongHashSet.class.getDeclaredField("states");
        Field countField = M3LongHashSet.class.getDeclaredField("occupied");
        keyField.setAccessible(true);
        stateField.setAccessible(true);
        countField.setAccessible(true);
        long[] keys = (long[]) keyField.get(set);
        byte[] states = (byte[]) stateField.get(set);
        assertEquals(keys.length, states.length);
        int used = 0, occupied = 0;
        for (int i = 0; i < states.length; i++) {
            assertTrue(states[i] >= 0 && states[i] <= 2);
            if (states[i] == 1) used++;
            if (states[i] != 0) occupied++;
            if (states[i] != 1) assertEquals(0L, keys[i], "retired payload");
        }
        assertEquals(used, set.size());
        assertEquals(occupied, countField.getInt(set));
    }
}
