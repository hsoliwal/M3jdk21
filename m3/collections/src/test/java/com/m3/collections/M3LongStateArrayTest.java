// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Differential contract proof for the primitive value/state dynamic-array owner. */
final class M3LongStateArrayTest {
    @Test void randomizedMutationsKeepStateFlagsAligned() {
        for (int seed = 0; seed < 48; seed++) {
            Random random = new Random(0x4d335354415445L ^ seed);
            var actual = new M3LongStateArray(seed % 7);
            var values = new ArrayList<Long>();
            var states = new ArrayList<Integer>();
            for (int step = 0; step < 500; step++) {
                long value = switch (step % 19) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(17) - 8;
                };
                int state = random.nextInt();
                int operation = values.size() > 110 ? 5 : random.nextInt(9);
                switch (operation) {
                    case 0 -> { values.add(value); states.add(state); actual.add(value, state); }
                    case 1 -> {
                        int at = random.nextInt(values.size() + 1);
                        values.add(at, value); states.add(at, state);
                        actual.insert(at, value, state);
                    }
                    case 2 -> {
                        if (!values.isEmpty()) {
                            int at = random.nextInt(values.size());
                            assertEquals(values.remove(at).longValue(), actual.removeAt(at));
                            states.remove(at);
                        }
                    }
                    case 3 -> {
                        if (!values.isEmpty()) {
                            int at = random.nextInt(values.size());
                            assertEquals(values.set(at, value).longValue(), actual.set(at, value));
                        }
                    }
                    case 4 -> {
                        if (!values.isEmpty()) {
                            int at = random.nextInt(values.size());
                            assertEquals(states.set(at, state).intValue(), actual.setState(at, state));
                        }
                    }
                    case 5 -> { values.clear(); states.clear(); actual.clear(); }
                    case 6 -> {
                        if (!values.isEmpty()) {
                            int at = random.nextInt(values.size());
                            int updated = (states.get(at) | state) & ~(state >>> 1);
                            states.set(at, updated);
                            assertEquals(updated, actual.updateState(at, state, state >>> 1));
                        }
                    }
                    case 7 -> actual.ensureCapacity(values.size() + random.nextInt(25));
                    default -> {
                        int required = random.nextBoolean() ? 0 : 1 << random.nextInt(32);
                        int forbidden = random.nextBoolean() ? 0 : 1 << random.nextInt(32);
                        int removed = 0;
                        for (int at = values.size() - 1; at >= 0; at--) {
                            int bits = states.get(at);
                            if ((bits & required) != required || (bits & forbidden) != 0) {
                                values.remove(at);
                                states.remove(at);
                                removed++;
                            }
                        }
                        assertEquals(removed, actual.retainByStateMask(required, forbidden));
                    }
                }
                assertEquals(values.size(), actual.size());
                assertTrue(actual.capacity() >= actual.size());
                assertArrayEquals(values.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
                assertArrayEquals(states.stream().mapToInt(Integer::intValue).toArray(), actual.statesCopy());
                if ((step & 31) == 0) {
                    var snapshot = actual.snapshot();
                    int required = 1 << random.nextInt(32);
                    int forbidden = random.nextBoolean() ? 0 : 1 << random.nextInt(32);
                    var prepared = snapshot.prepare(required, forbidden);
                    var selected = new ArrayList<Long>();
                    int[] prefix = new int[values.size() + 1];
                    for (int i = 0; i < values.size(); i++) {
                        int bits = states.get(i);
                        if ((bits & required) == required && (bits & forbidden) == 0) {
                            selected.add(values.get(i));
                        }
                        prefix[i + 1] = selected.size();
                    }
                    assertArrayEquals(selected.stream().mapToLong(Long::longValue).toArray(),
                            prepared.toArray());
                    for (int i = 0; i <= values.size(); i++) {
                        assertEquals(prefix[i], prepared.rank(i));
                    }
                    int from = random.nextInt(values.size() + 1);
                    int to = from + random.nextInt(values.size() - from + 1);
                    var window = prepared.window(from, to);
                    assertEquals(prefix[to] - prefix[from], window.size());
                    assertEquals(prefix[to] - prefix[from], window.rank(to - from));
                }
            }
        }
    }

    @Test void stateMaskCompactionUsesOneMutationEpochAndPreservesAlignment() {
        var owner = new M3LongStateArray();
        owner.add(10, 1);
        owner.add(20, 0);
        owner.add(30, 1);
        owner.add(40, 0);
        long before = owner.generation();
        assertEquals(2, owner.retainByStateMask(1, 0));
        assertArrayEquals(new long[] {10, 30}, owner.toArray());
        assertArrayEquals(new int[] {1, 1}, owner.statesCopy());
        assertEquals(before + 1, owner.generation());
        long stable = owner.generation();
        assertEquals(0, owner.retainByStateMask(1, 0));
        assertEquals(stable, owner.generation());
        assertEquals(2, owner.retainByStateMask(2, 0));
        assertTrue(owner.isEmpty());
        assertEquals(stable + 1, owner.generation());
    }

