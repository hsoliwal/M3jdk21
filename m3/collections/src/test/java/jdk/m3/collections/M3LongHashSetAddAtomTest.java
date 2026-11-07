/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Independent add(long) oracle for the source-sealed LC705 single-probe atom. */
final class M3LongHashSetAddAtomTest {
    private static final int TRIALS = 4;
    private static final int OPERATIONS_PER_TRIAL = 32_768;
    private static final int SNAPSHOT_INTERVAL_MASK = 255;
    private static final long SEED = 0x705A3410L;

    @Test
    void singleProbeAddMaintainsJdkMembershipUnderCollisionsAndTombstones() {
        for (int trial = 0; trial < TRIALS; trial++) {
            Random random = new Random(SEED + trial);
            LongHashSet actual = new LongHashSet();
            Set<Long> model = new HashSet<>();
            for (int step = 0; step < OPERATIONS_PER_TRIAL; step++) {
                long key = switch (step % 19) {
                    case 0 -> 0L;
                    case 1 -> Long.MIN_VALUE;
                    case 2 -> Long.MAX_VALUE;
                    case 3 -> -1L;
                    default -> random.nextLong() % 4099L;
                };
                if ((step & 7) < 5) {
                    assertEquals(model.add(key), actual.add(key));
                } else if ((step & 7) == 5) {
                    assertEquals(model.remove(key), actual.remove(key));
                } else {
                    assertEquals(model.contains(key), actual.contains(key));
                }
                assertEquals(model.size(), actual.size());
                if ((step & SNAPSHOT_INTERVAL_MASK) == 0) {
                    Set<Long> observed = new HashSet<>();
                    for (long value : actual.toArray()) assertTrue(observed.add(value));
                    assertEquals(model, observed);
                }
            }
        }
    }

    @Test
    void duplicateInsertionDoesNotAdvanceIteratorButNewInsertionDoes() {
        LongHashSet actual = new LongHashSet();
        for (long key : new long[]{0L, Long.MIN_VALUE, Long.MAX_VALUE, 17L}) {
            assertTrue(actual.add(key));
            assertFalse(actual.add(key));
        }
        LongIterator before = actual.iterator();
        assertTrue(before.hasNext());
        assertFalse(actual.add(Long.MIN_VALUE));
        assertTrue(before.hasNext());
        assertTrue(actual.add(29L));
        assertThrows(ConcurrentModificationException.class, before::hasNext);
        assertEquals(5, actual.size());
    }

    @Test
    void absentInsertionAfterDeletionRetainsUniquenessAndSafeCapacity() {
        LongHashSet actual = new LongHashSet();
        Set<Long> model = new HashSet<>();
        for (long key = 1; key <= 2_048L; key++) {
            assertEquals(model.add(key), actual.add(key));
        }
        int beforeCapacity = actual.capacity();
        for (long key = 1; key <= 2_048L; key += 2) {
            assertEquals(model.remove(key), actual.remove(key));
        }
        for (long key = -1; key >= -2_048L; key--) {
            assertEquals(model.add(key), actual.add(key));
            assertFalse(actual.add(key));
        }
        assertTrue(actual.capacity() >= beforeCapacity);
        assertEquals(model.size(), actual.size());
        Set<Long> observed = new HashSet<>();
        for (long key : actual.toArray()) assertTrue(observed.add(key));
        assertEquals(model, observed);
    }
}
