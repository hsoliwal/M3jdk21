// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Positive observations of a rejected change and an unresolved defect.
 * A successful diagnostic run reproduces these observations; it is not acceptance.
 * The independent M3CandidateListBoundaryProof keeps the required behavior RED.
 */
public final class M3ListMutationGuardCounterexamples {
    private M3ListMutationGuardCounterexamples() { }

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

    private static void observeStaleSubListMutation(String version, boolean empty, String operation) {
        LinkedList<Integer> root = new LinkedList<>(List.of(2, 4));
        List<Integer> span = root.subList(0, empty ? 0 : 2);
        ListIterator<Integer> direct = span.listIterator();
        ListIterator<Integer> filtered = M3FilteredLists.filtered(span, value -> true).listIterator();
        while (direct.hasNext()) direct.next();
        while (filtered.hasNext()) filtered.next();
        span.add(6);
        span.remove(span.size() - 1);
        require(root.equals(List.of(2, 4)), "the structural change restored contents and size");
        Object directResult = outcome(() -> mutate(direct, operation));
        Object filteredResult = outcome(() -> mutate(filtered, operation));
        require(directResult instanceof ConcurrentModificationException,
                "the actual JDK subList iterator rejects stale mutation");
        require("success".equals(filteredResult),
                "KNOWN DEFECT: the filtered cursor performs a stale mutation");
        List<Integer> defectiveContents = switch (operation) {
            case "add" -> empty ? List.of(8, 2, 4) : List.of(2, 4, 8);
            case "set" -> List.of(2, 8);
            case "remove" -> List.of(2);
            default -> throw new AssertionError(operation);
        };
        require(root.equals(defectiveContents), "exact observed stale write effect");
        System.out.println("KNOWN-DEFECT stale-sublist-mutation version=" + version
                + " empty=" + empty + " operation=" + operation
                + " native=ConcurrentModificationException filtered=success owner=" + root);
    }

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

    private static void observeChangedReadEffects(String version) {
        ReadSensitiveList owner = new ReadSensitiveList();
        ListIterator<Integer> cursor = M3FilteredLists.filtered(owner, value -> (value & 1) == 0).listIterator();
        require(cursor.next() == 2, "deliver the selected physical occurrence");
        owner.tape.clear();
        Object result = outcome(() -> cursor.set(8));
        if (version.equals("baseline")) {
            require("success".equals(result), "baseline set succeeds");
            require(owner.tape.equals(List.of("set:1:8")), "baseline callback tape");
            require(owner.values[1] == 8, "baseline writes the delivered occurrence");
        } else {
            require(result == owner.marker, "REJECTED CHANGE: exact caller failure from unrelated next element");
            require(owner.tape.equals(List.of("get:2")), "rejected change adds an unrequested read before writing");
            require(owner.values[1] == 2, "rejected change fails before the valid write");
        }
        System.out.println("REJECTED-CHANGE added-read-effect version=" + version
                + " outcome=" + (result == owner.marker ? "same caller marker" : result)
                + " tape=" + owner.tape + " stored=" + owner.values[1]);
    }

    public static void main(String[] args) {
        require(args.length == 1 && (args[0].equals("baseline") || args[0].equals("rejected")),
                "supply the explicitly replayed source version: baseline or rejected");
        String version = args[0];
        observeChangedReadEffects(version);
        observeStaleSubListMutation(version, true, "add");
        for (String operation : List.of("add", "set", "remove")) {
            observeStaleSubListMutation(version, false, operation);
        }
        System.out.println("DIAGNOSTIC OBSERVATIONS REPRODUCED version=" + version
                + "; unresolved acceptance oracles remain RED");
    }
}
