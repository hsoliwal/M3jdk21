// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Same-thread fail-fast, optional-write and physical-occurrence regression oracle. */
public final class M3FilteredListMutationProof {
    private static int checks;
    private M3FilteredListMutationProof() { }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message + ": " + actual + " != " + expected);
    }
    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type + ", got " + failure, failure);
        }
        throw new AssertionError("missing " + type.getSimpleName() + " before mutation");
    }
    private static List<Integer> list(int kind, List<Integer> values) {
        return switch (kind) {
            case 0 -> new ArrayList<>(values);
            case 1 -> new LinkedList<>(values);
            case 2 -> Collections.synchronizedList(new ArrayList<>(values));
            default -> throw new AssertionError(kind);
        };
    }
    private static void mutate(ListIterator<Integer> cursor, int operation) {
        switch (operation) {
            case 0 -> cursor.remove();
            case 1 -> cursor.set(80);
            case 2 -> cursor.add(80);
            default -> throw new AssertionError(operation);
        }
    }
    private static void invalidation() {
        for (int kind = 0; kind < 3; kind++) {
            for (int direction = 0; direction < 2; direction++) {
                for (int probing = 0; probing < 4; probing++) {
                    for (int operation = 0; operation < 3; operation++) {
                        List<Integer> backing = list(kind, List.of(1, 2, 3, 4, 5));
                        List<Integer> filtered = M3FilteredLists.filtered(backing, n -> n % 2 == 0);
                        ListIterator<Integer> cursor = filtered.listIterator(direction == 0 ? 0 : 2);
                        equal(direction == 0 ? cursor.next() : cursor.previous(), direction == 0 ? 2 : 4, "delivered");
                        if ((probing & 1) != 0) cursor.hasNext();
                        if ((probing & 2) != 0) cursor.hasPrevious();
                        backing.add(0, 6);
                        List<Integer> unchanged = new ArrayList<>(backing);
                        final int op = operation;
                        expect(ConcurrentModificationException.class, () -> mutate(cursor, op));
                        equal(backing, unchanged, "invalidated mutation must not write");
                    }
                }
            }
            // Exhausted and empty cursors still require the original owner's check.
            for (int size : new int[]{0, 1, 3}) {
                List<Integer> backing = list(kind, Collections.nCopies(size, 2));
                ListIterator<Integer> cursor = M3FilteredLists.filtered(backing, n -> true).listIterator(size);
                backing.add(2);
                backing.remove(backing.size() - 1); // same size, different structural generation
                expect(ConcurrentModificationException.class, () -> cursor.add(8));
                equal(backing.size(), size, "exhausted cursor nonmutation");
            }
        }
    }
    private static void optionalWrites() {
        List<Integer> fixed = Arrays.asList(1, 2, 3, 4);
        ListIterator<Integer> cursor = M3FilteredLists.filtered(fixed, n -> n % 2 == 0).listIterator();
        equal(cursor.next(), 2, "fixed list next");
        cursor.hasNext(); cursor.hasPrevious(); cursor.set(6);
        equal(fixed, List.of(1, 6, 3, 4), "fixed list set");
        expect(UnsupportedOperationException.class, cursor::remove);
        ListIterator<Integer> fixedCursor = cursor;
        expect(UnsupportedOperationException.class, () -> fixedCursor.add(8));
        // Its iterator forbids writes, but existing M3 backing-list mutation is supported.
        List<Integer> copyOnWrite = new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4));
        cursor = M3FilteredLists.filtered(copyOnWrite, n -> n % 2 == 0).listIterator();
        equal(cursor.next(), 2, "snapshot owner next");
        cursor.hasNext(); cursor.set(6); cursor.remove(); cursor.add(8);
        equal(copyOnWrite, List.of(1, 8, 3, 4), "backing write support retained");
        List<Integer> protectedList = M3FilteredLists.readOnlyFiltered(new ArrayList<>(List.of(2)), n -> true);
        ListIterator<Integer> readOnly = protectedList.listIterator();
        readOnly.next();
        expect(UnsupportedOperationException.class, readOnly::remove);
        expect(UnsupportedOperationException.class, () -> readOnly.set(4));
        expect(UnsupportedOperationException.class, () -> readOnly.add(4));
    }
    private static boolean accepted(Integer value) { return value == null || (value & 1) == 0; }
    private static List<Integer> selected(List<Integer> values) {
        List<Integer> result = new ArrayList<>();
        for (Integer value : values) if (accepted(value)) result.add(value);
        return result;
    }
    private static void randomized() {
        for (int kind = 0; kind < 2; kind++) for (int seed = 0; seed < 96; seed++) {
            Random random = new Random(seed * 71L + kind);
            List<Integer> model = new ArrayList<>(Arrays.asList(1, 2, null, 3, 4, 5, 2));
            List<Integer> backing = list(kind, model);
            int[] callbacks = {0};
            Predicate<Integer> predicate = n -> { callbacks[0]++; return accepted(n); };
            List<Integer> view = M3FilteredLists.filtered(backing, predicate);
            ListIterator<Integer> cursor = view.listIterator();
            int position = 1, last = -1, logical = 0;
            boolean lastNext = false;
            for (int step = 0; step < 600; step++) {
                int next = position, previous = position - 1;
                while (next < model.size() && !accepted(model.get(next))) next++;
                while (previous >= 0 && !accepted(model.get(previous))) previous--;
                equal(cursor.hasNext(), next < model.size(), "hasNext");
                equal(cursor.hasPrevious(), previous >= 0, "hasPrevious");
                int op = random.nextInt(5);
                if (op == 0 && next < model.size()) {
                    equal(cursor.next(), model.get(next), "next occurrence");
                    last = next; position = next + 1; logical++; lastNext = true;
                } else if (op == 1 && previous >= 0) {
                    equal(cursor.previous(), model.get(previous), "previous occurrence");
                    last = previous; position = previous; logical--; lastNext = false;
                } else if (op == 2 && last >= 0) {
                    int before = callbacks[0]; cursor.remove();
                    equal(callbacks[0], before, "guard must not invoke predicate on remove");
                    model.remove(last); if (last < position) position--;
                    if (lastNext) logical--; last = -1;
                } else if (op == 3 && last >= 0) {
                    Integer value = random.nextInt(6) == 0 ? null : random.nextInt(8) * 2;
                    int before = callbacks[0]; cursor.set(value);
                    equal(callbacks[0], before + 1, "one accepted-write predicate");
                    model.set(last, value);
                } else if (op == 4) {
                    Integer value = random.nextInt(6) == 0 ? null : random.nextInt(8) * 2;
                    int before = callbacks[0]; cursor.add(value);
                    equal(callbacks[0], before + 1, "one insertion predicate");
                    model.add(position++, value); logical++; last = -1;
                }
                equal(backing, model, "physical sequence and hidden occurrences");
                equal(cursor.nextIndex(), logical, "next logical index");
                equal(cursor.previousIndex(), logical - 1, "previous logical index");
                equal(view, selected(model), "live view sequence");
            }
        }
    }
    private static void boundedAndSorted() {
        List<Integer> owner = new ArrayList<>(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1));
        List<Integer> span = owner.subList(1, 8);
        List<Integer> filtered = M3FilteredLists.filtered(span, n -> (n & 1) == 0);
        filtered.sort(null);
        equal(owner, List.of(9, 2, 7, 4, 5, 6, 3, 8, 1), "sort only selected physical occurrences");
        ListIterator<Integer> reverse = filtered.reversed().listIterator();
        equal(reverse.next(), 8, "reversed next");
        reverse.hasNext(); reverse.hasPrevious(); reverse.set(10); reverse.remove(); reverse.add(12);
        equal(filtered.reversed(), List.of(12, 6, 4, 2), "reversed cursor edits");
    }
    public static void main(String[] args) {
        invalidation(); optionalWrites(); randomized(); boundedAndSorted();
        System.out.println("M3_FILTERED_LIST_MUTATION_PASS checks=" + checks);
    }
}
