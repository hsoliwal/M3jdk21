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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Late-bound filtered cursor over the exact JDK ArrayList owner. Splits through
 * the JDK's index-range spliterator; neither this class nor the source splitter
 * allocates per-element intermediate arrays. The split count is CPU-bounded.
 *
 * <p>Not a general-purpose Spliterator wrapper. Only an exact ArrayList is
 * admitted by the caller, which must exclude subclasses overriding spliterator().
 * Every emitted value is the original stored object; no entry or node is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredArrayListSplitCursor.java}
 * blob {@code eff3251bb8b5c3a7bea733dc38f9152b79e30c28} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class FilteredArrayListSplitCursor<E> implements Spliterator<E>, Consumer<E> {
    private final ArrayList<E> source;
    private final Predicate<? super E> predicate;
    private final int characteristics;
    private Spliterator<E> owner;
    private Consumer<? super E> action;
    private int remainingSplits;
    private boolean delivered;
    private boolean exhausted;

    FilteredArrayListSplitCursor(ArrayList<E> source, Predicate<? super E> predicate,
            int characteristics) {
        this(source, null, predicate, characteristics, splitDepth());
    }

    /** Receive the exact already-inspected, still late-binding JDK owner cursor. */
    FilteredArrayListSplitCursor(Spliterator<E> owner,
            Predicate<? super E> predicate, int characteristics) {
        this(null, Objects.requireNonNull(owner, "owner"), predicate,
                characteristics, splitDepth());
    }

    private FilteredArrayListSplitCursor(ArrayList<E> source, Spliterator<E> owner,
            Predicate<? super E> predicate, int characteristics, int remainingSplits) {
        this.source = source;
        this.owner = owner;
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.characteristics = characteristics & ~(SIZED | SUBSIZED);
        this.remainingSplits = remainingSplits;
    }

    private static int splitDepth() {
        // Maximum 128 shards; leave at least four shards for small CPUs.
        int processors = Math.min(64, Math.max(1, Runtime.getRuntime().availableProcessors()));
        int leaves = Math.max(4, processors * 2);
        return Integer.SIZE - Integer.numberOfLeadingZeros(leaves - 1);
    }

    private Spliterator<E> sourceCursor() {
        if (owner == null) {
            owner = source.spliterator();
        }
        return owner;
    }

    @Override
    public boolean tryAdvance(Consumer<? super E> downstream) {
        Objects.requireNonNull(downstream, "action");
        if (action != null) throw new IllegalStateException("reentrant filtered traversal");
        if (exhausted) return false;
        action = downstream;
        try {
            while (true) {
                delivered = false;
                if (!sourceCursor().tryAdvance(this)) {
                    exhausted = true;
                    return false;
                }
                if (delivered) return true;
            }
        } finally {
            action = null;
            delivered = false;
        }
    }

    @Override
    public void accept(E candidate) {
        if (predicate.test(candidate)) {
            delivered = true;
            action.accept(candidate);
        }
    }

    @Override
    public void forEachRemaining(Consumer<? super E> downstream) {
        Objects.requireNonNull(downstream, "action");
        if (action != null) throw new IllegalStateException("reentrant filtered traversal");
        if (exhausted) return;
        action = downstream;
        try {
            sourceCursor().forEachRemaining(this);
            exhausted = true;
        } finally {
            action = null;
            delivered = false;
        }
    }

    /** Only existing JDK index ranges are split; no Object[] batch buffers. */
    private static final long MIN_SOURCE_SLOTS_PER_NONROOT_SPLIT = 512L;

    @Override
    public Spliterator<E> trySplit() {
        if (action != null) throw new IllegalStateException("reentrant filtered traversal");
        if (exhausted || remainingSplits == 0) return null;
        Spliterator<E> current = sourceCursor();
        // Preserve the first split (including the two-element regression), but
        // avoid turning subsequent small ranges into hundreds of one-slot jobs.
        // estimateSize is consulted only *after* a split was requested, so the
        // owner remains late-binding at view/spliterator construction time.
        if (remainingSplits < splitDepth()
                && current.estimateSize() <= MIN_SOURCE_SLOTS_PER_NONROOT_SPLIT) {
            return null;
        }
        Spliterator<E> prefix = current.trySplit();
        if (prefix == null) return null;
        int budget = --remainingSplits;
        return new FilteredArrayListSplitCursor<>(null, prefix, predicate,
                characteristics, budget);
    }

    @Override
    public long estimateSize() {
        // The established M3 filtered view explicitly exposes unknown cardinality.
        return exhausted ? 0L : Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return characteristics;
    }

    @Override
    public Comparator<? super E> getComparator() {
        throw new IllegalStateException("filtered ArrayList is not SORTED");
    }
}
