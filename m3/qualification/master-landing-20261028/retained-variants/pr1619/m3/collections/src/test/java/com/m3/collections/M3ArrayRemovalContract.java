// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Frozen prior-API probe run unchanged against original and generated complete owners. */
public final class M3ArrayRemovalContract {
    private static long checks;
    private static long digest = 0xcbf29ce484222325L;
    private static volatile Object escaped;
    private static final Field ELEMENTS;
    static {
        try {
            ELEMENTS = M3ArrayList.class.getDeclaredField("elements");
            ELEMENTS.setAccessible(true);
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private M3ArrayRemovalContract() { }
    private static void check(boolean result, String label) {
        checks++;
        if (!result) throw new AssertionError(label);
    }
    private static void mix(long value) { digest = (digest ^ value) * 0x100000001b3L; }
    private static void tail(M3ArrayList<?> list) throws IllegalAccessException {
        Object[] storage = (Object[]) ELEMENTS.get(list);
        for (int i = list.size(); i < storage.length; i++) check(storage[i] == null, "retained tail reference");
    }
    private static void selection(boolean[] removed) throws Exception {
        Object a = new Object(), b = new Object();
        Object[] input = new Object[removed.length];
        for (int i = 0; i < input.length; i++) input[i] = i % 7 == 0 ? null : (i % 3 == 0 ? a : b);
        M3ArrayList<Object> owner = new M3ArrayList<>(Arrays.asList(input));
        int capacity = owner.capacity();
        Iterator<Object> iterator = owner.iterator();
        List<Object> view = owner.subList(0, owner.size());
        List<Object> expected = new ArrayList<>();
        for (int i = 0; i < input.length; i++) if (!removed[i]) expected.add(input[i]);
        int[] calls = {0};
        boolean changed = owner.removeIf(value -> {
            int ordinal = calls[0]++;
            check(value == input[ordinal], "callback order or identity");
            check(owner.size() == input.length, "compacted during predicate phase");
            return removed[ordinal];
        });
        check(calls[0] == input.length, "callback count");
        check(changed == (expected.size() != input.length), "return value");
        check(owner.capacity() == capacity, "capacity changed");
        check(owner.size() == expected.size(), "survivor size");
        for (int i = 0; i < expected.size(); i++) check(owner.get(i) == expected.get(i), "stable reference order");
        if (changed) {
            try { iterator.next(); throw new AssertionError("stale iterator admitted"); }
            catch (ConcurrentModificationException expectedFailure) { checks++; }
            try { view.size(); throw new AssertionError("stale view admitted"); }
            catch (ConcurrentModificationException expectedFailure) { checks++; }
        } else {
            for (Object value : input) check(iterator.next() == value, "no-op invalidated iterator");
            check(view.size() == input.length, "no-op invalidated view");
        }
        tail(owner);
        M3ArrayList<Object> copy = owner.clone();
        check(copy.equals(owner), "clone contents");
        check(copy.snapshot().equals(owner), "snapshot contents");
        mix(input.length);mix(expected.size());mix(calls[0]);
        for (Object value : owner) mix(value == null ? 0 : value == a ? 1 : 2);
    }
    private static void exhaustive() throws Exception {
        for (int n = 0; n <= 12; n++) {
            for (int mask = 0; mask < (1 << n); mask++) {
                boolean[] removed = new boolean[n];
                for (int i = 0; i < n; i++) removed[i] = (mask & (1 << i)) != 0;
                selection(removed);
            }
        }
        int[] sizes = {13, 31, 32, 33, 62, 63, 64, 65, 66, 127, 128, 129, 130, 255, 256, 257, 1023, 4097};
        for (int n : sizes) for (int shape = 0; shape < 12; shape++) {
            boolean[] removed = new boolean[n];
            for (int i = 0; i < n; i++) removed[i] = switch (shape) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> i == n - 1;
                case 3 -> i == 0;
                case 4 -> i >= n / 2;
                case 5 -> (i & 1) == 0;
                case 6 -> i == 1 || i == n - 1;
                case 7 -> i % 64 == 63;
                case 8 -> i % 64 == 0;
                case 9 -> i % 127 == 1;
                case 10 -> i == n / 2;
                default -> ((i * 2654435761L) >>> 27) % 13 == 4;
            };
            selection(removed);
        }
    }
    private static void exceptionsAndReentrancy() throws Exception {
        for (int n : new int[]{1, 4, 65, 129}) for (int fail = 0; fail < n; fail++) {
            M3ArrayList<Integer> owner = new M3ArrayList<>();
            for (int i = 0; i < n; i++) owner.add(i);
            Object[] before = owner.toArray();
            int[] calls = {0};
            RuntimeException marker = new IllegalStateException("client-failure");
            int at = fail;
            try {
                owner.removeIf(value -> {
                    int call = calls[0]++;
                    if (call == at) throw marker;
                    return true;
                });
                throw new AssertionError("missing callback failure");
            } catch (RuntimeException actual) { check(actual == marker, "exception identity"); }
            check(calls[0] == fail + 1, "exception short circuit");
            check(Arrays.equals(before, owner.toArray()), "partial compaction on failure");
            tail(owner);mix(calls[0]);
        }
        for (int operation = 0; operation < 4; operation++) {
            M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(0, 1, 2, 3));
            int[] calls = {0};
            int op = operation;
            try {
                owner.removeIf(value -> {
                    if (calls[0]++ == 1) {
                        switch (op) {
                            case 0 -> owner.add(9);
                            case 1 -> owner.clear();
                            case 2 -> owner.ensureCapacity(4096);
                            default -> owner.sort(Integer::compare);
                        }
                    }
                    return true;
                });
                throw new AssertionError("structural callback accepted");
            } catch (ConcurrentModificationException failure) {
                check("predicate structurally changed owner".equals(failure.getMessage()), "CME contract");
            }
            check(calls[0] == 2, "late reentrancy refusal");
            check(owner.size() == (op == 0 ? 5 : op == 1 ? 0 : 4), "unwanted rollback or compaction");
            tail(owner);mix(owner.size());
        }
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(10, 11, 12, 13));
        List<Integer> tape = new ArrayList<>();
        check(owner.removeIf(value -> {
            tape.add(value);
            if (value == 10) { owner.set(0, 99); owner.set(2, 77); }
            return value == 11;
        }), "set callback removal");
        check(tape.equals(List.of(10, 11, 77, 13)), "future set not observed");
        check(owner.equals(List.of(99, 77, 13)), "earlier set lost");tail(owner);
        try { owner.removeIf(null); throw new AssertionError("null predicate accepted"); }
        catch (NullPointerException failure) { check("predicate".equals(failure.getMessage()), "null message"); }
        Error marker = new AssertionError("client-error");
        try { owner.removeIf(value -> { throw marker; }); throw new AssertionError("error missing"); }
        catch (Error actual) { check(actual == marker, "error identity"); }
        check(owner.equals(List.of(99, 77, 13)), "error changed payload");
    }
    private static void boundaries() throws Exception {
        M3ArrayList<Integer> owner = new M3ArrayList<>(Arrays.asList(4, null, 4, 2, 1, 9));
        check(owner.removeIf(Objects::isNull), "null removal");
        check(owner.stream().mapToInt(Integer::intValue).sum() == 20, "stream sum");
        check(owner.reversed().equals(List.of(9, 1, 2, 4, 4)), "reverse order");
        owner.subList(1, 4).removeIf(i -> i % 2 == 0);
        check(owner.equals(List.of(4, 1, 9)), "backed sublist removal");
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(owner); }
        Object restored;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { restored = in.readObject(); }
        check(owner.equals(restored), "serialization");
        tail(owner);mix(bytes.size());
    }
    private static void measure() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation meter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int n : new int[]{0, 64, 4096}) for (int shape = 0; shape < 5; shape++) {
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < n; i++) source.add(i);
            int mode = shape;
            Predicate<Integer> predicate = value -> switch (mode) {
                case 0 -> false;
                case 1 -> value == n - 1;
                case 2 -> value % 127 == 0;
                case 3 -> (value & 1) == 0;
                default -> true;
            };
            M3ArrayList<Integer> list = new M3ArrayList<>(n);
            int repetitions = 1500;
            long bytes = 0, nanos = 0;
            for (int i = 0; i < repetitions + 2500; i++) {
                list.clear();list.addAll(source);
                long pre = bean.getThreadAllocatedBytes(thread);
                long start = System.nanoTime();
                list.removeIf(predicate);
                long elapsed = System.nanoTime() - start;
                long allocated = bean.getThreadAllocatedBytes(thread) - pre;
                escaped = list;
                if (i >= 2500) { bytes += allocated; nanos += elapsed; }
            }
            System.out.println("MEASURE\t" + n + "\t" + shape + "\t" + (bytes / (double) repetitions)
                    + "\t" + (nanos / (double) repetitions));
        }
    }
    public static void main(String[] args) throws Exception {
        exhaustive();exceptionsAndReentrancy();boundaries();
        System.out.println("CHECKS=" + checks + " DIGEST=" + Long.toUnsignedString(digest));
        if (args.length > 0 && args[0].equals("measure")) measure();
    }
}
