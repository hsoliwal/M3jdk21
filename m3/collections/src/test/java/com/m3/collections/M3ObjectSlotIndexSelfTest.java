/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free Java 21 differential/contract proof for object slot/index projections. */
public final class M3ObjectSlotIndexSelfTest {
    private static long checks;

    private M3ObjectSlotIndexSelfTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": " + expected + " != " + actual);
        }
    }

    private static void equal(int[] expected, int[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(message + ": " + Arrays.toString(expected)
                    + " != " + Arrays.toString(actual));
        }
    }

    private static void refuse(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong refusal, expected " + type, failure);
        }
        throw new AssertionError("missing refusal " + type);
    }

    private static void lifecycleAndState() {
        M3ObjectSlotStore28<String> store = new M3ObjectSlotStore28<>();
        long a = store.add("a", M3ObjectStateSlots28.VISIBLE);
        long b = store.add("b", M3ObjectStateSlots28.DIRTY);
        long c = store.add("c", M3ObjectStateSlots28.VISIBLE | M3ObjectStateSlots28.DIRTY);
        long n = store.add(null, M3ObjectStateSlots28.VISIBLE);

        equal(4, store.size(), "size");
        equal("a", store.get(a), "get");
        equal(null, store.get(n), "null payload");
        check(store.allocatedPayloadBlocks() == 1, "non-null payloads share first page");

        M3ObjectIndexSnapshot<String> before = store.snapshot();
        equal(Arrays.asList("a", "b", "c", null), Arrays.asList(before.toArray()), "physical-slot snapshot");
        equal(new int[] {0, 1, 2, 3}, before.physicalSlots(), "physical slots");
        check(before.order() == M3ObjectIndexSnapshot.Order.PHYSICAL_SLOT, "physical order");

        M3ObjectIndexSnapshot<String> visibleClean = before.filteredByState(
                M3ObjectStateSlots28.VISIBLE, M3ObjectStateSlots28.DIRTY);
        equal(2, visibleClean.size(), "visible non-dirty");
        equal("a", visibleClean.get(0), "first clean");
        equal(null, visibleClean.get(1), "null clean");

        equal("b", store.remove(b), "remove");
        refuse(IllegalStateException.class, () -> store.get(b));
        long d = store.add("d", M3ObjectStateSlots28.USER_0);
        equal(M3SlotAllocator28.slot(b), M3SlotAllocator28.slot(d), "slot reuse");
        check(b != d, "generation changes");
        equal("a", store.set(a, "A"), "replace");
        equal("a", before.get(0), "detached snapshot keeps old reference");
        equal("A", store.get(a), "store sees replacement");

        int old = store.updateState(d, M3ObjectStateSlots28.VISIBLE, 0);
        equal(M3ObjectStateSlots28.USER_0, old, "state update old");
        check((store.state(d) & M3ObjectStateSlots28.VISIBLE) != 0, "state update new");
        check(store.revision() > 0, "revision advances");

        M3ObjectIndexSnapshot<String> after = store.snapshot();
        equal(Arrays.asList("A", "d", "c", null), Arrays.asList(after.toArray()), "reused slot order");
        check(after.retainedPrimitiveBytes() > 0, "primitive metadata retained");
        equal(4, after.retainedReferenceSlots(), "captured references");
        refuse(UnsupportedOperationException.class, () -> after.add("x"));

        store.remove(a);
        store.remove(c);
        store.remove(n);
        store.remove(d);
        check(store.isEmpty(), "empty after releases");
    }

    private static void predicateAndSorting() {
        record Item(int rank, String id) { }
        M3ObjectSlotStore28<Item> store = new M3ObjectSlotStore28<>();
        store.add(new Item(2, "a"), 1);
        store.add(new Item(1, "b"), 1);
        store.add(new Item(1, "c"), 3);
        store.add(new Item(2, "d"), 1);

        M3ObjectIndexSnapshot<Item> all = store.snapshot();
        AtomicInteger calls = new AtomicInteger();
        M3ObjectIndexSnapshot<Item> rankOne = all.filtered(item -> {
            calls.incrementAndGet();
            return item.rank() == 1;
        });
        equal(4, calls.get(), "one predicate call per candidate");
        equal(List.of("b", "c"), rankOne.stream().map(Item::id).toList(), "filtered values");
        rankOne.get(0);
        rankOne.subList(0, rankOne.size());
        rankOne.sorted(Comparator.comparing(Item::id));
        equal(4, calls.get(), "predicate result is cached as index projection");

        M3ObjectIndexSnapshot<Item> stable = all.sorted(Comparator.comparingInt(Item::rank));
        equal(List.of("b", "c", "a", "d"), stable.stream().map(Item::id).toList(), "stable ties");

        AtomicInteger projected = new AtomicInteger();
        M3ObjectIndexSnapshot<Item> signal = all.sortedBySignal(item -> {
            projected.incrementAndGet();
            return item.rank();
        }, false);
        equal(4, projected.get(), "one signal projection per candidate");
        equal(List.of("a", "d", "b", "c"), signal.stream().map(Item::id).toList(), "signal order");
        signal.get(0);
        signal.subList(1, 3);
        equal(4, projected.get(), "signal is primitive precompute");

        AtomicInteger nativeProjected = new AtomicInteger();
        if (M3SignalOrderNative.available()) {
            M3ObjectIndexSnapshot<Item> nativeSignal = all.sortedBySignal(item -> {
                nativeProjected.incrementAndGet();
                return item.rank();
            }, false, M3SignalOrder.Backend.NATIVE_REQUIRED);
            equal(4, nativeProjected.get(), "one native signal projection per candidate");
            equal(signal.physicalSlots(), nativeSignal.physicalSlots(), "JNI signal-order parity");
        } else {
            refuse(UnsatisfiedLinkError.class, () -> all.sortedBySignal(
                    Item::rank, false, M3SignalOrder.Backend.NATIVE_REQUIRED));
        }

        int[] escaped = all.physicalSlots();
        escaped[0] = -1;
        check(all.physicalSlotAt(0) >= 0, "physical slot array is defensive");

        M3ObjectIndexSnapshot<Item> state = all.filteredByState(1, 2);
        equal(List.of("a", "b", "d"), state.stream().map(Item::id).toList(), "required/excluded mask");

        if (M3IndexMaskCompactor.nativeAvailable()) {
            M3ObjectIndexSnapshot<Item> nativeState = all.filteredByState(
                    1, 2, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            equal(state.physicalSlots(), nativeState.physicalSlots(), "JNI state-mask parity");
        } else {
            refuse(UnsatisfiedLinkError.class, () -> all.filteredByState(
                    1, 2, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }

    private static void sparseNullPayload() {
        M3ObjectSlotStore28<Object> store = new M3ObjectSlotStore28<>();
        long[] handles = new long[4097];
        for (int i = 0; i < handles.length; i++) handles[i] = store.add(null);
        equal(0, store.allocatedPayloadBlocks(), "null payload does not allocate reference pages");
        check(store.handlePlaneBytes() >= (long) 4097 * Long.BYTES,
                "handle plane grows by segmented primitive blocks");
        equal(4097, store.snapshot().size(), "multi-page snapshot");
        for (long handle : handles) store.remove(handle);
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        lifecycleAndState();
        predicateAndSorting();
        sparseNullPayload();
        System.out.println("PASS M3_OBJECT_SLOT_INDEX checks=" + checks);
    }
}
