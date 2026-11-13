/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.eclipse.impl.set.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contract oracle for clearing the existing array-owned UnifiedSet. */
final class UnifiedSetBulkClearTest {

    private static final class Collision {
        final int id;
        Collision(int id) {
            this.id = id;
        }
        @Override
        public int hashCode() {
            return 13;
        }
        @Override
        public boolean equals(Object value) {
            return value instanceof Collision && ((Collision) value).id == this.id;
        }
    }

    @Test
    void collisionAndNullPayloadsAreDroppedBeforeReuse() {
        UnifiedSet<Collision> actual = UnifiedSet.newSet();
        Set<Collision> reference = new HashSet<>();
        actual.add(null);
        reference.add(null);
        for (int i = 0; i < 100; i++) {
            Collision entry = new Collision(i);
            assertEquals(reference.add(entry), actual.add(entry));
        }
        assertEquals(reference.size(), actual.size());
        actual.clear();
        reference.clear();
        assertEquals(0, actual.size());
        assertFalse(actual.contains(null));
        for (int i = 0; i < 100; i++) {
            assertFalse(actual.contains(new Collision(i)));
            Collision candidate = new Collision(i);
            assertEquals(reference.add(candidate), actual.add(candidate));
        }
        assertEquals(reference.size(), actual.size());
        for (Collision candidate : reference) {
            assertTrue(actual.contains(candidate));
        }
    }

    @Test
    void seededAlternatingClearAndMutationPreservesSetContract() {
        for (int seed = 0; seed < 16; seed++) {
            UnifiedSet<Integer> actual = UnifiedSet.newSet();
            Set<Integer> reference = new HashSet<>();
            Random random = new Random(0x554e49464945444cL ^ seed);
            for (int step = 0; step < 3_000; step++) {
                Integer value = random.nextInt(701) - 350;
                switch (random.nextInt(7)) {
                    case 0:
                        actual.clear();
                        reference.clear();
                        break;
                    case 1:
                        assertEquals(reference.remove(value), actual.remove(value));
                        break;
                    default:
                        assertEquals(reference.add(value), actual.add(value));
                        break;
                }
                assertEquals(reference.size(), actual.size());
                assertEquals(reference.contains(value), actual.contains(value));
                if ((step & 63) == 0) {
                    for (Integer candidate : reference) {
                        assertTrue(actual.contains(candidate));
                    }
                }
            }
        }
    }
}
