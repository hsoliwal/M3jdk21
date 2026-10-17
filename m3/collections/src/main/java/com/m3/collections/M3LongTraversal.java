/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Derived from the M3LongFilteredView late-binding cursor; see recipe provenance.
 */
package com.m3.collections;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Shared package-local cursor atom for primitive live views, not an element store.
 * Construction, metadata, a refused null action and trySplit never acquire an iterator.
 * The first valid traversal binds once; later interference follows the owner's iterator.
 * No size scan, split buffer or per-element object is introduced.
 */
final class M3LongTraversal {
    private M3LongTraversal() { }

    /** Characteristics are facts supplied by the owning collection, not inferred by scanning. */
    static Spliterator.OfLong of(M3LongCollection source, int characteristics) {
        Objects.requireNonNull(source, "source");
        return new Spliterator.OfLong() {
            private M3LongIterator cursor;

            @Override
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) {
                    cursor = source.iterator();
                }
                if (!cursor.hasNext()) return false;
                action.accept(cursor.nextLong());
                return true;
            }

            @Override public OfLong trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return characteristics; }
            @Override public Comparator<? super Long> getComparator() {
                if ((characteristics & SORTED) == 0) throw new IllegalStateException();
                return null;
            }
        };
    }
}
