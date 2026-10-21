// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

/** Independent rejection witnesses for the proposed raw-cursor mutation guard. */
public final class M3CandidateListBoundaryProof {
    private M3CandidateListBoundaryProof() { }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Object outcome(Runnable action) {
        try { action.run(); return "success"; }
        catch (RuntimeException | Error failure) { return failure; }
    }
    private static void mutate(ListIterator<Integer> cursor, String operation) {
        switch (operation) {
            case "add" -> cursor.add(8);
            case "set" -> cursor.set(8);
            case "remove" -> cursor.remove();
            default -> throw new AssertionError(operation);
        }
    }

    private static void subListGeneration(boolean empty, String operation) {
        LinkedList<Integer> root = new LinkedList<>(List.of(2, 4));
        List<Integer> span = root.subList(0, empty ? 0 : 2);
        ListIterator<Integer> direct = span.listIterator();
        ListIterator<Integer> filtered = M3FilteredLists.filtered(span, value -> true).listIterator();
        while (direct.hasNext()) direct.next();
        while (filtered.hasNext()) filtered.next();
        span.add(6);
        span.remove(span.size() - 1);
        List<Integer> expected = new ArrayList<>(root);
        Object directResult = outcome(() -> mutate(direct, operation));
        Object filteredResult = outcome(() -> mutate(filtered, operation));
        System.out.println("SUBLIST-GENERATION empty=" + empty + " operation=" + operation
                + " direct=" + directResult.getClass().getSimpleName()
                + " filtered=" + filteredResult + " owner=" + root);
        require(directResult instanceof ConcurrentModificationException, "direct iterator rejects stale mutation");
        require(filteredResult instanceof ConcurrentModificationException,
                "filtered mutation must preserve the original subList iterator's generation check");
        require(root.equals(expected), "stale filtered mutation must not change owner");
    }

    /** A lawful writable lazy list: unrelated element loads can throw user failures. */
    private static final class ReadSensitiveList extends AbstractList<Integer> {
        private final Integer[] values = {1, 2, 3};
        private final ArrayList<String> tape = new ArrayList<>();
        private final IllegalStateException marker = new IllegalStateException("unrequested next-element load");
        @Override public int size() { return values.length; }
        @Override public Integer get(int index) {
            tape.add("get:" + index);
            if (index == 2) throw marker;
            return values[index];
        }
        @Override public Integer set(int index, Integer value) {
            tape.add("set:" + index + ":" + value);
            Integer previous = values[index];
            values[index] = value;
            return previous;
        }
    }

    private static void noUnrequestedRead() {
        ReadSensitiveList owner = new ReadSensitiveList();
        ListIterator<Integer> cursor = M3FilteredLists.filtered(owner, value -> (value & 1) == 0).listIterator();
        require(cursor.next() == 2, "first visible occurrence is already delivered");
        owner.tape.clear();
        Object result = outcome(() -> cursor.set(8));
        System.out.println("WRITE-READ-BOUNDARY outcome=" + (result == owner.marker ? "same caller marker" : result)
                + " tape=" + owner.tape + " stored=" + owner.values[1]);
        require("success".equals(result), "writing delivered occurrence must not load unrelated next element");
        require(owner.tape.equals(List.of("set:1:8")), "preserve original backing callback schedule on valid set");
        require(owner.values[1] == 8, "delivered physical occurrence was written");
    }

    public static void main(String[] args) {
        switch (args.length == 0 ? "transport" : args[0]) {
            case "transport" -> noUnrequestedRead();
            case "empty-add" -> subListGeneration(true, "add");
            case "exhausted-add" -> subListGeneration(false, "add");
            case "exhausted-set" -> subListGeneration(false, "set");
            case "exhausted-remove" -> subListGeneration(false, "remove");
            default -> throw new AssertionError(args[0]);
        }
    }
}
