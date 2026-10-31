/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

/** Independent value/trace oracle for the actual primitive filtered owners. */
public final class M3PrimitiveFilteredTraversalContract {
    private static long checks;
    private static long traceDigest = 0xcbf29ce484222325L;
    private static volatile long sink;

    private M3PrimitiveFilteredTraversalContract() { }

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("--benchmark")) {
            benchmark();
            return;
        }
        checks = 0;
        traceDigest = 0xcbf29ce484222325L;
        if (args.length == 0 || !args[0].equals("--deep")) {
            randomizedValues();
            callbackTraces();
            liveAndMutationContracts();
            sortedCompositions();
            System.out.println("SHALLOW_PASS checks=" + checks
                    + " trace=" + Long.toUnsignedString(traceDigest, 16));
        }
        if (args.length == 0 || !args[0].equals("--shallow")) {
            deepTraversal();
            System.out.println("DEEP_PASS checks=" + checks);
        }
    }

    private static void randomizedValues() {
        Random random = new Random(0x4d335052494d4954L);
        for (int sample = 0; sample < 384; sample++) {
            long[] values = new long[random.nextInt(97)];
            for (int i = 0; i < values.length; i++) values[i] = random.nextInt(129) - 64;
            if (values.length > 3) {
                values[0] = Long.MIN_VALUE;
                values[1] = Long.MAX_VALUE;
                values[2] = 0L;
            }
            int depth = 1 + random.nextInt(8);
            LongPredicate[] predicates = new LongPredicate[depth];
            for (int i = 0; i < depth; i++) {
                long mask = (1L << random.nextInt(6)) - 1;
                long rejected = random.nextInt(8);
                predicates[i] = value -> (value & mask) != rejected;
            }
            M3LongList list = M3LongArrayList.copyOf(values);
            M3LongDeque deque = deque(values);
            LongStream expectedStream = Arrays.stream(values);
            for (int i = 0; i < depth; i++) {
                list = (i & 1) == 0
                        ? M3PrimitiveFilteredSequences.list(list, predicates[i])
                        : M3PrimitiveFilteredSequences.readOnlyList(list, predicates[i]);
                deque = (i & 1) == 0
                        ? M3PrimitiveFilteredSequences.readOnlyDeque(deque, predicates[i])
                        : M3PrimitiveFilteredSequences.deque(deque, predicates[i]);
                expectedStream = expectedStream.filter(predicates[i]);
            }
            long[] expected = expectedStream.toArray();
            equal(expected, collect(list.iterator()));
            equal(expected, list.toArray());
            equal(expected, list.longStream().toArray());
            equal(expected, collect(deque.iterator()));
            equal(reverse(expected), collect(deque.descendingIterator()));
            check(list.size() == expected.length, "list size");
            check(deque.size() == expected.length, "deque size");
        }
    }

    private static void callbackTraces() {
        for (int kind = 0; kind < 3; kind++) {
            for (int failure = 0; failure <= 16; failure++) trace(kind, failure);
        }
        StringBuilder tape = new StringBuilder();
        M3LongList values = M3LongArrayList.copyOf(new long[]{1, 2, 4, 6});
        values = M3PrimitiveFilteredSequences.list(values, value -> {
            tape.append('a').append(value).append(';');
            return (value & 1) == 0;
        });
        values = M3PrimitiveFilteredSequences.readOnlyList(values, value -> {
            tape.append('b').append(value).append(';');
            return value < 6;
        });
        M3LongIterator iterator = values.iterator();
        check(tape.isEmpty(), "iterator construction is predicate-lazy");
        check(iterator.hasNext(), "first selected value");
        check(tape.toString().equals("a1;a2;b2;"), "backing-to-outer short circuit");
        int readyLength = tape.length();
        check(iterator.hasNext() && iterator.hasNext(), "repeated hasNext");
        check(tape.length() == readyLength, "no repeated predicate on prepared value");
        check(iterator.nextLong() == 2, "prepared primitive value");
        check(tape.length() == readyLength, "next consumes prepared value");
        Bomb expected = new Bomb();
        try {
            iterator.forEachRemaining(value -> { throw expected; });
            throw new AssertionError("consumer failure was swallowed");
        } catch (Bomb actual) {
            check(actual == expected, "consumer exception identity");
        }
        check(!iterator.hasNext(), "consumer failure consumes only delivered value");
        fails(NoSuchElementException.class, iterator::nextLong);
        fails(NullPointerException.class, () -> iterator.forEachRemaining(null));
    }

    private static void trace(int kind, int failure) {
        StringBuilder tape = new StringBuilder();
        int[] calls = {0};
        Bomb expected = new Bomb();
        LongPredicate[] predicates = new LongPredicate[3];
        for (int i = 0; i < predicates.length; i++) {
            int stage = i;
            predicates[i] = new LongPredicate() {
                @Override public boolean test(long value) {
                    tape.append(stage).append(':').append(value).append(';');
                    if (++calls[0] == failure) throw expected;
                    return stage == 0 ? value >= 0 : stage == 1 ? (value & 1) == 0 : value < 8;
                }
                @Override public LongPredicate and(LongPredicate other) {
                    throw new AssertionError("existing filters do not invoke predicate.and");
                }
            };
        }
        long[] input = {3, -2, 0, 4, 7, 8};
        M3LongList list = M3LongArrayList.copyOf(input);
        M3LongDeque deque = deque(input);
        for (int i = 0; i < predicates.length; i++) {
            list = M3PrimitiveFilteredSequences.list(list, predicates[i]);
            deque = M3PrimitiveFilteredSequences.readOnlyDeque(deque, predicates[i]);
        }
        M3LongIterator cursor = kind == 0 ? list.iterator()
                : kind == 1 ? deque.iterator() : deque.descendingIterator();
        check(tape.isEmpty(), "no eager predicate calls");
        try {
            while (cursor.hasNext()) {
                int length = tape.length();
                check(cursor.hasNext(), "prepared match");
                check(tape.length() == length, "prepared match does not retest");
                tape.append('v').append(cursor.nextLong()).append(';');
            }
            check(failure == 0 || calls[0] < failure, "expected predicate failure reached");
        } catch (Bomb actual) {
            check(actual == expected && calls[0] == failure, "predicate exception identity and demand");
            tape.append('x');
        }
        for (int i = 0; i < tape.length(); i++) {
            traceDigest = (traceDigest ^ tape.charAt(i)) * 0x100000001b3L;
        }
    }

    private static void liveAndMutationContracts() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{1, 2, 3, 4});
        M3LongList view = M3PrimitiveFilteredSequences.list(owner, value -> value >= 0);
        view = M3PrimitiveFilteredSequences.list(view, value -> (value & 1) == 0);
        owner.add(6);
        equal(new long[]{2, 4, 6}, view.toArray());
        M3LongIterator cursor = view.iterator();
        owner.add(8);
        fails(ConcurrentModificationException.class, cursor::hasNext);
        equal(new long[]{2, 4, 6, 8}, view.toArray());
        check(view.remove(4), "remove selected value");
        equal(new long[]{1, 2, 3, 6, 8}, owner.toArray());
        view.clear();
        equal(new long[]{1, 3}, owner.toArray());

        StringBuilder writes = new StringBuilder();
        M3LongList protectedList = M3PrimitiveFilteredSequences.readOnlyList(owner, value -> {
            writes.append('i');
            return true;
        });
        M3LongList outer = M3PrimitiveFilteredSequences.list(protectedList, value -> {
            writes.append('o');
            return true;
        });
        fails(UnsupportedOperationException.class, () -> outer.add(10));
        check(writes.toString().equals("o"), "protected backing keeps write predicate precedence");
        M3LongList strict = M3PrimitiveFilteredSequences.readOnlyList(outer, value -> {
            throw new AssertionError("read-only write evaluated predicate");
        });
        fails(UnsupportedOperationException.class, () -> strict.add(12));
        fails(UnsupportedOperationException.class, strict::clear);

        M3LongArrayDeque dequeOwner = deque(new long[]{1, 2, 3, 4});
        M3LongDeque dequeView = M3PrimitiveFilteredSequences.deque(dequeOwner, value -> value > 0);
        dequeView = M3PrimitiveFilteredSequences.deque(dequeView, value -> (value & 1) == 0);
        dequeOwner.addFirst(6);
        dequeOwner.addLast(8);
        equal(new long[]{8, 4, 2, 6}, collect(dequeView.descendingIterator()));
        check(dequeView.removeFirstLong() == 6, "deque removal owned by backing");
        check(dequeView.removeLastLong() == 8, "reverse removal owned by backing");
        equal(new long[]{1, 2, 3, 4}, dequeOwner.toArray());
        M3LongIterator reverse = dequeView.descendingIterator();
        dequeOwner.addFirst(10);
        fails(ConcurrentModificationException.class, reverse::hasNext);
    }

    private static void sortedCompositions() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{9, 8, 1, 6, 3, 4});
        M3LongList visible = M3PrimitiveFilteredSequences.list(owner, value -> value > 0);
        visible = M3PrimitiveFilteredSequences.list(visible, value -> (value & 1) == 0);
        visible.sort();
        equal(new long[]{9, 4, 1, 6, 3, 8}, owner.toArray());
        equal(new long[]{4, 6, 8}, visible.toArray());
        M3LongSortedSnapshot sorted = M3LongSortedSnapshot.fromSorted(visible.toArray());
        M3LongSortedSet filtered = M3LongSortedViews.filter(sorted, value -> value >= 6);
        equal(new long[]{6, 8}, filtered.toArray());
        check(filtered.first() == 6 && filtered.last() == 8, "existing sorted endpoints");
        check(filtered.lower(8, -1) == 6 && filtered.higher(6, -1) == 8, "existing navigation");
        M3LongSortedSet bounded = M3LongSortedViews.headSet(filtered, 8);
        equal(new long[]{6}, bounded.toArray());
        owner.set(1, 100);
        equal(new long[]{100, 6, 8}, visible.toArray());
        equal(new long[]{6, 8}, filtered.toArray());
        fails(UnsupportedOperationException.class, () -> M3LongSortedViews.unmodifiable(filtered).clear());
    }

    private static void deepTraversal() {
        int[] calls = {0};
        LongPredicate admits = value -> { calls[0]++; return value >= 0; };
        M3LongArrayList listOwner = M3LongArrayList.copyOf(new long[]{1, 2, 3});
        M3LongList list = listOwner;
        M3LongArrayDeque dequeOwner = deque(new long[]{1, 2, 3});
        M3LongDeque deque = dequeOwner;
        for (int i = 0; i < 20000; i++) {
            list = (i & 1) == 0 ? M3PrimitiveFilteredSequences.list(list, admits)
                    : M3PrimitiveFilteredSequences.readOnlyList(list, admits);
            deque = (i & 1) == 0 ? M3PrimitiveFilteredSequences.deque(deque, admits)
                    : M3PrimitiveFilteredSequences.readOnlyDeque(deque, admits);
        }
        check(calls[0] == 0, "deep construction is lazy");
        M3LongIterator cursor = list.iterator();
        check(calls[0] == 0, "deep iterator construction is lazy");
        equal(new long[]{1, 2, 3}, collect(cursor));
        check(calls[0] == 60000, "exactly one call per admitted occurrence and predicate");
        equal(new long[]{1, 2, 3}, collect(deque.iterator()));
        equal(new long[]{3, 2, 1}, collect(deque.descendingIterator()));
        check(list.size() == 3 && deque.size() == 3, "deep counts");
        equal(new long[]{1, 2, 3}, list.longStream().toArray());
        equal(new long[]{1, 2, 3}, deque.toArray());
        listOwner.add(4);
        dequeOwner.addFirst(4);
        equal(new long[]{1, 2, 3, 4}, list.toArray());
        equal(new long[]{4, 1, 2, 3}, collect(deque.iterator()));
        equal(new long[]{3, 2, 1, 4}, collect(deque.descendingIterator()));
    }

    private static M3LongArrayDeque deque(long[] values) {
        M3LongArrayDeque result = new M3LongArrayDeque();
        for (long value : values) result.addLast(value);
        return result;
    }

    private static long[] reverse(long[] values) {
        long[] result = values.clone();
        for (int lo = 0, hi = result.length - 1; lo < hi; lo++, hi--) {
            long value = result[lo]; result[lo] = result[hi]; result[hi] = value;
        }
        return result;
    }

    private static long[] collect(M3LongIterator iterator) {
        M3LongArrayList values = new M3LongArrayList();
        while (iterator.hasNext()) {
            check(iterator.hasNext(), "idempotent hasNext");
            values.add(iterator.nextLong());
        }
        check(!iterator.hasNext(), "idempotent exhaustion");
        fails(NoSuchElementException.class, iterator::nextLong);
        return values.toArray();
    }

    private static void equal(long[] expected, long[] actual) {
        check(Arrays.equals(expected, actual), "expected " + Arrays.toString(expected)
                + " but got " + Arrays.toString(actual));
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static final class Bomb extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /** Finite complete-traversal diagnostic; not a throughput admission benchmark. */
    private static void benchmark() {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        long thread = Thread.currentThread().threadId();
        long[] values = new long[128];
        Arrays.setAll(values, index -> index);
        System.out.println("depth\titerations\tallocated_bytes\tcpu_ns\twall_ns\tchecksum");
        for (int depth : new int[]{1, 2, 8, 64, 512}) {
            M3LongList view = M3LongArrayList.copyOf(values);
            for (int i = 0; i < depth; i++) {
                view = M3PrimitiveFilteredSequences.list(view, value -> true);
            }
            for (int i = 0; i < 150; i++) sink = consume(view);
            int iterations = 400;
            long allocated = bean.getThreadAllocatedBytes(thread);
            long cpu = bean.getCurrentThreadCpuTime();
            long started = System.nanoTime();
            long checksum = 0;
            for (int i = 0; i < iterations; i++) checksum += consume(view);
            long elapsed = System.nanoTime() - started;
            long cpuElapsed = bean.getCurrentThreadCpuTime() - cpu;
            long bytes = bean.getThreadAllocatedBytes(thread) - allocated;
            sink = checksum;
            System.out.println(depth + "\t" + iterations + "\t" + bytes + "\t"
                    + cpuElapsed + "\t" + elapsed + "\t" + checksum);
        }
    }

    private static long consume(M3LongList values) {
        long result = 0;
        M3LongIterator cursor = values.iterator();
        while (cursor.hasNext()) result += cursor.nextLong();
        return result;
    }
}
