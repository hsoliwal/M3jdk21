// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;

/** Independent JDK cursor observations for each formerly stale indexed write. */
public final class M3FilteredListMutationWitness {
    private M3FilteredListMutationWitness() { }

    private record Observation(String failure, List<Integer> values) { }

    private static Observation observe(boolean filtered, int operation) {
        List<Integer> backing = new ArrayList<>(List.of(2, 4));
        ListIterator<Integer> cursor = (filtered
                ? M3FilteredLists.filtered(backing, value -> true) : backing).listIterator();
        if (cursor.next() != 2) throw new AssertionError("initial occurrence");
        backing.add(0, 6);
        String failure = "NONE";
        try {
            switch (operation) {
                case 0 -> cursor.remove();
                case 1 -> cursor.set(80);
                case 2 -> cursor.add(80);
                default -> throw new AssertionError(operation);
            }
        } catch (RuntimeException thrown) {
            failure = thrown.getClass().getName();
        }
        return new Observation(failure, List.copyOf(backing));
    }

    public static void main(String[] args) {
        if (args.length != 1 || (!args[0].equals("--expect-broken")
                && !args[0].equals("--expect-fixed"))) {
            throw new IllegalArgumentException("usage: --expect-broken|--expect-fixed");
        }
        boolean expectBroken = args[0].equals("--expect-broken");
        String[] names = {"remove", "set", "add"};
        int mismatches = 0;
        for (int operation = 0; operation < names.length; operation++) {
            Observation reference = observe(false, operation);
            Observation actual = observe(true, operation);
            Observation expected = new Observation(
                    ConcurrentModificationException.class.getName(), List.of(6, 2, 4));
            if (!reference.equals(expected)) throw new AssertionError("JDK control changed");
            if (!reference.equals(actual)) mismatches++;
            System.out.println("mutation=" + names[operation] + " jdk=" + reference
                    + " filtered=" + actual);
        }
        if (mismatches != (expectBroken ? 3 : 0)) {
            throw new AssertionError("unexpected JDK differential count: " + mismatches);
        }
        System.out.println("MUTATION_WITNESS_PASS expectedBroken=" + expectBroken
                + " mismatches=" + mismatches);
    }
}
