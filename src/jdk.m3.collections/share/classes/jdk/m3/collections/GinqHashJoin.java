// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.collections;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Opt-in, bounded equality-join kernel for the existing replay-aware M3 ObjectStream.
 * This is a Java-keyed GINQ-inspired primitive, NOT a Groovy Queryable implementation.
 * Equality and hashing follow the Java Map contract, not Groovy's coercive ==.
 *
 * <p>Right rows are consumed, indexed and closed before left rows are opened.
 * The index retains right payloads once in M3ArrayList and uses primitive int
 * positions (IntArrayList) in M3LinkedHashMap buckets. Inner results are emitted
 * left-major, preserving source right order, including repeated keys and rows.
 * RIGHT/FULL unmatched right rows follow matched rows in original right order.
 * Every mapped row is materialized exactly once. No per-pair Tuple/Entry exists.
 * A returned list is read-only; client payloads are not deep-frozen.</p>
 *
 * <p>All source, distinct-key and output budgets are explicit. A budget failure
 * occurs before reading the first excess source element or emitting an excess
 * mapped output. Both opened streams close on callback or budget failure.
 * An input stream that cannot be opened also retains its normal one-shot rules.
 * This class does not alter Groovy MetaClass, GINQ grammar or String dispatch.</p>
 */
public final class GinqHashJoin {
    private GinqHashJoin() { }

    /** Which unmatched sides are included in the bounded result. */
    public enum Mode {
        /** Only matched pairs. */ INNER,
        /** Matched pairs and unmatched left rows. */ LEFT,
        /** Matched pairs and unmatched right rows. */ RIGHT,
        /** Matched pairs and unmatched rows from both sides. */ FULL
    }

    /**
     * Presence-aware projection; a present null value is not an absent side.
     * @param <L> left payload type
     * @param <R> right payload type
     * @param <O> projection result type
     */
    @FunctionalInterface
    public interface RowMapper<L, R, O> {
        /**
         * Project one result row, retaining a distinct presence bit for each side.
         * @param leftPresent true if a left row actually exists
         * @param left left value, possibly null even when present
         * @param rightPresent true if a right row actually exists
         * @param right right value, possibly null even when present
         * @return mapped value, which may be null
         */
        O apply(boolean leftPresent, L left, boolean rightPresent, R right);
    }

    /**
     * Convenience for an inner equijoin without allocating retained tuple rows.
     * @param <L> left value type
     * @param <R> right value type
     * @param <K> hash/equality key type
     * @param <O> result value type
     * @param left replayable or one-shot left stream plan
     * @param right replayable or one-shot right stream plan
     * @param leftKey key extractor, invoked once per admitted left row
     * @param rightKey key extractor, invoked once per admitted right row
     * @param project result projection, invoked once per emitted match
     * @param maxLeft maximum admitted left source rows
     * @param maxRight maximum admitted right source rows
     * @param maxKeys maximum distinct right index keys
     * @param maxOutput maximum emitted results
     * @return shallow-read-only materialized join result
     */
    public static <L, R, K, O> List<O> inner(
            ObjectStream<? extends L> left, ObjectStream<? extends R> right,
            Function<? super L, ? extends K> leftKey,
            Function<? super R, ? extends K> rightKey,
            BiFunction<? super L, ? super R, ? extends O> project,
            int maxLeft, int maxRight, int maxKeys, int maxOutput) {
        Objects.requireNonNull(project, "project");
        return execute(left, right, leftKey, rightKey,
                (hasLeft, l, hasRight, r) -> project.apply(l, r),
                Mode.INNER, maxLeft, maxRight, maxKeys, maxOutput);
    }

