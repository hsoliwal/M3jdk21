// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Fail-closed, low-heap growth oracle; never treats OOME as a successful add. */
public final class GrowFailureOracle {
    private static final String[] LANES = {
            "keys", "multiplicities", "subtreeCounts", "left", "right", "heights"
    };
    private GrowFailureOracle() { }

    private static Field field(String name) throws ReflectiveOperationException {
        Field value = LongOrderStatisticMultiset.class.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }

    private static Object stretched(Object old, int capacity) {
        if (old instanceof long[] values) return Arrays.copyOf(values, capacity);
        if (old instanceof int[] values) return Arrays.copyOf(values, capacity);
        return Arrays.copyOf((byte[]) old, capacity);
    }

    private static int length(Object array) {
        return array instanceof long[] values ? values.length
                : array instanceof int[] values ? values.length : ((byte[]) array).length;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("capacity required");
        int capacity = Integer.parseInt(args[0]);
        if (capacity < 17) throw new IllegalArgumentException("capacity below minimum");
        LongOrderStatisticMultiset set = new LongOrderStatisticMultiset();
        set.add(7L);
        Object[] before = new Object[LANES.length];
        for (int i = 0; i < LANES.length; i++) {
            Field lane = field(LANES[i]);
            Object expanded = stretched(lane.get(set), capacity);
            lane.set(set, expanded);
            before[i] = expanded;
        }
        // An isolated growth trigger with one valid pre-existing AVL entry.
        field("nextNode").setInt(set, capacity);
        boolean outOfMemory = false;
        try {
            set.add(11L);
        } catch (OutOfMemoryError failure) {
            outOfMemory = true;
        }
        int expectedLength = -1;
        for (int i = 0; i < LANES.length; i++) {
            Object current = field(LANES[i]).get(set);
            int currentLength = length(current);
            if (expectedLength < 0) expectedLength = currentLength;
            if (currentLength != expectedLength)
                throw new AssertionError("partially published growth: " + LANES[i]
                        + " length=" + currentLength + " vs first=" + expectedLength);
            if (outOfMemory && current != before[i])
                throw new AssertionError("growth failed but replaced live lane " + LANES[i]);
        }
        if (outOfMemory) {
            if (set.size() != 1L || set.distinctSize() != 1 || set.count(7L) != 1L
                    || set.count(11L) != 0L || set.select(0L) != 7L)
                throw new AssertionError("allocation failure changed committed AVL state");
        } else {
            if (set.size() != 2L || set.distinctSize() != 2 || set.count(7L) != 1L
                    || set.count(11L) != 1L || set.rank(11L) != 1L
                    || set.select(1L) != 11L)
                throw new AssertionError("successful array expansion lost its new key");
        }
        System.out.println("PASS Java21 OOME atomic AVL growth: "
                + (outOfMemory ? "OOME_ROLLBACK" : "GROWTH_COMMIT")
                + " capacity=" + capacity + " laneLength=" + expectedLength);
    }
}
