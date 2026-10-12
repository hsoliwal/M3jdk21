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
import java.util.IntSummaryStatistics;

import java.util.Comparator;
import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.IntToByteFunction;
import org.eclipse.collections.api.block.function.primitive.IntToShortFunction;
import org.eclipse.collections.api.block.function.primitive.IntToCharFunction;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanIntToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteIntToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharIntToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatIntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntIntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongIntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortIntToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * IntIterable is an interface which is memory-optimized for int primitives.
 * It is inspired by the interface RichIterable, and contains a subset of the internal iterator methods on RichIterable like collect, sum, etc.
 * The API also includes an external iterator method, which returns an IntIterator. IntIterator helps iterate over the IntIterable without boxing the primitives.
 * This file was automatically generated from template file primitiveIterable.stg.
 */
public interface IntIterable extends PrimitiveIterable
{
    /**
     * Returns a primitive iterator that can be used to iterate over the IntIterable in an
     * imperative style.
     */
    IntIterator intIterator();

    /**
     * Converts the IntIterable to a primitive int array.
     */
    int[] toArray();

    /**
     * Converts the IntIterable to a primitive int array. If the collection fits into the provided array it is used
     * to store its elements and is returned from the method, otherwise a new array of the appropriate size is allocated
     * and returned. If the iterable is empty, the target array is returned unchanged.
     */
    default int[] toArray(int[] target)
    {
        return this.toList().toArray(target);
    }

    /**
     * Returns true if the value is contained in the IntIterable, and false if it is not.
     */
    boolean contains(int value);

