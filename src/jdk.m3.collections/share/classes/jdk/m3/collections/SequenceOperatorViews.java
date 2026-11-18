// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Opt-in ordered-sequence composition without retained per-element copies.
 *
 * <p>The authoritative values remain in the original lists. One reference to
 * each contributing segment is retained; flattening an existing descriptor
 * copies only segment references, never the elements or their Java objects.
 * Backing mutations are visible on subsequent operations, while iterators
 * delegate source fail-fast behavior rather than inventing new guarantees.
 */
public final class SequenceOperatorViews {
    private SequenceOperatorViews() { }

    /** Java 21 default-method semantic owner for explicit sequence addition. */
    @FunctionalInterface
    public interface SequenceOperator<E> {
        List<E> source();

        default List<E> plus(List<? extends E> right) {
            return SequenceOperatorViews.concat(source(), right);
        }
    }

    /** Explicit adapter: normal List implementations acquire no new methods. */
    public static <E> SequenceOperator<E> asSequenceOperator(List<E> source) {
        Objects.requireNonNull(source, "source");
        return () -> source;
    }

    /**
     * Ordered, live and unmodifiable composition. Duplicate and null elements
     * retain their positions. Only two source references are needed initially;
     * nested compositions are flattened at the descriptor level.
     */
    public static <E> List<E> concat(List<? extends E> left, List<? extends E> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        int a = segments(left);
        int b = segments(right);
        @SuppressWarnings("unchecked")
        List<? extends E>[] inputs = (List<? extends E>[]) new List<?>[Math.addExact(a, b)];
        int offset = appendSegments(inputs, 0, left);
        appendSegments(inputs, offset, right);
        boolean random = true;
        for (List<? extends E> input : inputs) random &= input instanceof RandomAccess;
        return random ? new RandomConcatenation<>(inputs) : new Concatenation<>(inputs);
    }

    private static int segments(List<?> source) {
        return source instanceof Concatenation<?> composed ? composed.inputs.length : 1;
    }

    private static <E> int appendSegments(List<? extends E>[] output, int offset,
                                          List<? extends E> source) {
        if (source instanceof Concatenation<?> composed) {
            for (List<?> item : composed.inputs) {
                @SuppressWarnings("unchecked") List<? extends E> typed = (List<? extends E>) item;
                output[offset++] = typed;
            }
        } else {
            output[offset++] = source;
        }
        return offset;
    }

    private static class Concatenation<E> extends AbstractList<E> {
        final List<? extends E>[] inputs;

        Concatenation(List<? extends E>[] inputs) { this.inputs = inputs; }

        @Override public int size() {
            int total = 0;
            for (List<? extends E> input : inputs) total = Math.addExact(total, input.size());
            return total;
        }

        @Override public E get(int index) {
            if (index < 0) throw new IndexOutOfBoundsException(index);
            int remaining = index;
            for (List<? extends E> input : inputs) {
                int length = input.size();
                if (remaining < length) return input.get(remaining);
                remaining -= length;
            }
            throw new IndexOutOfBoundsException(index);
        }

        /** No iterator or element materialization occurs until first touch. */
        @Override public Iterator<E> iterator() {
            return new Iterator<>() {
                private int segment;
                private Iterator<? extends E> current;

                @Override public boolean hasNext() {
                    while (current == null || !current.hasNext()) {
                        if (segment >= inputs.length) return false;
                        current = inputs[segment++].iterator();
                    }
                    return true;
                }

                @Override public E next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return current.next();
                }
            };
        }

        /** Late-bind each source iterator when the spliterator is first used. */
        @Override public Spliterator<E> spliterator() {
            return new Spliterator<>() {
                private Iterator<E> cursor;

                @Override public boolean tryAdvance(Consumer<? super E> action) {
                    Objects.requireNonNull(action, "action");
                    if (cursor == null) cursor = Concatenation.this.iterator();
                    if (!cursor.hasNext()) return false;
                    action.accept(cursor.next());
                    return true;
                }

                @Override public Spliterator<E> trySplit() { return null; }
                @Override public long estimateSize() { return Long.MAX_VALUE; }
                @Override public int characteristics() { return ORDERED; }
            };
        }

        @Override public E set(int index, E element) { throw readonly(); }
        @Override public void add(int index, E element) { throw readonly(); }
        @Override public E remove(int index) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
        @Override public boolean addAll(java.util.Collection<? extends E> elements) {
            throw readonly();
        }
        @Override public boolean addAll(int index, java.util.Collection<? extends E> elements) {
            throw readonly();
        }
        @Override public boolean remove(Object element) { throw readonly(); }
        @Override public boolean removeAll(java.util.Collection<?> elements) { throw readonly(); }
        @Override public boolean retainAll(java.util.Collection<?> elements) { throw readonly(); }
        @Override public boolean removeIf(Predicate<? super E> condition) { throw readonly(); }
        @Override public void replaceAll(UnaryOperator<E> replacement) { throw readonly(); }
        @Override public void sort(java.util.Comparator<? super E> comparator) { throw readonly(); }

        private UnsupportedOperationException readonly() {
            return new UnsupportedOperationException("read-only sequence composition");
        }
    }

    /** RandomAccess iff every leaf source has that marker. */
    private static final class RandomConcatenation<E>
            extends Concatenation<E> implements RandomAccess {
        RandomConcatenation(List<? extends E>[] inputs) { super(inputs); }
    }
}
