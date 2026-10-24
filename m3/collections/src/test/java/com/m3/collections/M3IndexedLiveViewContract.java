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
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Self-contained Java21 oracle; no JUnit or native binary needed. */
public final class M3IndexedLiveViewContract {
    private M3IndexedLiveViewContract() { }
    private static int checks;
    private static void assertEqual(Object actual, Object expected) {
        if (!Objects.equals(actual, expected))
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        checks++;
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) { checks++; return; }
            throw new AssertionError("expected " + expected + ", got " + problem, problem);
        }
        throw new AssertionError("did not throw " + expected);
    }

    private record Pair(int key, int original) { }

    public static void contracts() {
        M3ArrayList<Pair> rows = new M3ArrayList<>(List.of(
                new Pair(2, 0), new Pair(1, 1), new Pair(2, 2),
                new Pair(3, 3), new Pair(2, 4)));
        AtomicInteger calls = new AtomicInteger();
        M3IndexedLiveView<Pair> view = M3IndexedLiveView.filteredSorted(rows, p -> {
            calls.incrementAndGet();
            return p.key() >= 2;
        }, Comparator.comparingInt(Pair::key));
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 2, 4, 3));
        assertEqual(Arrays.toString(view.physicalIndices()), "[0, 2, 4, 3]");
        assertEqual(view.physicalIndex(2), 4);
        assertEqual(calls.get(), 5);
        assertEqual(view.get(0).original(), 0);
        assertEqual(calls.get(), 5); // cached until owner mutation
        Iterator<Pair> cursor = view.iterator();
        assertEqual(cursor.next().original(), 0);
        rows.set(2, new Pair(0, 20));
        fails(ConcurrentModificationException.class, cursor::hasNext);
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 4, 3));
        assertEqual(calls.get(), 10);
        rows.add(0, new Pair(5, 9));
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 4, 3, 9));
        assertEqual(view.physicalIndex(3), 0);
        rows.remove(1);
        assertEqual(view.stream().map(Pair::original).toList(), List.of(4, 3, 9));
        assertEqual(view.reversed().stream().map(Pair::original).toList(), List.of(9, 3, 4));
        fails(UnsupportedOperationException.class, () -> view.clear());
        fails(UnsupportedOperationException.class, () -> view.addAll(List.of()));
        fails(UnsupportedOperationException.class, () -> view.removeIf(x -> false));
        fails(UnsupportedOperationException.class, () -> view.reversed().removeFirst());
        fails(UnsupportedOperationException.class, () -> view.listIterator().remove());
        M3ArrayList<Integer> nulls = new M3ArrayList<>(Arrays.asList(null, 2, null, 1, 2));
        M3IndexedLiveView<Integer> filtered = M3IndexedLiveView.filtered(nulls, x -> x == null || x == 2);
        assertEqual(filtered, Arrays.asList(null, 2, null, 2));
        assertEqual(Arrays.toString(filtered.physicalIndices()), "[0, 1, 2, 4]");
        M3IndexedLiveView<Integer> sorted = M3IndexedLiveView.sorted(nulls,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEqual(sorted, Arrays.asList(null, null, 1, 2, 2));
        int[] detached = sorted.physicalIndices();
        detached[0] = 999;
        assertEqual(sorted.get(0), null);
        Iterator<Integer> invalidated = sorted.iterator();
        sorted.invalidate();
        fails(ConcurrentModificationException.class, invalidated::hasNext);
        assertEqual(sorted.get(0), null);
        AtomicInteger never = new AtomicInteger();
        M3IndexedLiveView<Integer> nativeRequired = M3IndexedLiveView.filteredSorted(nulls, x -> {
            never.incrementAndGet(); return true;
        }, null, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, nativeRequired::size);
            assertEqual(never.get(), 0);
        }
        M3IndexedLiveView<Integer> reentrant = M3IndexedLiveView.filteredSorted(nulls, x -> {
            nulls.set(0, 10);
            return true;
        }, null);
        fails(ConcurrentModificationException.class, reentrant::size);
        M3ArrayList<Integer> ints = new M3ArrayList<>(List.of(3, 1, 2));
        M3IndexedLiveView<Integer> natural = M3IndexedLiveView.sorted(ints, null);
        assertEqual(natural, List.of(1, 2, 3));
        ints.sort(null);
        assertEqual(natural, List.of(1, 2, 3));
        assertEqual(natural.physicalIndex(0), 0);
        randomDifferential();
    }

    private static void randomDifferential() {
        Random random = new Random(0x3AF61L);
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of());
        M3IndexedLiveView<Integer> sorted = M3IndexedLiveView.filteredSorted(
                owner, x -> x == null || (x & 1) == 0,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        for (int i = 0; i < 1000; i++) {
            switch (random.nextInt(4)) {
                case 0 -> owner.add(randomValue(random));
                case 1 -> { if (!owner.isEmpty()) owner.set(random.nextInt(owner.size()), randomValue(random)); }
                case 2 -> { if (!owner.isEmpty()) owner.remove(random.nextInt(owner.size())); }
                default -> { if (!owner.isEmpty()) owner.sort(Comparator.nullsFirst(Comparator.naturalOrder())); }
            }
            ArrayList<Integer> oracle = new ArrayList<>();
            for (Integer value : owner) {
                if (value == null || (value & 1) == 0) oracle.add(value);
            }
            oracle.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
            assertEqual(sorted, oracle);
            int[] phys = sorted.physicalIndices();
            for (int index = 0; index < phys.length; index++) {
                assertEqual(sorted.get(index), owner.get(phys[index]));
            }
        }
    }

    private static Integer randomValue(Random r) { return r.nextInt(8) == 0 ? null : r.nextInt(13) - 6; }

    public static void main(String[] args) {
        contracts();
        System.out.println("M3_INDEXED_LIVE_VIEW_PASS checks=" + checks);
    }
}
