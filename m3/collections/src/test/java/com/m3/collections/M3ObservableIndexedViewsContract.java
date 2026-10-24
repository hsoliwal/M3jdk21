// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Standalone Java 21 differential contract for observable indexed selection. */
public final class M3ObservableIndexedViewsContract {
    private static long checks;
    private M3ObservableIndexedViewsContract() { }

    public static void main(String[] arguments) {
        M3ObservableList<String> owner =
                new M3ObservableList<>(Arrays.asList("pear", null, "fig", "fig", "plum"));
        var messages = new ArrayList<String>();
        owner.addListener((list, change, index, count, oldValue, newValue, generation) ->
                messages.add(change + ":" + index + ":" + count + ":" + generation));
        var order = Comparator.nullsFirst(Comparator.<String>naturalOrder());
        M3IndexedLiveView<String> combined =
                owner.filteredSortedView(v -> v == null || v.length() == 3, order);
        check(combined.equals(Arrays.asList(null, "fig", "fig")), "stable filtered sort and null");
        check(Arrays.equals(combined.physicalIndices(), new int[] {1, 2, 3}),
                "original physical coordinates including equal keys");
        check(owner.equals(Arrays.asList("pear", null, "fig", "fig", "plum")),
                "sorting does not mutate payload owner");
        var filtered = owner.filteredView(v -> v != null && v.length() == 4);
        check(filtered.equals(List.of("pear", "plum")), "encounter order retained");
        check(owner.sortedView(order).equals(Arrays.asList(null, "fig", "fig", "pear", "plum")),
                "sorted-only projection includes hidden values");
        owner.add(0, "ant");
        check(combined.equals(Arrays.asList(null, "ant", "fig", "fig")),
                "structural mutation invalidates index");
        check(Arrays.equals(combined.physicalIndices(), new int[] {2, 0, 3, 4}),
                "stable indirect ordering after insertion");
        owner.set(3, "no");
        check(combined.equals(Arrays.asList(null, "ant", "fig")),
                "nonstructural set invalidates mask");
        check(owner.generation() == 2 && messages.equals(List.of("ADD:0:1:1", "SET:3:1:2")),
                "original notification generation and order unchanged");
        var stale = combined.iterator();
        stale.next();
        owner.set(1, "yak");
        expect(ConcurrentModificationException.class, stale::hasNext);
        expect(UnsupportedOperationException.class, () -> combined.add("no"));
        expect(UnsupportedOperationException.class, combined::clear);
        expect(UnsupportedOperationException.class, () -> combined.set(0, "changed"));
        expect(NullPointerException.class, () -> owner.filteredView(null));
        check(owner.listenerCount() == 1, "projection does not register listeners");

        M3ObservableList<StringBuilder> mutable =
                new M3ObservableList<>(List.of(new StringBuilder("zzz"), new StringBuilder("a")));
        M3IndexedLiveView<StringBuilder> mutableProjection = mutable.filteredSortedView(
                v -> v.length() > 1, Comparator.comparing(StringBuilder::toString));
        check(mutableProjection.size() == 1, "first predicate materialization");
        mutable.get(1).append("aaa");
        check(mutableProjection.size() == 1, "external referent mutation is not silently guessed");
        mutableProjection.invalidate();
        check(mutableProjection.size() == 2 && mutableProjection.get(0) == mutable.get(1),
                "explicit invalidation refreshes selection and stable order");

        if (!M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger calls = new AtomicInteger();
            var nativeRequired = owner.filteredSortedView(v -> {
                calls.incrementAndGet();
                return true;
            }, order, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            expect(UnsatisfiedLinkError.class, nativeRequired::size);
            check(calls.get() == 0, "native-required refuses before Java callback");
        }

        Random random = new Random(0x51e7c011L);
        M3ObservableList<Integer> actual = new M3ObservableList<>();
        ArrayList<Integer> reference = new ArrayList<>();
        Comparator<Integer> compare = Comparator.nullsFirst(Integer::compareTo);
        var indexed = actual.filteredSortedView(v -> v == null || (v & 1) == 0, compare);
        for (int step = 0; step < 1_200; step++) {
            int size = reference.size();
            int action = size >= 80 ? 2 : random.nextInt(4);
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(50) - 25;
            if (action == 0 || size == 0) {
                int at = random.nextInt(size + 1);
                reference.add(at, value);
                actual.add(at, value);
            } else if (action == 1) {
                int at = random.nextInt(size);
                reference.set(at, value);
                actual.set(at, value);
            } else if (action == 2) {
                int at = random.nextInt(size);
                check(java.util.Objects.equals(reference.remove(at), actual.remove(at)),
                        "removal parity");
            } else {
                int at = random.nextInt(size + 1);
                var values = Arrays.asList(value, random.nextInt(4));
                reference.addAll(at, values);
                actual.addAll(at, values);
            }
            check(actual.equals(reference), "payload oracle step=" + step);
            List<Integer> selected = reference.stream().filter(v -> v == null || (v & 1) == 0)
                    .sorted(compare).toList();
            check(indexed.equals(selected), "filtered/sorted oracle step=" + step);
            int[] expectedSlots = IntStream.range(0, reference.size())
                    .filter(i -> reference.get(i) == null || (reference.get(i) & 1) == 0)
                    .boxed().sorted(Comparator.comparing(reference::get, compare))
                    .mapToInt(Integer::intValue).toArray();
            check(Arrays.equals(expectedSlots, indexed.physicalIndices()),
                    "stable physical slot oracle step=" + step);
        }
        System.out.println("M3_OBSERVABLE_INDEXED_VIEWS_PASS checks=" + checks);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static void expect(Class<? extends Throwable> type, Runnable work) {
        try {
            work.run();
        } catch (Throwable thrown) {
            if (!type.isInstance(thrown)) throw new AssertionError(type.getName(), thrown);
            checks++;
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }
}
