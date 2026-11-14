/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.LongSummaryStatistics;

import java.util.Comparator;
import org.eclipse.collections.api.block.comparator.primitive.LongComparator;

import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.LongToByteFunction;
import org.eclipse.collections.api.block.function.primitive.LongToShortFunction;
import org.eclipse.collections.api.block.function.primitive.LongToCharFunction;
import org.eclipse.collections.api.block.function.primitive.LongToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanLongToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteLongToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharLongToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleLongToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatLongToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntLongToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortLongToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * LongIterable is an interface which is memory-optimized for long primitives.
 * It is inspired by the interface RichIterable, and contains a subset of the internal iterator methods on RichIterable like collect, sum, etc.
 * The API also includes an external iterator method, which returns an LongIterator. LongIterator helps iterate over the LongIterable without boxing the primitives.
 * This file was automatically generated from template file primitiveIterable.stg.
 */
public interface LongIterable extends PrimitiveIterable
{
    /**
     * Returns a primitive iterator that can be used to iterate over the LongIterable in an
     * imperative style.
     */
    LongIterator longIterator();

    /**
     * Converts the LongIterable to a primitive long array.
     */
    long[] toArray();

    /**
     * Converts the LongIterable to a primitive long array. If the collection fits into the provided array it is used
     * to store its elements and is returned from the method, otherwise a new array of the appropriate size is allocated
     * and returned. If the iterable is empty, the target array is returned unchanged.
     */
    default long[] toArray(long[] target)
    {
        return this.toList().toArray(target);
    }

    /**
     * Returns true if the value is contained in the LongIterable, and false if it is not.
     */
    boolean contains(long value);

