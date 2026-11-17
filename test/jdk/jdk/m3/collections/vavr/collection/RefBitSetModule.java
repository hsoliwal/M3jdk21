/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.vavr.collection;

import java.io.Serializable;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.m3.collections.vavr.Function1;
import jdk.m3.collections.vavr.Tuple2;

/**
 * Pre-patch reference: lines 895-1473 of the vavr BitSet.java before leaf
 * vavr-bitset-word-ops, with Ref-prefixed type names. Long lines are wrapped and the two
 * methods over 15 lines are split; nothing else differs.
 */
//FLLDCIM: LEETCODIFY — pre-patch master copy, the oracle of BitSetWordOpsOracleTest
interface RefBitSetModule {

    int ADDRESS_BITS_PER_WORD = 6;
    int BITS_PER_WORD = 64;

    abstract class RefAbstractBitSet<T extends Object> implements BitSet<T>, Serializable {

        private static final long serialVersionUID = 1L;

        final Function1<Integer, T> fromInt;
        final Function1<T, Integer> toInt;

        RefAbstractBitSet(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
            this.fromInt = fromInt;
            this.toInt = toInt;
        }

        abstract int getWordsNum();

        abstract long[] copyExpand(int wordsNum);

        abstract long getWord(int index);

        BitSet<T> createEmpty() {
            return new RefBitSet1<>(fromInt, toInt, 0L);
        }

        @SuppressWarnings("unchecked")
        BitSet<T> createFromAll(Iterable<? extends T> values) {
            return values instanceof BitSet ? (BitSet<T>) values : createEmpty().addAll(values);
        }

        BitSet<T> fromBitMaskNoCopy(long[] elements) {
            switch (elements.length) {
                case 0:
                    return createEmpty();
                case 1:
                    return new RefBitSet1<>(fromInt, toInt, elements[0]);
                case 2:
                    return new RefBitSet2<>(fromInt, toInt, elements[0], elements[1]);
                default:
                    return new RefBitSetN<>(fromInt, toInt, elements);
            }
        }

        private void setElement(long[] words, int element) {
            final int index = element >> ADDRESS_BITS_PER_WORD;
            words[index] |= (1L << element);
        }

        private void unsetElement(long[] words, int element) {
            final int index = element >> ADDRESS_BITS_PER_WORD;
            words[index] &= ~(1L << element);
        }

        long[] shrink(long[] elements) {
            int newlen = elements.length;
            while (newlen > 0 && elements[newlen - 1] == 0) {
                newlen--;
            }
            final long[] newelems = new long[newlen];
            System.arraycopy(elements, 0, newelems, 0, newlen);
            return newelems;
        }

        BitSet<T> addElement(int element) {
            final long[] copy = copyExpand(1 + (element >> ADDRESS_BITS_PER_WORD));
            setElement(copy, element);
            return fromBitMaskNoCopy(copy);
        }

        @Override
        public BitSet<T> distinctBy(Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator, "comparator is null");
            return isEmpty() ? this : createFromAll(iterator().distinctBy(comparator));
        }

        @Override
        public <U extends Object> BitSet<T> distinctBy(
                Function<? super T, ? extends U> keyExtractor) {
            Objects.requireNonNull(keyExtractor, "keyExtractor is null");
            return isEmpty() ? this : createFromAll(iterator().distinctBy(keyExtractor));
        }

        @Override
        public BitSet<T> drop(int n) {
            if (n <= 0 || isEmpty()) {
                return this;
            } else if (n >= length()) {
                return createEmpty();
            } else {
                return createFromAll(iterator().drop(n));
            }
        }

        @Override
        public BitSet<T> dropRight(int n) {
            if (n <= 0 || isEmpty()) {
                return this;
            } else if (n >= length()) {
                return createEmpty();
            } else {
                return createFromAll(iterator().dropRight(n));
            }
        }

        @Override
        public BitSet<T> dropWhile(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            final BitSet<T> bitSet = createFromAll(iterator().dropWhile(predicate));
            return (bitSet.length() == length()) ? this : bitSet;
        }

        @Override
        public BitSet<T> intersect(Set<? extends T> elements) {
            Objects.requireNonNull(elements, "elements is null");
            if (isEmpty()) {
                return this;
            } else if (elements.isEmpty()) {
                return createEmpty();
            } else {
                final int size = size();
                if (size <= elements.size()) {
                    return retainAll(elements);
                } else {
                    final BitSet<T> results = createFromAll(elements).retainAll(this);
                    return (size == results.size()) ? this : results;
                }
            }
        }

        /** Pre-patch orElse(Iterable): this when nonempty, otherwise createFromAll(other). */
        @Override
        public BitSet<T> orElse(Iterable<? extends T> other) {
            return isEmpty() ? createFromAll(other) : this;
        }

