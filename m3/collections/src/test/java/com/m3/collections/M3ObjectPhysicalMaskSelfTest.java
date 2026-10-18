// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Dependency-free differential contract for physical-slot-indexed 32-bit mask views. */
public final class M3ObjectPhysicalMaskSelfTest {
    private static long checks;

    private M3ObjectPhysicalMaskSelfTest() { }

    private record Item(int rank, String label) { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void equal(Object expected, Object actual, String reason) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(reason + ": " + expected + " != " + actual);
        }
    }

    private static void refuse(Class<? extends Throwable> exception, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (exception.isInstance(failure)) return;
            throw new AssertionError("wrong exception: " + exception, failure);
        }
        throw new AssertionError("missing refusal: " + exception);
    }

    private static void projectedSortedAndSliced() {
        M3ObjectSlotStore28<Item> owner = new M3ObjectSlotStore28<>();
        owner.add(new Item(2, "a"));
        long removed = owner.add(new Item(0, "removed"));
        owner.add(new Item(1, "b"));
        owner.add(new Item(1, "c"));
        owner.add(new Item(2, "d"));
        owner.remove(removed); // Leave a physical-slot gap in the detached snapshot.

        M3ObjectIndexSnapshot<Item> sorted =
                owner.snapshot().sorted(Comparator.comparingInt(Item::rank));
        equal(List.of("b", "c", "a", "d"),
                sorted.stream().map(Item::label).toList(), "stable sorted path");
        int maxSlot = Arrays.stream(sorted.physicalSlots()).max().orElse(-1);
        int[] stateBySlot = new int[maxSlot + 1];
        for (int i = 0; i < sorted.size(); i++) {
            Item item = sorted.get(i);
            int slot = sorted.physicalSlotAt(i);
            stateBySlot[slot] = switch (item.label()) {
                case "a", "c" -> 0x8000_0001;
                case "b" -> 0x8000_0003; // forbidden bit 1.
                default -> 1;
            };
        }

        M3ObjectIndexSnapshot<Item> selection = sorted.filteredByPhysicalStatePlane(
                stateBySlot, 0x8000_0000, 0x0000_0002);
        equal(List.of("c", "a"), selection.stream().map(Item::label).toList(),
                "mask accepts signed high bit in existing logical order");
        check(selection.order() == M3ObjectIndexSnapshot.Order.COMPARATOR,
                "order identity preserved");
        equal(sorted.physicalSlotAt(1), selection.physicalSlotAt(0),
                "logical ordinal maps to original physical slot");

        M3ObjectIndexSnapshot<Item> slice = sorted.subList(1, 3);
        equal(List.of("c", "a"),
                slice.filteredByPhysicalStatePlane(stateBySlot, 0x8000_0000, 2)
                        .stream().map(Item::label).toList(),
                "slice preserves coordinate system");

        Arrays.fill(stateBySlot, 0);
        equal(List.of("c", "a"), selection.stream().map(Item::label).toList(),
                "external mutable state is not retained");

        if (M3IndexPathMasks.nativeAvailable()) {
            for (int i = 0; i < sorted.size(); i++) {
                stateBySlot[sorted.physicalSlotAt(i)] =
                        "a".equals(sorted.get(i).label()) || "c".equals(sorted.get(i).label())
                                ? 0x8000_0001 : 0x0000_0002;
            }
            M3ObjectIndexSnapshot<Item> nativeResult = sorted.filteredByPhysicalStatePlane(
                    stateBySlot, 0x8000_0000, 0x0000_0002,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            equal(List.of("c", "a"), nativeResult.stream().map(Item::label).toList(),
                    "C/JNI signed-mask and row-order parity");
            check(Arrays.equals(selection.physicalSlots(), nativeResult.physicalSlots()),
                    "Java/JNI physical-offset parity");
        } else {
            refuse(UnsatisfiedLinkError.class,
                    () -> sorted.filteredByPhysicalStatePlane(
                            stateBySlot, 0, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }

        refuse(IndexOutOfBoundsException.class,
                () -> sorted.filteredByPhysicalStatePlane(new int[0], 0, 0));
        refuse(IndexOutOfBoundsException.class,
                () -> M3ObjectIndexSnapshot.<String>capture(
                        new Object[] { "x" }, new int[] { -1 }, new byte[] { 0 })
                        .filteredByPhysicalStatePlane(new int[1], 0, 0));
        check(sorted.size() == 4, "failed selections do not mutate source");
    }

    private static void strictReadonlyEvenForNoOps() {
        M3ObjectIndexSnapshot<String> empty =
                new M3ObjectSlotStore28<String>().snapshot();
        equal(0, empty.size(), "empty snapshot");
        equal(0, empty.filteredByPhysicalStatePlane(new int[0], 0, 0).size(),
                "empty zero-capacity state plane");
        refuse(UnsupportedOperationException.class, empty::clear);
        refuse(UnsupportedOperationException.class, () -> empty.addAll(List.of()));
        refuse(UnsupportedOperationException.class, () -> empty.removeAll(Set.of()));
        refuse(UnsupportedOperationException.class, () -> empty.retainAll(Set.of()));
        refuse(UnsupportedOperationException.class, () -> empty.remove("absent"));
        refuse(UnsupportedOperationException.class, () -> empty.removeIf(value -> false));
        refuse(UnsupportedOperationException.class, () -> empty.replaceAll(value -> value));
        refuse(UnsupportedOperationException.class, () -> empty.sort(null));
        refuse(UnsupportedOperationException.class, () -> empty.addFirst("a"));
        refuse(UnsupportedOperationException.class, () -> empty.addLast("a"));
        refuse(UnsupportedOperationException.class, empty::removeFirst);
        refuse(UnsupportedOperationException.class, empty::removeLast);
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        projectedSortedAndSliced();
        strictReadonlyEvenForNoOps();
        System.out.println("PASS M3_OBJECT_PHYSICAL_MASK checks=" + checks);
    }
}
