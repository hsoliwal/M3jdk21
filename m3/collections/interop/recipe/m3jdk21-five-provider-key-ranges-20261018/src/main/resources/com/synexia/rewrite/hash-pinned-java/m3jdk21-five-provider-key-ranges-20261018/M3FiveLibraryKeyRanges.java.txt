/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Recipe-produced from canonical Synexia source-sealed Java 21 inputs.
 */
package com.m3.collections.interop;

import com.m3.collections.M3IndexMaskCompactor;
import java.util.Comparator;
import java.util.Objects;

/**
 * Search and range planning over the existing M3 five-provider indexed row directory.
 *
 * <p>Neither this plan nor its subplans own object payloads, state planes or an
 * independent index: they borrow the immutable {@link M3FiveLibraryIndexViews}
 * snapshot. The initial sort is stable and rearranges only the original index
 * plane. Subsequent filters and half-open windows preserve the established
 * comparator order and share immutable storage with the selected snapshot.
 *
 * <p>All ranks count row <em>occurrences</em>, not distinct keys. A comparator-
 * equivalent key may have multiple different values and repeated physical rows;
 * lower/upper bounds partition the stable sorted directory without dropping any.
 * The comparator must be deterministic and consistent during construction and
 * queries. Nulls are accepted only if it admits them. This is an immutable
 * descriptor, not a live JDK NavigableMap view, and does not change the provider
 * materializations' duplicate/null/iterator/serialization contracts.</p>
 *
 * <p>Binary-search queries take O(log n) comparisons and O(1) additional memory;
 * range windows take O(log n) comparisons with no payload/index copy. Filtering
 * delegates to the existing Java/optional C17 JNI mask selector, so no callbacks
 * or Java objects cross JNI, and NATIVE_REQUIRED never silently falls back.</p>
 */
public final class M3FiveLibraryKeyRanges<K, V> {
    private final M3FiveLibraryIndexViews<K, V> ordered;
    private final Comparator<? super K> comparator;

    private M3FiveLibraryKeyRanges(M3FiveLibraryIndexViews<K, V> ordered,
            Comparator<? super K> comparator) {
        this.ordered = ordered;
        this.comparator = comparator;
    }

    /** Stable-sort the current occurrence window by comparator, keeping key ties. */
    public static <K, V> M3FiveLibraryKeyRanges<K, V> sorted(
            M3FiveLibraryIndexViews<K, V> source, Comparator<? super K> comparator) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(comparator, "comparator");
        return new M3FiveLibraryKeyRanges<>(source.sortedByKey(comparator), comparator);
    }

    /** Borrow the immutable directory; convert into actual provider APIs using its methods. */
    public M3FiveLibraryIndexViews<K, V> view() { return ordered; }

    public Comparator<? super K> comparator() { return comparator; }
    public int size() { return ordered.size(); }
    public boolean isEmpty() { return ordered.isEmpty(); }

    /** Insertion ordinal before all keys comparator-equivalent to {@code key}. */
    public int lowerBound(K key) {
        int low = 0;
        int high = size();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (comparator.compare(ordered.keyAt(mid), key) < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** Insertion ordinal after all keys comparator-equivalent to {@code key}. */
    public int upperBound(K key) {
        int low = 0;
        int high = size();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (comparator.compare(ordered.keyAt(mid), key) <= 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    public int countEquivalent(K key) { return upperBound(key) - lowerBound(key); }
    public boolean containsEquivalent(K key) { return lowerBound(key) != upperBound(key); }

    /** Half-open comparator-equality window; preserves duplicate occurrences. */
    public M3FiveLibraryKeyRanges<K, V> equalTo(K key) {
        return ordinalWindow(lowerBound(key), upperBound(key));
    }

    /** Comparator-ordered half-open ordinal view, with no source/index copy. */
    public M3FiveLibraryKeyRanges<K, V> ordinalWindow(int from, int to) {
        return new M3FiveLibraryKeyRanges<>(ordered.slice(from, to), comparator);
    }

    /** All keys preceding the bound in comparator order. */
    public M3FiveLibraryKeyRanges<K, V> head(K end, boolean inclusive) {
        return ordinalWindow(0, inclusive ? upperBound(end) : lowerBound(end));
    }

    /** All keys following the bound in comparator order. */
    public M3FiveLibraryKeyRanges<K, V> tail(K start, boolean inclusive) {
        return ordinalWindow(inclusive ? lowerBound(start) : upperBound(start), size());
    }

    /**
     * Comparator-ordered interval; endpoints outside comparator order refuse.
     * Equal endpoints with either exclusive bound select the empty interval.
     * This is not the JDK's backed range endpoint-validation protocol.
     */
    public M3FiveLibraryKeyRanges<K, V> between(K start, boolean startInclusive,
            K end, boolean endInclusive) {
        if (comparator.compare(start, end) > 0) {
            throw new IllegalArgumentException("start sorts after end");
        }
        int first = startInclusive ? lowerBound(start) : upperBound(start);
        int last = endInclusive ? upperBound(end) : lowerBound(end);
        if (last < first) last = first;
        return ordinalWindow(first, last);
    }

    /**
     * Apply required/forbidden unsigned-byte state bits without re-sorting the
     * directory. Java or explicit existing JNI selects occurrence indexes only.
     */
    public M3FiveLibraryKeyRanges<K, V> filteredByState(
            int required, int excluded, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        return new M3FiveLibraryKeyRanges<>(
                ordered.filteredByState(required, excluded, backend), comparator);
    }

    public M3FiveLibraryKeyRanges<K, V> filteredByState(int required, int excluded) {
        return filteredByState(required, excluded, M3IndexMaskCompactor.Backend.JAVA);
    }
}