    @Test void sortedAndFilteredViewsPreserveDuplicateOccurrences() {
        var owner = new M3LongStateArray();
        long[] values = {5, 1, 5, -2, 1};
        int[] states = {1, 0, 1, 1, 1};
        for (int i = 0; i < values.length; i++) owner.add(values[i], states[i]);
        var snapshot = owner.snapshot();
        var selected = snapshot.prepare(1, 0);
        assertArrayEquals(new long[] {5, 5, -2, 1}, selected.toArray());
        assertEquals(0, selected.sourceOrdinal(0));
        assertEquals(2, selected.sourceOrdinal(1));
        assertEquals(3, selected.sourceOrdinal(2));
        assertEquals(4, selected.sourceOrdinal(3));
        assertEquals(3, selected.rank(4));
        assertArrayEquals(new long[] {5, -2}, selected.window(1, 4).toArray());
        assertArrayEquals(new long[] {-2, 1, 5, 5}, selected.sortedAscending().toArray());
        assertArrayEquals(new int[] {3, 4, 0, 2}, selected.sortedAscending().physicalIndices());
        assertArrayEquals(new long[] {5, 5, 1, -2}, selected.sortedDescending().toArray());
        assertArrayEquals(new int[] {0, 2, 4, 3}, selected.sortedDescending().physicalIndices());
        assertArrayEquals(values, snapshot.prepare(0, 0).toArray());
        assertEquals(0, snapshot.prepare(-1, -1).size());
        assertEquals(0, snapshot.prepare(1, 1).size());
        owner.insert(0, Long.MIN_VALUE, Integer.MIN_VALUE);
        owner.removeAt(3);
        owner.clear();
        assertArrayEquals(values, snapshot.prepare(0, 0).toArray());
        assertArrayEquals(new long[] {5, 5, -2, 1}, selected.toArray());
    }

    @Test void fullWordMasksAndExistingNativeSelectorStayInParity() {
        var owner = new M3LongStateArray();
        owner.add(-1, 0x8000_0000);
        owner.add(Long.MIN_VALUE, 0x8000_0003);
        owner.add(Long.MAX_VALUE, 3);
        owner.add(0, -1);
        var snapshot = owner.snapshot();
        var javaResult = snapshot.prepare(0x8000_0000, 2, M3IndexPathMasks.Backend.JAVA);
        assertArrayEquals(new long[] {-1}, javaResult.toArray());
        if (M3IndexPathMasks.nativeAvailable()) {
            var nativeResult = snapshot.prepare(0x8000_0000, 2,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            assertArrayEquals(javaResult.toArray(), nativeResult.toArray());
            assertEquals(javaResult.sourceOrdinal(0), nativeResult.sourceOrdinal(0));
        } else {
            assertThrows(UnsatisfiedLinkError.class, () ->
                    snapshot.prepare(0x8000_0000, 2,
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
        assertArrayEquals(new long[]{-1, Long.MIN_VALUE, 0},
                snapshot.prepare(0x8000_0000, 0).toArray());
    }

    @Test void validationAndGenerationAreNonmutatingOnRefusal() {
        assertThrows(IllegalArgumentException.class, () -> new M3LongStateArray(-1));
        var owner = new M3LongStateArray();
        assertThrows(IndexOutOfBoundsException.class, () -> owner.insert(1, 10, 3));
        owner.add(10, 3);
        long generation = owner.generation();
        owner.set(0, 10); owner.setState(0, 3); owner.updateState(0, 0, 0);
        assertEquals(0, owner.retainByStateMask(0, 0));
        assertEquals(generation, owner.generation());
        assertThrows(IndexOutOfBoundsException.class, () -> owner.stateAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> owner.removeAt(1));
        assertThrows(NullPointerException.class, () -> owner.snapshot().prepare(0, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> owner.snapshot().prepare(3, 0).rank(2));
        assertThrows(IndexOutOfBoundsException.class, () -> owner.snapshot().prepare(3, 0).get(1));
        assertEquals(generation, owner.generation());
        assertArrayEquals(new long[]{10}, owner.toArray());
        assertArrayEquals(new int[]{3}, owner.statesCopy());
        owner.updateState(0, Integer.MIN_VALUE, 1);
        assertEquals(0x8000_0002, owner.stateAt(0));
        assertTrue(owner.generation() > generation);
        assertEquals(0x8000_0002, owner.setState(0, 3));
    }

    @Test void onlyOneValueOwnerAndOneAlignedMetadataLaneAreRetained() {
        var types = Arrays.stream(M3LongStateArray.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getType)
                .toList();
        assertEquals(3, types.size());
        assertEquals(Set.of(M3LongArrayList.class, int[].class, long.class), Set.copyOf(types));
    }
}