    /**
     * Bounded right-indexed equijoin with explicit outer-row presence.
     * Null keys compare equal to null keys; null payloads remain distinguishable.
     * Client key objects must obey stable equals/hashCode while indexed.
     * @param <L> left value type
     * @param <R> right value type
     * @param <K> Java hash/equality key type
     * @param <O> result value type
     * @param left replayable or one-shot left stream plan
     * @param right replayable or one-shot right stream plan
     * @param leftKey key extractor, invoked once per admitted left row
     * @param rightKey key extractor, invoked once per admitted right row
     * @param project presence-aware projection, invoked per emitted row
     * @param mode which unmatched sides to retain
     * @param maxLeft maximum admitted left source rows
     * @param maxRight maximum admitted right source rows
     * @param maxKeys maximum distinct right index keys
     * @param maxOutput maximum emitted results
     * @return shallow-read-only materialized join result
     */
    public static <L, R, K, O> List<O> execute(
            ObjectStream<? extends L> left, ObjectStream<? extends R> right,
            Function<? super L, ? extends K> leftKey,
            Function<? super R, ? extends K> rightKey,
            RowMapper<? super L, ? super R, ? extends O> project,
            Mode mode, int maxLeft, int maxRight, int maxKeys, int maxOutput) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(leftKey, "leftKey");
        Objects.requireNonNull(rightKey, "rightKey");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(mode, "mode");
        nonnegative(maxLeft, "maxLeft");
        nonnegative(maxRight, "maxRight");
        nonnegative(maxKeys, "maxKeys");
        nonnegative(maxOutput, "maxOutput");

        M3LinkedHashMap<K, IntArrayList> buckets = new M3LinkedHashMap<>();
        M3ArrayList<R> rightRows = new M3ArrayList<>();
        try (Stream<? extends R> source = right.stream()) {
            Iterator<? extends R> it = source.iterator();
            while (it.hasNext()) {
                if (rightRows.size() >= maxRight) {
                    throw new IllegalStateException("GINQ right source budget exceeded");
                }
                R value = it.next();
                K key = rightKey.apply(value);
                IntArrayList indices = buckets.get(key);
                if (indices == null) {
                    if (buckets.size() >= maxKeys) {
                        throw new IllegalStateException("GINQ right key budget exceeded");
                    }
                    indices = new IntArrayList();
                    buckets.put(key, indices);
                }
                int ordinal = rightRows.size();
                rightRows.add(value);
                indices.add(ordinal);
            }
        }

        M3ArrayList<O> output = new M3ArrayList<>();
        BitSet rightSeen = mode == Mode.RIGHT || mode == Mode.FULL ? new BitSet() : null;
        int leftRows = 0;
        try (Stream<? extends L> source = left.stream()) {
            Iterator<? extends L> it = source.iterator();
            while (it.hasNext()) {
                if (leftRows >= maxLeft) {
                    throw new IllegalStateException("GINQ left source budget exceeded");
                }
                L value = it.next();
                leftRows++;
                IntArrayList candidates = buckets.get(leftKey.apply(value));
                if (candidates != null) {
                    for (int i = 0; i < candidates.size(); i++) {
                        ensureRoom(output, maxOutput);
                        output.add(project.apply(true, value, true,
                                rightRows.get(candidates.get(i))));
                        if (rightSeen != null) rightSeen.set(candidates.get(i));
                    }
                } else if (mode == Mode.LEFT || mode == Mode.FULL) {
                    ensureRoom(output, maxOutput);
                    output.add(project.apply(true, value, false, null));
                }
            }
        }
        if (rightSeen != null) {
            for (int i = 0; i < rightRows.size(); i++) {
                if (!rightSeen.get(i)) {
                    ensureRoom(output, maxOutput);
                    output.add(project.apply(false, null, true, rightRows.get(i)));
                }
            }
        }
        return Collections.unmodifiableList(output);
    }

    private static void ensureRoom(M3ArrayList<?> output, int maximum) {
        if (output.size() >= maximum) {
            throw new IllegalStateException("GINQ join output budget exceeded");
        }
    }

    private static void nonnegative(int size, String label) {
        if (size < 0) throw new IllegalArgumentException(label + " must be nonnegative");
    }
}
