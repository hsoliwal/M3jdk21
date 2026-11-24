/* ____  ______________  ________________________  __________
 * \   \/   /      \   \/   /   __/   /      \   \/   /      \
 *  \______/___/\___\______/___/_____/___/\___\______/___/\___\
 *
 * Copyright 2014-2026 Vavr, https://vavr.io
 * Modified 2026 by Hitesh Soliwal and contributors: bound prefix selection allocation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.m3.collections.vavr.collection;

import jdk.m3.collections.vavr.Function1;
import jdk.m3.collections.vavr.PartialFunction;
import jdk.m3.collections.vavr.Tuple2;
import jdk.m3.collections.vavr.Tuple3;
import jdk.m3.collections.vavr.control.Option;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * An immutable {@code BitSet} implementation.
 * <p>
 * Elements are stored as bits, so every element must map to a non-negative {@code int} via the builder's
 * {@code toInt} function (see {@link #withRelations(Function1, Function1)}). Adding, querying or removing a value
 * that maps to a negative int throws {@link IllegalArgumentException}. Elements are ordered by their int mapping.
 *
 * @param <T> Component type
 * @author Ruslan Sennov
 */
public interface BitSet<T extends Object> extends SortedSet<T> {

    /**
     * The serial version UID for serialization.
     */
    long serialVersionUID = 1L;

    /**
     * Builder of the BitSet. Encapsulates the conversion functions for T.
     *
     * @param <T> Component type
     */
    class Builder<T extends Object> {

        final static Builder<Integer> DEFAULT = new Builder<>(i -> i, i -> i);

        final Function1<Integer, T> fromInt;
        final Function1<T, Integer> toInt;

        Builder(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
            this.fromInt = fromInt;
            this.toInt = toInt;
        }

        /**
         * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
         * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link BitSet}.
         *
         * @return A {@link BitSet} Collector.
         */
        public Collector<T, ArrayList<T>, BitSet<T>> collector() {
            final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
                left.addAll(right);
                return left;
            };
            return Collector.of(ArrayList::new, ArrayList::add, combiner, this::ofAll);
        }

        /**
         * Returns a new empty BitSet
         *
         * @return new empty BitSet
         */
        public BitSet<T> empty() {
            return new BitSetModule.BitSet1<>(fromInt, toInt, 0L);
        }

        /**
         * Builds a new BitSet from a specific value t
         *
         * @param t value to build the BitSet from
         * @return new BitSet
         */
        public BitSet<T> of(T t) {
            final int value = toInt.apply(t);
            final int index = value >> BitSetModule.ADDRESS_BITS_PER_WORD;
            final int bit = value & (BitSetModule.BITS_PER_WORD - 1);

            switch (index) {
                case 0:
                    return new BitSetModule.BitSet1<>(fromInt, toInt, 1L << bit);
                case 1:
                    return new BitSetModule.BitSet2<>(fromInt, toInt, 0L, 1L << bit);
                default:
                    return empty().add(t);
            }
        }

        /**
         * Builds a new BitSet from a list of values
         *
         * @param values values to build the BitSet from
         * @return new BitSet
         */
        @SuppressWarnings("varargs")
        @SafeVarargs
        public final BitSet<T> of(T ... values) {
            return empty().addAll(Array.wrap(values));
        }

        /**
         * Builds a new BitSet from an {@link Iterable}
         *
         * @param values iterable to build the BitSet from
         * @return new BitSet
         */
        public BitSet<T> ofAll(Iterable<? extends T> values) {
            Objects.requireNonNull(values, "values is null");
            return empty().addAll(values);
        }

