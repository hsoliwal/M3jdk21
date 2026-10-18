/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Source-sealed Synexia JDK21 no-op read-only collection contract proof.
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** The same immutable contract must hold for direct, filtered, sorted, sliced
 * and JDK21 reversed indexed-array views, regardless of logical size.
 */
public final class M3IndexedReadOnlyNoopProof {
    private static long checks;
    private static final Comparator<Integer> ORDER =
            Comparator.nullsFirst(Comparator.naturalOrder());

    private M3IndexedReadOnlyNoopProof() { }

    private static void check(boolean good, String why) {
        checks++;
        if (!good) throw new AssertionError(why + " check=" + checks);
    }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void refusal(Runnable mutation, String label) {
        checks++;
        try {
            mutation.run();
        } catch (UnsupportedOperationException refused) {
            return;
        }
        throw new AssertionError("silent/no-op mutation accepted: " + label);
    }

    private static void verify(List<Integer> readOnly, String name) {
        List<Integer> original = new ArrayList<>(readOnly);
        Object[] referenceArray = readOnly.toArray();
        check(readOnly instanceof java.util.RandomAccess, name + " random access");
        refusal(() -> readOnly.set(0, 9), name + " set");
        refusal(() -> readOnly.add(0, 9), name + " add at index");
        refusal(() -> readOnly.add(9), name + " add");
        refusal(() -> readOnly.addAll(List.of()), name + " empty addAll");
        refusal(() -> readOnly.addAll(0, List.of()), name + " empty indexed addAll");
        refusal(() -> readOnly.remove(0), name + " remove at index");
        refusal(() -> readOnly.remove(Integer.valueOf(999)), name + " remove absent");
        refusal(() -> readOnly.removeAll(List.of(999)), name + " removeAll absent");
        refusal(() -> readOnly.retainAll(new ArrayList<>(readOnly)), name + " retain all");
        AtomicInteger predicateCalls = new AtomicInteger();
        refusal(() -> readOnly.removeIf(x -> {
            predicateCalls.incrementAndGet();
            return false;
        }), name + " removeIf false");
        equal(0, predicateCalls.get(), name + " refuse before predicate");
        AtomicInteger functionCalls = new AtomicInteger();
        refusal(() -> readOnly.replaceAll(x -> {
            functionCalls.incrementAndGet();
            return x;
        }), name + " replaceAll identity");
        equal(0, functionCalls.get(), name + " refuse before unary operator");
        refusal(() -> readOnly.sort((a, b) -> {
            functionCalls.incrementAndGet();
            return 0;
        }), name + " sort comparator");
        equal(0, functionCalls.get(), name + " refuse before comparator");
        refusal(readOnly::clear, name + " clear");
        refusal(() -> readOnly.addFirst(9), name + " addFirst");
        refusal(() -> readOnly.addLast(9), name + " addLast");

        // On an empty JDK reverse view removeFirst/removeLast can be specified as
        // NoSuchElementException. The owner itself intentionally refuses always.
        if (readOnly instanceof M3ObjectIndexSnapshot<?>
                || readOnly instanceof M3IndexedListSnapshot<?>) {
            refusal(readOnly::removeFirst, name + " removeFirst");
            refusal(readOnly::removeLast, name + " removeLast");
        }
        List<Integer> reverse = readOnly.reversed();
        equal(new ArrayList<>(readOnly.reversed()), new ArrayList<>(reverse),
                name + " JDK21 reverse encounter order");
        refusal(reverse::clear, name + " reverse empty clear");
        refusal(() -> reverse.removeIf(x -> false), name + " reverse removeIf false");
        refusal(() -> reverse.addAll(List.of()), name + " reverse empty addAll");
        refusal(() -> reverse.removeAll(List.of(999)), name + " reverse remove absent");
        refusal(() -> reverse.retainAll(new ArrayList<>(reverse)),
                name + " reverse retain all");
        refusal(() -> reverse.reversed().clear(), name + " double reverse empty clear");
        refusal(() -> readOnly.subList(0, 0).clear(), name + " empty subList clear");
        refusal(() -> readOnly.subList(0, 0).removeIf(x -> false),
                name + " empty subList removeIf");
        Iterator<Integer> cursor = readOnly.iterator();
        if (cursor.hasNext()) {
            Integer first = cursor.next();
            equal(original.get(0), first, name + " iterator original first");
            refusal(cursor::remove, name + " iterator remove");
            ListIterator<Integer> listCursor = readOnly.listIterator();
            listCursor.next();
            refusal(() -> listCursor.set(99), name + " list iterator set");
            refusal(() -> listCursor.add(99), name + " list iterator add");
        }
        equal(original, readOnly, name + " unchanged contents");
        check(Arrays.equals(referenceArray, readOnly.toArray()), name + " unchanged array");
    }

    private static M3ObjectIndexSnapshot<Integer> objectSnapshot(Integer[] input) {
        Object[] owned = Arrays.copyOf(input, input.length, Object[].class);
        int[] slots = new int[input.length];
        byte[] flags = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            slots[i] = i;
            flags[i] = (byte) ((i % 3 == 0) ? 1 : 0);
        }
        return M3ObjectIndexSnapshot.capture(owned, slots, flags);
    }

    private static void checkOwner(Integer[] input, String name) {
        M3ObjectIndexSnapshot<Integer> object = objectSnapshot(input);
        verify(object, name + " object direct");
        verify(object.filtered(x -> x == null || x >= 0), name + " object filtered");
        verify(object.filteredByState(1, 0), name + " object state mask");
        verify(object.sorted(ORDER), name + " object sorted");
        verify(object.subList(0, object.size()), name + " object slice");
        M3IndexedListSnapshot<Integer> indexed = M3IndexedListSnapshot.copyOf(input);
        verify(indexed, name + " indexed direct");
        verify(indexed.filtered(x -> x == null || x >= 0), name + " indexed filtered");
        verify(indexed.sorted(ORDER), name + " indexed sorted");
        verify(indexed.subList(0, indexed.size()), name + " indexed slice");
        if (input.length != 0) {
            verify(object.subList(1, object.size()), name + " object suffix slice");
            verify(indexed.subList(1, indexed.size()), name + " indexed suffix slice");
        }
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        checkOwner(new Integer[0], "empty");
        checkOwner(new Integer[] {null}, "null only");
        checkOwner(new Integer[] {4, 4, null, -3, -3, 0, 7}, "duplicates/null");
        Random random = new Random(0x4d33494d4d555441L);
        for (int round = 0; round < 175; round++) {
            Integer[] input = new Integer[random.nextInt(32)];
            for (int i = 0; i < input.length; i++) {
                input[i] = random.nextInt(7) == 0 ? null : random.nextInt(31) - 15;
            }
            checkOwner(input, "random " + round);
        }
        System.out.println("PASS M3IndexedReadOnlyNoopProof checks=" + checks);
    }
}
