// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reusable one-pass kernels for common cross-language collection idioms.
 * State is confined to a newly opened traversal; object references stay in
 * arrays, with primitive ordinals for stable top-K selection.
 */
final class ObjectStreamAdvancedKernels {
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
}
