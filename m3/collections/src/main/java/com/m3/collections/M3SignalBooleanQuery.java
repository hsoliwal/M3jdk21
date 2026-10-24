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
            int[] matches = rowsFor(indexes[i], predicates[i], version, digest);
            long[] target = union || i == 0 ? membership : scratch;
            if (!union && i > 0) Arrays.fill(target, 0L);
            for (int row : matches) target[row >>> 6] |= 1L << (row & 63);
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

    private static int[] rowsFor(M3SignalExactRangeIndex index, Predicate predicate,
            long version, String sha) {
        if (predicate instanceof Missing) return index.missingRows(version, sha);
        if (predicate instanceof IntBetween range) {
            return index.betweenInts(range.lower, range.upper, version, sha);
        }
        if (predicate instanceof LongBetween range) {
            return index.betweenLongs(range.lower, range.upper, version, sha);
        }
        if (predicate instanceof DoubleBetween range) {
            return index.betweenDoubles(range.lower, range.upper, version, sha);
        }
        throw new IllegalArgumentException("unsupported signal predicate");
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
            requireFresh(version, sourceSha256);
            int words = (int) (((long) rows + 63L) >>> 6);
            long totalWords = Math.multiplyExact((long) words, (long) maximumDepth);
            long heapBudget = Runtime.getRuntime().maxMemory() / 8L / Long.BYTES;
            long upperWords = Math.min(8_388_608L, heapBudget); // 64 MiB max scratch.
            if (totalWords > upperWords) {
                throw new IllegalStateException("signal Boolean workspace exceeds memory budget");
            }
            long[][] stack = new long[maximumDepth][words];
            int top = 0;
            for (int pc = 0; pc < codes.length; pc++) {
                byte opcode = codes[pc];
                if (opcode == 0) {
                    long[] target = stack[top++];
                    // Every stack slot may be recycled by a prior binary operation.
                    Arrays.fill(target, 0L);
                    int[] matching = rowsFor(indexes[slots[pc]], filters[pc], version, sourceSha256);
                    for (int row : matching) target[row >>> 6] |= 1L << (row & 63);
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
                            for (int word = 0; word < words; word++) target[word] = ~target[word];
                            maskTail(target);
                        }
                        case 1, 2 -> { // AND, OR
                            long[] target = stack[top - 2], other = stack[top - 1];
                            for (int word = 0; word < words; word++) {
                                target[word] = opcode == 1
                                        ? target[word] & other[word]
                                        : target[word] | other[word];
                            }
                            top--;
                        }
                        default -> throw new AssertionError("unknown compiled opcode");
                    }
                }
            }
            int count = 0;
            for (long word : stack[0]) count = Math.addExact(count, Long.bitCount(word));
            int[] answer = new int[count];
            int position = 0;
            for (int i = 0; i < words; i++) {
                long bits = stack[0][i];
                while (bits != 0L) {
                    answer[position++] = (i << 6) + Long.numberOfTrailingZeros(bits);
                    bits &= bits - 1;
                }
            }
            requireFresh(version, sourceSha256);
            return answer;
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
