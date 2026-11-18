/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Copy-on-write owner publishes immutable epoch arrays with 64-bit frozen masks
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED CowListEpochArrayTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.collections.CowList;

public final class CowListEpochArrayTest {
    private static long assertions;

    public static void main(String[] args) throws Exception {
        emptyAndSnapshotContracts();
        randomizedJdkDifferential();
        reentrantEqualsDoesNotLoseWrites();
        concurrentAddIfAbsent();
        System.out.println("CowListEpochArrayTest PASS " + assertions + " assertions");
    }

    private static void emptyAndSnapshotContracts() throws Exception {
        CowList<String> list = new CowList<>();
        check(list.isEmpty() && list.size() == 0, "initial");
        check(list.indexOf(null) == -1 && !list.contains(null), "empty null search");
        check(CowList.scaffoldDescriptor().equals(
                "M3CowList|COW_LIST|OBJECT|FROZEN_EPOCH|Object[] valueLane"), "descriptor");
        for (Method method : CowList.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                check(!Modifier.isSynchronized(method.getModifiers()),
                        "public reflection modifiers are not synchronized");
            }
        }
        expect(IndexOutOfBoundsException.class, () -> list.get(0));
        expect(IndexOutOfBoundsException.class, () -> list.add(-1, "x"));
        expect(IndexOutOfBoundsException.class, () -> list.add(1, "x"));
        expect(IndexOutOfBoundsException.class, () -> list.set(0, "x"));
        expect(IndexOutOfBoundsException.class, () -> list.remove(0));
        check(!list.remove("x"), "empty remove object");
        Iterator<String> empty = list.iterator();
        check(!empty.hasNext(), "empty iterator");
        expect(java.util.NoSuchElementException.class, empty::next);
        expect(UnsupportedOperationException.class, empty::remove);
        check(list.add(null), "null append");
        list.add("A");
        list.add(1, "B");
        check(list.indexOf(null) == 0, "null entry");
        check(list.contains("B"), "middle insert");
        check(!list.addIfAbsent("B"), "addIfAbsent existing");
        check(list.addIfAbsent("C"), "addIfAbsent unique");
        check(list.size() == 4, "size");
        Object[] detached = list.snapshot();
        Object[] held = detached.clone();
        detached[1] = "client mutation";
        check("B".equals(list.get(1)), "snapshot ownership detached");
        Iterator<String> frozen = list.iterator();
        list.set(1, "now");
        list.remove(0);
        list.add("new");
        List<Object> epoch = new ArrayList<>();
        while (frozen.hasNext()) epoch.add(frozen.next());
        check(epoch.equals(Arrays.asList(held)), "iterator frozen against three writes");
        check(Objects.equals(list.remove(1), "A"), "positional removal");
        verifyStorage(list);
        list.clear();
        check(list.isEmpty() && list.size() == 0, "clear");
        verifyStorage(list);
    }

    private static void randomizedJdkDifferential() throws Exception {
        for (int seed = 0; seed < 32; seed++) {
            Random random = new Random(0xC0FFEE5AAL ^ (seed * 0x9E3779B97F4A7C15L));
            CowList<Object> actual = new CowList<>();
            CopyOnWriteArrayList<Object> expected = new CopyOnWriteArrayList<>();
            for (int step = 0; step < 5_000; step++) {
                Object value = randomValue(random);
                int choice = random.nextInt(100);
                int size = expected.size();
                if (choice < 24) {
                    check(actual.add(value) == expected.add(value), "append");
                } else if (choice < 38) {
                    int index = random.nextInt(size + 1);
                    actual.add(index, value);
                    expected.add(index, value);
                } else if (choice < 53 && size > 0) {
                    int index = random.nextInt(size);
                    check(Objects.equals(actual.set(index, value), expected.set(index, value)), "set old");
                } else if (choice < 64 && size > 0) {
                    int index = random.nextInt(size);
                    check(Objects.equals(actual.remove(index), expected.remove(index)), "remove rank");
                } else if (choice < 75) {
                    check(actual.remove(value) == expected.remove(value), "remove equality");
                } else if (choice < 83) {
                    check(actual.addIfAbsent(value) == expected.addIfAbsent(value), "addIfAbsent");
                } else if (choice < 89) {
                    check(actual.indexOf(value) == expected.indexOf(value), "indexOf");
                    check(actual.contains(value) == expected.contains(value), "contains");
                } else if (choice < 93) {
                    actual.clear();
                    expected.clear();
                } else {
                    Object[] snapshot = actual.snapshot();
                    Object[] oracle = expected.toArray();
                    check(Arrays.equals(snapshot, oracle), "detached snapshot");
                    if (size > 0) {
                        int position = random.nextInt(size);
                        check(Objects.equals(actual.get(position), expected.get(position)), "get");
                    }
                    Iterator<Object> first = actual.iterator();
                    Iterator<Object> second = expected.iterator();
                    actual.add("later");
                    expected.add("later");
                    while (second.hasNext())
                        check(Objects.equals(first.next(), second.next()), "snapshot iterator");
                    check(!first.hasNext(), "completed epoch");
                }
                check(actual.size() == expected.size(), "size");
                check(actual.isEmpty() == expected.isEmpty(), "isEmpty");
                if ((step & 63) == 0) {
                    compareAll(actual, expected);
                    verifyStorage(actual);
                }
            }
            compareAll(actual, expected);
            verifyStorage(actual);
        }
    }

    private static void reentrantEqualsDoesNotLoseWrites() {
        CowList<Object> list = new CowList<>();
        list.add("seed");
        ReentrantProbe probe = new ReentrantProbe(list);
        check(!list.remove(probe), "reentrant remove returns miss");
        check(list.contains("reentrant-write"), "reentrant remove retains nested write");
        check(list.addIfAbsent(new ReentrantProbe(list)), "reentrant addIfAbsent inserts");
        check(list.contains("reentrant-write") && list.size() == 4, "no lost callback writes");
    }

    private static final class ReentrantProbe {
        private final CowList<Object> owner;
        private boolean fired;

        ReentrantProbe(CowList<Object> owner) { this.owner = owner; }

        @Override
        public boolean equals(Object other) {
            if (!fired) {
                fired = true;
                owner.add("reentrant-write");
            }
            return false;
        }

        @Override
        public int hashCode() { return 37; }
    }

    private static void concurrentAddIfAbsent() throws Exception {
        final int threads = 8;
        CowList<Integer> list = new CowList<>();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int i = 0; i < 2_000; i++) list.addIfAbsent(i % 101);
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            }, "cow-list-" + t);
            workers[t].start();
        }
        ready.await();
        start.countDown();
        for (Thread worker : workers) worker.join();
        check(failure.get() == null, "no worker failure");
        check(list.size() == 101, "concurrent idempotent writes");
        for (int i = 0; i < 101; i++) check(list.contains(i), "all unique values");
    }

    private static Object randomValue(Random random) {
        return switch (random.nextInt(8)) {
            case 0, 1 -> null;
            case 2 -> "";
            case 3 -> Integer.valueOf(random.nextInt(9) - 4);
            case 4 -> "common";
            default -> "value-" + random.nextInt(80);
        };
    }

    private static void compareAll(CowList<?> actual, CopyOnWriteArrayList<?> expected) {
        check(actual.size() == expected.size(), "full length");
        Object[] snapshot = actual.snapshot();
        check(Arrays.equals(snapshot, expected.toArray()), "full snapshot contents");
        for (int i = 0; i < expected.size(); i++)
            check(Objects.equals(actual.get(i), expected.get(i)), "full indexed contents");
    }

    private static void verifyStorage(CowList<?> list) throws Exception {
        Object[] elements = (Object[]) field(list, "valueLane");
        long[] states = (long[]) field(list, "stateMaskWords");
        int length = list.size();
        check(elements.length == length, "no retained capacity slack");
        check(states.length == (int) (((long) length + 63) >>> 6), "packed frozen bits");
        for (int i = 0; i < states.length * 64; i++) {
            long live = states[i >>> 6] & (1L << (i & 63));
            check((live != 0) == (i < length), "frozen live position");
        }
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            check(type.isInstance(failure), "expected " + type + " but got " + failure);
            return;
        }
        throw new AssertionError("expected exception " + type);
    }

    private static void check(boolean okay, String message) {
        assertions++;
        if (!okay) throw new AssertionError(message + " at assertion " + assertions);
    }
}
