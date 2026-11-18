// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Explicit, Java-equality set algebra; no existing {@link Set} is modified.
 *
 * <p>Unlike sequence concatenation, set algebra deduplicates using
 * {@link Object#equals(Object)} and {@link Object#hashCode()}. Callers must
 * explicitly select this equality policy: neither identity-set equivalence
 * nor a sorted-set comparator is inferred or silently inherited. Each result
 * retains only the two source sets and a mode. Every read builds a temporary
 * reference-column and primitive membership masks; it never retains a second
 * per-element entry store. Reads see later source changes, while an iterator
 * visits the equality-projected snapshot captured when that iterator is made.
 * Sources mutated concurrently during projection have their ordinary source
 * behavior; no atomic cross-set snapshot is promised.
 */
public final class SetOperatorViews {
    private SetOperatorViews() { }

    /** Named Java 21 default-method capability, deliberately separate from lists. */
    @FunctionalInterface
    public interface SetOperator<E> {
        Set<E> source();

        default Set<E> or(Set<? extends E> right) {
            return SetOperatorViews.unionByEquals(source(), right);
        }

        default Set<E> and(Set<? extends E> right) {
            return SetOperatorViews.intersectionByEquals(source(), right);
        }

        default Set<E> minus(Set<? extends E> right) {
            return SetOperatorViews.differenceByEquals(source(), right);
        }

        default Set<E> xor(Set<? extends E> right) {
            return SetOperatorViews.symmetricDifferenceByEquals(source(), right);
        }
    }

    /** Explicit adapter: no method is added to java.util.Set or a source class. */
    public static <E> SetOperator<E> asEqualsOperator(Set<E> source) {
        Objects.requireNonNull(source, "source");
        return () -> source;
    }

    /** Live, read-only union under Java equals/hashCode equality. */
    public static <E> Set<E> unionByEquals(Set<? extends E> left,
                                           Set<? extends E> right) {
        return new AlgebraView<>(left, right, Mode.UNION);
    }

    /** Live, read-only intersection under Java equals/hashCode equality. */
    public static <E> Set<E> intersectionByEquals(Set<? extends E> left,
                                                  Set<? extends E> right) {
        return new AlgebraView<>(left, right, Mode.INTERSECTION);
    }

    /** Live, read-only left difference under Java equals/hashCode equality. */
    public static <E> Set<E> differenceByEquals(Set<? extends E> left,
                                                Set<? extends E> right) {
        return new AlgebraView<>(left, right, Mode.DIFFERENCE);
    }

    /** Live, read-only symmetric difference under Java equals/hashCode equality. */
    public static <E> Set<E> symmetricDifferenceByEquals(Set<? extends E> left,
                                                         Set<? extends E> right) {
        return new AlgebraView<>(left, right, Mode.SYMMETRIC_DIFFERENCE);
    }

    private enum Mode {
        UNION, INTERSECTION, DIFFERENCE, SYMMETRIC_DIFFERENCE;

        boolean contains(boolean inLeft, boolean inRight) {
            return switch (this) {
                case UNION -> inLeft || inRight;
                case INTERSECTION -> inLeft && inRight;
                case DIFFERENCE -> inLeft && !inRight;
                case SYMMETRIC_DIFFERENCE -> inLeft != inRight;
            };
        }

        long select(long left, long right) {
            return switch (this) {
                case UNION -> left | right;
                case INTERSECTION -> left & right;
                case DIFFERENCE -> left & ~right;
                case SYMMETRIC_DIFFERENCE -> left ^ right;
            };
        }
    }

    private static final class AlgebraView<E> extends AbstractSet<E> {
        private final Set<? extends E> left;
        private final Set<? extends E> right;
        private final Mode mode;

        AlgebraView(Set<? extends E> left, Set<? extends E> right, Mode mode) {
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
            this.mode = mode;
        }

        private Snapshot<E> project() {
            Accumulator<E> accumulation = new Accumulator<>();
            accumulation.mark(left, true);
            accumulation.mark(right, false);
            return accumulation.finish(mode);
        }

        @Override public int size() { return project().size; }

        @Override public boolean isEmpty() { return project().size == 0; }

        @Override public Iterator<E> iterator() { return project().iterator(); }

        @Override public boolean contains(Object key) {
            return mode.contains(containsEquals(left, key), containsEquals(right, key));
        }

        @Override public boolean containsAll(Collection<?> values) {
            Objects.requireNonNull(values, "values");
            Snapshot<E> projected = project();
            for (Object value : values) {
                if (!projected.contains(value)) return false;
            }
            return true;
        }

        /** Deferred first-touch snapshot; do not advertise stale SIZED metadata. */
        @Override public Spliterator<E> spliterator() {
            return new Spliterator<>() {
                private Iterator<E> iterator;

                @Override public boolean tryAdvance(Consumer<? super E> action) {
                    Objects.requireNonNull(action, "action");
                    if (iterator == null) iterator = project().iterator();
                    if (!iterator.hasNext()) return false;
                    action.accept(iterator.next());
                    return true;
                }

                @Override public Spliterator<E> trySplit() { return null; }
                @Override public long estimateSize() { return Long.MAX_VALUE; }
                @Override public int characteristics() { return DISTINCT | ORDERED; }
            };
        }

        @Override public boolean add(E value) { throw readOnly(); }
        @Override public boolean addAll(Collection<? extends E> values) { throw readOnly(); }
        @Override public boolean remove(Object value) { throw readOnly(); }
        @Override public void clear() { throw readOnly(); }
        @Override public boolean removeAll(Collection<?> values) { throw readOnly(); }
        @Override public boolean retainAll(Collection<?> values) { throw readOnly(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw readOnly(); }

        private UnsupportedOperationException readOnly() {
            return new UnsupportedOperationException("read-only equals-set algebra");
        }
    }

    /** Deliberately ignores a source Set's comparator or identity contains policy. */
    private static boolean containsEquals(Set<?> source, Object key) {
        for (Object value : source) {
            if (Objects.equals(key, value)) return true;
        }
        return false;
    }

    /** Transient owner: dynamic references plus two packed per-slot masks. */
    private static final class Accumulator<E> {
        private final HashMap<Object, Integer> offsets = new HashMap<>();
        private Object[] values = new Object[8];
        private long[] leftBits = new long[1];
        private long[] rightBits = new long[1];
        private int size;

        void mark(Set<? extends E> source, boolean left) {
            for (E value : source) {
                Integer previous = offsets.get(value);
                int slot;
                if (previous != null) {
                    slot = previous;
                } else {
                    slot = size;
                    grow();
                    values[size++] = value;
                    offsets.put(value, slot);
                }
                long[] mask = left ? leftBits : rightBits;
                mask[slot >>> 6] |= 1L << (slot & 63);
            }
        }

        private void grow() {
            if (size < values.length) return;
            int capacity = Math.multiplyExact(values.length, 2);
            values = Arrays.copyOf(values, capacity);
            int words = (capacity >>> 6) + ((capacity & 63) == 0 ? 0 : 1);
            leftBits = Arrays.copyOf(leftBits, words);
            rightBits = Arrays.copyOf(rightBits, words);
        }

        Snapshot<E> finish(Mode mode) {
            int words = (size >>> 6) + ((size & 63) == 0 ? 0 : 1);
            long[] selected = new long[words];
            int count = 0;
            for (int word = 0; word < words; word++) {
                selected[word] = mode.select(leftBits[word], rightBits[word]);
                count += Long.bitCount(selected[word]);
            }
            return new Snapshot<>(Arrays.copyOf(values, size), selected, count);
        }
    }

    /** Stable per-iteration reference/bitmask projection, not retained by a view. */
    private static final class Snapshot<E> {
        final Object[] values;
        final long[] selected;
        final int size;

        Snapshot(Object[] values, long[] selected, int size) {
            this.values = values;
            this.selected = selected;
            this.size = size;
        }

        boolean contains(Object key) {
            for (int position = next(0); position >= 0; position = next(position + 1)) {
                if (Objects.equals(key, values[position])) return true;
            }
            return false;
        }

        private int next(int offset) {
            int word = offset >>> 6;
            if (word >= selected.length) return -1;
            long bits = selected[word] & (-1L << (offset & 63));
            while (bits == 0) {
                if (++word == selected.length) return -1;
                bits = selected[word];
            }
            return (word << 6) + Long.numberOfTrailingZeros(bits);
        }

        Iterator<E> iterator() {
            return new Iterator<>() {
                private int position = Snapshot.this.next(0);

                @Override public boolean hasNext() { return position >= 0; }

                @Override public E next() {
                    if (position < 0) throw new NoSuchElementException();
                    @SuppressWarnings("unchecked") E value = (E) values[position];
                    position = Snapshot.this.next(position + 1);
                    return value;
                }
            };
        }
    }
}
