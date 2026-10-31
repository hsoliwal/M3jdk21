/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated from the canonical Synexia source-sealed Maven recipe.
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiPredicate;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.springframework.util.MultiValueMap;

/**
 * One reusable immutable filter plan over the existing M3 five-provider indexed snapshot.
 * Predicates run once per logical occurrence; repeated physical row IDs remain distinct.
 * The only retained selection data is a BitSet of logical ordinals. Every projection
 * stores int[] ordinals and half-open offsets, sharing the existing captured payload.
 * No per-row Entry/Node object is retained, and no Java callback enters JNI.
 *
 * <p>Provider outputs are independently materialized via existing M3-backed factories.
 * JDK Map uses last-write-wins; Guava/Apache/Eclipse multimaps count value pairs;
 * Spring MultiValueMap is a map of lists whose size counts distinct keys.
 * This plan is a detached snapshot, not a live vendor filtered view.
 */
public final class M3FiveFamilyPreparedSelection<K, V> {
    private final M3FiveLibraryIndexViews<K, V> source;
    private final BitSet bits;
    private final int count;
    private volatile Projection<K, V> javaProjection;

    private M3FiveFamilyPreparedSelection(M3FiveLibraryIndexViews<K, V> source, BitSet bits) {
        this.source = source;
        this.bits = bits;
        this.count = bits.cardinality();
    }

    public static <K, V> M3FiveFamilyPreparedSelection<K, V> all(
            M3FiveLibraryIndexViews<K, V> source) {
        Objects.requireNonNull(source, "source");
        BitSet mask = new BitSet();
        mask.set(0, source.size());
        return new M3FiveFamilyPreparedSelection<>(source, mask);
    }

    public static <K, V> M3FiveFamilyPreparedSelection<K, V> where(
            M3FiveLibraryIndexViews<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        BitSet mask = new BitSet();
        for (int i = 0; i < source.size(); i++) {
            if (predicate.test(source.keyAt(i), source.valueAt(i))) mask.set(i);
        }
        return new M3FiveFamilyPreparedSelection<>(source, mask);
    }

