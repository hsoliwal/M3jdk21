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

import jdk.m3.collections.LongFilteredView;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Read-only live filtered view over a primitive sorted-long owner.
 *
 * <p>The existing {@link LongFilteredView} owns filtered iteration, membership and projection.
 * This class only adds sorted navigation. Arbitrary Java predicates stay in Java; no per-element
 * JNI callback ABI is introduced.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongFilteredSortedSetView.java}
 * blob {@code 335d5c4894a532a44ea5aa740a4bebe7ee07ed79} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections filtered sorted-set view (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongFilteredSortedSetView {
    private final LongSortedSet source;
    private final LongPredicate predicate;
    private final LongFilteredView filtered;

    public LongFilteredSortedSetView(LongSortedSet source, LongPredicate predicate) {
        this.source = Objects.requireNonNull(source, "source");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.filtered = new LongFilteredView(source, predicate);
    }

    public int size() {
        return filtered.size();
    }

    public boolean isEmpty() {
        return filtered.isEmpty();
    }

    public boolean contains(long value) {
        return filtered.contains(value);
    }

    public LongIterator iterator() {
        return filtered.iterator();
    }

    public void forEachLong(LongConsumer consumer) {
        filtered.forEachLong(consumer);
    }

    public long[] toArray() {
        return filtered.toArray();
    }

    public long first() {
        LongIterator iterator = iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        return iterator.nextLong();
    }

    public long last() {
        LongIterator iterator = iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        long last = iterator.nextLong();
        while (iterator.hasNext()) {
            last = iterator.nextLong();
        }
        return last;
    }

    public long floor(long value, long defaultValue) {
        return seekBackward(value, true, defaultValue);
    }

    public long lower(long value, long defaultValue) {
        return seekBackward(value, false, defaultValue);
    }

    public long ceiling(long value, long defaultValue) {
        return seekForward(value, true, defaultValue);
    }

    public long higher(long value, long defaultValue) {
        return seekForward(value, false, defaultValue);
    }

    private long seekBackward(long value, boolean inclusive, long defaultValue) {
        long candidate = inclusive
                ? source.floor(value, Long.MIN_VALUE)
                : source.lower(value, Long.MIN_VALUE);
        if (!backwardCandidateExists(candidate, value, inclusive)) {
            return defaultValue;
        }
        while (true) {
            if (predicate.test(candidate)) {
                return candidate;
            }
            if (candidate == Long.MIN_VALUE) {
                return defaultValue;
            }
            long previous = source.lower(candidate, Long.MIN_VALUE);
            if (previous == Long.MIN_VALUE && !source.contains(Long.MIN_VALUE)) {
                return defaultValue;
            }
            candidate = previous;
        }
    }

    private long seekForward(long value, boolean inclusive, long defaultValue) {
        long candidate = inclusive
                ? source.ceiling(value, Long.MAX_VALUE)
                : source.higher(value, Long.MAX_VALUE);
        if (!forwardCandidateExists(candidate, value, inclusive)) {
            return defaultValue;
        }
        while (true) {
            if (predicate.test(candidate)) {
                return candidate;
            }
            if (candidate == Long.MAX_VALUE) {
                return defaultValue;
            }
            long next = source.higher(candidate, Long.MAX_VALUE);
            if (next == Long.MAX_VALUE && !source.contains(Long.MAX_VALUE)) {
                return defaultValue;
            }
            candidate = next;
        }
    }

    private boolean backwardCandidateExists(long candidate, long value, boolean inclusive) {
        if (candidate != Long.MIN_VALUE) {
            return true;
        }
        return source.contains(Long.MIN_VALUE)
                && (inclusive || value != Long.MIN_VALUE);
    }

    private boolean forwardCandidateExists(long candidate, long value, boolean inclusive) {
        if (candidate != Long.MAX_VALUE) {
            return true;
        }
        return source.contains(Long.MAX_VALUE)
                && (inclusive || value != Long.MAX_VALUE);
    }
}
