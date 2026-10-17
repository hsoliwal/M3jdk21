/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/** Purpose-owned wrapper atoms. Invoked through {@link M3CollectionWrappers}. */
final class M3ConcatenatedViews {
    private M3ConcatenatedViews() {
    }


    static <E> Collection<E> of(
            Collection<? extends E> left, Collection<? extends E> right) {
        return Collections.unmodifiableCollection(new ConcatenatedCollection<>(left, right));
    }

    private static final class ConcatenatedCollection<E> extends AbstractCollection<E> {
        private final Collection<? extends E> left;
        private final Collection<? extends E> right;

        private ConcatenatedCollection(
                Collection<? extends E> left, Collection<? extends E> right) {
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<? extends E> a = left.iterator();
            Iterator<? extends E> b = right.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return a.hasNext() || b.hasNext();
                }

                @Override
                public E next() {
                    return a.hasNext() ? a.next() : b.next();
                }
            };
        }

        /**
         * JDK Collection.size() must saturate rather than overflow or throw when a
         * logical concatenation contains more than Integer.MAX_VALUE elements.
         * This operation reads only two owner metadata values, not element payloads.
         */
        @Override
        public int size() {
            long total = (long) left.size() + right.size();
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        /**
         * The ordinary JDK Collection spliterator is SIZED because it trusts size().
         * Once an input reaches the int ceiling its true cardinality is unknown, so
         * claiming SIZED would incorrectly let Stream.count() return the saturated
         * int instead of traversing the logical elements. Preserve the JDK default
         * fast path for exact small sizes. The overflow path has O(1) working state,
         * does not construct a result array and deliberately refuses parallel splits.
         * An iteration is ordered left-then-right as defined by this wrapper.
         */
        @Override
        public Spliterator<E> spliterator() {
            int leftSize = left.size();
            int rightSize = right.size();
            if (leftSize < Integer.MAX_VALUE && rightSize < Integer.MAX_VALUE
                    && (long) leftSize + rightSize <= Integer.MAX_VALUE) {
                return super.spliterator();
            }
            Iterator<E> cursor = iterator();
            return new Spliterator<>() {
                @Override
                public boolean tryAdvance(Consumer<? super E> action) {
                    Objects.requireNonNull(action, "action");
                    if (!cursor.hasNext()) {
                        return false;
                    }
                    action.accept(cursor.next());
                    return true;
                }

                @Override
                public Spliterator<E> trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return Long.MAX_VALUE;
                }

                @Override
                public int characteristics() {
                    return ORDERED;
                }
            };
        }
    }
}