    public static <K, V> M3FiveFamilyPreparedSelection<K, V> fromBits(
            M3FiveLibraryIndexViews<K, V> source, BitSet mask) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mask, "mask");
        if (mask.length() > source.size()) throw new IndexOutOfBoundsException("mask exceeds logical row count");
        return new M3FiveFamilyPreparedSelection<>(source, (BitSet) mask.clone());
    }

    public static <K, V> M3FiveFamilyPreparedSelection<K, V> fromStates(
            M3FiveLibraryIndexViews<K, V> source, int required, int excluded) {
        Objects.requireNonNull(source, "source");
        if (((required | excluded) & ~0xff) != 0 || (required & excluded) != 0) {
            throw new IllegalArgumentException("state masks must be disjoint unsigned bytes");
        }
        BitSet mask = new BitSet();
        for (int i = 0; i < source.size(); i++) {
            int state = source.unsignedStateAt(i);
            if ((state & required) == required && (state & excluded) == 0) mask.set(i);
        }
        return new M3FiveFamilyPreparedSelection<>(source, mask);
    }

    public int size() { return count; }
    public boolean isEmpty() { return count == 0; }
    public boolean containsOrdinal(int ordinal) {
        return bits.get(Objects.checkIndex(ordinal, source.size()));
    }
    public BitSet bitsCopy() { return (BitSet) bits.clone(); }

    private void sameSource(M3FiveFamilyPreparedSelection<K, V> other) {
        Objects.requireNonNull(other, "other");
        if (source != other.source) {
            throw new IllegalArgumentException("filters refer to different immutable source snapshots");
        }
    }
    public M3FiveFamilyPreparedSelection<K, V> and(M3FiveFamilyPreparedSelection<K, V> other) {
        sameSource(other); BitSet combined = bitsCopy(); combined.and(other.bits);
        return new M3FiveFamilyPreparedSelection<>(source, combined);
    }
    public M3FiveFamilyPreparedSelection<K, V> or(M3FiveFamilyPreparedSelection<K, V> other) {
        sameSource(other); BitSet combined = bitsCopy(); combined.or(other.bits);
        return new M3FiveFamilyPreparedSelection<>(source, combined);
    }
    public M3FiveFamilyPreparedSelection<K, V> minus(M3FiveFamilyPreparedSelection<K, V> other) {
        sameSource(other); BitSet combined = bitsCopy(); combined.andNot(other.bits);
        return new M3FiveFamilyPreparedSelection<>(source, combined);
    }
    public M3FiveFamilyPreparedSelection<K, V> xor(M3FiveFamilyPreparedSelection<K, V> other) {
        sameSource(other); BitSet combined = bitsCopy(); combined.xor(other.bits);
        return new M3FiveFamilyPreparedSelection<>(source, combined);
    }
    public M3FiveFamilyPreparedSelection<K, V> not() {
        BitSet combined = bitsCopy(); combined.flip(0, source.size());
        return new M3FiveFamilyPreparedSelection<>(source, combined);
    }
    /** Only currently accepted occurrences are evaluated by an additional predicate. */
    public M3FiveFamilyPreparedSelection<K, V> refine(BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        BitSet refined = bitsCopy();
        for (int i = refined.nextSetBit(0); i >= 0; i = refined.nextSetBit(i + 1)) {
            if (!predicate.test(source.keyAt(i), source.valueAt(i))) refined.clear(i);
        }
        return new M3FiveFamilyPreparedSelection<>(source, refined);
    }

    /** Java projection is created once and reused without reevaluating predicates. */
    public Projection<K, V> projection() {
        Projection<K, V> seen = javaProjection;
        if (seen != null) return seen;
        synchronized (this) {
            seen = javaProjection;
            if (seen == null) {
                int[] ordinals = new int[count];
                int n = 0;
                for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) ordinals[n++] = i;
                seen = new Projection<>(source, ordinals, 0, ordinals.length);
                javaProjection = seen;
            }
            return seen;
        }
    }
    /** Native selection reuses the existing M3 int[]/byte[] compactor; no callbacks. */
    public Projection<K, V> projection(M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        if (backend == M3IndexMaskCompactor.Backend.JAVA) return projection();
        int n = source.size();
        int[] ordered = new int[n];
        byte[] admitted = new byte[n];
        for (int i = 0; i < n; i++) ordered[i] = i;
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) admitted[i] = 1;
        int[] chosen = M3IndexMaskCompactor.select(ordered, admitted, 1, backend);
        return new Projection<>(source, chosen, 0, chosen.length);
    }

    /** One ordered int[] logical-path view; source payloads are never duplicated. */
    public static final class Projection<K, V> implements Iterable<V> {
        private final M3FiveLibraryIndexViews<K, V> source;
        private final int[] ordinals;
        private final int from, to;
        private Projection(M3FiveLibraryIndexViews<K, V> source, int[] ordinals, int from, int to) {
            this.source = source; this.ordinals = ordinals; this.from = from; this.to = to;
        }
        public int size() { return to - from; }
        public boolean isEmpty() { return from == to; }
        public int originalOrdinal(int index) { return ordinals[from + Objects.checkIndex(index, size())]; }
        public K keyAt(int index) { return source.keyAt(originalOrdinal(index)); }
        public V valueAt(int index) { return source.valueAt(originalOrdinal(index)); }
        public Projection<K, V> slice(int start, int end) {
            Objects.checkFromToIndex(start, end, size());
            return new Projection<>(source, ordinals, from + start, from + end);
        }
        /** Stable mergesort of int ordinal metadata, preserving encounter-order ties. */
        public Projection<K, V> sortedByKey(Comparator<? super K> comparator) {
            Objects.requireNonNull(comparator, "comparator");
            int[] current = Arrays.copyOfRange(ordinals, from, to);
            int[] scratch = new int[current.length];
            for (long width = 1; width < current.length; width <<= 1) {
                for (long base = 0; base < current.length; base += width * 2) {
                    int a = (int) base, mid = (int) Math.min(base + width, current.length);
                    int b = mid, end = (int) Math.min(base + width * 2, current.length);
                    int out = a;
                    while (a < mid && b < end) {
                        if (comparator.compare(source.keyAt(current[a]), source.keyAt(current[b])) <= 0)
                            scratch[out++] = current[a++];
                        else scratch[out++] = current[b++];
                    }
                    while (a < mid) scratch[out++] = current[a++];
                    while (b < end) scratch[out++] = current[b++];
                }
                int[] temp = current; current = scratch; scratch = temp;
            }
            return new Projection<>(source, current, 0, current.length);
        }
        @Override public Iterator<V> iterator() {
            return new Iterator<>() {
                private int i;
                @Override public boolean hasNext() { return i < size(); }
                @Override public V next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return valueAt(i++);
                }
            };
        }
        public List<V> asJdkList() {
            List<V> result = M3LibraryCollections.jdkList();
            for (int i = 0; i < size(); i++) result.add(valueAt(i));
            return result;
        }
        public Map<K, V> asJdkMapLastWins() {
            Map<K, V> result = M3LibraryCollections.jdkMap();
            for (int i = 0; i < size(); i++) result.put(keyAt(i), valueAt(i));
            return result;
        }
        public ListMultimap<K, V> asGuavaListMultimap() {
            ListMultimap<K, V> result = M3LibraryCollections.guavaListMultimap();
            for (int i = 0; i < size(); i++) result.put(keyAt(i), valueAt(i));
            return result;
        }
        public MutableListMultimap<K, V> asEclipseListMultimap() {
            MutableListMultimap<K, V> result = new M3EclipseListMultimap<>();
            for (int i = 0; i < size(); i++) result.put(keyAt(i), valueAt(i));
            return result;
        }
        public ListValuedMap<K, V> asApacheListValuedMap() {
            ListValuedMap<K, V> result = M3LibraryCollections.apacheListValuedMap();
            for (int i = 0; i < size(); i++) result.put(keyAt(i), valueAt(i));
            return result;
        }
        public MultiValueMap<K, V> asSpringMultiValueMap() {
            MultiValueMap<K, V> result = M3LibraryCollections.springMultiValueMap();
            for (int i = 0; i < size(); i++) result.add(keyAt(i), valueAt(i));
            return result;
        }
    }
}
