/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The expiry-order index kernel: ring order, the switch to a 4-ary heap, lazy invalidation and compaction. */
final class M3ExpiryOrderIndexTest {

    @Test
    void ringKeepsArrivalOrderAcrossWrapAndGrowth() {
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        long next = 0;
        long expected = 0;
        for (int round = 0; round < 40; round++) {
            for (int i = 0; i < 7; i++) {
                q.add(next++ / 2, i, round);
            }
            for (int i = 0; i < 5; i++) {
                assertEquals(expected++ / 2, q.firstDue());
                q.removeFirst();
            }
            q.verify();
            assertFalse(q.isHeap());
        }
        assertEquals(80, q.size());
        assertEquals(expected / 2, q.dueAt(0));
    }

    @Test
    void firstLowerDeadlineSwitchesToHeapAndPopsInOrder() {
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        for (int i = 0; i < 10; i++) {
            q.add(100 + i, i, 1);
        }
        q.removeFirst();
        q.removeFirst();
        q.add(5, 42, 9);
        assertTrue(q.isHeap());
        q.verify();
        assertEquals(5, q.firstDue());
        assertEquals(42, q.firstSlot());
        assertEquals(9, q.firstStamp());
        long last = Long.MIN_VALUE;
        while (q.size() > 0) {
            assertTrue(q.firstDue() >= last);
            last = q.firstDue();
            q.removeFirst();
            q.verify();
        }
        assertFalse(q.isHeap(), "an empty index is a ring again");
    }

    @Test
    void wrappedRingIsUnrolledBeforeTheSwitch() {
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        for (int i = 0; i < 16; i++) {
            q.add(i, i, 0);
        }
        for (int i = 0; i < 12; i++) {
            q.removeFirst();
        }
        for (int i = 16; i < 24; i++) {
            q.add(i, i, 0);
        }
        q.add(1, 99, 0);
        assertTrue(q.isHeap());
        q.verify();
        assertEquals(1, q.firstDue());
        assertEquals(13, q.size());
    }

    @Test
    void retainKeepsRingOrderAndRebuildsTheHeap() {
        M3ExpiryOrderIndex ring = new M3ExpiryOrderIndex();
        int[] stamps = new int[64];
        for (int i = 0; i < 40; i++) {
            ring.add(i, i, 0);
        }
        for (int i = 0; i < 40; i += 3) {
            stamps[i] = 1;
        }
        ring.retain(stamps);
        ring.verify();
        assertFalse(ring.isHeap());
        assertEquals(26, ring.size());
        assertEquals(1, ring.firstDue());
        M3ExpiryOrderIndex heap = new M3ExpiryOrderIndex();
        Random r = new Random(7);
        for (int i = 0; i < 60; i++) {
            heap.add(r.nextInt(1000), i, 0);
        }
        assertTrue(heap.isHeap());
        int[] heapStamps = new int[64];
        for (int i = 0; i < 60; i += 2) {
            heapStamps[i] = 3;
        }
        heap.retain(heapStamps);
        heap.verify();
        assertEquals(30, heap.size());
        int[] allStale = new int[64];
        java.util.Arrays.fill(allStale, 5);
        heap.retain(allStale);
        assertEquals(0, heap.size());
        assertFalse(heap.isHeap());
        heap.verify();
    }

    @Test
    void randomizedAgainstASortedReference() {
        Random r = new Random(20261024L);
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        int[] stamps = new int[256];
        List<long[]> model = new ArrayList<>();
        long clock = 0;
        for (int step = 0; step < 20_000; step++) {
            int op = r.nextInt(10);
            if (op < 5) {
                int s = r.nextInt(256);
                long due = r.nextInt(8) == 0 ? clock - r.nextInt(50) : clock + r.nextInt(200);
                q.add(due, s, stamps[s]);
                model.add(new long[] {due, s, stamps[s]});
                clock += r.nextInt(3);
            } else if (op < 8 && q.size() > 0) {
                long min = Collections.min(model, (a, b) -> Long.compare(a[0], b[0]))[0];
                assertEquals(min, q.firstDue());
                long[] popped = {q.firstDue(), q.firstSlot(), q.firstStamp()};
                q.removeFirst();
                assertTrue(removeOne(model, popped), "popped record exists");
            } else if (op == 8) {
                stamps[r.nextInt(256)]++;
            } else {
                q.retain(stamps);
                model.removeIf(rec -> stamps[(int) rec[1]] != rec[2]);
            }
            assertEquals(model.size(), q.size());
            q.verify();
        }
    }

    private static boolean removeOne(List<long[]> model, long[] rec) {
        for (int i = 0; i < model.size(); i++) {
            long[] m = model.get(i);
            if (m[0] == rec[0] && m[1] == rec[1] && m[2] == rec[2]) {
                model.remove(i);
                return true;
            }
        }
        return false;
    }

    @Test
    void clearKeepsLanesAndReturnsToRing() {
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        q.add(5, 1, 0);
        q.add(3, 2, 0);
        assertTrue(q.isHeap());
        q.clear();
        assertEquals(0, q.size());
        assertFalse(q.isHeap());
        q.add(1, 3, 0);
        assertEquals(1, q.firstDue());
        q.verify();
    }

    @Test
    void capacityIsAPowerOfTwoWithAnUpperBound() {
        assertEquals(16, M3ExpiryOrderIndex.nextCapacity(0));
        assertEquals(64, M3ExpiryOrderIndex.nextCapacity(32));
        assertEquals(1 << 30, M3ExpiryOrderIndex.nextCapacity(1 << 29));
        assertThrows(OutOfMemoryError.class, () -> M3ExpiryOrderIndex.nextCapacity(1 << 30));
    }

    @Test
    void verifyRejectsCorruptShapesAndOrder() throws ReflectiveOperationException {
        assertThrows(IllegalStateException.class, () -> corrupt(ringOf(4), "size", 99));
        M3ExpiryOrderIndex heap = ringOf(4);
        heap.add(-1, 9, 0);
        assertThrows(IllegalStateException.class, () -> corrupt(heap, "head", 1));
        M3ExpiryOrderIndex emptyHeap = new M3ExpiryOrderIndex();
        assertThrows(IllegalStateException.class, () -> corrupt(emptyHeap, "heap", true));
        M3ExpiryOrderIndex emptyRing = new M3ExpiryOrderIndex();
        assertThrows(IllegalStateException.class, () -> corrupt(emptyRing, "head", 3));
        M3ExpiryOrderIndex ring = ringOf(4);
        ((long[]) field("due").get(ring))[1] = -50;
        assertThrows(IllegalStateException.class, ring::verify);
        M3ExpiryOrderIndex broken = ringOf(4);
        broken.add(-1, 9, 0);
        ((long[]) field("due").get(broken))[2] = -100;
        assertThrows(IllegalStateException.class, broken::verify);
    }

    private static M3ExpiryOrderIndex ringOf(int n) {
        M3ExpiryOrderIndex q = new M3ExpiryOrderIndex();
        for (int i = 0; i < n; i++) {
            q.add(i * 10L, i, 0);
        }
        return q;
    }

    private static void corrupt(M3ExpiryOrderIndex q, String name, Object value) throws ReflectiveOperationException {
        field(name).set(q, value);
        q.verify();
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field f = M3ExpiryOrderIndex.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
