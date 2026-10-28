/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact multi-dimensional AND/OR over already-owned typed signal range indexes.
 *
 * <p>A plan retains one M3SignalExactRangeIndex per selected dimension and primitive
 * dimension IDs; it never stores application objects, value copies, per-row Entry or
 * Node objects. A query evaluates typed inclusive ranges or the explicit missing
 * selector and accumulates physical-row membership in primitive {@code long[]} masks.
 * Results are in ascending physical-row order, regardless of source column sort
 * direction; no weighted score or approximate signal is treated as an exact value.
 *
 * <p>This is an exact *stored-coordinate* query, not a semantic-equivalence proof.
 * Its source snapshot must be immutable, and callers must signal source changes
 * to the original M3SignalDependencyEpochs owner (including {@code invalidateAll()}
 * when dependencies are unknown). Epoch checks before and after the computation
 * reject observed changes, but cannot hold a lock after results are returned.
 */
public final class M3SignalBooleanQuery {
    /** One dimension-specific predicate; these are query parameters, not row objects. */
    public sealed interface Predicate permits IntBetween, LongBetween, DoubleBetween, Missing { }

    /** Inclusive signed 32-bit integer coordinates. */
    public record IntBetween(int lower, int upper) implements Predicate {
        public IntBetween {
            if (lower > upper) throw new IllegalArgumentException("inverted integer range");
        }
    }

    /** Inclusive signed 64-bit integer coordinates, without double conversion. */
    public record LongBetween(long lower, long upper) implements Predicate {
        public LongBetween {
            if (lower > upper) throw new IllegalArgumentException("inverted long range");
        }
    }

    /** Inclusive JDK Double.compare total order, preserving signed zero and NaN. */
    public record DoubleBetween(double lower, double upper) implements Predicate {
        public DoubleBetween {
            if (Double.compare(lower, upper) > 0) {
                throw new IllegalArgumentException("inverted double total-order range");
            }
        }
    }

    /** Missing is not numeric zero, NaN or an unknown estimate. */
    public enum Missing implements Predicate { INSTANCE }

    private final M3SignalQualitySchema schema;
    private final M3SignalDependencyEpochs epochs;
    private final M3SignalExactRangeIndex[] indexes;
    private final int[] dimensions;
    private final int rows;
    private final long builtEpoch;

