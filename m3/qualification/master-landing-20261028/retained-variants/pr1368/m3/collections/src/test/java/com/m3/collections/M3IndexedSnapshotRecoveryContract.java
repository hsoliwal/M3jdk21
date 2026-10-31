/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Fresh 2026-10-19 occurrence-coordinate and immutable-composition witness. */
public final class M3IndexedSnapshotRecoveryContract {
    private static long checks;
    private static final Comparator<Integer> ORDER = Comparator.nullsFirst(Comparator.naturalOrder());

    private M3IndexedSnapshotRecoveryContract() { }

    public static void main(String[] args) {
        String scope = args.length == 0 ? "all" : args[0];
        if (scope.equals("all") || scope.equals("coordinates")) coordinates(false);
        if (scope.equals("all") || scope.equals("composition")) immutableComposition();
        if (scope.equals("all") || scope.equals("failure")) failureAndRefusal();
        if (scope.equals("native")) {
            check(M3IndexMaskCompactor.nativeAvailable(), "required native owner is available");
            coordinates(true);
        }
        if (scope.equals("missing")) {
            check(!M3IndexMaskCompactor.nativeAvailable(), "missing-library fixture isolation");
            int[] calls = {0};
            expect(UnsatisfiedLinkError.class, () -> M3IndexedListSnapshot.copyOf(List.of(1))
                    .filtered(value -> { calls[0]++; return true; },
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "native refusal");
            equal(0, calls[0], "native availability precedes callback");
        }
        System.out.println("SNAPSHOT_RECOVERY_PASS scope=" + scope + " checks=" + checks);
    }

    private static Integer[] values(int length) {
        Integer[] result = new Integer[length];
        Integer shared = Integer.valueOf(4096);
        for (int i = 0; i < length; i++) {
            result[i] = switch (i % 9) {
                case 0 -> null;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> Integer.MAX_VALUE;
                case 3, 4 -> shared;
                case 5 -> -i;
                case 6 -> 0;
                default -> i % 13 - 6;
            };
        }
        return result;
    }

    private static List<Integer> ordinals(int length) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < length; i++) result.add(i);
        return result;
    }

    private static List<Integer> sortedOrdinals(List<Integer> input, Integer[] values,
                                                Comparator<Integer> order) {
        List<Integer> expected = new ArrayList<>(input);
        // JDK List.sort is the independent stable occurrence oracle.
        expected.sort((left, right) -> order.compare(values[left], values[right]));
        return expected;
    }

    private static boolean selected(int ordinal, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> ordinal % 4 == 0;
            case 3 -> ordinal % 64 == 63;
            default -> ordinal % 3 != 1;
        };
    }

    private static void coordinates(boolean nativeRequired) {
        for (int length : new int[] {0, 1, 2, 8, 63, 64, 65, 127, 128, 129, 257, 1024}) {
            Integer[] source = values(length);
            Integer[] expectedValues = source.clone();
            M3IndexedListSnapshot<Integer> snapshot = M3IndexedListSnapshot.copyOf(source);
            Arrays.fill(source, 73);
            List<Integer> original = ordinals(length);
            verify(snapshot, original, expectedValues, "detached input");
            for (Comparator<Integer> order : List.of(ORDER, ORDER.reversed())) {
                List<Integer> sorted = sortedOrdinals(original, expectedValues, order);
                M3IndexedListSnapshot<Integer> ordered = snapshot.sorted(order);
                verify(ordered, sorted, expectedValues, "stable signed/null ordering");
                verify(snapshot, original, expectedValues, "sorting leaves source immutable");
                for (int slice : new int[] {0, 1, 2}) {
                    int begin = length == 0 ? 0 : slice == 0 ? 0 : slice == 1 ? length / 3 : length;
                    int end = slice == 1 ? Math.max(begin, length - length / 4) : length;
                    M3IndexedListSnapshot<Integer> view = ordered.slice(begin, end - begin);
                    List<Integer> window = new ArrayList<>(sorted.subList(begin, end));
                    verify(view, window, expectedValues, "slice physical coordinates");
                    verify(ordered.subList(begin, end), window, expectedValues, "sublist coordinates");
                    for (int mode = 0; mode < 5; mode++) {
                        final int selection = mode;
                        int[] calls = {0};
                        List<Integer> admitted = new ArrayList<>();
                        for (int i = 0; i < window.size(); i++) if (selected(i, mode)) admitted.add(window.get(i));
                        Predicate<Integer> predicate = value -> {
                            int ordinal = calls[0]++;
                            check(value == expectedValues[window.get(ordinal)], "callback occurrence identity/order");
                            return selected(ordinal, selection);
                        };
                        M3IndexedListSnapshot<Integer> filtered = view.filtered(predicate,
                                nativeRequired ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                                        : M3IndexMaskCompactor.Backend.JAVA);
                        equal(window.size(), calls[0], "exactly one callback per logical occurrence");
                        verify(filtered, admitted, expectedValues, "filtered physical coordinates");
                        verify(filtered.sorted(order.reversed()), sortedOrdinals(admitted, expectedValues,
                                order.reversed()), expectedValues, "filtered/sorted stable composition");
                        equal(window.size(), calls[0], "materialized reads never repeat callback");
                        verify(view, window, expectedValues, "filter/sort preserves input view");
                    }
                }
            }
        }
    }

    private static void verify(M3IndexedListSnapshot<Integer> actual, List<Integer> expected,
                               Integer[] values, String label) {
        equal(expected.size(), actual.size(), label + " size");
        int[] physical = actual.physicalIndices();
        BitSet mask = new BitSet();
        for (int i = 0; i < expected.size(); i++) {
            int slot = expected.get(i);
            equal(slot, actual.physicalIndex(i), label + " ordinal");
            equal(slot, physical[i], label + " detached directory");
            check(actual.get(i) == values[slot], label + " payload reference");
            mask.set(slot);
        }
        equal(mask, actual.physicalMaskCopy(), label + " physical mask");
        for (int slot = -1; slot <= values.length; slot++) {
            boolean included = slot >= 0 && mask.get(slot);
            equal(included, actual.includesPhysicalIndex(slot), label + " membership");
        }
        BitSet detached = actual.physicalMaskCopy();
        detached.flip(0, values.length + 1);
        equal(mask, actual.physicalMaskCopy(), label + " mask copy isolation");
        if (physical.length != 0) {
            physical[0] = -1;
            equal(expected.getFirst(), actual.physicalIndex(0), label + " directory copy isolation");
        }
        Object[] payload = actual.toArray();
        if (payload.length != 0) {
            payload[0] = new Object();
            check(actual.get(0) == values[expected.getFirst()], label + " array copy isolation");
        }
    }

    private static void immutableComposition() {
        Integer[] values = values(129);
        List<Integer> original = ordinals(values.length);
        M3IndexedListSnapshot<Integer> root = M3IndexedListSnapshot.copyOf(values);
        M3IndexedListSnapshot<Integer> ordered = root.sorted(ORDER.reversed());
        List<Integer> orderedSlots = sortedOrdinals(original, values, ORDER.reversed());
        for (int begin : new int[] {0, 1, 63, 128, 129}) {
            M3IndexedListSnapshot<Integer> slice = ordered.slice(begin, 129 - begin);
            List<Integer> slots = new ArrayList<>(orderedSlots.subList(begin, 129));
            M3IndexedListSnapshot<Integer> all = slice.filtered(value -> true);
            M3IndexedListSnapshot<Integer> sibling = all.subList(0, all.size());
            all.sorted(ORDER);
            verify(all, slots, values, "shared all-selected directory immutable");
            verify(sibling, slots, values, "sibling directory immutable");
            verify(slice, slots, values, "parent slice immutable");
            verify(ordered, orderedSlots, values, "full parent directory immutable");
        }
        for (int failAt : new int[] {1, 2, 3, 7, 31}) {
            RuntimeException token = new IllegalStateException("sort failure " + failAt);
            int[] calls = {0};
            M3IndexedListSnapshot<Integer> all = root.filtered(value -> true);
            sameThrowable(token, () -> all.sorted((left, right) -> {
                if (++calls[0] == failAt) throw token;
                return ORDER.compare(left, right);
            }), "comparator failure identity");
            equal(failAt, calls[0], "comparison stops at injected failure");
            verify(all, original, values, "failed sort preserves shared input");
            verify(root, original, values, "failed sort preserves original");
        }
        M3IndexedListSnapshot<Integer> signed = M3IndexedListSnapshot.copyOf(
                new Integer[] {Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 0, -1});
        equal(List.of(Integer.MIN_VALUE, -1, -1, 0, Integer.MAX_VALUE), signed.sorted(),
                "natural signed order");
        equal(List.of(2, 1, 4, 3, 0), Arrays.stream(signed.sorted().physicalIndices()).boxed().toList(),
                "natural equal-key stability");
    }

    private static void failureAndRefusal() {
        Integer[] values = values(129);
        M3IndexedListSnapshot<Integer> snapshot = M3IndexedListSnapshot.copyOf(values);
        for (int failAt : new int[] {0, 1, 63, 64, 65, 128}) {
            RuntimeException token = new IllegalArgumentException("predicate failure " + failAt);
            int[] calls = {0};
            sameThrowable(token, () -> snapshot.filtered(value -> {
                if (calls[0]++ == failAt) throw token;
                return true;
            }), "predicate failure identity");
            equal(failAt + 1, calls[0], "predicate failure prevents later callbacks");
            verify(snapshot, ordinals(values.length), values, "failed filter preserves input");
        }
        expect(NullPointerException.class, () -> snapshot.filtered(null), "null predicate");
        expect(NullPointerException.class, () -> snapshot.filtered(value -> true, null), "null backend");
        expect(IndexOutOfBoundsException.class, () -> snapshot.get(-1), "negative ordinal");
        expect(IndexOutOfBoundsException.class, () -> snapshot.physicalIndex(129), "end ordinal");
        expect(IndexOutOfBoundsException.class, () -> snapshot.slice(Integer.MAX_VALUE, 1), "slice overflow");
        expect(IndexOutOfBoundsException.class, () -> snapshot.subList(1, 0), "reversed slice");
        for (M3IndexedListSnapshot<Integer> view : List.of(snapshot, snapshot.slice(0, 0))) {
            expect(UnsupportedOperationException.class, () -> view.addAll(List.of()), "strict empty addAll");
            expect(UnsupportedOperationException.class, () -> view.removeAll(List.of()), "strict empty removeAll");
            expect(UnsupportedOperationException.class, () -> view.remove(999), "strict ordinal remove");
            expect(UnsupportedOperationException.class, () -> view.remove(Integer.valueOf(999)), "strict absent remove");
            expect(UnsupportedOperationException.class, () -> view.removeIf(value -> { throw new AssertionError(); }), "strict callback refusal");
            expect(UnsupportedOperationException.class, () -> view.sort(ORDER), "strict in-place sort refusal");
            expect(UnsupportedOperationException.class, view::clear, "strict clear");
        }
    }

    private static void sameThrowable(Throwable expected, Runnable operation, String label) {
        checks++;
        try { operation.run(); }
        catch (Throwable thrown) {
            if (thrown == expected) return;
            throw new AssertionError(label + ": wrong throwable", thrown);
        }
        throw new AssertionError(label + ": expected throwable");
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation, String label) {
        checks++;
        try { operation.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(label + ": wrong throwable", thrown);
        }
        throw new AssertionError(label + ": expected " + type.getSimpleName());
    }

    private static void equal(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual), label + ": " + expected + " != " + actual);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