        /** Pre-patch orElse(Supplier): this when nonempty, otherwise createFromAll(get()). */
        @Override
        public BitSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
            return isEmpty() ? createFromAll(supplier.get()) : this;
        }

        @Override
        public Iterator<BitSet<T>> slideBy(Function<? super T, ?> classifier) {
            return iterator().slideBy(classifier).map(this::createFromAll);
        }

        @Override
        public Iterator<BitSet<T>> sliding(int size, int step) {
            return iterator().sliding(size, step).map(this::createFromAll);
        }

        @Override
        public Tuple2<BitSet<T>, BitSet<T>> span(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            return iterator().span(predicate).map(this::createFromAll, this::createFromAll);
        }

        @Override
        public BitSet<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
            return Collections.scanLeft(this, zero, operation, this::createFromAll);
        }

        @Override
        public Tuple2<BitSet<T>, BitSet<T>> partition(Predicate<? super T> predicate) {
            return Collections.partition(this, this::createFromAll, predicate);
        }

        @Override
        public BitSet<T> filter(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            final BitSet<T> bitSet = createFromAll(iterator().filter(predicate));
            return (bitSet.length() == length()) ? this : bitSet;
        }

        @Override
        public BitSet<T> reject(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            final BitSet<T> bitSet = createFromAll(iterator().reject(predicate));
            return (bitSet.length() == length()) ? this : bitSet;
        }

        @Override
        public <C extends Object> Map<C, BitSet<T>> groupBy(
                Function<? super T, ? extends C> classifier) {
            return Collections.groupBy(this, classifier, this::createFromAll);
        }

        @Override
        public Comparator<T> comparator() {
            return Comparator.comparing(toInt);
        }

        @Override
        public BitSet<T> takeWhile(Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            final BitSet<T> result = createFromAll(iterator().takeWhile(predicate));
            return (result.length() == length()) ? this : result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public BitSet<T> addAll(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(toInt);
            if (source.isEmpty()) {
                return this;
            } else {
                final long[] copy =
                        copyExpand(1 + (source.max().getOrElse(0) >> ADDRESS_BITS_PER_WORD));
                source.forEach(element -> {
                    if (element < 0) {
                        throw new IllegalArgumentException("bitset element must be >= 0");
                    }
                    setElement(copy, element);
                });
                final BitSet<T> bitSet = fromBitMaskNoCopy(copy);
                return (bitSet.length() == length()) ? this : bitSet;
            }
        }

        @Override
        public boolean contains(T t) {
            final int element = toInt.apply(t);
            if (element < 0) {
                throw new IllegalArgumentException("bitset element must be >= 0");
            }
            final int index = element >> ADDRESS_BITS_PER_WORD;
            final int bit = element & (RefBitSetModule.BITS_PER_WORD - 1);
            return index < getWordsNum() && (getWord(index) & (1L << bit)) != 0;
        }

        @Override
        public BitSet<T> init() {
            if (isEmpty()) {
                throw new UnsupportedOperationException("init of empty TreeSet");
            } else {
                final long last = getWord(getWordsNum() - 1);
                final int element = BITS_PER_WORD * (getWordsNum() - 1) + BITS_PER_WORD
                        - Long.numberOfLeadingZeros(last) - 1;
                return remove(fromInt.apply(element));
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new RefBitSetIterator<>(this);
        }

        @Override
        public BitSet<T> take(int n) {
            if (isEmpty() || n >= length()) {
                return this;
            } else if (n <= 0) {
                return createEmpty();
            } else {
                return createFromAll(iterator().take(n));
            }
        }

        @Override
        public BitSet<T> takeRight(int n) {
            if (isEmpty() || n >= length()) {
                return this;
            } else if (n <= 0) {
                return createEmpty();
            } else {
                return createFromAll(iterator().takeRight(n));
            }
        }

        @Override
        public BitSet<T> remove(T t) {
            if (contains(t)) {
                final int element = toInt.apply(t);
                final long[] copy = copyExpand(getWordsNum());
                unsetElement(copy, element);
                return fromBitMaskNoCopy(shrink(copy));
            } else {
                return this;
            }
        }

        @Override
        public BitSet<T> removeAll(Iterable<? extends T> elements) {
            if (isEmpty()) {
                return this;
            } else {
                return removeAllFrom(elements);
            }
        }

        private BitSet<T> removeAllFrom(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(toInt);
            if (source.isEmpty()) {
                return this;
            } else {
                final int wordsNum = getWordsNum();
                final long[] copy = copyExpand(wordsNum);
                source.forEach(element -> {
                    // elements that cannot be present (negative or beyond the current words)
                    // are a no-op
                    if (element >= 0 && (element >> ADDRESS_BITS_PER_WORD) < wordsNum) {
                        unsetElement(copy, element);
                    }
                });
                return fromBitMaskNoCopy(shrink(copy));
            }
        }

        @Override
        public String toString() {
            return mkString(stringPrefix() + "(", ", ", ")");
        }

        @Override
        public boolean equals(Object o) {
            return Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return Collections.hashUnordered(this);
        }
    }

    class RefBitSetIterator<T extends Object> extends AbstractIterator<T> {

        private final RefAbstractBitSet<T> bitSet;

        private long element;
        private int index;

        RefBitSetIterator(RefAbstractBitSet<T> bitSet) {
            this.bitSet = bitSet;
            element = bitSet.getWord(0);
            index = 0;
        }

        @Override
        protected T getNext() {
            final int pos = Long.numberOfTrailingZeros(element);
            element &= ~(1L << pos);
            return bitSet.fromInt.apply(pos + (index << ADDRESS_BITS_PER_WORD));
        }

        @Override
        public boolean hasNext() {
            if (element == 0) {
                while (element == 0 && index < bitSet.getWordsNum() - 1) {
                    element = bitSet.getWord(++index);
                }
                return element != 0;
            } else {
                return true;
            }
        }
    }

    class RefBitSet1<T extends Object> extends RefAbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements;
        private final int len;

        RefBitSet1(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long elements) {
            super(fromInt, toInt);
            this.elements = elements;
            len = Long.bitCount(elements);
        }

        @Override
        int getWordsNum() {
            return 1;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if (wordsNum < 1) {
                wordsNum = 1;
            }
            final long[] arr = new long[wordsNum];
            arr[0] = elements;
            return arr;
        }

        @Override
        long getWord(int index) {
            return elements;
        }

        @Override
        public T head() {
            if (elements == 0) {
                throw new NoSuchElementException("head of empty BitSet");
            } else {
                return fromInt.apply(Long.numberOfTrailingZeros(elements));
            }
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public BitSet<T> add(T t) {
            final int element = toInt.apply(t);
            if (element < 0) {
                throw new IllegalArgumentException("bitset element must be >= 0");
            }
            if (element < BITS_PER_WORD) {
                final long mask = 1L << element;
                if ((elements & mask) != 0) {
                    return this;
                } else {
                    return new RefBitSet1<>(fromInt, toInt, elements | mask);
                }
            } else {
                return addElement(element);
            }
        }
    }

    class RefBitSet2<T extends Object> extends RefAbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements1, elements2;
        private final int len;

        RefBitSet2(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long elements1,
                long elements2) {
            super(fromInt, toInt);
            this.elements1 = elements1;
            this.elements2 = elements2;
            len = Long.bitCount(elements1) + Long.bitCount(elements2);
        }

        @Override
        int getWordsNum() {
            return 2;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if (wordsNum < 2) {
                wordsNum = 2;
            }
            final long[] arr = new long[wordsNum];
            arr[0] = elements1;
            arr[1] = elements2;
            return arr;
        }

        @Override
        long getWord(int index) {
            if (index == 0) {
                return elements1;
            } else {
                return elements2;
            }
        }

        @Override
        public T head() {
            if (elements1 == 0) {
                return fromInt.apply(BITS_PER_WORD + Long.numberOfTrailingZeros(elements2));
            } else {
                return fromInt.apply(Long.numberOfTrailingZeros(elements1));
            }
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public BitSet<T> add(T t) {
            final int element = toInt.apply(t);
            if (element < 0) {
                throw new IllegalArgumentException("bitset element must be >= 0");
            }
            return addChecked(element);
        }

        private BitSet<T> addChecked(int element) {
            final long mask = 1L << element;
            if (element < BITS_PER_WORD) {
                if ((elements1 & mask) != 0) {
                    return this;
                } else {
                    return new RefBitSet2<>(fromInt, toInt, elements1 | mask, elements2);
                }
            } else {
                return addHigh(element, mask);
            }
        }

        private BitSet<T> addHigh(int element, long mask) {
            if (element < 2 * BITS_PER_WORD) {
                if ((elements2 & mask) != 0) {
                    return this;
                } else {
                    return new RefBitSet2<>(fromInt, toInt, elements1, elements2 | mask);
                }
            } else {
                return addElement(element);
            }
        }
    }

    class RefBitSetN<T extends Object> extends RefAbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long[] elements;
        private final int len;

        RefBitSetN(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long[] elements) {
            super(fromInt, toInt);
            this.elements = elements;
            len = calcLength(elements);
        }

        private static int calcLength(long[] elements) {
            int len = 0;
            for (long element : elements) {
                len += Long.bitCount(element);
            }
            return len;
        }

        @Override
        int getWordsNum() {
            return elements.length;
        }

        @Override
        long[] copyExpand(int wordsNum) {
            if (wordsNum < elements.length) {
                wordsNum = elements.length;
            }
            final long[] arr = new long[wordsNum];
            System.arraycopy(elements, 0, arr, 0, elements.length);
            return arr;
        }

        @Override
        long getWord(int index) {
            return elements[index];
        }

        @Override
        public T head() {
            int offset = 0;
            int element = 0;
            for (int i = 0; i < getWordsNum(); i++) {
                if (elements[i] == 0) {
                    offset += BITS_PER_WORD;
                } else {
                    element = offset + Long.numberOfTrailingZeros(elements[i]);
                    break;
                }
            }
            return fromInt.apply(element);
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public BitSet<T> add(T t) {
            if (contains(t)) {
                return this;
            } else {
                return addElement(toInt.apply(t));
            }
        }
    }
}
