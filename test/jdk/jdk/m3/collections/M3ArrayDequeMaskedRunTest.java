/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-owned tests of the packed-mask survivor-run optimization. */
final class M3ArrayDequeMaskedRunTest {
    private record Key(int value) { }

    private static void check(int capacity, int rotation, int resident, boolean[] removed) {
        M3ArrayDeque<Key> deque = new M3ArrayDeque<>(capacity);
        List<Key> original = new ArrayList<>();
        Key shared = new Key(-1);
        for (int i = 0; i < capacity; i++) {
            Key value = i % 5 == 0 ? shared : new Key(i % 7);
            deque.addLast(value);
            original.add(value);
        }
        for (int i = 0; i < rotation; i++) {
            assertSame(original.remove(0), deque.pollFirst());
            Key value = i % 5 == 0 ? shared : new Key(i % 11);
            deque.addLast(value);
            original.add(value);
        }
        while (deque.size() > resident) {
            assertSame(original.remove(original.size() - 1), deque.pollLast());
        }
        int[] flags = new int[resident];
        for (int i = 0; i < resident; i++) {
            int appBits = ((i * 31 + rotation) & 127) << 1;
            deque.updateState(i, appBits, 0);
            flags[i] = deque.stateAt(i);
        }
        long epoch = deque.revision();
        int head = resident == 0 ? -1 : deque.physicalIndex(0);
        Iterator<Key> prior = deque.iterator();
        List<Key> expected = new ArrayList<>();
        List<Integer> retainedFlags = new ArrayList<>();
        boolean expectedChanged = false;
        for (int i = 0; i < resident; i++) {
            if (removed[i]) expectedChanged = true;
            else { expected.add(original.get(i)); retainedFlags.add(flags[i]); }
        }
        int[] callbacks = {0};
        boolean changed = deque.removeIf(value -> {
            int ordinal = callbacks[0]++;
            assertSame(original.get(ordinal), value, "source encounter order");
            return removed[ordinal];
        });
        assertEquals(resident, callbacks[0], "one callback per occurrence");
        assertEquals(expectedChanged, changed);
        assertEquals(epoch + (expectedChanged ? 1 : 0), deque.revision());
        assertEquals(capacity, deque.capacity());
        assertEquals(expected.size(), deque.size());
        Object[] actual = deque.toArray();
        assertEquals(expected.size(), actual.length);
        if (!expected.isEmpty()) assertEquals(head, deque.physicalIndex(0));
        for (int i = 0; i < actual.length; i++) {
            assertSame(expected.get(i), actual[i], "reference identity");
            assertEquals(retainedFlags.get(i).intValue(), deque.stateAt(i), "byte-state alignment");
        }
        int[] positions = deque.selectedPhysicalIndices(M3ArrayDeque.LIVE, 0,
                IndexMaskCompactor.Backend.JAVA);
        assertEquals(expected.size(), positions.length);
        for (int i = 0; i < positions.length; i++) {
            assertEquals(deque.physicalIndex(i), positions[i], "physical selector");
        }
        if (expectedChanged) {
            assertThrows(ConcurrentModificationException.class, prior::hasNext);
        } else {
            assertEquals(resident != 0, prior.hasNext(), "no-op retains iterator");
        }
        M3ArrayDeque<Key> cloned = deque.clone();
        Object[] snapshot = cloned.toArray();
        for (int i = 0; i < snapshot.length; i++) {
            assertSame(expected.get(i), snapshot[i], "clone identity");
            assertEquals(retainedFlags.get(i).intValue(), cloned.stateAt(i));
        }
        if (expected.isEmpty()) {
            Key newValue = new Key(101);
            deque.addLast(newValue);
            assertSame(newValue, deque.pollFirst(), "empty ring reuse");
            assertEquals(0, deque.size());
        }
    }

    @Test void exhaustiveSmallRingsAndSeededLargeRanges() {
        for (int capacity = 1; capacity <= 8; capacity++) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 0; size <= capacity; size++) {
                    for (int mask = 0; mask < (1 << size); mask++) {
                        boolean[] removed = new boolean[size];
                        for (int bit = 0; bit < size; bit++) {
                            removed[bit] = (mask & (1 << bit)) != 0;
                        }
                        check(capacity, head, size, removed);
                    }
                }
            }
        }
        Random random = new Random(0x4d335f6465717565L);
        for (int pass = 0; pass < 2500; pass++) {
            int capacity = 1 + random.nextInt(900);
            int head = random.nextInt(capacity);
            int size = random.nextInt(capacity + 1);
            boolean[] removed = new boolean[size];
            int density = random.nextInt(12);
            for (int i = 0; i < size; i++) {
                removed[i] = density == 0 ? false
                        : density == 11 || random.nextInt(12) < density;
            }
            check(capacity, head, size, removed);
        }
    }

    @Test void predicateFailureAndReentrantMutationKeepOriginalAuthority() {
        M3ArrayDeque<Key> deque = new M3ArrayDeque<>(8);
        for (int i = 0; i < 8; i++) deque.addLast(new Key(i));
        Object[] before = deque.toArray();
        long epoch = deque.revision();
        int[] called = {0};
        assertThrows(IllegalStateException.class, () -> deque.removeIf(key -> {
            if (++called[0] == 5) throw new IllegalStateException("predicate");
            return (key.value() & 1) == 0;
        }));
        assertEquals(5, called[0]);
        assertEquals(epoch, deque.revision());
        Object[] after = deque.toArray();
        for (int i = 0; i < before.length; i++) assertSame(before[i], after[i]);
        assertThrows(NullPointerException.class, () -> deque.removeIf(null));
        int[] reentrant = {0};
        assertThrows(ConcurrentModificationException.class, () -> deque.removeIf(key -> {
            if (++reentrant[0] == 2) deque.addLast(new Key(99));
            return true;
        }));
        assertEquals(9, deque.size(), "callback's own modification survives refusal");
    }
}
