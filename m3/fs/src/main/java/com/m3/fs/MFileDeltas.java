/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A bulk, index-aligned comparison of independently observed M3FS file evidence.
 *
 * <p>Each ordinal refers to the same exact provider address in both inputs.
 * Missing inputs are never interpreted as absence: callers must supply an
 * explicit {@link MFileObservation#absent(String)} when a provider has actually
 * established that an address does not exist. A filtered, interrupted, or failed
 * traversal cannot establish deletion by omitting an entry. As with an individual
 * {@link MFileDelta}, metadata equality never proves equal content, and these
 * comparisons do not establish an atomic filesystem snapshot.</p>
 *
 * <p>The retained representation consists of two copied reference arrays and
 * one primitive {@code long[]} of evidence masks, not retained per-row deltas,
 * map entries, index nodes, or reconstructed provider objects. A delta object is
 * materialized only on explicit API demand. Every input pair is checked and
 * evaluated before the immutable result is published.</p>
 */
public final class MFileDeltas {
    private MFileDeltas() { }

    /**
     * Compare matched observations in caller order and capture their reason masks.
     * Input arrays are copied before evaluation; their later replacement does not
     * change the batch. Observation values themselves are immutable.
     *
     * @throws NullPointerException if either array or any observation is null
     * @throws IllegalArgumentException for different lengths or source identities
     */
    public static Batch betweenAligned(MFileObservation[] before, MFileObservation[] after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        return compareOwned(before.clone(), after.clone());
    }

    /**
     * List convenience overload; copies both lists without indexed traversal,
     * retaining the same explicit pairwise identity and absence requirements.
     */
    public static Batch betweenAligned(
            List<? extends MFileObservation> before,
            List<? extends MFileObservation> after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        return compareOwned(before.toArray(new MFileObservation[0]),
                after.toArray(new MFileObservation[0]));
    }

    /** One serial, per-pair behavioral atom shared by both input overloads. */
    private static Batch compareOwned(MFileObservation[] before, MFileObservation[] after) {
        if (before.length != after.length) {
            throw new IllegalArgumentException("unaligned observation counts");
        }
        long[] reasons = new long[before.length];
        for (int ordinal = 0; ordinal < reasons.length; ordinal++) {
            MFileObservation oldValue = before[ordinal];
            MFileObservation newValue = after[ordinal];
            if (oldValue == null || newValue == null) {
                throw new NullPointerException("null observation at ordinal " + ordinal);
            }
            if (!oldValue.sourceKey().equals(newValue.sourceKey())) {
                // Do not leak a provider address; it may contain credentials.
                throw new IllegalArgumentException("source mismatch at ordinal " + ordinal);
            }
            reasons[ordinal] = MFileDelta.between(oldValue, newValue).reasons();
        }
        return new Batch(before, after, reasons);
    }

    /** An immutable primitive-mask result indexed in original encounter order. */
    public static final class Batch {
        private final MFileObservation[] before;
        private final MFileObservation[] after;
        private final long[] reasons;

        private Batch(MFileObservation[] before, MFileObservation[] after, long[] reasons) {
            this.before = before;
            this.after = after;
            this.reasons = reasons;
        }

        public int size() { return reasons.length; }

        /** Returns the captured complete 64-bit reason mask at an ordinal. */
        public long reasonsAt(int ordinal) { return reasons[ordinal]; }

        /** Materializes a single original-contract delta; none are stored by this batch. */
        public MFileDelta deltaAt(int ordinal) {
            return MFileDelta.between(before[ordinal], after[ordinal]);
        }

        /**
         * Counts ordinals with every required bit and no excluded bit.
         * A zero required mask matches all ordinals not vetoed by the exclusions.
         */
        public int count(long requiredAll, long forbiddenAny) {
            int count = 0;
            for (long value : reasons) {
                if (selected(value, requiredAll, forbiddenAny)) count++;
            }
            return count;
        }

        /**
         * Returns only matching original ordinals, with exact capacity and no
         * retained Entry/Node objects. The caller owns the returned {@code int[]}.
         */
        public int[] selectedOrdinals(long requiredAll, long forbiddenAny) {
            int[] result = new int[count(requiredAll, forbiddenAny)];
            int written = 0;
            for (int ordinal = 0; ordinal < reasons.length; ordinal++) {
                if (selected(reasons[ordinal], requiredAll, forbiddenAny)) {
                    result[written++] = ordinal;
                }
            }
            return result;
        }

        /** Visit matching ordinals without allocating a selection array. */
        public void forEachOrdinal(long requiredAll, long forbiddenAny, IntConsumer action) {
            Objects.requireNonNull(action, "action");
            for (int ordinal = 0; ordinal < reasons.length; ordinal++) {
                if (selected(reasons[ordinal], requiredAll, forbiddenAny)) {
                    action.accept(ordinal);
                }
            }
        }

        /** Materialize and visit only requested deltas; callback failures propagate. */
        public void forEachDelta(long requiredAll, long forbiddenAny,
                Consumer<? super MFileDelta> action) {
            Objects.requireNonNull(action, "action");
            for (int ordinal = 0; ordinal < reasons.length; ordinal++) {
                if (selected(reasons[ordinal], requiredAll, forbiddenAny)) {
                    action.accept(deltaAt(ordinal));
                }
            }
        }

        /** Pure primitive-mask selection reused by counting, cursor and projection. */
        private static boolean selected(long evidence, long requiredAll, long forbiddenAny) {
            return (evidence & requiredAll) == requiredAll
                    && (evidence & forbiddenAny) == 0L;
        }
    }
}