        /**
         * Builds a new BitSet from a {@link java.util.stream.Stream} of values
         *
         * @param javaStream stream of values to build the BitSet from
         * @return new BitSet
         */
        public BitSet<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
            Objects.requireNonNull(javaStream, "javaStream is null");
            return empty().addAll(Iterator.ofAll(javaStream.iterator()));
        }

        /**
         * Builds a new BitSet tabulating a value n times
         *
         * @param n number of times to tabulate
         * @param f function to tabulate
         * @return new BitSet
         */
        public BitSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
            Objects.requireNonNull(f, "f is null");
            return empty().addAll(Collections.tabulate(n, f));
        }

        /**
         * Builds a new BitSet from the values supplied by {@code n} invocations of {@code s}.
         * Because a BitSet cannot contain duplicates, equal values collapse into a single element
         * (e.g. a constant supplier yields a BitSet of size 1, not n).
         *
         * @param n number of times to invoke the supplier
         * @param s value supplier
         * @return new BitSet
         */
        public BitSet<T> fill(int n, Supplier<? extends T> s) {
            Objects.requireNonNull(s, "s is null");
            return empty().addAll(Collections.fill(n, s));
        }
    }

    /**
     * Returns new {@link BitSet} Builder with custom conversion functions between Integer and type T
     *
     * @param <T> Component type
     * @param fromInt function to convert from Integer to T
     * @param toInt function to convert from T to Integer
     * @return new Builder
     */
    static <T extends Object> Builder<T> withRelations(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
        return new Builder<>(fromInt, toInt);
    }

    /**
     * Returns new {@link BitSet} Builder for an enum type
     *
     * @param <T> Enum type
     * @param enumClass the enum class
     * @return new Builder
     */
    @SuppressWarnings("RedundantTypeArguments")
    static <T extends Enum<T>> Builder<T> withEnum(Class<T> enumClass) {
        return new Builder<>(i -> enumClass.getEnumConstants()[i], Enum<T>::ordinal);
    }

    /**
     * Returns new {@link BitSet} Builder for type {@link Character}
     *
     * @return new Builder
     */
    static Builder<Character> withCharacters() {
        return new Builder<>(i -> (char) i.intValue(), c -> (int) c);
    }

    /**
     * Returns new {@link BitSet} Builder for type {@link Byte}
     *
     * @return new Builder
     */
    static Builder<Byte> withBytes() {
        return new Builder<>(Integer::byteValue, Byte::intValue);
    }

    /**
     * Returns new {@link BitSet} Builder for type {@link Long}
     * <p>
     * Values are converted via {@link Long#intValue()}, i.e. only values in {@code [0, Integer.MAX_VALUE]} are
     * supported; larger values are silently truncated to their low 32 bits (and rejected if the result is negative).
     *
     * @return new Builder
     */
    static Builder<Long> withLongs() {
        return new Builder<>(Integer::longValue, Long::intValue);
    }

    /**
     * Returns new {@link BitSet} Builder for type {@link Short}
     *
     * @return new Builder
     */
    static Builder<Short> withShorts() {
        return new Builder<>(Integer::shortValue, Short::intValue);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link BitSet}.
     *
     * @return A {@link BitSet} Collector.
     */
    static Collector<Integer, ArrayList<Integer>, BitSet<Integer>> collector() {
        return Builder.DEFAULT.collector();
    }

    /**
     * Returns a BitSet containing no elements.
     *
     * @return an empty BitSet
     */
    static BitSet<Integer> empty() {
        return Builder.DEFAULT.empty();
    }

    /**
     * Returns a BitSet containing a single value.
     *
     * @param value A single value
     * @return A BitSet containing the given value
     */
    static BitSet<Integer> of(Integer value) {
        return Builder.DEFAULT.of(value);
    }

    /**
     * Creates a BitSet of the given int values.
     *
     * @param values int values
     * @return A new BitSet of int values
     */
    static BitSet<Integer> of(Integer... values) {
        return Builder.DEFAULT.of(values);
    }

    /**
     * Returns a BitSet containing the distinct values of a given Function {@code f}
     * applied over a range of integer values from 0 to {@code n - 1}.
     *
     * @param n The number of times {@code f} is invoked
     * @param f The Function computing element values
     * @return A BitSet consisting of the distinct elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    static BitSet<Integer> tabulate(int n, Function<Integer, Integer> f) {
        return Builder.DEFAULT.tabulate(n, f);
    }

    /**
     * Returns a BitSet containing the distinct values supplied by {@code n} invocations of a given Supplier {@code s}.
     *
     * @param n The number of times the supplier is invoked
     * @param s The Supplier computing element values
     * @return A BitSet of at most {@code n} elements, containing the distinct values returned by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    static BitSet<Integer> fill(int n, Supplier<Integer> s) {
        return Builder.DEFAULT.fill(n, s);
    }

    /**
     * Creates a BitSet of the given Iterable of int values.
     *
     * @param values int values
     * @return A new BitSet of int values
     */
    static BitSet<Integer> ofAll(Iterable<Integer> values) {
        return Builder.DEFAULT.ofAll(values);
    }

    /**
     * Creates a BitSet from a {@link java.util.stream.Stream} of int values.
     *
     * @param javaStream A java.util.stream.Stream of int values
     * @return A new BitSet of int values
     */
    static BitSet<Integer> ofAll(java.util.stream.Stream<Integer> javaStream) {
        return Builder.DEFAULT.ofAll(javaStream);
    }

    /**
     * Creates a BitSet from boolean values.
     *
     * @param elements boolean values
     * @return A new BitSet of boolean values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Boolean> ofAll(boolean... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.withRelations(i -> i != 0, b -> b ? 1 : 0).ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet from byte values.
     *
     * @param elements byte values
     * @return A new BitSet of byte values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Byte> ofAll(byte... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.withBytes().ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet from char values.
     *
     * @param elements char values
     * @return A new BitSet of char values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Character> ofAll(char... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.withCharacters().ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet from int values.
     *
     * @param elements int values
     * @return A new BitSet of int values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Integer> ofAll(int... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet from long values.
     *
     * @param elements long values
     * @return A new BitSet of long values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Long> ofAll(long... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.withLongs().ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet from short values.
     *
     * @param elements short values
     * @return A new BitSet of short values
     * @throws NullPointerException if elements is null
     */
    static BitSet<Short> ofAll(short... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return BitSet.withShorts().ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     * @throws IllegalArgumentException if the range contains a negative number
     */
    static BitSet<Integer> range(int from, int toExclusive) {
        return BitSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a BitSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    static BitSet<Character> range(char from, char toExclusive) {
        return BitSet.withCharacters().ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a BitSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     * @throws IllegalArgumentException if the range contains a negative number
     */
    static BitSet<Long> range(long from, long toExclusive) {
        return BitSet.withLongs().ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero or the range contains a negative number
     */
    static BitSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return BitSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /** Creates a BitSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static BitSet<Character> rangeBy(char from, char toExclusive, int step) {
        return BitSet.withCharacters().ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /** Creates a BitSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero or the range contains a negative number
     */
    static BitSet<Long> rangeBy(long from, long toExclusive, long step) {
        return BitSet.withLongs().ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toInclusive}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     * @throws IllegalArgumentException if the range contains a negative number
     */
    static BitSet<Integer> rangeClosed(int from, int toInclusive) {
        return BitSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a BitSet of char numbers starting from {@code from}, extending to {@code toInclusive}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    static BitSet<Character> rangeClosed(char from, char toInclusive) {
        return BitSet.withCharacters().ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a BitSet of long numbers starting from {@code from}, extending to {@code toInclusive}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     * @throws IllegalArgumentException if the range contains a negative number
     */
    static BitSet<Long> rangeClosed(long from, long toInclusive) {
        return BitSet.withLongs().ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a BitSet of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero or the range contains a negative number
     */
    static BitSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return BitSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a BitSet of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static BitSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return BitSet.withCharacters().ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a BitSet of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero or the range contains a negative number
     */
    static BitSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return BitSet.withLongs().ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code element} maps to a negative int
     */
    @Override
    BitSet<T> add(T element);

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if one of the {@code elements} maps to a negative int
     */
    @Override
    BitSet<T> addAll(Iterable<? extends T> elements);

    /**
     * Tests if the given {@code element} is contained, by comparing its int mapping.
     *
     * @param element An element
     * @return true, if element is contained, false otherwise.
     * @throws IllegalArgumentException if {@code element} maps to a negative int
     */
    @Override
    boolean contains(T element);

    @Override
    default <R extends Object> SortedSet<R> collect(PartialFunction<? super T, ? extends R> partialFunction) {
        Objects.requireNonNull(partialFunction, "partialFunction is null");
        return TreeSet.ofAll(Comparators.naturalComparator(), iterator().collect(partialFunction));
    }

    @Override
    default BitSet<T> diff(Set<? extends T> elements) {
        return removeAll(elements);
    }

    @Override
    default BitSet<T> distinct() {
        return this;
    }

    @Override
    BitSet<T> distinctBy(Comparator<? super T> comparator);

    @Override
    <U extends Object> BitSet<T> distinctBy(Function<? super T, ? extends U> keyExtractor);

    @Override
    BitSet<T> drop(int n);

    @Override
    BitSet<T> dropRight(int n);

    @Override
    default BitSet<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    @Override
    BitSet<T> dropWhile(Predicate<? super T> predicate);

    @Override
    BitSet<T> filter(Predicate<? super T> predicate);

    @Override
    BitSet<T> reject(Predicate<? super T> predicate);

    @Override
    default <U extends Object> SortedSet<U> flatMap(Comparator<? super U> comparator, Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, iterator().flatMap(mapper));
    }

    @Override
    default <U extends Object> SortedSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return flatMap(Comparators.naturalComparator(), mapper);
    }

    @Override
    default <U extends Object> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return iterator().foldRight(zero, f);
    }

    @Override
    <C extends Object> Map<C, BitSet<T>> groupBy(Function<? super T, ? extends C> classifier);

    @Override
    default Iterator<BitSet<T>> grouped(int size) {
        return sliding(size, size);
    }

    @Override
    default boolean hasDefiniteSize() {
        return true;
    }

    @Override
    BitSet<T> init();

    @Override
    default Option<BitSet<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * An {@code BitSet}'s value is computed synchronously.
     *
     * @return false
     */
    @Override
    default boolean isAsync() {
        return false;
    }

    @Override
    default boolean isTraversableAgain() {
        return true;
    }

    /**
     * An {@code BitSet}'s value is computed eagerly.
     *
     * @return false
     */
    @Override
    default boolean isLazy() {
        return false;
    }

    @Override
    BitSet<T> intersect(Set<? extends T> elements);

    @Override
    default T last() {
        return Collections.last(this);
    }

    @Override
    Tuple2<BitSet<T>, BitSet<T>> partition(Predicate<? super T> predicate);

    @Override
    default BitSet<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (!isEmpty()) {
            action.accept(head());
        }
        return this;
    }

    @Override
    default String stringPrefix() {
        return "BitSet";
    }

    @Override
    default <U extends Object> SortedSet<U> map(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, iterator().map(mapper));
    }

    @Override
    default <U extends Object> SortedSet<U> map(Function<? super T, ? extends U> mapper) {
        return map(Comparators.naturalComparator(), mapper);
    }

    @Override
    default <U extends Object> SortedSet<U> mapTo(U value) {
        return map(ignored -> value);
    }

    @Override
    default SortedSet<Void> mapToVoid() {
        return this.<Void>map((o1, o2) -> 0, ignored -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code element} maps to a negative int
     */
    @Override
    BitSet<T> remove(T element);

    /**
     * {@inheritDoc}
     * <p>
     * Elements that cannot be contained in this BitSet (e.g. values mapping to a negative int) are ignored.
     */
    @Override
    BitSet<T> removeAll(Iterable<? extends T> elements);

    @Override
    default BitSet<T> replace(T currentElement, T newElement) {
        if (contains(currentElement)) {
            return remove(currentElement).add(newElement);
        } else {
            return this;
        }
    }

    @Override
    default BitSet<T> replaceAll(T currentElement, T newElement) {
        // a set has only one occurrence
        return replace(currentElement, newElement);
    }

    @Override
    default BitSet<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements);
    }

    @Override
    BitSet<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation);

    @Override
    default <U extends Object> Set<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return Collections.scanLeft(this, zero, operation, HashSet::ofAll);
    }

    @Override
    default <U extends Object> Set<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return Collections.scanRight(this, zero, operation, HashSet::ofAll);
    }

    @Override
    Iterator<BitSet<T>> slideBy(Function<? super T, ?> classifier);

    @Override
    default Iterator<BitSet<T>> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    Iterator<BitSet<T>> sliding(int size, int step);

    @Override
    Tuple2<BitSet<T>, BitSet<T>> span(Predicate<? super T> predicate);

    @Override
    default BitSet<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty BitSet");
        } else {
            return drop(1);
        }
    }

    @Override
    default Option<BitSet<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    @Override
    BitSet<T> take(int n);

    @Override
    BitSet<T> takeRight(int n);

    @Override
    default BitSet<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    @Override
    BitSet<T> takeWhile(Predicate<? super T> predicate);

    @Override
    default java.util.SortedSet<T> toJavaSet() {
        return toJavaSet(ignore -> new java.util.TreeSet<>(comparator()));
    }

    @Override
    default BitSet<T> union(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return elements.isEmpty() ? this : addAll(elements);
    }

    @Override
    default <T1 extends Object, T2 extends Object> Tuple2<TreeSet<T1>, TreeSet<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return iterator().unzip(unzipper).map(i1 -> TreeSet.ofAll(Comparators.naturalComparator(), i1),
                i2 -> TreeSet.ofAll(Comparators.naturalComparator(), i2));
    }

    @Override
    default <T1 extends Object, T2 extends Object, T3 extends Object> Tuple3<TreeSet<T1>, TreeSet<T2>, TreeSet<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return iterator().unzip3(unzipper).map(
                i1 -> TreeSet.ofAll(Comparators.naturalComparator(), i1),
                i2 -> TreeSet.ofAll(Comparators.naturalComparator(), i2),
                i3 -> TreeSet.ofAll(Comparators.naturalComparator(), i3));
    }

    @Override
    default <U extends Object> TreeSet<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final Comparator<Tuple2<T, U>> tuple2Comparator = Tuple2.comparator(comparator(), Comparators.naturalComparator());
        return TreeSet.ofAll(tuple2Comparator, iterator().zip(that));
    }

    @Override
    default <U extends Object, R extends Object> TreeSet<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(Comparators.naturalComparator(), iterator().zipWith(that, mapper));
    }

    @Override
    default <U extends Object> TreeSet<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        final Comparator<Tuple2<T, U>> tuple2Comparator = Tuple2.comparator(comparator(), Comparators.naturalComparator());
        return TreeSet.ofAll(tuple2Comparator, iterator().zipAll(that, thisElem, thatElem));
    }

    @Override
    default TreeSet<Tuple2<T, Integer>> zipWithIndex() {
        final Comparator<? super T> component1Comparator = comparator();
        final Comparator<Tuple2<T, Integer>> tuple2Comparator = (t1, t2) -> component1Comparator.compare(t1._1, t2._1);
        return TreeSet.ofAll(tuple2Comparator, iterator().zipWithIndex());
    }

    @Override
    default <U extends Object> TreeSet<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(Comparators.naturalComparator(), iterator().zipWithIndex(mapper));
    }
}

