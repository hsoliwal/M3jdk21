// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MIndexOpenAddressCompactionTest {
    @Test
    void sparseSetShiftDeletionPreservesCollidingProbeCluster() {
        IntegerSpace space = new IntegerSpace();
        List<Integer> colliding = collidingIds(64, 11, 24);
        MIndexSet<Integer> set = new MIndexSet<>(space, 32);
        colliding.forEach(set::add);

        int capacity = set.sparseCapacity();
        Set<Integer> removed = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            int id = colliding.get(index);
            removed.add(id);
            assertTrue(set.remove(id));
            assertEquals(capacity, set.sparseCapacity());
        }
        for (int id : colliding) {
            assertEquals(!removed.contains(id), set.contains(id));
        }

        set.compact();
        assertTrue(set.sparseCapacity() < capacity);
        for (int id : colliding.subList(20, colliding.size())) {
            assertTrue(set.contains(id));
        }
    }

    @Test
    void sparseMapShiftDeletionKeepsParallelValueLaneAligned() {
        IntegerSpace space = new IntegerSpace();
        List<Integer> colliding = collidingIds(64, 13, 24);
        MIndexMap<Integer, Integer> map =
                new MIndexMap<>(space, space, 32);

        for (int id : colliding) {
            map.put(id, id);
        }
        int capacity = map.sparseCapacity();

        for (int index = 0; index < 20; index++) {
            int id = colliding.get(index);
            assertTrue(map.remove(id));
            assertEquals(capacity, map.sparseCapacity());
            assertFalse(map.containsKey(id));
        }
        for (int id : colliding.subList(20, colliding.size())) {
            assertEquals(id, map.valueIdOrDefault(id, -1));
        }

        map.compact();
        assertTrue(map.sparseCapacity() < capacity);
        for (int id : colliding.subList(20, colliding.size())) {
            assertEquals(id, map.valueIdOrDefault(id, -1));
        }
    }

    private static List<Integer> collidingIds(
            int capacity, int bucket, int count) {
        ArrayList<Integer> result = new ArrayList<>(count);
        int mask = capacity - 1;
        for (int id = 0; result.size() < count; id++) {
            if ((IdSupport.mix(id) & mask) == bucket) {
                result.add(id);
            }
        }
        return result;
    }

    private static final class IntegerSpace
            implements MIndexSpace<Integer> {
        private final BitSet admitted = new BitSet();

        @Override
        public int id(Integer value) {
            if (value == null || value < 0 || value > IdSupport.MAX_ID) {
                throw new IllegalArgumentException("invalid integer ID");
            }
            admitted.set(value);
            return value;
        }

        @Override
        public int findId(Integer value) {
            return value != null
                            && value >= 0
                            && admitted.get(value)
                    ? value
                    : -1;
        }

        @Override
        public Integer value(int id) {
            requireId(id);
            return id;
        }

        @Override
        public int size() {
            return admitted.cardinality();
        }

        @Override
        public boolean containsId(int id) {
            return id >= 0 && admitted.get(id);
        }

        @Override
        public boolean javaEqualityCompatible() {
            return true;
        }
    }
}
