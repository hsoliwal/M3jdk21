// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/** The original provider iterator owns structural invalidation, including bounded ends. */
class M3FilteredListProviderInvalidationTest {
    @Test
    void exhaustedNonemptySublistMutationsMatchOriginalProviderRefusal() {
        for (boolean nested : List.of(false, true)) {
            for (int operation = 0; operation < 3; operation++) {
                List<Integer> owner = new LinkedList<>(List.of(0, 2, 4, 6));
                List<Integer> span = span(owner, nested, false);
                ListIterator<Integer> raw = span.listIterator();
                ListIterator<Integer> filtered = M3FilteredLists.filtered(span, x -> true).listIterator();
                assertEquals(raw.next(), filtered.next());
                assertEquals(raw.next(), filtered.next());
                span.add(0, 8);
                span.remove(2);
                List<Integer> before = new ArrayList<>(owner);
                int selected = operation;
                assertThrows(ConcurrentModificationException.class, () -> mutate(raw, selected));
                assertThrows(ConcurrentModificationException.class, () -> mutate(filtered, selected));
                assertEquals(before, owner, "invalidated writes must not target a replacement occurrence");
            }
        }
    }

    @Test
    void emptySublistAddMatchesOriginalProviderGenerationCheck() {
        for (boolean nested : List.of(false, true)) {
            List<Integer> owner = new LinkedList<>(List.of(0, 6));
            List<Integer> span = span(owner, nested, true);
            ListIterator<Integer> raw = span.listIterator();
            ListIterator<Integer> filtered = M3FilteredLists.filtered(span, x -> true).listIterator();
            span.add(8);
            span.remove(0);
            List<Integer> before = new ArrayList<>(owner);
            assertThrows(ConcurrentModificationException.class, () -> raw.add(10));
            assertThrows(ConcurrentModificationException.class, () -> filtered.add(10));
            assertEquals(before, owner);
        }
    }

    @Test
    void clearedDeliveredSpanRefusesMutationsFromEitherDirection() {
        for (boolean nested : List.of(false, true)) {
            for (boolean reverse : List.of(false, true)) {
                for (int operation = 0; operation < 3; operation++) {
                    List<Integer> owner = new LinkedList<>(List.of(0, 2, 4, 6));
                    List<Integer> span = span(owner, nested, false);
                    int position = reverse ? span.size() : 0;
                    ListIterator<Integer> raw = span.listIterator(position);
                    ListIterator<Integer> filtered = M3FilteredLists.filtered(span, x -> true).listIterator(position);
                    assertEquals(reverse ? raw.previous() : raw.next(),
                            reverse ? filtered.previous() : filtered.next());
                    span.clear();
                    List<Integer> before = new ArrayList<>(owner);
                    int selected = operation;
                    assertThrows(ConcurrentModificationException.class, () -> mutate(raw, selected));
                    assertThrows(ConcurrentModificationException.class, () -> mutate(filtered, selected));
                    assertEquals(before, owner);
                }
            }
        }
    }

    @Test
    void wholeLinkedListEndRemainsFailFastAndUnchanged() {
        for (int operation = 0; operation < 3; operation++) {
            List<Integer> owner = new LinkedList<>(List.of(2, 4));
            ListIterator<Integer> raw = owner.listIterator();
            ListIterator<Integer> filtered = M3FilteredLists.filtered(owner, x -> true).listIterator();
            raw.next(); raw.next(); filtered.next(); filtered.next();
            owner.add(0, 8);
            owner.remove(2);
            List<Integer> before = new ArrayList<>(owner);
            int selected = operation;
            assertThrows(ConcurrentModificationException.class, () -> mutate(raw, selected));
            assertThrows(ConcurrentModificationException.class, () -> mutate(filtered, selected));
            assertEquals(before, owner);
        }
    }

    @Test
    void validEmptyProviderWritesKeepCursorIndexesAndInsertOnce() {
        for (List<Integer> owner : List.<List<Integer>>of(new LinkedList<>(),
                new ArrayList<>(), new CopyOnWriteArrayList<>())) {
            ListIterator<Integer> filtered = M3FilteredLists.filtered(owner, x -> x % 2 == 0).listIterator();
            filtered.add(2);
            assertEquals(List.of(2), owner);
            assertEquals(1, filtered.nextIndex());
            assertEquals(0, filtered.previousIndex());
            assertThrows(IllegalStateException.class, filtered::remove);
            assertEquals(2, filtered.previous());
            filtered.set(4);
            assertEquals(List.of(4), owner);
            filtered.remove();
            assertEquals(List.of(), owner);
            assertEquals(0, filtered.nextIndex());
            filtered.add(6);
            assertEquals(List.of(6), owner);
        }
    }

    @Test
    void fixedSizeAndUnmodifiableEmptyProvidersStillRefuseInsertion() {
        for (List<Integer> owner : List.of(Arrays.<Integer>asList(),
                Collections.<Integer>emptyList(),
                Collections.unmodifiableList(new LinkedList<Integer>()))) {
            ListIterator<Integer> cursor = M3FilteredLists.filtered(owner, x -> true).listIterator();
            assertThrows(UnsupportedOperationException.class, () -> cursor.add(2));
            assertEquals(List.of(), owner);
            assertEquals(0, cursor.nextIndex());
        }
    }

    @Test
    void validNonemptyWritesRetainSnapshotAndFixedSizeProviderPolicies() {
        List<Integer> fixed = Arrays.asList(1, 2, 3);
        ListIterator<Integer> fixedCursor = M3FilteredLists.filtered(fixed, x -> x % 2 == 0).listIterator();
        assertEquals(2, fixedCursor.next());
        fixedCursor.set(4);
        assertEquals(List.of(1, 4, 3), fixed);
        assertThrows(UnsupportedOperationException.class, fixedCursor::remove);
        List<Integer> copyOnWrite = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        ListIterator<Integer> snapshot = M3FilteredLists.filtered(copyOnWrite, x -> x % 2 == 0).listIterator();
        assertEquals(2, snapshot.next());
        snapshot.set(4);
        snapshot.remove();
        snapshot.add(6);
        assertEquals(List.of(1, 6, 3), copyOnWrite);
    }

    private static List<Integer> span(List<Integer> owner, boolean nested, boolean empty) {
        List<Integer> result = empty ? owner.subList(1, 1) : owner.subList(1, 3);
        return nested ? result.subList(0, result.size()) : result;
    }

    private static void mutate(ListIterator<Integer> iterator, int operation) {
        switch (operation) {
            case 0 -> iterator.remove();
            case 1 -> iterator.set(10);
            case 2 -> iterator.add(10);
            default -> throw new AssertionError(operation);
        }
    }
}