    /**
     * Returns true if all of the values specified in the source array are contained
     * in the IntIterable, and false if they are not.
     */
    default boolean containsAll(int... source)
    {
        if (this.size() <= 32 || source.length < 4)
        {
            for (int item : source)
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
            IntSet set = this instanceof IntSet ? (IntSet) this : this.toSet();
            for (int item : source)
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
     * Returns true if all of the values specified in the source IntIterable are contained
     * in the IntIterable, and false if they are not.
     */
    default boolean containsAll(IntIterable source)
    {
        if (this.size() <= 32 || source.size() < 4)
        {
            return source.allSatisfy(this::contains);
        }
        else
        {
            IntSet set = this instanceof IntSet ? (IntSet) this : this.toSet();
            return source.allSatisfy(set::contains);
        }
    }

    /**
     * Returns true if any of the values specified in the source array are contained
     * in the IntIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(int... source)
    {
        IntIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof IntSet))
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
     * Returns true if any of the values specified in the source IntIterable are contained
     * in the IntIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(IntIterable source)
    {
        IntIterable outside = this;
        IntIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof IntSet)
        {
            IntIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof IntSet))
        {
            inside = inside.toSet();
        }
        return outside.anySatisfy(inside::contains);
    }

    /**
     * Returns true if none of the values specified in the source array are contained
     * in the IntIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(int... source)
    {
        IntIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof IntSet))
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
     * Returns true if none of the values specified in the source IntIterable are contained
     * in the IntIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(IntIterable source)
    {
        IntIterable outside = this;
        IntIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof IntSet)
        {
            IntIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof IntSet))
        {
            inside = inside.toSet();
        }
        return outside.noneSatisfy(inside::contains);
    }

    /**
     * Applies the IntProcedure to each element in the IntIterable.
     */
    default void forEach(IntProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * A synonym for forEach.
     *
     * @since 7.0.
     */
    void each(IntProcedure procedure);

    /**
     * @since 9.0.
     */
    default IntIterable tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new IntIterable with all of the elements in the IntIterable that
     * return true for the specified predicate.
     */
    IntIterable select(IntPredicate predicate);

    /**
     * Returns a new IntIterable with all of the elements in the IntIterable that
     * return false for the specified predicate.
     */
    IntIterable reject(IntPredicate predicate);

    /**
     * Same as {@link #select(IntPredicate)} , only the results are added to the target MutableIntCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableIntCollection> R select(IntPredicate predicate, R target)
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
     * Same as {@link #reject(IntPredicate)} , only the results are added to the target MutableIntCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableIntCollection> R reject(IntPredicate predicate, R target)
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
    <V> RichIterable<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Same as {@link #collect(IntToObjectFunction)} , only the results are added to the target Collection.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R collect(IntToObjectFunction<? extends V> function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * {@code flatCollect} is a special case of {@link #collect(IntToObjectFunction)}. With {@code collect}, when the {@link IntToObjectFunction} returns
     * a collection, the result is a collection of collections. {@code flatCollect} outputs a single "flattened" collection
     * instead.  This method is commonly called flatMap.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R flatCollect(IntToObjectFunction<? extends Iterable<V>> function, R target)
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
    default <R extends MutableBooleanCollection> R collectBoolean(IntToBooleanFunction function, R target)
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
    default <R extends MutableByteCollection> R collectByte(IntToByteFunction function, R target)
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
    default <R extends MutableCharCollection> R collectChar(IntToCharFunction function, R target)
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
    default <R extends MutableShortCollection> R collectShort(IntToShortFunction function, R target)
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
    default <R extends MutableIntCollection> R collectInt(IntToIntFunction function, R target)
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
    default <R extends MutableFloatCollection> R collectFloat(IntToFloatFunction function, R target)
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
    default <R extends MutableLongCollection> R collectLong(IntToLongFunction function, R target)
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
    default <R extends MutableDoubleCollection> R collectDouble(IntToDoubleFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    int detectIfNone(IntPredicate predicate, int ifNone);

    /**
     * Returns a count of the number of elements in the IntIterable that return true for the
     * specified predicate.
     */
    int count(IntPredicate predicate);

    /**
     * Returns true if any of the elements in the IntIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean anySatisfy(IntPredicate predicate);

    /**
     * Returns true if all of the elements in the IntIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean allSatisfy(IntPredicate predicate);

    /**
     * Returns true if none of the elements in the IntIterable return true for the
     * specified predicate, otherwise returns false.
     */
    default boolean noneSatisfy(IntPredicate predicate)
    {
        return !this.anySatisfy(predicate);
    }

    /**
     * Converts the IntIterable to a new MutableIntList.
     */
    MutableIntList toList();

    /**
     * Converts the IntIterable to a new MutableIntSet.
     */
    MutableIntSet toSet();

    /**
     * Converts the IntIterable to a new MutableIntBag.
     */
    MutableIntBag toBag();

    /**
     * Returns a LazyIntIterable adapter wrapping the source IntIterable.
     */
    LazyIntIterable asLazy();

    <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function);

    /**
     * Returns the final boolean result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default boolean injectIntoBoolean(boolean injectedValue, BooleanIntToBooleanFunction function)
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
    default byte injectIntoByte(byte injectedValue, ByteIntToByteFunction function)
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
    default char injectIntoChar(char injectedValue, CharIntToCharFunction function)
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
    default short injectIntoShort(short injectedValue, ShortIntToShortFunction function)
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
    default int injectIntoInt(int injectedValue, IntIntToIntFunction function)
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
    default float injectIntoFloat(float injectedValue, FloatIntToFloatFunction function)
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
    default long injectIntoLong(long injectedValue, LongIntToLongFunction function)
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
    default double injectIntoDouble(double injectedValue, DoubleIntToDoubleFunction function)
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
     * @see #reduce(LongIntToLongFunction)
     *
     * @since 10.0
     */
    default long reduceIfEmpty(LongIntToLongFunction accumulator, long defaultValue)
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
    default long reduce(LongIntToLongFunction accumulator)
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
                result[0] = (long) each;
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
     * @return A {@code RichIterable} containing {@code IntIterable}s of size {@code size}, except the last will be
     * truncated if the elements don't divide evenly.
     *
     * @since 9.2
     */
    default RichIterable<IntIterable> chunk(int size)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    long sum();

    /**
     * @since 8.0
     */
    default IntSummaryStatistics summaryStatistics()
    {
        IntSummaryStatistics stats = new IntSummaryStatistics();
        this.forEach(stats::accept);
        return stats;
    }

    int max();

    int maxIfEmpty(int defaultValue);

    int min();

    int minIfEmpty(int defaultValue);

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

    int[] toSortedArray();

    MutableIntList toSortedList();

    /**
     * Converts the collection to a MutableIntList implementation sorted using the provided comparator.
     */
    default MutableIntList toSortedList(IntComparator comparator)
    {
        return this.toList().sortThis(comparator);
    }

    /**
     * Converts the collection to a MutableIntListImplementation sorted based on the natural order of the key returned
     * by {@code function}.
     */
    default <T> MutableIntList toSortedListBy(IntToObjectFunction<T> function)
    {
        return this.toList().sortThisBy(function);
    }

    /**
     * Converts the collection to a MutableIntList implementation, which is sorted based on the key returned by
     * {@code function} using the provided {@code comparator}.
     */
    default <T> MutableIntList toSortedListBy(IntToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.toList().sortThisBy(function, comparator);
    }
}
