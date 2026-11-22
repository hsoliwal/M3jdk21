// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.m3.collections.FrozenMap;

/**
 * Reusable one-pass kernels for common cross-language collection idioms.
 * State is confined to a newly opened traversal; object references stay in
 * arrays, with primitive ordinals for stable top-K selection.
 * Bounded keyed compositions share the existing primitive ordinal directory
 * and stable heap; grouped occurrence order is represented by CSR int lanes.
 */
final class ObjectStreamAdvancedKernels {
    static final int INNER_JOIN = 0;
    static final int LEFT_JOIN = 1;
    static final int SEMI_JOIN = 2;
    static final int ANTI_JOIN = 3;

    private ObjectStreamAdvancedKernels() { }

    private static <R> Stream<R> wrap(Stream<?> first, Stream<?> second,
            Spliterator<R> output) {
        return StreamSupport.stream(output, false)
                .onClose(first::close).onClose(second::close);
    }

    static <L, R, O> Stream<O> zipLongest(Stream<? extends L> left,
            Stream<? extends R> right,
            ObjectStream.ZipLongestMapper<? super L, ? super R, ? extends O> mapper) {
        Iterator<? extends L> a = left.iterator();
        Iterator<? extends R> b = right.iterator();
        Spliterator<O> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override public boolean tryAdvance(Consumer<? super O> action) {
                Objects.requireNonNull(action, "action");
                boolean hasLeft = a.hasNext();
                boolean hasRight = b.hasNext();
                if (!hasLeft && !hasRight) return false;
                L leftValue = hasLeft ? a.next() : null;
                R rightValue = hasRight ? b.next() : null;
                action.accept(mapper.apply(hasLeft, leftValue, hasRight, rightValue));
                return true;
            }
        };
        return wrap(left, right, output);
    }

    static <T> Stream<T> interleave(Stream<? extends T> left,
            Stream<? extends T> right) {
        Iterator<? extends T> a = left.iterator();
        Iterator<? extends T> b = right.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean takeLeft = true;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (takeLeft) {
                    if (a.hasNext()) {
                        takeLeft = false;
                        action.accept(a.next());
                        return true;
                    }
                    if (!b.hasNext()) return false;
                    action.accept(b.next());
                    return true;
                }
                if (b.hasNext()) {
                    takeLeft = true;
                    action.accept(b.next());
                    return true;
                }
                if (!a.hasNext()) return false;
                action.accept(a.next());
                return true;
            }
        };
        return wrap(left, right, output);
    }

    /** Java 24 Gatherers.windowSliding semantics without requiring JDK 24. */
    static <T> Stream<List<T>> windowSlidingPartial(Stream<T> input, int width) {
        Iterator<T> source = input.iterator();
        Spliterator<List<T>> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            private final Object[] ring = new Object[width];
            private boolean initialized;
            private boolean completed;
            private int count;
            private int head;

            @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
                Objects.requireNonNull(action, "action");
                if (completed) return false;
                if (!initialized) {
                    initialized = true;
                    while (count < width && source.hasNext()) ring[count++] = source.next();
                    if (count == 0) { completed = true; return false; }
                    if (count < width) completed = true;
                } else {
                    if (!source.hasNext()) { completed = true; return false; }
                    ring[head] = source.next();
                    head = (head + 1) % width;
                }
                ArrayList<T> values = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    @SuppressWarnings("unchecked") T value = (T) ring[(head + i) % width];
                    values.add(value);
                }
                action.accept(Collections.unmodifiableList(values));
                return true;
            }
        };
        return StreamSupport.stream(output, false).onClose(input::close);
    }

    /** Inclusive take-until: emit the first matching element, then stop. */
    static <T> Stream<T> takeUntilInclusive(Stream<T> input,
            Predicate<? super T> boundary) {
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean stopped;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (stopped || !source.hasNext()) return false;
                T value = source.next();
                stopped = boundary.test(value);
                action.accept(value);
                return true;
            }
        };
        return StreamSupport.stream(output, false).onClose(input::close);
    }

    /**
     * Larger comparator value is better. Equal values retain earlier source
     * occurrences; the worst candidate is the root of a primitive-ordinal heap.
     * Total cost O(n log k + k log k) with O(k) references and ordinals.
     */
    static <T> Stream<T> topK(Stream<T> input, int k,
            Comparator<? super T> comparator) {
        if (k == 0) {
            input.close();
            return Stream.empty();
        }
        Object[] values = new Object[k];
        long[] ordinals = new long[k];
        int size = 0;
        long ordinal = 0L;
        try (input) {
            Iterator<T> source = input.iterator();
            while (source.hasNext()) {
                T candidate = source.next();
                long candidateOrdinal = ordinal;
                ordinal = Math.addExact(ordinal, 1L);
                if (size < k) {
                    values[size] = candidate;
                    ordinals[size] = candidateOrdinal;
                    siftUp(values, ordinals, size, comparator);
                    size++;
                } else if (quality(candidate, candidateOrdinal,
                        values[0], ordinals[0], comparator) > 0) {
                    values[0] = candidate;
                    ordinals[0] = candidateOrdinal;
                    siftDown(values, ordinals, 0, size, comparator);
                }
            }
        }
        int selected = size;
        Object[] descending = new Object[selected];
        while (size > 0) {
            descending[--size] = values[0];
            values[0] = values[size];
            ordinals[0] = ordinals[size];
            if (size > 0) siftDown(values, ordinals, 0, size, comparator);
        }
        ArrayList<T> result = new ArrayList<>(selected);
        for (Object value : descending) {
            @SuppressWarnings("unchecked") T typed = (T) value;
            result.add(typed);
        }
        return Collections.unmodifiableList(result).stream();
    }

    private static <T> void siftUp(Object[] values, long[] ordinals,
            int position, Comparator<? super T> comparator) {
        while (position > 0) {
            int parent = (position - 1) >>> 1;
            if (qualityAt(values, ordinals, position, parent, comparator) >= 0) break;
            swap(values, ordinals, parent, position);
            position = parent;
        }
    }

    private static <T> void siftDown(Object[] values, long[] ordinals,
            int position, int size, Comparator<? super T> comparator) {
        while (true) {
            int left = (position << 1) + 1;
            if (left >= size) return;
            int right = left + 1;
            int worst = right < size && qualityAt(values, ordinals,
                    right, left, comparator) < 0 ? right : left;
            if (qualityAt(values, ordinals, position, worst, comparator) <= 0) return;
            swap(values, ordinals, position, worst);
            position = worst;
        }
    }

    private static <T> int qualityAt(Object[] values, long[] ordinals,
            int left, int right, Comparator<? super T> comparator) {
        return quality(values[left], ordinals[left], values[right], ordinals[right], comparator);
    }

    private static <T> int quality(Object left, long leftOrdinal,
            Object right, long rightOrdinal, Comparator<? super T> comparator) {
        @SuppressWarnings("unchecked") T a = (T) left;
        @SuppressWarnings("unchecked") T b = (T) right;
        int compared = comparator.compare(a, b);
        return compared != 0 ? compared : Long.compare(rightOrdinal, leftOrdinal);
    }

    private static void swap(Object[] values, long[] ordinals, int left, int right) {
        Object object = values[left];
        values[left] = values[right];
        values[right] = object;
        long ordinal = ordinals[left];
        ordinals[left] = ordinals[right];
        ordinals[right] = ordinal;
    }

    /**
     * A bounded source snapshot and its stable per-key occurrence projection.
     * There is one instance per input, not one holder per key or occurrence.
     */
    private static final class KeyedRows<T, K> {
        final Object[] values;
        final Object[] keys;
        final int[] keyOrdinals;
        final int[] offsets;
        final int[] rows;
        final ObjectIntHashMap<K> directory;

        KeyedRows(Object[] values, Object[] keys, int[] keyOrdinals,
                int[] offsets, int[] rows, ObjectIntHashMap<K> directory) {
            this.values = values;
            this.keys = keys;
            this.keyOrdinals = keyOrdinals;
            this.offsets = offsets;
            this.rows = rows;
            this.directory = directory;
        }

        @SuppressWarnings("unchecked")
        T valueAt(int row) { return (T) values[row]; }

        @SuppressWarnings("unchecked")
        K keyAt(int ordinal) { return (K) keys[ordinal]; }
    }

    /** Capture once, then stable-scatter source ordinals into contiguous key ranges. */
    private static <T, K> KeyedRows<T, K> keyedRows(Stream<? extends T> input,
            Function<? super T, ? extends K> keyExtractor,
            int maxElements, int maxKeys) {
        Object[] values = new Object[0];
        Object[] keys = new Object[0];
        int[] keyOrdinals = new int[0];
        int[] counts = new int[0];
        ObjectIntHashMap<K> directory = new ObjectIntHashMap<>(0);
        int size = 0;
        int keyCount = 0;
        Iterator<? extends T> source = input.iterator();
        while (source.hasNext()) {
            if (size == maxElements) throw new IllegalStateException("input exceeds maxElements");
            T value = source.next();
            K key = keyExtractor.apply(value);
            int ordinal = directory.getOrDefault(key, -1);
            if (ordinal < 0) {
                if (keyCount == maxKeys) throw new IllegalStateException("keys exceed maxKeys");
                if (keyCount == keys.length) {
                    int capacity = grownCapacity(keys.length, keyCount + 1, maxKeys);
                    Object[] grownKeys = Arrays.copyOf(keys, capacity);
                    int[] grownCounts = Arrays.copyOf(counts, capacity);
                    keys = grownKeys;
                    counts = grownCounts;
                }
                ordinal = keyCount;
                directory.put(key, ordinal);
                keys[keyCount++] = key;
            }
            if (size == values.length) {
                int capacity = grownCapacity(values.length, size + 1, maxElements);
                Object[] grownValues = Arrays.copyOf(values, capacity);
                int[] grownOrdinals = Arrays.copyOf(keyOrdinals, capacity);
                values = grownValues;
                keyOrdinals = grownOrdinals;
            }
            values[size] = value;
            keyOrdinals[size] = ordinal;
            counts[ordinal]++;
            size++;
        }
        int[] offsets = new int[Math.addExact(keyCount, 1)];
        for (int key = 0; key < keyCount; key++) {
            offsets[key + 1] = Math.addExact(offsets[key], counts[key]);
        }
        int[] rows = new int[size];
        int[] positions = Arrays.copyOf(offsets, keyCount);
        for (int row = 0; row < size; row++) rows[positions[keyOrdinals[row]]++] = row;
        return new KeyedRows<>(Arrays.copyOf(values, size), Arrays.copyOf(keys, keyCount),
                Arrays.copyOf(keyOrdinals, size), offsets, rows, directory);
    }

    private static int grownCapacity(int current, int needed, int maximum) {
        long grown = current == 0 ? 8L : (long) current + (current >>> 1) + 1;
        return (int) Math.min(maximum, Math.max(needed, grown));
    }

    /**
     * Defer preparation until demand, close inputs before projection, and keep
     * traversal unsplittable even if a caller requests a parallel stream.
     */
    private static <R> Stream<R> deferred(Stream<?> first, Stream<?> second,
            Supplier<Stream<R>> prepare) {
        final class Deferred extends Spliterators.AbstractSpliterator<R> {
            private Stream<R> evaluated;
            private Spliterator<R> output;
            private boolean initialized;
            private boolean inputsClosed;
            private boolean closed;

            Deferred() { super(Long.MAX_VALUE, Spliterator.ORDERED); }

            private void initialize() {
                initialized = true;
                try {
                    try (first; second) {
                        evaluated = Objects.requireNonNull(prepare.get(), "prepared stream");
                    } finally {
                        inputsClosed = true;
                    }
                    output = evaluated.spliterator();
                } catch (RuntimeException | Error failure) {
                    closed = true;
                    Stream<R> failed = evaluated;
                    evaluated = null;
                    output = null;
                    if (failed != null) {
                        try { failed.close(); }
                        catch (RuntimeException | Error closing) {
                            if (closing != failure) failure.addSuppressed(closing);
                        }
                    }
                    throw failure;
                }
            }

            @Override public boolean tryAdvance(Consumer<? super R> action) {
                Objects.requireNonNull(action, "action");
                if (closed) return false;
                if (!initialized) initialize();
                return output.tryAdvance(action);
            }

            @Override public Spliterator<R> trySplit() { return null; }

            void close() {
                if (closed) return;
                closed = true;
                Stream<R> result = evaluated;
                evaluated = null;
                output = null;
                if (inputsClosed) {
                    if (result != null) result.close();
                } else {
                    inputsClosed = true;
                    try (first; second; result) { /* all owned traversals */ }
                }
            }
        }
        Deferred output = new Deferred();
        return StreamSupport.stream(output, false).onClose(output::close);
    }

    /** Equality lookup and complete multiplicity preflight precede projection. */
    static <L, R, K, O> Stream<O> joinByKey(
            Stream<? extends L> left, Stream<? extends R> right,
            Function<? super L, ? extends K> leftKey,
            Function<? super R, ? extends K> rightKey,
            ObjectStream.LeftJoinMapper<? super L, ? super R, ? extends O> mapper,
            int kind, int maxLeftElements, int maxRightElements,
            int maxLeftKeys, int maxRightKeys, int maxResults) {
        return deferred(left, right, () -> {
            KeyedRows<L, K> a = keyedRows(left, leftKey, maxLeftElements, maxLeftKeys);
            KeyedRows<R, K> b = keyedRows(right, rightKey, maxRightElements, maxRightKeys);
            int[] matching = new int[a.keys.length];
            long cardinality = 0;
            for (int key = 0; key < a.keys.length; key++) {
                int match = b.directory.getOrDefault(a.keyAt(key), -1);
                matching[key] = match;
                int leftCount = a.offsets[key + 1] - a.offsets[key];
                int rightCount = match < 0 ? 0 : b.offsets[match + 1] - b.offsets[match];
                long added = switch (kind) {
                    case INNER_JOIN -> Math.multiplyExact((long) leftCount, rightCount);
                    case LEFT_JOIN -> Math.multiplyExact((long) leftCount, Math.max(1, rightCount));
                    case SEMI_JOIN -> rightCount == 0 ? 0 : leftCount;
                    case ANTI_JOIN -> rightCount == 0 ? leftCount : 0;
                    default -> throw new AssertionError("unknown join kind");
                };
                cardinality = Math.addExact(cardinality, added);
                if (cardinality > maxResults)
                    throw new IllegalStateException("join exceeds maxResults");
            }
            Spliterator<O> output = new Spliterators.AbstractSpliterator<>(
                    cardinality, Spliterator.ORDERED) {
                private int nextLeft;
                private int activeLeft = -1;
                private int nextRight;
                private int rightEnd;

                @Override public boolean tryAdvance(Consumer<? super O> action) {
                    Objects.requireNonNull(action, "action");
                    while (true) {
                        if (activeLeft >= 0 && nextRight < rightEnd) {
                            L value = a.valueAt(activeLeft);
                            R paired = b.valueAt(b.rows[nextRight++]);
                            if (nextRight == rightEnd) activeLeft = -1;
                            action.accept(mapper.apply(value, true, paired));
                            return true;
                        }
                        if (nextLeft == a.values.length) return false;
                        int row = nextLeft++;
                        int match = matching[a.keyOrdinals[row]];
                        if (kind == SEMI_JOIN) {
                            if (match < 0) continue;
                            action.accept(mapper.apply(a.valueAt(row), true, null));
                            return true;
                        }
                        if (kind == ANTI_JOIN) {
                            if (match >= 0) continue;
                            action.accept(mapper.apply(a.valueAt(row), false, null));
                            return true;
                        }
                        if (match < 0) {
                            if (kind == INNER_JOIN) continue;
                            action.accept(mapper.apply(a.valueAt(row), false, null));
                            return true;
                        }
                        activeLeft = row;
                        nextRight = b.offsets[match];
                        rightEnd = b.offsets[match + 1];
                    }
                }

                @Override public Spliterator<O> trySplit() { return null; }
            };
            return StreamSupport.stream(output, false);
        });
    }

    private static <T> List<T> window(KeyedRows<T, ?> source, int from, int to) {
        ArrayList<T> values = new ArrayList<>(to - from);
        for (int at = from; at < to; at++) values.add(source.valueAt(source.rows[at]));
        return Collections.unmodifiableList(values);
    }

    /** Count chunks are grouped results, independently of centered mapping windows. */
    static <T, K> Map<K, List<List<T>>> countWindowsByKey(Stream<T> input,
            Function<? super T, ? extends K> keyExtractor, int width,
            ObjectStream.TailPolicy tailPolicy, int maxElements, int maxKeys) {
        KeyedRows<T, K> source;
        try (input) { source = keyedRows(input, keyExtractor, maxElements, maxKeys); }
        Object[] results = new Object[source.keys.length];
        for (int key = 0; key < source.keys.length; key++) {
            int end = source.offsets[key + 1];
            ArrayList<List<T>> chunks = new ArrayList<>();
            for (int at = source.offsets[key]; at < end;) {
                int length = Math.min(width, end - at);
                if (length < width && tailPolicy == ObjectStream.TailPolicy.DROP) break;
                chunks.add(window(source, at, at + length));
                at += length;
            }
            results[key] = Collections.unmodifiableList(chunks);
        }
        return FrozenMap.<K, List<List<T>>>copyOfNullableLanes(
                source.keys, results, source.keys.length).asMap();
    }

    /** Invert the stable CSR positions once so results follow original source order. */
    static <T, K, R> Stream<R> mapWindowsByKey(Stream<T> input,
            Function<? super T, ? extends K> keyExtractor, int before, int after,
            BiFunction<? super T, ? super List<T>, ? extends R> mapper,
            int maxElements, int maxKeys) {
        return deferred(input, null, () -> {
            KeyedRows<T, K> source = keyedRows(input, keyExtractor, maxElements, maxKeys);
            int[] positions = new int[source.values.length];
            for (int at = 0; at < source.rows.length; at++) positions[source.rows[at]] = at;
            Spliterator<R> output = new Spliterators.AbstractSpliterator<>(
                    source.values.length, Spliterator.ORDERED) {
                private int next;
                @Override public boolean tryAdvance(Consumer<? super R> action) {
                    Objects.requireNonNull(action, "action");
                    if (next == source.values.length) return false;
                    int row = next++;
                    int key = source.keyOrdinals[row];
                    int center = positions[row];
                    int from = Math.max(source.offsets[key], center - before);
                    int to = center + Math.min(after + 1, source.offsets[key + 1] - center);
                    List<T> values = window(source, from, to);
                    action.accept(mapper.apply(source.valueAt(row), values));
                    return true;
                }
                @Override public Spliterator<R> trySplit() { return null; }
            };
            return StreamSupport.stream(output, false);
        });
    }

    /** Reuse the unchanged stable heap on each bounded CSR group; never sort all input. */
    static <T, K> Map<K, List<T>> topKByKey(Stream<T> input,
            Function<? super T, ? extends K> keyExtractor, int k,
            Comparator<? super T> comparator, int maxElements, int maxKeys) {
        KeyedRows<T, K> source;
        try (input) { source = keyedRows(input, keyExtractor, maxElements, maxKeys); }
        Object[] results = new Object[source.keys.length];
        for (int key = 0; key < source.keys.length; key++) {
            int begin = source.offsets[key];
            int end = source.offsets[key + 1];
            Stream<T> group = IntStream.range(begin, end)
                    .mapToObj(at -> source.valueAt(source.rows[at]));
            try (Stream<T> selected = topK(group, Math.min(k, end - begin), comparator)) {
                results[key] = selected.toList();
            }
        }
        return FrozenMap.<K, List<T>>copyOfNullableLanes(
                source.keys, results, source.keys.length).asMap();
    }
}
