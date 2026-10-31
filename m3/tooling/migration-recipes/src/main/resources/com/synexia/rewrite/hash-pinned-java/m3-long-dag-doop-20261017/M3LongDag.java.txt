// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Immutable, stack-safe composition plan over parent-owned primitive-long collections.
 *
 * <p>DOOP: source elements and their lifetimes belong to the backing collection. A plan
 * holds only DAG edges, immutable operation descriptors and client functions. It does
 * not retain a result array or one facade per element. Each terminal opens a fresh
 * traversal, so live changes made before an evaluation remain visible according to
 * the backing owner's iterator contract. Client predicates/mappers must not mutate
 * a nonconcurrent source during traversal. It is not a distributed execution graph.</p>
 *
 * <p>A sorted stage is an explicit bounded snapshot; ordinary filter/map/skip/limit
 * and concatenation are pull-based. Stream instances returned by {@link #stream()}
 * must be closed by the caller; the provided terminal operators always close them.</p>
 */
public final class M3LongDag {
    private static final int INITIAL_SCRATCH = 16;
    private final Node node;

    private M3LongDag(Node node) { this.node = node; }

    /** New plan over the canonical primitive owner, not a cloned collection. */
    public static M3LongDag from(M3LongCollection owner) {
        return new M3LongDag(new Source(Objects.requireNonNull(owner, "owner")));
    }

    /** Reuse Synexia's existing late-bound, read-only primitive filtered view. */
    public static M3LongDag filtered(M3LongCollection owner, LongPredicate predicate) {
        return from(new M3LongFilteredView(owner, predicate));
    }

    /** Reuse the existing sorted-set live filter; do not copy or replace its owner. */
    public static M3LongDag sortedFiltered(M3LongSortedSet owner, LongPredicate predicate) {
        return from(M3LongSortedViews.filter(owner, predicate));
    }

    public M3LongDag filter(LongPredicate predicate) {
        return stage(new Filter(Objects.requireNonNull(predicate, "predicate")));
    }

    public M3LongDag map(LongUnaryOperator mapper) {
        return stage(new MapValue(Objects.requireNonNull(mapper, "mapper")));
    }

    public M3LongDag skip(long count) {
        if (count < 0) throw new IllegalArgumentException("negative skip");
        return stage(new Skip(count));
    }

    public M3LongDag limit(long count) {
        if (count < 0) throw new IllegalArgumentException("negative limit");
        return stage(new Limit(count));
    }

    /** Bounded stable first-occurrence deduplication with an existing primitive M3 hash set. */
    public M3LongDag distinctBounded(int maxDistinct) {
        if (maxDistinct < 0) throw new IllegalArgumentException("negative distinct budget");
        return stage(new Distinct(maxDistinct));
    }

    /** Materializes at most maxElements in signed order, refusing the next element. */
    public M3LongDag sortedBounded(int maxElements) {
        if (maxElements < 0) throw new IllegalArgumentException("negative sort budget");
        return stage(new Sort(maxElements));
    }

    /** Combine two DAGs without retaining either input's payload array. */
    public M3LongDag concat(M3LongDag other) {
        return new M3LongDag(new Concat(node, Objects.requireNonNull(other, "other").node));
    }

    private M3LongDag stage(Stage stage) { return new M3LongDag(new Unary(node, stage)); }

    /** Number of distinct logical plan nodes, counting common subexpressions once. */
    public int nodeCount() {
        IdentityHashMap<Node, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Node> pending = new ArrayDeque<>();
        pending.push(node);
        while (!pending.isEmpty()) {
            Node next = pending.pop();
            if (seen.put(next, Boolean.TRUE) != null) continue;
            if (next instanceof Unary u) pending.push(u.parent());
            else if (next instanceof Concat c) {
                pending.push(c.left());
                pending.push(c.right());
            }
        }
        return seen.size();
    }

    /** Fresh primitive stream for each invocation; caller must close this stream. */
    public LongStream stream() {
        ArrayDeque<Frame> traversal = new ArrayDeque<>();
        ArrayDeque<LongStream> products = new ArrayDeque<>();
        traversal.push(new Frame(node, false));
        try {
            while (!traversal.isEmpty()) {
                Frame frame = traversal.pop();
                Node current = frame.node();
                if (current instanceof Source source) {
                    products.push(source.owner().longStream().sequential());
                } else if (!frame.completed()) {
                    traversal.push(new Frame(current, true));
                    if (current instanceof Unary unary) {
                        traversal.push(new Frame(unary.parent(), false));
                    } else if (current instanceof Concat concat) {
                        traversal.push(new Frame(concat.right(), false));
                        traversal.push(new Frame(concat.left(), false));
                    }
                } else if (current instanceof Unary unary) {
                    LongStream upstream = products.pop();
                    try { products.push(unary.stage().apply(upstream)); }
                    catch (RuntimeException | Error error) { upstream.close(); throw error; }
                } else if (current instanceof Concat) {
                    LongStream right = products.pop();
                    LongStream left = products.pop();
                    try { products.push(LongStream.concat(left, right)); }
                    catch (RuntimeException | Error error) {
                        try { left.close(); } finally { right.close(); }
                        throw error;
                    }
                }
            }
            if (products.size() != 1) throw new IllegalStateException("invalid plan DAG");
            return products.pop();
        } catch (RuntimeException | Error error) {
            for (LongStream product : products) {
                try { product.close(); } catch (RuntimeException close) { error.addSuppressed(close); }
            }
            throw error;
        }
    }

    /** Primitive terminal with stream closing even if the client callback fails. */
    public void forEachOrdered(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        try (LongStream values = stream()) { values.forEachOrdered(consumer); }
    }

    /** An exact source traversal; no SIZED shortcut can skip client operations. */
    public long count() {
        try (LongStream input = stream()) {
            long[] count = new long[1];
            input.forEachOrdered(value -> count[0] = Math.addExact(count[0], 1));
            return count[0];
        }
    }

    /** Bounded result capture; does not change or replace parent-owned storage. */
    public long[] toArrayBounded(int maxElements) {
        if (maxElements < 0) throw new IllegalArgumentException("negative element budget");
        try (LongStream input = stream()) { return collectArray(input, maxElements); }
    }

    /** Batched, ordered and bounded primitive chunks. Close when finished or abandoned. */
    public Stream<long[]> batches(int width) {
        if (width <= 0 || width > 65_536) throw new IllegalArgumentException("batch width must be 1..65536");
        LongStream input = stream();
        final PrimitiveIterator.OfLong cursor;
        try { cursor = input.iterator(); }
        catch (RuntimeException | Error failure) { input.close(); throw failure; }
        Spliterator<long[]> chunks = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE,
                Spliterator.ORDERED | Spliterator.NONNULL) {
            @Override public boolean tryAdvance(java.util.function.Consumer<? super long[]> action) {
                Objects.requireNonNull(action, "action");
                if (!cursor.hasNext()) return false;
                long[] next = new long[width];
                int count = 0;
                while (count < width && cursor.hasNext()) next[count++] = cursor.nextLong();
                action.accept(count == width ? next : Arrays.copyOf(next, count));
                return true;
            }
        };
        return StreamSupport.stream(chunks, false).onClose(input::close);
    }

    public <A> M3VirtualAccumulator<A> virtualAccumulator(
            Supplier<? extends A> seed, ObjLongConsumer<? super A> fold) {
        return new M3VirtualAccumulator<>(this, seed, fold);
    }

    public <A, R> M3VirtualCollector<A, R> virtualCollector(
            Supplier<? extends A> seed, ObjLongConsumer<? super A> fold,
            Function<? super A, ? extends R> finish) {
        return new M3VirtualCollector<>(this, seed, fold, finish);
    }

    /** Explicit JDK collector bridge: boxes values only at this chosen boundary. */
    public <A, R> R collect(Collector<Long, A, R> collector) {
        Objects.requireNonNull(collector, "collector");
        try (LongStream input = stream()) { return input.boxed().collect(collector); }
    }

    private static long[] collectArray(LongStream source, int limit) {
        long[] output = new long[Math.min(INITIAL_SCRATCH, limit)];
        int size = 0;
        PrimitiveIterator.OfLong input = source.iterator();
        while (input.hasNext()) {
            if (size == limit) throw new IllegalStateException("row materialization budget exceeded");
            if (size == output.length) {
                int grown = Math.min(limit, Math.max(size + 1, output.length + (output.length >>> 1) + 1));
                output = Arrays.copyOf(output, grown);
            }
            output[size++] = input.nextLong();
        }
        return Arrays.copyOf(output, size);
    }

    private sealed interface Node permits Source, Unary, Concat { }
    private record Source(M3LongCollection owner) implements Node { }
    private record Unary(Node parent, Stage stage) implements Node { }
    private record Concat(Node left, Node right) implements Node { }
    private record Frame(Node node, boolean completed) { }

    private sealed interface Stage permits Filter, MapValue, Skip, Limit, Distinct, Sort {
        LongStream apply(LongStream source);
    }
    private record Filter(LongPredicate predicate) implements Stage {
        public LongStream apply(LongStream source) { return source.filter(predicate); }
    }
    private record MapValue(LongUnaryOperator mapper) implements Stage {
        public LongStream apply(LongStream source) { return source.map(mapper); }
    }
    private record Skip(long count) implements Stage {
        public LongStream apply(LongStream source) { return source.skip(count); }
    }
    private record Limit(long count) implements Stage {
        public LongStream apply(LongStream source) { return source.limit(count); }
    }
    private record Distinct(int maxKeys) implements Stage {
        public LongStream apply(LongStream source) {
            M3LongHashSet keys = new M3LongHashSet();
            return source.filter(value -> {
                if (keys.contains(value)) return false;
                if (keys.size() == maxKeys) throw new IllegalStateException("distinct key budget exceeded");
                keys.add(value);
                return true;
            });
        }
    }
    private record Sort(int budget) implements Stage {
        public LongStream apply(LongStream source) {
            final long[] values;
            try (source) { values = collectArray(source, budget); }
            Arrays.sort(values);
            return Arrays.stream(values);
        }
    }
}
