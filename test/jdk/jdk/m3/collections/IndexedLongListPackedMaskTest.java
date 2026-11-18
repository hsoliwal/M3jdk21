/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary IndexedLongList offset-array AVL and packed per-slot masks preserve list contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm IndexedLongListPackedMaskTest
 */
import java.util.ArrayList;
import java.util.Random;
import jdk.internal.m3.collections.IndexedLongList;

public final class IndexedLongListPackedMaskTest {
    private static long checks;

    public static void main(String[] args) {
        for (int seed = 0; seed < 96; seed++) randomTrace(seed);
        growthAndReuse();
        invalidIndexContracts();
        System.out.println("IndexedLongListPackedMaskTest PASS: " + checks + " assertions");
    }

    private static void randomTrace(int seed) {
        Random random = new Random(0x17E5B1A57L + seed * 104729L);
        IndexedLongList actual = new IndexedLongList();
        ArrayList<Long> expected = new ArrayList<>();
        for (int step = 0; step < 2300; step++) {
            int choice = random.nextInt(100);
            int size = expected.size();
            if (choice < 39 && size < 750) {
                int position = random.nextInt(size + 1);
                long value = value(random);
                expected.add(position, value);
                actual.add(position, value);
                check(actual.size() == expected.size(), "insert size");
            } else if (choice < 57 && size < 750) {
                long value = value(random);
                expected.add(value);
                check(actual.add(value), "append true");
            } else if (choice < 79 && size > 0) {
                int position = random.nextInt(size);
                long old = expected.remove(position);
                check(actual.removeAt(position) == old, "remove return");
            } else if (choice < 90 && size > 0) {
                int position = random.nextInt(size);
                long replacement = value(random);
                long old = expected.set(position, replacement);
                check(actual.set(position, replacement) == old, "set old value");
            } else if (choice < 93) {
                expected.clear();
                actual.clear();
            } else if (size > 0) {
                int position = random.nextInt(size);
                check(actual.get(position) == expected.get(position), "point get");
            }
            check(actual.size() == expected.size(), "trace size");
            for (int probe = 0; probe < 3 && !expected.isEmpty(); probe++) {
                int position = random.nextInt(expected.size());
                check(actual.get(position) == expected.get(position), "trace get");
            }
            if (step % 17 == 0) {
                long seek = value(random);
                check(actual.indexOf(seek) == expected.indexOf(seek),
                        "trace indexOf");
            }
            if (step % 211 == 0) {
                for (int i = 0; i < expected.size(); i++) {
                    check(actual.get(i) == expected.get(i), "full trace snapshot");
                }
            }
        }
    }

    private static long value(Random random) {
        return switch (random.nextInt(11)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            case 4 -> 1L;
            case 5, 6 -> random.nextInt(21) - 10L;
            default -> random.nextLong();
        };
    }

    private static void growthAndReuse() {
        IndexedLongList actual = new IndexedLongList();
        int n = 131072;
        for (int i = 0; i < n; i++) {
            check(actual.add((long) i), "large append");
        }
        check(actual.size() == n, "large size");
        for (int i = 0; i < n; i++) {
            check(actual.get(i) == i, "large ordinal");
        }
        for (int i = 0; i < 1024; i++) {
            int ordinal = actual.size() / 2;
            long removed = actual.removeAt(ordinal);
            check(removed >= 0 && removed < n, "middle removal");
            actual.add(ordinal, removed);
            check(actual.get(ordinal) == removed, "free-list slot reuse");
        }
        check(actual.size() == n, "size after reuse");
        actual.clear();
        check(actual.size() == 0, "large clear");
        check(actual.indexOf(1L) == -1, "empty lookup");
        for (int i = 0; i < 8193; i++) {
            actual.add(0, -i);
        }
        check(actual.size() == 8193, "repopulate after clear");
        for (int i = 0; i < 8193; i++) {
            check(actual.get(i) == i - 8192L, "repopulation order");
        }
        while (actual.size() > 0) {
            actual.removeAt(0);
        }
        check(actual.size() == 0, "all recycled");
        for (int i = 0; i < 200; i++) {
            actual.add(i, i * 2L);
        }
        for (int i = 0; i < 200; i++) {
            check(actual.get(i) == i * 2L, "recycled slot active again");
        }
    }

    private static void invalidIndexContracts() {
        IndexedLongList owner = new IndexedLongList();
        fails(IndexOutOfBoundsException.class, () -> owner.get(0), "empty get");
        fails(IndexOutOfBoundsException.class, () -> owner.set(0, 3), "empty set");
        fails(IndexOutOfBoundsException.class, () -> owner.removeAt(0), "empty remove");
        fails(IndexOutOfBoundsException.class, () -> owner.add(-1, 3), "negative insert");
        fails(IndexOutOfBoundsException.class, () -> owner.add(1, 3), "past end insert");
        owner.add(42);
        fails(IndexOutOfBoundsException.class, () -> owner.get(1), "high ordinal");
        fails(IndexOutOfBoundsException.class, () -> owner.add(2, 3), "high insert");
        check(owner.get(0) == 42, "rejected mutation unchanged");
        check(IndexedLongList.scaffoldDescriptor().startsWith("M3IndexedLongList|"), "descriptor");
    }

    private static void check(boolean accepted, String reason) {
        checks++;
        if (!accepted) throw new AssertionError(reason);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action, String reason) {
        try {
            action.run();
        } catch (Throwable ex) {
            check(type.isInstance(ex), reason + ": " + ex);
            return;
        }
        throw new AssertionError(reason + " did not throw");
    }
}
