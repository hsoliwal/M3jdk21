/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Canonical ArrayList removal uses original contracts with inline mask words
 * @run main/othervm M3ArrayListInlineMaskTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** An independent reference-identity and subList differential oracle. */
public final class M3ArrayListInlineMaskTest {
    private static final Object REPEATED = new Object();
    private static long checks;
    private static long cases;

    private M3ArrayListInlineMaskTest() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static Object[] values(int length) {
        Object[] source = new Object[length];
        for (int i = 0; i < length; i++) {
            source[i] = i % 7 == 0 ? null : i % 5 == 0 ? REPEATED : new Object();
        }
        return source;
    }

    private static boolean rejected(int position, int length, int pattern) {
        return switch (pattern) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> position == 0;
            case 3 -> position == length - 1;
            case 4 -> (position & 1) == 0;
            case 5 -> position == 63;
            case 6 -> position == 64;
            case 7 -> position == 127;
            case 8 -> position == 128;
            case 9 -> position % 3 == 0;
            case 10 -> position < length / 2;
            case 11 -> position >= length / 2;
            case 12 -> (position & 63) == 0;
            case 13 -> (position & 63) == 63;
            case 14 -> position >= 3 && position < 7;
            default -> position == length / 2;
        };
    }

    private static void identities(List<?> expected, List<?> actual, String operation) {
        check(expected.size() == actual.size(), operation + " size");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), operation + " identity " + i);
        }
    }

    private static void rootCase(int length, int pattern) {
        cases++;
        Object[] inputs = values(length);
        ArrayList<Object> actual = new ArrayList<>(Arrays.asList(inputs));
        Iterator<Object> before = actual.iterator();
        ArrayList<Object> expected = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            if (!rejected(i, length, pattern)) expected.add(inputs[i]);
        }
        int[] called = {0};
        boolean changed = actual.removeIf(element -> {
            int ordinal = called[0]++;
            check(element == inputs[ordinal], "callback physical identity");
            return rejected(ordinal, length, pattern);
        });
        check(changed == (expected.size() != length), "changed flag");
        check(called[0] == length, "exactly one callback per original occurrence");
        identities(expected, actual, "root");
        if (changed) {
            try {
                before.next();
                throw new AssertionError("pre-removal iterator retained structural epoch");
            } catch (ConcurrentModificationException expectedException) {
                checks++;
            }
        } else if (length > 0) {
            check(before.next() == inputs[0], "no-op retains iterator epoch");
        }
    }

    private static void subListCase(int length, int from, int to, int pattern) {
        cases++;
        Object[] inputs = values(length);
        ArrayList<Object> actual = new ArrayList<>(Arrays.asList(inputs));
        List<Object> live = actual.subList(from, to);
        int rangeLength = to - from;
        ArrayList<Object> expected = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            if (i < from || i >= to || !rejected(i - from, rangeLength, pattern)) {
                expected.add(inputs[i]);
            }
        }
        int[] called = {0};
        boolean changed = live.removeIf(value -> {
            int ordinal = called[0]++;
            check(value == inputs[from + ordinal], "subList callback identity");
            return rejected(ordinal, rangeLength, pattern);
        });
        check(changed == (expected.size() != length), "subList changed flag");
        check(called[0] == rangeLength, "subList callback count");
        identities(expected, actual, "root after subList removeIf");
        int expectedVisible = 0;
        for (int i = from; i < to; i++) {
            if (!rejected(i - from, rangeLength, pattern)) expectedVisible++;
        }
        check(live.size() == expectedVisible, "subList remains live with updated length");
    }

    private static void callbackFailure(int n) {
        Object[] inputs = values(n);
        ArrayList<Object> actual = new ArrayList<>(Arrays.asList(inputs));
        Iterator<Object> previous = actual.iterator();
        RuntimeException marker = new IllegalStateException("marker " + n);
        int[] seen = {0};
        try {
            actual.removeIf(value -> {
                if (seen[0]++ == n / 2) throw marker;
                return true;
            });
            throw new AssertionError("predicate exception swallowed");
        } catch (IllegalStateException actualException) {
            check(actualException == marker, "preserve thrown predicate identity");
        }
        check(seen[0] == n / 2 + 1, "predicate exception callback boundary");
        identities(Arrays.asList(inputs), actual, "exception before compaction");
        check(previous.next() == inputs[0], "failure retains iterator epoch");
    }

    private static void reentrantModification(int n) {
        Object[] inputs = values(n);
        ArrayList<Object> actual = new ArrayList<>(Arrays.asList(inputs));
        Object marker = new Object();
        boolean[] once = {false};
        try {
            actual.removeIf(value -> {
                if (!once[0]) {
                    once[0] = true;
                    actual.add(marker);
                }
                return true;
            });
            throw new AssertionError("reentrant structural mutation accepted");
        } catch (ConcurrentModificationException expected) {
            checks++;
        }
        check(actual.size() == n + 1, "reentrant edit retained");
        check(actual.get(n) == marker, "original outer compaction did not commit");
        for (int i = 0; i < n; i++) {
            check(actual.get(i) == inputs[i], "old prefix intact");
        }
    }

    private static void serializationContract() throws Exception {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(null, -1, 0, 1, 1, 2, 63, 64));
        list.removeIf(item -> item != null && item == 1);
        byte[] encoded;
        try (var bytes = new ByteArrayOutputStream();
                var out = new ObjectOutputStream(bytes)) {
            out.writeObject(list);
            out.flush();
            encoded = bytes.toByteArray();
        }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(encoded))) {
            Object restored = in.readObject();
            check(restored instanceof ArrayList<?>, "retain serialized JDK ArrayList identity");
            check(list.equals(restored), "roundtrip payload and order");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no test arguments");
        for (int n = 0; n <= 130; n++) {
            for (int pattern = 0; pattern < 16; pattern++) rootCase(n, pattern);
        }
        for (int n : new int[] {191, 192, 255, 256, 257, 511, 512, 1024}) {
            for (int p = 0; p < 16; p++) rootCase(n, p);
        }
        Random rng = new Random(0x4D334A444B415252L);
        for (int k = 0; k < 600; k++) {
            int n = rng.nextInt(1025);
            int from = n == 0 ? 0 : rng.nextInt(n + 1);
            int to = from + rng.nextInt(n - from + 1);
            subListCase(n, from, to, rng.nextInt(16));
        }
        for (int n : new int[] {1, 63, 64, 65, 96, 127, 128, 129, 255, 256}) {
            callbackFailure(n);
            reentrantModification(n);
        }
        serializationContract();
        System.out.println("M3JDK_CANONICAL_ARRAYLIST_INLINE_MASK_PASS cases=" + cases
                + " assertions=" + checks);
    }
}
