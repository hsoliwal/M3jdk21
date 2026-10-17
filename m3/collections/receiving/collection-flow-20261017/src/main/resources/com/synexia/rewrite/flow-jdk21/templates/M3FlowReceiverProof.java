// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Target-only composition proof: no replacement payload owner or progress SPI. */
public final class M3FlowReceiverProof {
    private static int checks;
    private M3FlowReceiverProof() { }
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static <X extends Throwable> void fails(Class<X> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(type.getName(), failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
    static final class Monitor implements M3ProgressMonitor {
        boolean cancelled;
        public void beginTask(String name, long total) { }
        public void done() { }
        public void worked(long work) { }
        public void setProgress(long work, long total) { }
        public void setTaskName(String name) { }
        public void subTask(String name) { }
        public boolean isCanceled() { return cancelled; }
        public void setCanceled(boolean value) { cancelled = value; }
        public M3ProgressState state() { return new M3ProgressState("receiver", "", 0, 1, cancelled, false, 0, 0); }
    }
    public static void main(String[] args) {
        boolean nativeMode = args.length != 0;
        if (nativeMode) System.load(args[0]);
        M3SortedView.Backend backend = nativeMode ? M3SortedView.Backend.JNI_REQUIRED : M3SortedView.Backend.JAVA;
        long[] keys = {3, 1, 1, 2, Long.MIN_VALUE, Long.MAX_VALUE};
        var reads = new AtomicInteger();
        var version = new AtomicLong();
        try (var order = M3SortedView.byLong(keys.length,
                i -> { reads.incrementAndGet(); return keys[i]; }, version::get,
                i -> true, i -> keys[i], backend, 48L * keys.length, null)) {
            equal(0, reads.get());
            equal(List.of(4, 1, 2, 3, 0, 5), java.util.stream.IntStream.range(0, order.size()).map(order::sourceIndex).boxed().toList());
            var plan = M3Flow.indexed(order::size, order::get, version::get)
                    .whereIndex(i -> i > 0 && i < 5)
                    .groupByIndex(i -> keys[order.sourceIndex(i)] & 1L)
                    .sumLongAt(i -> keys[order.sourceIndex(i)]);
            equal(0, reads.get());
            equal(Map.of(1L, 5L, 0L, 2L), plan.toMap());
            equal(0, reads.get());
            equal(List.of(Long.MIN_VALUE, 1L, 1L, 2L, 3L, Long.MAX_VALUE), M3Flow.indexed(order::size, order::get).toList());
            equal(6, reads.get());
            List<Long> sub = order.subList(1, 3);
            version.incrementAndGet();
            fails(ConcurrentModificationException.class, order::size);
            fails(ConcurrentModificationException.class, sub::size);
        }
        var order = M3SortedView.byLong(1, i -> keys[i], () -> 0L,
                i -> true, i -> keys[i], backend, 48, null);
        var escaped = order.subList(0, 1);
        order.close(); order.close();
        fails(IllegalStateException.class, escaped::size);
        var monitor = new Monitor(); monitor.cancelled = true;
        fails(CancellationException.class, () -> M3SortedView.byLong(0, i -> i, () -> 0L,
                i -> true, i -> i, backend, 0, monitor));
        var change = new AtomicLong();
        List<Integer> changingSize = new AbstractList<>() {
            public int size() { change.incrementAndGet(); return 2; }
            public Integer get(int i) { return i; }
        };
        fails(ConcurrentModificationException.class, () -> M3SortedView.by(changingSize,
                Comparator.naturalOrder(), change::get, 96, null));
        AtomicInteger callbacks = new AtomicInteger();
        var pending = M3LongFlow.of(new long[]{1, 2, 3, 4})
                .mapLong(v -> { callbacks.incrementAndGet(); return v; })
                .windowSum(2, 1, M3Windows.Tail.DROP)
                .accumulate(0L, Long::sum);
        equal(0, callbacks.get());
        equal(15L, pending.findFirst().orElseThrow());
        equal(4, callbacks.get());
        equal(15L, pending.findFirst().orElseThrow());
        equal(8, callbacks.get());
        System.out.println("TARGET_RECEIVER_CHECKS=" + checks + " NATIVE=" + nativeMode + " PASS");
    }
}
