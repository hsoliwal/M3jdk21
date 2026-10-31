/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent stable-insertion oracle and incumbent callback trace for selected index sorting. */
public final class M3MaskedSortFusionContract {
    private static int checks;
    private record Value(int key, int id) { }

    private M3MaskedSortFusionContract() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(int[] expected, int[] actual, String label) {
        check(Arrays.equals(expected, actual), label + ": " + Arrays.toString(expected)
                + " != " + Arrays.toString(actual));
    }

    private static Object payload(M3IndexedListSnapshot<?> value) throws ReflectiveOperationException {
        Field field = M3IndexedListSnapshot.class.getDeclaredField("values");
        field.setAccessible(true);
        return field.get(value);
    }

    private static Throwable failure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected refusal");
        } catch (RuntimeException | LinkageError expected) {
            return expected;
        }
    }

    private static <E> int[] insertionOracle(M3IndexedListSnapshot<E> view, E[] values,
            int[] states, int required, int forbidden, Comparator<? super E> order) {
        int[] selected = new int[view.size()];
        int count = 0;
        for (int slot : view.physicalIndices()) {
            if ((states[slot] & required) == required && (states[slot] & forbidden) == 0) {
                selected[count++] = slot;
            }
        }
        // A separate O(k^2) stable insertion oracle, never the implementation's merge sorter.
        for (int i = 1; i < count; i++) {
            int slot = selected[i], at = i;
            while (at > 0 && order.compare(values[selected[at - 1]], values[slot]) > 0) {
                selected[at] = selected[at - 1];
                at--;
            }
            selected[at] = slot;
        }
        return Arrays.copyOf(selected, count);
    }

    private static void cases(M3IndexPathMasks.Backend backend) throws ReflectiveOperationException {
        Random random = new Random(0x4d33534f52543231L);
        Comparator<Value> ascending = Comparator.nullsFirst(Comparator.comparingInt(Value::key));
        for (int trial = 0; trial < 1200; trial++) {
            Value[] input = new Value[trial < 4 ? trial : random.nextInt(97)];
            for (int i = 0; i < input.length; i++) {
                input[i] = i > 0 && i % 5 == 0 ? input[i - 1]
                        : i % 7 == 0 ? null : new Value(random.nextInt(7) - 3, i);
            }
            Value[] canonical = input.clone();
            M3IndexedListSnapshot<Value> owner = M3IndexedListSnapshot.copyOf(input);
            Arrays.fill(input, new Value(999, -1)); // Caller storage is detached at admission.
            M3IndexedListSnapshot<Value> reordered = (trial & 1) == 0 ? owner : owner.sorted(ascending.reversed());
            int from = random.nextInt(reordered.size() + 1);
            int to = from + random.nextInt(reordered.size() - from + 1);
            M3IndexedListSnapshot<Value> view = trial % 3 == 0 ? reordered : reordered.subList(from, to);
            int[] prior = view.physicalIndices();
            int[] states = new int[input.length];
            for (int i = 0; i < states.length; i++) states[i] = random.nextInt();
            int required = switch (trial % 5) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> 1;
                case 3 -> -1;
                default -> random.nextInt();
            };
            int forbidden = trial % 4 == 0 ? 0 : trial % 4 == 1 ? 2 : required;
            Comparator<Value> order = (trial & 2) == 0 ? ascending : ascending.reversed();
            int[] expected = insertionOracle(view, canonical, states, required, forbidden, order);
            var priorMembership = view.physicalMaskCopy();
            M3IndexedListSnapshot<Value> actual = view.sortedMaskedByPhysicalState(
                    states, required, forbidden, order, backend);
            equal(expected, actual.physicalIndices(), "independent stable ordering trial " + trial);
            equal(prior, view.physicalIndices(), "source directory unchanged");
            check(priorMembership.equals(view.physicalMaskCopy()), "source membership unchanged");
            check(payload(owner) == payload(actual), "canonical object payload shared");
            for (int i = 0; i < actual.size(); i++) {
                check(actual.get(i) == canonical[expected[i]], "physical reference identity and duplicates");
            }
            Arrays.fill(states, ~required);
            equal(expected, actual.physicalIndices(), "state lane not retained");
            int[] export = actual.physicalIndices();
            Arrays.fill(export, -1);
            equal(expected, actual.physicalIndices(), "exported index lane detached");
            var membership = actual.physicalMaskCopy();
            membership.clear();
            for (int slot = 0; slot < canonical.length; slot++) {
                boolean present = false;
                for (int admitted : expected) if (admitted == slot) present = true;
                check(actual.includesPhysicalIndex(slot) == present, "selected physical membership");
            }
        }
    }

    private static Comparator<Integer> trace(List<String> calls, int stop, RuntimeException error) {
        return (left, right) -> {
            calls.add(left + ":" + right);
            if (calls.size() == stop) throw error;
            return Integer.compare(left % 3, right % 3);
        };
    }

    private static void callbackAndFailure(M3IndexPathMasks.Backend backend) {
        Integer[] input = {9, 2, 7, 4, 1, 6, 10, 5, 3, 8, 0, 11};
        var source = M3IndexedListSnapshot.copyOf(input).subList(1, 11);
        int[] original = source.physicalIndices();
        int[] states = new int[input.length];
        Arrays.fill(states, Integer.MIN_VALUE | 1);
        states[3] |= 2;
        List<String> incumbent = new ArrayList<>(), fused = new ArrayList<>();
        RuntimeException sentinel = new IllegalStateException("caller comparator sentinel");
        var expected = source.maskedByPhysicalState(states, Integer.MIN_VALUE, 2, backend)
                .sorted(trace(incumbent, -1, sentinel));
        var actual = source.sortedMaskedByPhysicalState(states, Integer.MIN_VALUE, 2,
                trace(fused, -1, sentinel), backend);
        equal(expected.physicalIndices(), actual.physicalIndices(), "incumbent result parity");
        check(incumbent.equals(fused), "exact comparator demand and order");
        int successfulComparisons = incumbent.size();
        for (int stop = 1; stop <= successfulComparisons; stop++) {
            incumbent.clear();
            fused.clear();
            final int failureAt = stop;
            Throwable before = failure(() -> source.maskedByPhysicalState(states,
                    Integer.MIN_VALUE, 2, backend).sorted(trace(incumbent, failureAt, sentinel)));
            Throwable after = failure(() -> source.sortedMaskedByPhysicalState(states,
                    Integer.MIN_VALUE, 2, trace(fused, failureAt, sentinel), backend));
            check(before == sentinel && after == sentinel, "caller throwable identity");
            check(incumbent.equals(fused), "failure comparator demand and order");
            equal(original, source.physicalIndices(), "failure leaves source immutable");
        }
    }

    private static void boundaries(M3IndexPathMasks.Backend backend) {
        var source = M3IndexedListSnapshot.copyOf(new Integer[] {3, 1, 2, 1});
        equal(new int[] {1, 3, 2, 0}, source.sortedMaskedByPhysicalState(new int[4], 0, 0,
                null, backend).physicalIndices(), "natural order retains equal occurrences");
        var empty = M3IndexedListSnapshot.<Integer>copyOf(new Integer[0]);
        check(empty.sortedMaskedByPhysicalState(new int[0], 0, 0, null, backend).isEmpty(), "empty selection");
        var oneNull = M3IndexedListSnapshot.copyOf(new Integer[] {null});
        check(oneNull.sortedMaskedByPhysicalState(new int[1], 0, 0, null, backend).get(0) == null,
                "singleton natural ordering does not compare null");
        check(failure(() -> source.sortedMaskedByPhysicalState(null, 0, 0, null, backend))
                instanceof NullPointerException, "null state lane");
        check(failure(() -> source.sortedMaskedByPhysicalState(new int[4], 0, 0, null, null))
                instanceof NullPointerException, "null backend");
        AtomicInteger demand = new AtomicInteger();
        check(failure(() -> source.sortedMaskedByPhysicalState(new int[3], 0, 0,
                (a, b) -> { demand.incrementAndGet(); return 0; }, backend))
                instanceof IllegalArgumentException, "alignment refusal");
        check(demand.get() == 0, "invalid metadata refuses before comparator");
        var nulls = M3IndexedListSnapshot.copyOf(new Integer[] {1, null});
        check(failure(() -> nulls.sortedMaskedByPhysicalState(new int[2], 0, 0, null, backend))
                instanceof NullPointerException, "natural null comparison refusal");
        check(failure(() -> source.sortedMaskedByPhysicalState(new int[4], 0, 0, null, backend)
                .removeFirst()) instanceof UnsupportedOperationException, "strict immutable endpoint");
    }

    private static void missingNative() {
        check(!M3IndexPathMasks.nativeAvailable(), "missing mode requires an empty library path");
        for (Integer[] input : List.of(new Integer[0], new Integer[] {2, 1})) {
            var source = M3IndexedListSnapshot.copyOf(input);
            AtomicInteger calls = new AtomicInteger();
            check(failure(() -> source.sortedMaskedByPhysicalState(new int[input.length], 0, 0,
                    (a, b) -> { calls.incrementAndGet(); return Integer.compare(a, b); },
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED)) instanceof UnsatisfiedLinkError,
                    "native-required refuses, including empty input");
            check(calls.get() == 0, "native refusal precedes comparator");
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        String mode = args.length == 0 ? "java" : args.length == 1 ? args[0] : "invalid";
        if (mode.equals("--missing")) {
            missingNative();
        } else {
            M3IndexPathMasks.Backend backend = switch (mode) {
                case "java" -> M3IndexPathMasks.Backend.JAVA;
                case "--native" -> M3IndexPathMasks.Backend.NATIVE_REQUIRED;
                default -> throw new IllegalArgumentException("use no argument, --native, or --missing");
            };
            if (backend == M3IndexPathMasks.Backend.NATIVE_REQUIRED) {
                check(M3IndexPathMasks.nativeAvailable(), "actual pathmask library required");
            }
            cases(backend);
            callbackAndFailure(backend);
            boundaries(backend);
        }
        System.out.println("PASS M3MaskedSortFusionContract mode=" + mode + " checks=" + checks);
    }
}
