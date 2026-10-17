// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Parent-owned immutable byte-state masks and composable source-ordinal ranges.
 *
 * <p>DOOP: the original M3LongCollection owns primitive payloads. One copied byte-state
 * array belongs to the shared parent descriptor; successive range/flag plans store only
 * offsets and a mask, without copying payloads, masks, or allocating per-row facades.
 * Live payload mutations retain the original owner's rules. The plan refuses changes in
 * source cardinality, but cannot diagnose mutations that preserve cardinality. No
 * concurrent mutation or position reordering is supported while a plan is in use.</p>
 *
 * <p>Ranges use original source ordinals, NOT ranks in an earlier filtered view.
 * The required bits are conjunctive, and forbidden bits must all be clear.
 * A conflicting required/forbidden bit combination selects no rows, not an error.
 * A native backend is available only for explicitly materialized source row IDs and
 * reuses M3IndexMaskCompactor; arbitrary Java callbacks never cross JNI.</p>
 */
public final class M3LongMaskRows {
    private static final int ALL_BYTE_BITS = 0xff;

    private final Parent parent;
    private final int from;
    private final int to;
    private final int requiredFlags;
    private final int excludedFlags;

    private record Parent(M3LongCollection owner, byte[] flags) { }

    private M3LongMaskRows(Parent parent, int from, int to,
            int requiredFlags, int excludedFlags) {
        this.parent = parent;
        this.from = from;
        this.to = to;
        this.requiredFlags = requiredFlags;
        this.excludedFlags = excludedFlags;
    }