    private M3SignalBooleanQuery(M3SignalQualitySchema schema, int[] selected) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.epochs = schema.invalidationEpochsOwner();
        if (selected.length == 0) {
            throw new IllegalArgumentException("at least one signal dimension is required");
        }
        this.rows = schema.rowCount();
        this.dimensions = selected.clone();
        boolean[] used = new boolean[schema.dimensionCount()];
        for (int dimension : dimensions) {
            Objects.checkIndex(dimension, used.length);
            if (used[dimension]) throw new IllegalArgumentException("duplicate signal dimension");
            used[dimension] = true;
        }
        long firstEpoch = epochs.epoch();
        indexes = new M3SignalExactRangeIndex[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            indexes[i] = M3SignalExactRangeIndex.build(schema, dimensions[i]);
        }
        if (epochs.epoch() != firstEpoch) {
            throw new IllegalStateException("signals changed while building query indexes");
        }
        this.builtEpoch = firstEpoch;
    }

    /** Build one reusable typed index plan from the *same* original schema owner. */
    public static M3SignalBooleanQuery build(M3SignalQualitySchema schema, int... dimensions) {
        Objects.requireNonNull(dimensions, "dimensions");
        return new M3SignalBooleanQuery(schema, dimensions);
    }

    public int predicateCount() { return dimensions.length; }
    public int dimensionAt(int ordinal) {
        return dimensions[Objects.checkIndex(ordinal, dimensions.length)];
    }

    /** Select coordinates matching every declared predicate (physical row order). */
    public int[] and(long sourceVersion, String sourceSha256, Predicate... predicates) {
        return select(false, sourceVersion, sourceSha256, predicates);
    }

    /** Select coordinates matching at least one predicate (physical row order). */
    public int[] or(long sourceVersion, String sourceSha256, Predicate... predicates) {
        return select(true, sourceVersion, sourceSha256, predicates);
    }

    private int[] select(boolean union, long version, String digest, Predicate[] predicates) {
        Objects.requireNonNull(predicates, "predicates");
        if (predicates.length != indexes.length) {
            throw new IllegalArgumentException("query predicates must match indexed dimensions");
        }
        // Validate the whole plan before any candidate materialization or mutation.
        for (int i = 0; i < predicates.length; i++) {
            Predicate predicate = Objects.requireNonNull(predicates[i], "predicate[" + i + "]");
            if (predicate instanceof Missing) continue;
            M3SignalColumns.Kind kind = indexes[i].kind();
            if ((predicate instanceof IntBetween && kind != M3SignalColumns.Kind.INTEGER)
                    || (predicate instanceof LongBetween && kind != M3SignalColumns.Kind.LONG)
                    || (predicate instanceof DoubleBetween && kind != M3SignalColumns.Kind.DOUBLE)) {
                throw new IllegalArgumentException("typed predicate does not match column");
            }
        }
        requireFresh(version, digest);
        int words = (int) (((long) rows + 63) >>> 6);
        long[] membership = new long[words];
        long[] scratch = union ? null : new long[words];
        for (int i = 0; i < indexes.length; i++) {
            long[] target = union || i == 0 ? membership : scratch;
            if (!union && i > 0) Arrays.fill(target, 0L);
            markPredicate(indexes[i], predicates[i], target, version, digest, null);
            if (!union && i > 0) {
                for (int word = 0; word < words; word++) {
                    membership[word] &= scratch[word];
                }
            }
        }
        int count = 0;
        for (long word : membership) count = Math.addExact(count, Long.bitCount(word));
        int[] result = new int[count];
        int cursor = 0;
        for (int wordIndex = 0; wordIndex < membership.length; wordIndex++) {
            long word = membership[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                result[cursor++] = (wordIndex << 6) + bit;
                word &= word - 1L;
            }
        }
        requireFresh(version, digest);
        return result;
    }

    /** Fused range-to-bitmask projection; no intermediate int[] result per predicate. */
    private static void markPredicate(M3SignalExactRangeIndex index, Predicate predicate,
            long[] target, long version, String sha, M3ProgressMonitor monitor) {
        if (predicate instanceof Missing) {
            index.markMissing(target, version, sha, monitor);
        } else if (predicate instanceof IntBetween range) {
            index.markInts(target, range.lower, range.upper, version, sha, monitor);
        } else if (predicate instanceof LongBetween range) {
            index.markLongs(target, range.lower, range.upper, version, sha, monitor);
        } else if (predicate instanceof DoubleBetween range) {
            index.markDoubles(target, range.lower, range.upper, version, sha, monitor);
        } else {
            throw new IllegalArgumentException("unsupported signal predicate");
        }
    }

    /**
     * Postfix expression steps are query-time plans, never per-entity metadata.
     * Term.slot addresses the ordinal in the query's selected dimensions, not the
     * global column number. AND/OR are binary, NOT unary, ALL/NONE are constants.
     */
    public sealed interface Step permits Term, Logic { }

    public record Term(int slot, Predicate predicate) implements Step {
        public Term { Objects.requireNonNull(predicate, "predicate"); }
    }

    public enum Logic implements Step { AND, OR, NOT, ALL, NONE }

    /**
     * Validate and compile one bounded postfix Boolean expression into primitive
     * opcodes. Arbitrary nested AND/OR/NOT is supported without recursive evaluation.
     * A missing coordinate remains distinct from NaN, zero and an unknown estimate.
     */
    public Compiled compile(Step... expression) {
        Objects.requireNonNull(expression, "expression");
        if (expression.length == 0 || expression.length > 4096) {
            throw new IllegalArgumentException("postfix expression length outside [1,4096]");
        }
        requireFresh(schema.sourceVersion(), schema.sourceSha256());
        byte[] opcodes = new byte[expression.length];
        int[] slots = new int[expression.length];
        Predicate[] filters = new Predicate[expression.length];
        int depth = 0;
        int maximumDepth = 0;
        for (int i = 0; i < expression.length; i++) {
            Step step = Objects.requireNonNull(expression[i], "expression[" + i + "]");
            if (step instanceof Term term) {
                int slot = Objects.checkIndex(term.slot(), indexes.length);
                validatePredicate(slot, term.predicate());
                opcodes[i] = 0;
                slots[i] = slot;
                filters[i] = term.predicate();
                depth++;
            } else if (step instanceof Logic logical) {
                opcodes[i] = (byte) (logical.ordinal() + 1);
                switch (logical) {
                    case ALL, NONE -> depth++;
                    case NOT -> {
                        if (depth < 1) throw new IllegalArgumentException("postfix NOT stack underflow");
                    }
                    case AND, OR -> {
                        if (depth < 2) throw new IllegalArgumentException("postfix binary stack underflow");
                        depth--;
                    }
                }
            } else {
                throw new IllegalArgumentException("unsupported signal instruction");
            }
            maximumDepth = Math.max(maximumDepth, depth);
        }
        if (depth != 1) throw new IllegalArgumentException("postfix expression must produce one result");
        return new Compiled(opcodes, slots, filters, maximumDepth);
    }

    private void validatePredicate(int slot, Predicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (predicate instanceof Missing) return;
        M3SignalColumns.Kind expected = indexes[Objects.checkIndex(slot, indexes.length)].kind();
        if ((predicate instanceof IntBetween && expected != M3SignalColumns.Kind.INTEGER)
                || (predicate instanceof LongBetween && expected != M3SignalColumns.Kind.LONG)
                || (predicate instanceof DoubleBetween && expected != M3SignalColumns.Kind.DOUBLE)) {
            throw new IllegalArgumentException("postfix predicate does not match typed column");
        }
    }

    /** Reusable immutable bytecode; no retained per-row expression or value wrappers. */
    public final class Compiled {
        private final byte[] codes;
        private final int[] slots;
        private final Predicate[] filters;
        private final int maximumDepth;

        private Compiled(byte[] codes, int[] slots, Predicate[] filters, int maximumDepth) {
            this.codes = codes;
            this.slots = slots;
            this.filters = filters;
            this.maximumDepth = maximumDepth;
        }

        public int instructionCount() { return codes.length; }
        public int maximumStackDepth() { return maximumDepth; }

        /**
         * Evaluate nested postfix predicates using bounded, temporary primitive
         * bitmasks. No recursive traversal or per-source-row graph is retained.
         * The caller must revalidate source lifecycle before publishing the result.
         */
        public int[] evaluate(long version, String sourceSha256) {
            return evaluate(version, sourceSha256, null);
        }

        /**
         * Optional existing M3 progress/cancellation owner, not a new scheduler.
         * Progress is credited per completed instruction; cancellation is polled
         * during large primitive scans. No partial result is ever published and
         * done() is called only after a successful complete evaluation.
         */
        public int[] evaluate(long version, String sourceSha256,
                M3ProgressMonitor monitor) {
            return executeWindow(0, Integer.MAX_VALUE, version, sourceSha256, monitor).rows;
        }

        /**
         * Rank the exact stored-coordinate matches without materializing all
         * matched physical IDs. The caller explicitly supplies typed precedence;
         * these coordinates are not semantic equality or sound-distance proofs.
         * At most k rows are returned, ordered by the original stable Top-K owner.
         */
        public int[] evaluateTopK(int limit, int[] precedence,
                long version, String digest) {
            return evaluateTopK(limit, precedence, version, digest, null);
        }

        /** Cancellable Top-K execution over the original packed result mask. */
        public int[] evaluateTopK(int limit, int[] precedence,
                long version, String digest, M3ProgressMonitor monitor) {
            if (limit < 0) throw new IllegalArgumentException("negative ranked result limit");
            Objects.requireNonNull(precedence, "precedence");
            requireFresh(version, digest);
            M3SignalTopK ranking = new M3SignalTopK(schema.valueColumnsOwner(), precedence);
            return executeWindow(0, limit, version, digest, monitor, ranking).rows;
        }

        /**
         * Exact nearest-neighbor ranking of matching rows, using the existing
         * integer-distance owner's unsigned 128-bit primitive metric and heap.
         * A plan from a different schema is refused even if row counts and SHA
         * values coincide. This does not certify semantic/object equivalence.
         */
        public int[] evaluateNearest(int sourceRow, int limit,
                M3SignalExactIntegerDistance metric, long version, String digest) {
            return evaluateNearest(sourceRow, limit, metric, version, digest, null);
        }

        /** Poll cancellation without resetting the enclosing M3 progress task. */
        public int[] evaluateNearest(int sourceRow, int limit,
                M3SignalExactIntegerDistance metric, long version, String digest,
                M3ProgressMonitor monitor) {
            Objects.requireNonNull(metric, "exact integer metric");
            if (limit < 0) throw new IllegalArgumentException("negative nearest result limit");
            if (!metric.ownsSchema(schema)) {
                throw new IllegalArgumentException("foreign signal-image integer metric");
            }
            requireFresh(version, digest);
            Objects.checkIndex(sourceRow, rows);
            return executeWindow(0, limit, version, digest,
                    monitor, null, metric, sourceRow).rows;
        }

        /**
         * One page in ascending physical-row order, including the complete exact
         * match count. The answer owns its row storage; the caller cannot rewrite
         * the underlying computed page. Pagination is tied to the original source
         * identity and invalidation generation, NOT an unlocked live source view.
         */
        public static final class Window {
            private final int totalMatches;
            private final int start;
            private final int[] rows;

            private Window(int totalMatches, int start, int[] rows) {
                this.totalMatches = totalMatches;
                this.start = start;
                this.rows = rows;
            }
            public int totalMatches() { return totalMatches; }
            public int start() { return start; }
            public int size() { return rows.length; }
            public int[] rows() { return rows.clone(); }
        }

        /** Evaluates a bounded viewport without allocating a full result row array. */
        public Window evaluateWindow(int skip, int limit, long version, String sourceSha256) {
            return evaluateWindow(skip, limit, version, sourceSha256, null);
        }

        /**
         * Cancellable exact page evaluation; validates paging before starting a task.
         * Matching and total count are based on the same complete bit-mask result.
         * Only [skip,skip+limit) matching physical IDs are materialized.
         */
        public Window evaluateWindow(int skip, int limit, long version,
                String sourceSha256, M3ProgressMonitor monitor) {
            return executeWindow(skip, limit, version, sourceSha256, monitor);
        }

        private Window executeWindow(int skip, int limit, long version,
                String sourceSha256, M3ProgressMonitor monitor) {
            return executeWindow(skip, limit, version, sourceSha256, monitor, null);
        }

        private Window executeWindow(int skip, int limit, long version,
                String sourceSha256, M3ProgressMonitor monitor, M3SignalTopK ranking) {
            return executeWindow(skip, limit, version, sourceSha256,
                    monitor, ranking, null, -1);
        }

        private Window executeWindow(int skip, int limit, long version,
                String sourceSha256, M3ProgressMonitor monitor, M3SignalTopK ranking,
                M3SignalExactIntegerDistance metric, int sourceRow) {
            if (skip < 0 || limit < 0) {
                throw new IllegalArgumentException("negative signal query page coordinate");
            }
            requireFresh(version, sourceSha256);
            if (monitor != null) monitor.checkCanceled();
            int words = (int) (((long) rows + 63L) >>> 6);
            long totalWords = Math.multiplyExact((long) words, (long) maximumDepth);
            long heapBudget = Runtime.getRuntime().maxMemory() / 8L / Long.BYTES;
            long upperWords = Math.min(8_388_608L, heapBudget); // 64 MiB max scratch.
            if (totalWords > upperWords) {
                throw new IllegalStateException("signal Boolean workspace exceeds memory budget");
            }
            if (monitor != null) {
                monitor.beginTask("M3 Boolean signal query", codes.length);
                monitor.checkCanceled();
                requireFresh(version, sourceSha256);
            }
            long[][] stack = new long[maximumDepth][words];
            int top = 0;
            for (int pc = 0; pc < codes.length; pc++) {
                if (monitor != null) monitor.checkCanceled();
                requireFresh(version, sourceSha256);
                byte opcode = codes[pc];
                if (opcode == 0) {
                    long[] target = stack[top++];
                    // Every stack slot may be recycled by a prior binary operation.
                    Arrays.fill(target, 0L);
                    markPredicate(indexes[slots[pc]], filters[pc], target,
                            version, sourceSha256, monitor);
                } else {
                    switch (opcode) {
                        case 4 -> { // ALL
                            Arrays.fill(stack[top], -1L);
                            maskTail(stack[top]);
                            top++;
                        }
                        case 5 -> { // NONE
                            Arrays.fill(stack[top], 0L);
                            top++;
                        }
                        case 3 -> { // NOT
                            long[] target = stack[top - 1];
                            for (int word = 0; word < words; word++) {
                                if (monitor != null && (word & 1023) == 0) monitor.checkCanceled();
                                target[word] = ~target[word];
                            }
                            maskTail(target);
                        }
                        case 1, 2 -> { // AND, OR
                            long[] target = stack[top - 2], other = stack[top - 1];
                            for (int word = 0; word < words; word++) {
                                if (monitor != null && (word & 1023) == 0) monitor.checkCanceled();
                                target[word] = opcode == 1
                                        ? target[word] & other[word]
                                        : target[word] | other[word];
                            }
                            top--;
                        }
                        default -> throw new AssertionError("unknown compiled opcode");
                    }
                }
                if (monitor != null) {
                    monitor.worked(1L);
                    monitor.checkCanceled();
                }
            }
            int count = 0;
            for (int wordIndex = 0; wordIndex < words; wordIndex++) {
                if (monitor != null && (wordIndex & 1023) == 0) monitor.checkCanceled();
                count = Math.addExact(count, Long.bitCount(stack[0][wordIndex]));
            }
            int from = ranking == null && metric == null ? Math.min(skip, count) : 0;
            int selected = Math.min(limit, count - from);
            final int[] answer;
            if (metric != null) {
                // Reuse the original exact unsigned 128-bit metric heap directly
                // from the Boolean membership lane, without a selected int[].
                answer = metric.nearestByMaskWithinTask(sourceRow, stack[0],
                        selected, version, sourceSha256, monitor);
            } else if (ranking != null) {
                // Reuse original primitive Top-K heap on the bitmap; no full
                // physical-row selection array is generated for ranked results.
                answer = ranking.topKByMask(stack[0], selected);
            } else {
                // Preserve original physical-order viewport and count-only API.
                answer = new int[selected];
                int position = 0, remainingSkip = from;
                for (int i = 0; selected > 0 && i < words; i++) {
                    if (monitor != null && (i & 1023) == 0) monitor.checkCanceled();
                    long bits = stack[0][i];
                    if (remainingSkip > 0) {
                        int available = Long.bitCount(bits);
                        if (remainingSkip >= available) {
                            remainingSkip -= available;
                            continue;
                        }
                        while (remainingSkip > 0) {
                            bits &= bits - 1L;
                            remainingSkip--;
                        }
                        remainingSkip = 0;
                    }
                    while (bits != 0L && position < selected) {
                        answer[position++] = (i << 6) + Long.numberOfTrailingZeros(bits);
                        bits &= bits - 1L;
                    }
                    if (position == selected) break;
                }
            }
            if (monitor != null) monitor.checkCanceled();
            requireFresh(version, sourceSha256);
            if (monitor != null) {
                monitor.done();
                // A listener is free to invalidate the original source during done().
                monitor.checkCanceled();
                requireFresh(version, sourceSha256);
            }
            return new Window(count, from, answer);
        }

        /** Remove bits beyond the last original physical row after ALL/NOT. */
        private void maskTail(long[] bitset) {
            int tail = rows & 63;
            if (tail != 0 && bitset.length != 0) {
                bitset[bitset.length - 1] &= -1L >>> (64 - tail);
            }
        }
    }

    private void requireFresh(long version, String digest) {
        schema.requireCurrent(version, digest);
        if (epochs.epoch() != builtEpoch) {
            throw new IllegalStateException("signal query plan stale; rebuild after source change");
        }
    }
}
