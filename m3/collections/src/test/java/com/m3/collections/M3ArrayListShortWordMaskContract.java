/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Executable Java 21 JDK differential for a parent-owned Object[] and transient state word. */
public final class M3ArrayListShortWordMaskContract {
    private static long checks;
    private M3ArrayListShortWordMaskContract() { }

    private static void require(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    private static Object[] input(int n) {
        Object[] values = new Object[n];
        Object repeated = new Object();
        for (int i = 0; i < n; i++) values[i] = i % 7 == 0 ? null
                : i % 5 == 0 ? repeated : new Object();
        return values;
    }

    private static boolean removeAt(int ordinal, int n, int pattern) {
        return switch (pattern) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> ordinal == 0;
            case 3 -> ordinal == n - 1;
            case 4 -> (ordinal & 1) == 0;
            case 5 -> ordinal == 63;
            case 6 -> ordinal == 64;
            case 7 -> (ordinal & 63) == 63;
            case 8 -> ordinal % 3 == 0;
            case 9 -> ordinal < n / 2;
            case 10 -> ordinal >= n / 2;
            case 11 -> ordinal == 127;
            case 12 -> ordinal == 128;
            case 13 -> ordinal == 65;
            case 14 -> (ordinal & 63) == 0;
            case 15 -> ordinal == n / 2;
            default -> ordinal == n / 2;
        };
    }

    private static void identities(List<?> expected, List<?> actual) {
        require(expected.size() == actual.size(), "size changed");
        for (int i = 0; i < expected.size(); i++)
            require(expected.get(i) == actual.get(i), "physical occurrence identity at " + i);
    }

    private static void verifyCase(int n, int pattern) {
        Object[] original = input(n);
        M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(original));
        actual.ensureCapacity(n + 11);
        int capacity = actual.capacity();
        ArrayList<Object> reference = new ArrayList<>(Arrays.asList(original));
        Iterator<Object> oldCursor = actual.iterator();
        int[] calls = {0}, controlCalls = {0};
        boolean changed = actual.removeIf(element -> removeAt(calls[0]++, n, pattern));
        boolean expected = reference.removeIf(element -> removeAt(controlCalls[0]++, n, pattern));
        require(changed == expected, "boolean return");
        require(calls[0] == n && controlCalls[0] == n, "callback count/encounter order");
        require(actual.capacity() == capacity, "same dynamic Object[] capacity");
        identities(reference, actual);
        if (changed) {
            try {
                oldCursor.next();
                throw new AssertionError("stale iterator not invalidated");
            } catch (ConcurrentModificationException accepted) {
                checks++;
            }
        } else if (n != 0) {
            require(oldCursor.next() == original[0], "no-op preserves iterator epoch");
        }
    }

    private static void callbackFailure() {
        for (int n : new int[] {1, 31, 63, 64, 65, 96, 127, 128, 129, 256}) {
            Object[] original = input(n);
            M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(original));
            IllegalStateException marker = new IllegalStateException("callback marker");
            Iterator<Object> old = actual.iterator();
            int[] called = {0};
            try {
                actual.removeIf(value -> {
                    if (called[0]++ == n / 2) throw marker;
                    return true;
                });
                throw new AssertionError("callback failure was lost");
            } catch (IllegalStateException caught) {
                require(caught == marker, "same callback exception instance");
            }
            identities(Arrays.asList(original), actual);
            require(old.next() == original[0], "exception retains epoch");
        }
    }

    private static void structuralMutation() {
        for (int n : new int[] {1, 63, 64, 65, 127, 128, 129}) {
            M3ArrayList<Object> actual = new M3ArrayList<>(Arrays.asList(input(n)));
            Object appended = new Object();
            boolean[] once = {false};
            try {
                actual.removeIf(value -> {
                    if (!once[0]) { once[0] = true; actual.add(appended); }
                    return true;
                });
                throw new AssertionError("reentrant edit was accepted");
            } catch (ConcurrentModificationException expected) {
                require(actual.size() == n + 1, "callback edit survives");
                require(actual.get(n) == appended, "outer pass did not commit");
            }
        }
    }

    public static void main(String[] args) {
        require(args.length == 0, "unexpected arguments");
        // Exhaust both 64/65 and 128/129 register-mask transitions.
        for (int n = 0; n <= 129; n++) for (int p = 0; p < 16; p++) verifyCase(n, p);
        for (int n : new int[] {191, 192, 255, 256, 257, 511, 512, 1024}) {
            for (int p = 0; p < 16; p++) verifyCase(n, p);
        }
        Random seeded = new Random(0x4d33534d414c4cL);
        for (int trial = 0; trial < 1000; trial++)
            verifyCase(seeded.nextInt(1025), seeded.nextInt(16));
        callbackFailure();
        structuralMutation();
        System.out.println("PASS M3ArrayListShortWordMaskContract checks=" + checks);
    }
}