    /** Admission copies the caller's state bytes only once, never the long payload. */
    public static M3LongMaskRows from(M3LongCollection owner, byte[] alignedFlags) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(alignedFlags, "alignedFlags");
        if (owner.size() != alignedFlags.length) {
            throw new IllegalArgumentException("state mask must align to source ordinals");
        }
        return new M3LongMaskRows(new Parent(owner, alignedFlags.clone()), 0,
                alignedFlags.length, 0, 0);
    }

    /** Compose required flags without cloning the already-owned state. */
    public M3LongMaskRows requireFlags(int bits) {
        if ((bits & ~ALL_BYTE_BITS) != 0) {
            throw new IllegalArgumentException("required flags must fit an unsigned byte");
        }
        return new M3LongMaskRows(parent, from, to, requiredFlags | bits, excludedFlags);
    }

    /** Compose forbidden flags without copying the parent bytes or the payload. */
    public M3LongMaskRows excludeFlags(int bits) {
        if ((bits & ~ALL_BYTE_BITS) != 0) {
            throw new IllegalArgumentException("excluded flags must fit an unsigned byte");
        }
        return new M3LongMaskRows(parent, from, to, requiredFlags, excludedFlags | bits);
    }

    /** Intersect original source-offset ranges; no copying or iterator traversal. */
    public M3LongMaskRows sourceRange(int firstInclusive, int lastExclusive) {
        if (firstInclusive < from || lastExclusive > to || firstInclusive > lastExclusive) {
            throw new IndexOutOfBoundsException("source range not within current range");
        }
        return new M3LongMaskRows(parent, firstInclusive, lastExclusive,
                requiredFlags, excludedFlags);
    }

    public int sourceStart() { return from; }
    public int sourceEnd() { return to; }
    public int requiredFlags() { return requiredFlags; }
    public int excludedFlags() { return excludedFlags; }

    /** Number of accepted original ordinals in [sourceStart, exclusiveOrdinal). No index retained. */
    public int rankBefore(int exclusiveOrdinal) {
        requireAligned();
        if (exclusiveOrdinal < from || exclusiveOrdinal > to) {
            throw new IndexOutOfBoundsException("source ordinal outside selected range");
        }
        int count = 0;
        for (int i = from; i < exclusiveOrdinal; i++) if (matches(i)) count++;
        requireAligned();
        return count;
    }

    /** Original source ordinal of an accepted row at zero-based visible rank, without materialization. */
    public int sourceRowAtRank(int rank) {
        requireAligned();
        if (rank < 0) throw new IndexOutOfBoundsException("negative selected rank");
        for (int i = from; i < to; i++) {
            if (matches(i) && rank-- == 0) {
                requireAligned();
                return i;
            }
        }
        requireAligned();
        throw new IndexOutOfBoundsException("selected rank outside view");
    }

    /** Count selected ordinals; O(range length), O(1) allocation. */
    public int selectedCount() {
        requireAligned();
        int count = 0;
        for (int i = from; i < to; i++) {
            if (matches(i)) count++;
        }
        return count;
    }

    /**
     * Exact selected original ordinals. The Java backend makes only the O(k) result,
     * with no O(range) temporary index/mask copies. The explicit JNI backend retains
     * the original array-based native interface and its O(range) transient inputs.
     */
    public int[] selectedSourceRows(M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        requireAligned();
        if (backend == M3IndexMaskCompactor.Backend.JAVA) {
            int[] result = new int[selectedCount()];
            int written = 0;
            for (int i = from; i < to; i++) {
                if (matches(i)) {
                    if (written >= result.length) {
                        throw new ConcurrentModificationException("selected row count drift");
                    }
                    result[written++] = i;
                }
            }
            if (written != result.length) {
                throw new ConcurrentModificationException("selected row count drift");
            }
            requireAligned();
            return result;
        }
        int width = to - from;
        int[] ordinals = new int[width];
        for (int i = 0; i < width; i++) ordinals[i] = from + i;
        byte[] flags;
        int required;
        if (excludedFlags == 0) {
            flags = Arrays.copyOfRange(parent.flags(), from, to);
            required = requiredFlags;
        } else {
            // Adapter: Java owns boolean predicate composition; JNI receives only bytes/indices.
            flags = new byte[width];
            for (int i = 0; i < width; i++) flags[i] = (byte) (matches(from + i) ? 1 : 0);
            required = 1;
        }
        int[] result = M3IndexMaskCompactor.select(ordinals, flags, required, backend);
        requireAligned();
        return result;
    }

    /** Lightweight read-only facade: no per-row object and no cached payload array. */
    public M3LongCollection view() { return new View(this); }

    /** Reuse the existing M3 DAG/LongStream owner for further declarative stages. */
    public M3LongDag dag() { return M3LongDag.from(view()); }

    private boolean matches(int position) {
        int state = parent.flags()[position] & ALL_BYTE_BITS;
        return (state & requiredFlags) == requiredFlags && (state & excludedFlags) == 0;
    }

    private void requireAligned() {
        if (parent.owner().size() != parent.flags().length) {
            throw new ConcurrentModificationException("parent cardinality changed after mask admission");
        }
    }

    /** One facade per requested view, not per value. Parent owns all values and flags. */
    private static final class View implements M3LongCollection {
        private final M3LongMaskRows plan;

        private View(M3LongMaskRows plan) { this.plan = plan; }

        @Override public int size() { return plan.selectedCount(); }
        @Override public boolean isEmpty() { return size() == 0; }
        @Override public boolean contains(long value) {
            M3LongIterator input = iterator();
            while (input.hasNext()) if (input.nextLong() == value) return true;
            return false;
        }
        @Override public void clear() {
            throw new UnsupportedOperationException("mask facade is read-only");
        }
        @Override public long[] toArray() {
            long[] values = new long[size()];
            int index = 0;
            M3LongIterator input = iterator();
            while (input.hasNext()) {
                if (index == values.length) throw new ConcurrentModificationException("selected count drift");
                values[index++] = input.nextLong();
            }
            if (index != values.length) throw new ConcurrentModificationException("selected count drift");
            return values;
        }
        @Override public M3LongIterator iterator() {
            plan.requireAligned();
            M3LongIterator source = plan.parent.owner().iterator();
            return new M3LongIterator() {
                private int position;
                private boolean ready;
                private boolean finished;
                private long next;

                @Override public boolean hasNext() {
                    plan.requireAligned();
                    if (ready) return true;
                    if (finished) return false;
                    while (position < plan.to) {
                        if (!source.hasNext()) {
                            throw new ConcurrentModificationException("source shortened during mask traversal");
                        }
                        long value = source.nextLong();
                        int ordinal = position++;
                        if (ordinal >= plan.from && plan.matches(ordinal)) {
                            next = value;
                            ready = true;
                            return true;
                        }
                    }
                    finished = true;
                    return false;
                }
                @Override public long nextLong() {
                    if (!hasNext()) throw new NoSuchElementException();
                    ready = false;
                    return next;
                }
            };
        }
        /** Late binding: source iterator is created on first demand, not on stream creation. */
        @Override public Spliterator.OfLong longSpliterator() {
            return new Spliterator.OfLong() {
                private M3LongIterator cursor;
                @Override public boolean tryAdvance(LongConsumer action) {
                    Objects.requireNonNull(action, "action");
                    if (cursor == null) cursor = iterator();
                    if (!cursor.hasNext()) return false;
                    action.accept(cursor.nextLong());
                    return true;
                }
                @Override public OfLong trySplit() { return null; }
                @Override public long estimateSize() { return Long.MAX_VALUE; }
                @Override public int characteristics() { return ORDERED | NONNULL; }
            };
        }
    }
}