    /**
     * Returns true if all of the values specified in the source array are contained
     * in the LongIterable, and false if they are not.
     */
    default boolean containsAll(long... source)
    {
        if (this.size() <= 32 || source.length < 4)
        {
            for (long item : source)
            {
                if (!this.contains(item))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            LongSet set = this instanceof LongSet ? (LongSet) this : this.toSet();
            for (long item : source)
            {
                if (!set.contains(item))
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Returns true if all of the values specified in the source LongIterable are contained
     * in the LongIterable, and false if they are not.
     */
    default boolean containsAll(LongIterable source)
    {
        if (this.size() <= 32 || source.size() < 4)
        {
            return source.allSatisfy(this::contains);
        }
        else
        {
            LongSet set = this instanceof LongSet ? (LongSet) this : this.toSet();
            return source.allSatisfy(set::contains);
        }
    }

    /**
     * Returns true if any of the values specified in the source array are contained
     * in the LongIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(long... source)
    {
        LongIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof LongSet))
        {
            inside = this.toSet();
        }
        for (int i = 0; i < source.length; i++)
        {
            if (inside.contains(source[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any of the values specified in the source LongIterable are contained
     * in the LongIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(LongIterable source)
    {
        LongIterable outside = this;
        LongIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof LongSet)
        {
            LongIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof LongSet))
        {
            inside = inside.toSet();
        }
        return outside.anySatisfy(inside::contains);
    }

    /**
     * Returns true if none of the values specified in the source array are contained
     * in the LongIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(long... source)
    {
        LongIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof LongSet))
        {
            inside = this.toSet();
        }
        for (int i = 0; i < source.length; i++)
        {
            if (inside.contains(source[i]))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if none of the values specified in the source LongIterable are contained
     * in the LongIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(LongIterable source)
    {
        LongIterable outside = this;
        LongIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof LongSet)
        {
            LongIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof LongSet))
        {
            inside = inside.toSet();
        }
        return outside.noneSatisfy(inside::contains);
    }

    /**
     * Applies the LongProcedure to each element in the LongIterable.
     */
    default void forEach(LongProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * A synonym for forEach.
     *
     * @since 7.0.
     */
    void each(LongProcedure procedure);

    /**
     * @since 9.0.
     */
    default LongIterable tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new LongIterable with all of the elements in the LongIterable that
     * return true for the specified predicate.
     */
    LongIterable select(LongPredicate predicate);

    /**
     * Returns a new LongIterable with all of the elements in the LongIterable that
     * return false for the specified predicate.
     */
    LongIterable reject(LongPredicate predicate);

    /**
     * Same as {@link #select(LongPredicate)} , only the results are added to the target MutableLongCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableLongCollection> R select(LongPredicate predicate, R target)
    {
        this.each(each ->
        {
            if (predicate.accept(each))
            {
                target.add(each);
            }
        });
        return target;
    }

    /**
     * Same as {@link #reject(LongPredicate)} , only the results are added to the target MutableLongCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableLongCollection> R reject(LongPredicate predicate, R target)
    {
        this.each(each ->
        {
            if (!predicate.accept(each))
            {
                target.add(each);
            }
        });
        return target;
    }

    /**
     * Returns a new collection with the results of applying the specified function on each element of the source
     * collection.  This method is also commonly called transform or map.
     */
    <V> RichIterable<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Same as {@link #collect(LongToObjectFunction)} , only the results are added to the target Collection.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R collect(LongToObjectFunction<? extends V> function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * {@code flatCollect} is a special case of {@link #collect(LongToObjectFunction)}. With {@code collect}, when the {@link LongToObjectFunction} returns
     * a collection, the result is a collection of collections. {@code flatCollect} outputs a single "flattened" collection
     * instead.  This method is commonly called flatMap.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R flatCollect(LongToObjectFunction<? extends Iterable<V>> function, R target)
    {
        this.each(each ->
        {
            Iterable<V> iterable = function.valueOf(each);
            if (iterable instanceof Collection)
            {
                target.addAll((Collection<V>) iterable);
            }
            else
            {
                iterable.forEach(target::add);
            }
        });
        return target;
    }

    /**
     * Returns the target {@code MutableBooleanCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableBooleanCollection> R collectBoolean(LongToBooleanFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableByteCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableByteCollection> R collectByte(LongToByteFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableCharCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableCharCollection> R collectChar(LongToCharFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableShortCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableShortCollection> R collectShort(LongToShortFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableIntCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableIntCollection> R collectInt(LongToIntFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableFloatCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableFloatCollection> R collectFloat(LongToFloatFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableLongCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableLongCollection> R collectLong(LongToLongFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * Returns the target {@code MutableDoubleCollection} with the results of applying the specified function on each element
     * of the source collection.
     *
     * @since 8.1.
     */
    default <R extends MutableDoubleCollection> R collectDouble(LongToDoubleFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    long detectIfNone(LongPredicate predicate, long ifNone);

    /**
     * Returns a count of the number of elements in the LongIterable that return true for the
     * specified predicate.
     */
    int count(LongPredicate predicate);

    /**
     * Returns true if any of the elements in the LongIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean anySatisfy(LongPredicate predicate);

    /**
     * Returns true if all of the elements in the LongIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean allSatisfy(LongPredicate predicate);

    /**
     * Returns true if none of the elements in the LongIterable return true for the
     * specified predicate, otherwise returns false.
     */
    default boolean noneSatisfy(LongPredicate predicate)
    {
        return !this.anySatisfy(predicate);
    }

    /**
     * Converts the LongIterable to a new MutableLongList.
     */
    MutableLongList toList();

    /**
     * Converts the LongIterable to a new MutableLongSet.
     */
    MutableLongSet toSet();

    /**
     * Converts the LongIterable to a new MutableLongBag.
     */
    MutableLongBag toBag();

    /**
     * Returns a LazyLongIterable adapter wrapping the source LongIterable.
     */
    LazyLongIterable asLazy();

    <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function);

    /**
     * Returns the final boolean result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default boolean injectIntoBoolean(boolean injectedValue, BooleanLongToBooleanFunction function)
    {
        boolean[] result = new boolean[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final byte result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default byte injectIntoByte(byte injectedValue, ByteLongToByteFunction function)
    {
        byte[] result = new byte[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final char result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default char injectIntoChar(char injectedValue, CharLongToCharFunction function)
    {
        char[] result = new char[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final short result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default short injectIntoShort(short injectedValue, ShortLongToShortFunction function)
    {
        short[] result = new short[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final int result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default int injectIntoInt(int injectedValue, IntLongToIntFunction function)
    {
        int[] result = new int[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final float result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default float injectIntoFloat(float injectedValue, FloatLongToFloatFunction function)
    {
        float[] result = new float[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final long result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default long injectIntoLong(long injectedValue, LongLongToLongFunction function)
    {
        long[] result = new long[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * Returns the final double result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default double injectIntoDouble(double injectedValue, DoubleLongToDoubleFunction function)
    {
        double[] result = new double[1];
        result[0] = injectedValue;
        this.each(each ->
        {
            result[0] = function.valueOf(result[0], each);
        });
        return result[0];
    }

    /**
     * @see #reduce(LongLongToLongFunction)
     *
     * @since 10.0
     */
    default long reduceIfEmpty(LongLongToLongFunction accumulator, long defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        else
        {
            return this.reduce(accumulator);
        }
    }

    /**
     * @see RichIterable#reduce(BinaryOperator)
     *
     * @since 10.0
     */
    default long reduce(LongLongToLongFunction accumulator)
    {
        boolean[] seenOne = new boolean[1];
        long[] result = new long[1];
        this.each(each ->
        {
            if (seenOne[0])
            {
                result[0] = accumulator.valueOf(result[0], each);
            }
            else
            {
                seenOne[0] = true;
                result[0] = each;
            }
        });
        if (!seenOne[0])
        {
            throw new NoSuchElementException();
        }
        else
        {
            return result[0];
        }
    }

    /**
     * Partitions elements in fixed size chunks.
     *
     * @param size the number of elements per chunk
     *
     * @return A {@code RichIterable} containing {@code LongIterable}s of size {@code size}, except the last will be
     * truncated if the elements don't divide evenly.
     *
     * @since 9.2
     */
    default RichIterable<LongIterable> chunk(int size)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    long sum();

    /**
     * @since 8.0
     */
    default LongSummaryStatistics summaryStatistics()
    {
        LongSummaryStatistics stats = new LongSummaryStatistics();
        this.forEach(stats::accept);
        return stats;
    }

    long max();

    long maxIfEmpty(long defaultValue);

    long min();

    long minIfEmpty(long defaultValue);

    double average();

    /**
     * @since 9.0
     */
    default double averageIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.average();
    }

    double median();

    /**
     * @since 9.0
     */
    default double medianIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.median();
    }

    long[] toSortedArray();

    MutableLongList toSortedList();

    /**
     * Converts the collection to a MutableLongList implementation sorted using the provided comparator.
     */
    default MutableLongList toSortedList(LongComparator comparator)
    {
        return this.toList().sortThis(comparator);
    }

    /**
     * Converts the collection to a MutableLongListImplementation sorted based on the natural order of the key returned
     * by {@code function}.
     */
    default <T> MutableLongList toSortedListBy(LongToObjectFunction<T> function)
    {
        return this.toList().sortThisBy(function);
    }

    /**
     * Converts the collection to a MutableLongList implementation, which is sorted based on the key returned by
     * {@code function} using the provided {@code comparator}.
     */
    default <T> MutableLongList toSortedListBy(LongToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.toList().sortThisBy(function, comparator);
    }
}