interface BitSetModule {

    int ADDRESS_BITS_PER_WORD = 6;
    int BITS_PER_WORD = 64;

    abstract class AbstractBitSet<T extends Object> implements BitSet<T>, Serializable {

        private static final long serialVersionUID = 1L;

        final Function1<Integer, T> fromInt;
        final Function1<T, Integer> toInt;

        AbstractBitSet(Function1<Integer, T> fromInt, Function1<T, Integer> toInt) {
            this.fromInt = fromInt;
            this.toInt = toInt;
        }

        abstract int getWordsNum();

        abstract long[] copyExpand(int wordsNum);

        abstract long getWord(int index);

        BitSet<T> createEmpty() {
            return new BitSet1<>(fromInt, toInt, 0L);
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
                    return new BitSet1<>(fromInt, toInt, elements[0]);
                case 2:
                    return new BitSet2<>(fromInt, toInt, elements[0], elements[1]);
                default:
                    return new BitSetN<>(fromInt, toInt, elements);
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

        //FLLDCIM: HPCPOP - identity table of the built-in mappings that are pure, injective
        // and round-trip on every reachable bit
        private static final BitSet.Builder<?>[] EXACT_MAPPINGS = {
                BitSet.Builder.DEFAULT, BitSet.withCharacters(), BitSet.withBytes(),
                BitSet.withShorts(), BitSet.withLongs()
        };

        //FLLDCIM: HPCPOP - DIRECT_ACCESS: word fast paths only for a built-in mapping,
        // so no user callback can tell
        private boolean exact() {
            for (BitSet.Builder<?> builder : EXACT_MAPPINGS) {
                if (builder.fromInt == (Object) fromInt && builder.toInt == (Object) toInt) {
                    return true;
                }
            }
            return false;
        }

        //FLLDCIM: HPCPOP - same built-in mapping on both sides, so equal bits mean equal elements
        private boolean sameWords(Iterable<?> other) {
            return other instanceof AbstractBitSet<?> that
                    && that.fromInt == (Object) fromInt && that.toInt == (Object) toInt && exact();
        }

        //FLLDCIM: HPCPOP - COMPACT: count of words up to the last nonzero one in words[0, n)
        private static int usedWords(long[] words, int n) {
            int len = n;
            while (len > 0 && words[len - 1] == 0) {
                len--;
            }
            return len;
        }

        //FLLDCIM: HPCPOP - COMPACT: the shape fromBitMaskNoCopy(shrink(...)) builds,
        // with at most one copy
        private BitSet<T> canonical(long[] words, int n) {
            final int len = usedWords(words, n);
            return fromBitMaskNoCopy(
                    len == words.length ? words : java.util.Arrays.copyOf(words, len));
        }

        //FLLDCIM: HPCPOP - MASK: keep the lowest m set bits (1 <= m <= bitCount),
        // PDEP select of the m-th bit
        private static long keepLow(long word, int m) {
            final long bit = Long.expand(1L << (m - 1), word);
            return word & ((bit << 1) - 1);
        }

        //FLLDCIM: HPCPOP - MASK: keep the highest m set bits (1 <= m <= bitCount),
        // PDEP select of the lowest kept bit
        private static long keepHigh(long word, int m) {
            final long bit = Long.expand(1L << (Long.bitCount(word) - m), word);
            return word & -bit;
        }

        //FLLDCIM: HPCPOP - word walk down to the last nonzero word (one step for a canonical set)
        private int lastWordIndex() {
            int i = getWordsNum() - 1;
            while (i > 0 && getWord(i) == 0) {
                i--;
            }
            return i;
        }

        @Override
        public BitSet<T> distinctBy(Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator, "comparator is null");
            return isEmpty() ? this : createFromAll(iterator().distinctBy(comparator));
        }

