// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;

/** Prepared read-only stable index order over a borrowed source, NOT a live resorting list.
 * Preparation is a terminal: selection/comparison/key callbacks run now. The owner must
 * remain stable during preparation and reads. Its supplied revision must change for every
 * relevant mutation and must never be reused. Checks detect reported drift; they are not
 * locking, atomic snapshots, or detection of unreported in-place object mutations.
 * No element or key array is retained. Close invalidates this view and its sublists.
 */
public final class M3SortedView<E> extends AbstractList<E> implements RandomAccess, AutoCloseable {
    public enum Backend { JAVA, JNI_REQUIRED }
    public static final int MAX_ROWS = 1_048_576;
    private IntFunction<? extends E> read;
    private LongSupplier revision;
    private final long stamp;
    private int[] indices;

    private M3SortedView(IntFunction<? extends E> read, LongSupplier revision, long stamp, int[] indices) {
        this.read = read; this.revision = revision; this.stamp = stamp; this.indices = indices;
    }
    public static <E> M3SortedView<E> by(List<E> source, Comparator<? super E> comparator,
                                        LongSupplier revision) {
        return byList(source, comparator, revision, 0, true, null);
    }
    /** Ordinary list boundary. Metadata-only callers should use byIndex or byLong. */
    public static <E> M3SortedView<E> by(List<E> source, Comparator<? super E> comparator,
                                        LongSupplier revision, long scratchBytes, M3ProgressMonitor monitor) {
        return byList(source, comparator, revision, scratchBytes, false, monitor);
    }
    private static <E> M3SortedView<E> byList(List<E> source, Comparator<? super E> comparator,
            LongSupplier revision, long scratchBytes, boolean automaticBudget, M3ProgressMonitor monitor) {
        Objects.requireNonNull(source); Objects.requireNonNull(comparator); Objects.requireNonNull(revision);
        M3Flow.check(monitor);
        long stamp = revision.getAsLong();
        Runnable guarded = guard(() -> M3Flow.check(monitor), revision, stamp);
        guarded.run();
        int count = source.size();
        guarded.run();
        IntFunction<E> checkedRead = index -> {
            guarded.run(); E value = source.get(index); guarded.run(); return value;
        };
        M3SortedView<E> prepared = byIndex(count, source::get, revision, i -> true,
                (a, b) -> comparator.compare(checkedRead.apply(a), checkedRead.apply(b)),
                automaticBudget ? 48L * count : scratchBytes, monitor);
        guarded.run(); return prepared;
    }
    /** Comparison receives source indices; selection is evaluated exactly once per source row. */
    public static <E> M3SortedView<E> byIndex(int count, IntFunction<? extends E> read,
            LongSupplier revision, IntPredicate selected, IntBinaryOperator comparison,
            long scratchBytes, M3ProgressMonitor monitor) {
        Objects.requireNonNull(comparison);
        Runnable check = admission(count, read, revision, selected, scratchBytes, monitor);
        long stamp = revision.getAsLong();
        Runnable guarded = guard(check, revision, stamp);
        int[] selectedIndices = select(count, selected, guarded);
        int[] order = DenseArrayOrder.order(selectedIndices.length,
                (a, b) -> {
                    guarded.run();
                    int result = comparison.applyAsInt(selectedIndices[a], selectedIndices[b]);
                    guarded.run(); return result;
                }, guarded);
        for (int i = 0; i < order.length; i++) order[i] = selectedIndices[order[i]];
        guarded.run(); return new M3SortedView<>(read, revision, stamp, order);
    }
    /** Signed long keys, captured once per selected row without loading element objects.
     * Native execution requires explicit System.load by the host and never silently falls back.
     * Cancellation is checked before/after one bounded native call, not during that call.
     */
    public static <E> M3SortedView<E> byLong(int count, IntFunction<? extends E> read,
            LongSupplier revision, IntPredicate selected, IntToLongFunction key, Backend backend,
            long scratchBytes, M3ProgressMonitor monitor) {
        Objects.requireNonNull(key); Objects.requireNonNull(backend);
        Runnable check = admission(count, read, revision, selected, scratchBytes, monitor);
        long stamp = revision.getAsLong(); Runnable guarded = guard(check, revision, stamp);
        int[] selectedIndices = select(count, selected, guarded);
        long[] keys = new long[selectedIndices.length];
        for (int i = 0; i < keys.length; i++) { guarded.run(); keys[i] = key.applyAsLong(selectedIndices[i]); }
        guarded.run();
        if (backend == Backend.JAVA && alreadyOrdered(keys, guarded)) {
            guarded.run();
            return new M3SortedView<>(read, revision, stamp, selectedIndices);
        }
        int[] order = backend == Backend.JAVA
                ? DenseArrayOrder.order(keys.length, (a, b) -> Long.compare(keys[a], keys[b]), guarded)
                : nativeOrder(keys);
        guarded.run();
        for (int i = 0; i < order.length; i++) order[i] = selectedIndices[order[i]];
        return new M3SortedView<>(read, revision, stamp, order);
    }
    /** A numeric check on captured metadata: no additional user callbacks or payload reads. */
    private static boolean alreadyOrdered(long[] keys, Runnable checkpoint) {
        for (int i = 1; i < keys.length; i++) {
            if ((i & 1023) == 0) checkpoint.run();
            if (keys[i - 1] > keys[i]) return false;
        }
        checkpoint.run(); return true;
    }
    /** Conservative reservation for numeric scratch (48 bytes per source row), not a
     * total-heap guarantee: excludes array headers, callbacks, payload and VM overhead. */
    private static Runnable admission(int count, Object read, LongSupplier revision, Object selected,
                                       long scratchBytes, M3ProgressMonitor monitor) {
        Objects.requireNonNull(read); Objects.requireNonNull(revision); Objects.requireNonNull(selected);
        M3Flow.check(monitor);
        if (count < 0 || count > MAX_ROWS || scratchBytes < 48L * count)
            throw new IllegalArgumentException("row/scratch budget");
        return () -> M3Flow.check(monitor);
    }
    private static Runnable guard(Runnable check, LongSupplier revision, long stamp) {
        return () -> { check.run(); if (revision.getAsLong() != stamp) throw new ConcurrentModificationException("source revision"); };
    }
    private static int[] select(int count, IntPredicate selected, Runnable check) {
        int[] indices = new int[count]; int size = 0;
        for (int i = 0; i < count; i++) { check.run(); if (selected.test(i)) indices[size++] = i; }
        check.run(); return Arrays.copyOf(indices, size);
    }
    private static native int[] nativeOrder(long[] keys);
    private void valid() {
        if (indices == null) throw new IllegalStateException("closed sorted view");
        if (revision.getAsLong() != stamp) throw new ConcurrentModificationException("source revision");
    }
    @Override public int size() { valid(); return indices.length; }
    public int sourceIndex(int index) { valid(); return indices[Objects.checkIndex(index, indices.length)]; }
    @Override public E get(int index) { int source = sourceIndex(index); E value = read.apply(source); valid(); return value; }
    @Override public List<E> subList(int from, int to) {
        Objects.checkFromToIndex(from, to, size());
        return Collections.unmodifiableList(new AbstractList<>() {
            public E get(int index) { return M3SortedView.this.get(from + Objects.checkIndex(index, size())); }
            public int size() { valid(); return to - from; }
        });
    }
    /** Copy current element references into an independent list; not a deep payload snapshot. */
    public List<E> materialize() { return new ArrayList<>(this); }
    public boolean isClosed() { return indices == null; }
    @Override public void close() { indices = null; read = null; revision = null; }
}