        @Override
        public <U extends Object> BitSet<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
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
                return exact() ? highest(length() - n) : createFromAll(iterator().drop(n));
            }
        }

        @Override
        public BitSet<T> dropRight(int n) {
            if (n <= 0 || isEmpty()) {
                return this;
            } else if (n >= length()) {
                return createEmpty();
            } else {
                return exact() ? lowest(length() - n) : createFromAll(iterator().dropRight(n));
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

        /**
         * Returns this {@code BitSet} if it is nonempty, otherwise a {@code BitSet} built from {@code other}.
         * If {@code other} is itself a {@code BitSet} it is returned as-is (keeping its own element encoding),
         * otherwise a new {@code BitSet} using this instance's element encoding is created.
         *
         * @param other An alternative {@code Traversable}
         * @return this {@code BitSet} if it is nonempty, otherwise a {@code BitSet} built from {@code other}
         */
        @Override
        public BitSet<T> orElse(Iterable<? extends T> other) {
            return isEmpty() ? createFromAll(other) : this;
        }

        /**
         * Returns this {@code BitSet} if it is nonempty, otherwise a {@code BitSet} built from the result of
         * evaluating {@code supplier}. If that result is itself a {@code BitSet} it is returned as-is (keeping its
         * own element encoding), otherwise a new {@code BitSet} using this instance's element encoding is created.
         *
         * @param supplier A supplier of an alternative {@code Iterable}, evaluated only if this BitSet is empty
         * @return this {@code BitSet} if it is nonempty, otherwise a {@code BitSet} built from {@code supplier.get()}
         */
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
        public <C extends Object> Map<C, BitSet<T>> groupBy(Function<? super T, ? extends C> classifier) {
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
        public BitSet<T> addAll(Iterable<? extends T> elements) {
            if (sameWords(elements)) {
                return orWords((AbstractBitSet<?>) elements);
            } else if (laneable(elements)) {
                return addLane(elements.iterator());
            } else {
                return addAllStream(elements);
            }
        }

        //FLLDCIM: HPCPOP - MASK: OR word loop, sized like the stream path (1 + max >> 6 words)
        private BitSet<T> orWords(AbstractBitSet<?> that) {
            if (that.isEmpty()) {
                return this;
            }
            final long[] other = that.copyExpand(that.getWordsNum());
            final int n = usedWords(other, other.length);
            final long[] copy = copyExpand(n);
            for (int i = 0; i < n; i++) {
                copy[i] |= other[i];
            }
            final BitSet<T> bitSet = fromBitMaskNoCopy(copy);
            return (bitSet.length() == length()) ? this : bitSet;
        }

        //FLLDCIM: HPCPOP - the lane replays Stream.ofAll call for call;
        // Stream and ListView sources keep the stream path
        private boolean laneable(Iterable<?> elements) {
            return elements != null && !(elements instanceof Stream<?>)
                    && !(elements instanceof JavaConverters.ListView<?, ?>) && exact();
        }

        //FLLDCIM: HPCPOP - dynamic array: amortised doubling, capped below the VM array limit
        private static int[] grow(int[] lane) {
            return java.util.Arrays.copyOf(lane, Math.max(lane.length + 1,
                    (int) Math.min(Integer.MAX_VALUE - 8, 2L * lane.length)));
        }

        //FLLDCIM: HPCPOP - OFFSET_ARRAY lane: [0] count (0 = lone null), [1] max,
        // [2, 2 + count) values; compares as max() does
        private int[] laneOf(java.util.Iterator<? extends T> it) {
            final Comparator<Integer> natural = Comparators.naturalComparator();
            Integer max = toInt.apply(it.next());
            int[] lane = new int[16];
            lane[2] = max == null ? 0 : max;
            int n = 1;
            while (it.hasNext()) {
                final Integer v = toInt.apply(it.next());
                // The pre-optimization stream comparator throws an NPE without an
                // enhanced VM diagnostic if either mapped value is null. Keep that
                // observable exception contract without touching callback order.
                if (max == null || v == null) {
                    throw new NullPointerException();
                }
                max = natural.compare(max, v) >= 0 ? max : v;
                lane = n + 2 < lane.length ? lane : grow(lane);
                lane[2 + n++] = v;
            }
            lane[0] = max == null ? 0 : n;
            lane[1] = max == null ? 0 : max;
            return lane;
        }

        //FLLDCIM: HPCPOP - one growth to the max element, then bit sets;
        // Option.getOrElse replays the lone-null NPE
        private BitSet<T> addLane(java.util.Iterator<? extends T> it) {
            if (!it.hasNext()) {
                return this;
            }
            final int[] lane = laneOf(it);
            // A single mapped null reaches getOrElse/unboxing in the original
            // stream path. The lane's zero marker represents only that case.
            if (lane[0] == 0) {
                throw new NullPointerException();
            }
            final Integer max = lane[1];
            final long[] copy =
                    copyExpand(1 + (Option.some(max).getOrElse(0) >> ADDRESS_BITS_PER_WORD));
            for (int i = 2; i < lane[0] + 2; i++) {
                if (lane[i] < 0) {
                    throw new IllegalArgumentException("bitset element must be >= 0");
                }
                setElement(copy, lane[i]);
            }
            final BitSet<T> bitSet = fromBitMaskNoCopy(copy);
            return (bitSet.length() == length()) ? this : bitSet;
        }

        @SuppressWarnings("unchecked")
        private BitSet<T> addAllStream(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(toInt);
            if (source.isEmpty()) {
                return this;
            } else {
                final long[] copy = copyExpand(1 + (source.max().getOrElse(0) >> ADDRESS_BITS_PER_WORD));
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
            final int bit = element & (BitSetModule.BITS_PER_WORD - 1);
            return index < getWordsNum() && (getWord(index) & (1L << bit)) != 0;
        }

        @Override
        public BitSet<T> init() {
            if (isEmpty()) {
                throw new UnsupportedOperationException("init of empty TreeSet");
            } else {
                final long last = getWord(getWordsNum() - 1);
                final int element = BITS_PER_WORD * (getWordsNum() - 1) + BITS_PER_WORD - Long.numberOfLeadingZeros(last) - 1;
                return remove(fromInt.apply(element));
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new BitSetIterator<>(this);
        }

        //FLLDCIM: HPCPOP - MASK: highest set bit (63 - numberOfLeadingZeros)
        // instead of iterating every element
        @Override
        public T last() {
            if (isEmpty() || !exact()) {
                return BitSet.super.last();
            }
            final int i = lastWordIndex();
            return fromInt.apply((i << ADDRESS_BITS_PER_WORD) + BITS_PER_WORD - 1
                    - Long.numberOfLeadingZeros(getWord(i)));
        }

        //FLLDCIM: HPCPOP - AND word loop for a same-mapping BitSet, otherwise the interface default
        @Override
        public BitSet<T> retainAll(Iterable<? extends T> elements) {
            return !isEmpty() && sameWords(elements)
                    ? andWords((AbstractBitSet<?>) elements)
                    : BitSet.super.retainAll(elements);
        }

        //FLLDCIM: HPCPOP - MASK: AND word loop; this when every bit survives, as filter returns it
        private BitSet<T> andWords(AbstractBitSet<?> that) {
            final long[] and = copyExpand(getWordsNum());
            final long[] other = that.copyExpand(that.getWordsNum());
            final int n = Math.min(and.length, other.length);
            int count = 0;
            for (int i = 0; i < n; i++) {
                and[i] &= other[i];
                count += Long.bitCount(and[i]);
            }
            return count == length() ? this : canonical(and, n);
        }

        //FLLDCIM: HPCPOP - OFFSET_ARRAY prefix popcount + MASK: keep the lowest k set bits
        // (0 < k < length())
        private BitSet<T> lowest(int k) {
            int i = 0;
            int left = k;
            for (int count = Long.bitCount(getWord(0)); left > count;
                    count = Long.bitCount(getWord(++i))) {
                left -= count;
            }
            // Allocate only the retained prefix, even when a sparse suffix is very large.
            final long[] words = new long[i + 1];
            for (int word = 0; word < i; word++) {
                words[word] = getWord(word);
            }
            words[i] = keepLow(getWord(i), left);
            return fromBitMaskNoCopy(words);
        }

        //FLLDCIM: HPCPOP - suffix popcount + MASK: keep the highest k set bits (0 < k < length())
        private BitSet<T> highest(int k) {
            final long[] words = copyExpand(getWordsNum());
            int i = words.length - 1;
            int left = k;
            for (int count = Long.bitCount(words[i]); left > count;
                    count = Long.bitCount(words[--i])) {
                left -= count;
            }
            words[i] = keepHigh(words[i], left);
            java.util.Arrays.fill(words, 0, i, 0L);
            return canonical(words, words.length);
        }

        @Override
        public BitSet<T> take(int n) {
            if (isEmpty() || n >= length()) {
                return this;
            } else if (n <= 0) {
                return createEmpty();
            } else {
                return exact() ? lowest(n) : createFromAll(iterator().take(n));
            }
        }

        @Override
        public BitSet<T> takeRight(int n) {
            if (isEmpty() || n >= length()) {
                return this;
            } else if (n <= 0) {
                return createEmpty();
            } else {
                return exact() ? highest(n) : createFromAll(iterator().takeRight(n));
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
            } else if (sameWords(elements)) {
                return andNotWords((AbstractBitSet<?>) elements);
            } else {
                return removeAllStream(elements);
            }
        }

        //FLLDCIM: HPCPOP - MASK: ANDNOT word loop; a new instance even when nothing is removed,
        // as the stream path
        private BitSet<T> andNotWords(AbstractBitSet<?> that) {
            if (that.isEmpty()) {
                return this;
            }
            final long[] copy = copyExpand(getWordsNum());
            final long[] other = that.copyExpand(that.getWordsNum());
            for (int i = 0, n = Math.min(copy.length, other.length); i < n; i++) {
                copy[i] &= ~other[i];
            }
            return canonical(copy, copy.length);
        }

        private BitSet<T> removeAllStream(Iterable<? extends T> elements) {
            final Stream<Integer> source = Stream.ofAll(elements).map(toInt);
            if (source.isEmpty()) {
                return this;
            } else {
                final int wordsNum = getWordsNum();
                final long[] copy = copyExpand(wordsNum);
                source.forEach(element -> {
                    // The original reference raises an NPE with a null message
                    // when a mapped value is null. An implicit Java 21 unbox
                    // otherwise leaks a VM-specific helpful diagnostic.
                    if (element == null) {
                        throw new NullPointerException();
                    }
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

    class BitSetIterator<T extends Object> extends AbstractIterator<T> {

        private final AbstractBitSet<T> bitSet;

        private long element;
        private int index;

        BitSetIterator(AbstractBitSet<T> bitSet) {
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

    class BitSet1<T extends Object> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements;
        private final int len;

        BitSet1(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long elements) {
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
                    return new BitSet1<>(fromInt, toInt, elements | mask);
                }
            } else {
                return addElement(element);
            }
        }
    }

    class BitSet2<T extends Object> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long elements1, elements2;
        private final int len;

        BitSet2(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long elements1, long elements2) {
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
            final long mask = 1L << element;
            if (element < BITS_PER_WORD) {
                if ((elements1 & mask) != 0) {
                    return this;
                } else {
                    return new BitSet2<>(fromInt, toInt, elements1 | mask, elements2);
                }
            } else if (element < 2 * BITS_PER_WORD) {
                if ((elements2 & mask) != 0) {
                    return this;
                } else {
                    return new BitSet2<>(fromInt, toInt, elements1, elements2 | mask);
                }
            } else {
                return addElement(element);
            }
        }
    }

    class BitSetN<T extends Object> extends AbstractBitSet<T> {

        private static final long serialVersionUID = 1L;

        private final long[] elements;
        private final int len;

        BitSetN(Function1<Integer, T> fromInt, Function1<T, Integer> toInt, long[] elements) {
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
