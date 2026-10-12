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
import java.util.DoubleSummaryStatistics;

import java.util.Comparator;
import org.eclipse.collections.api.block.comparator.primitive.FloatComparator;

import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToByteFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToShortFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToCharFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToIntFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToLongFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFloatToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFloatToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharFloatToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntFloatToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongFloatToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFloatToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * FloatIterable is an interface which is memory-optimized for float primitives.
 * It is inspired by the interface RichIterable, and contains a subset of the internal iterator methods on RichIterable like collect, sum, etc.
 * The API also includes an external iterator method, which returns an FloatIterator. FloatIterator helps iterate over the FloatIterable without boxing the primitives.
 * This file was automatically generated from template file primitiveIterable.stg.
 */
public interface FloatIterable extends PrimitiveIterable
{
    /**
     * Returns a primitive iterator that can be used to iterate over the FloatIterable in an
     * imperative style.
     */
    FloatIterator floatIterator();

    /**
     * Converts the FloatIterable to a primitive float array.
     */
    float[] toArray();

    /**
     * Converts the FloatIterable to a primitive float array. If the collection fits into the provided array it is used
     * to store its elements and is returned from the method, otherwise a new array of the appropriate size is allocated
     * and returned. If the iterable is empty, the target array is returned unchanged.
     */
    default float[] toArray(float[] target)
    {
        return this.toList().toArray(target);
    }

    /**
     * Returns true if the value is contained in the FloatIterable, and false if it is not.
     */
    boolean contains(float value);

    /**
     * Returns true if all of the values specified in the source array are contained
     * in the FloatIterable, and false if they are not.
     */
    default boolean containsAll(float... source)
    {
        if (this.size() <= 32 || source.length < 4)
        {
            for (float item : source)
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
            FloatSet set = this instanceof FloatSet ? (FloatSet) this : this.toSet();
            for (float item : source)
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
     * Returns true if all of the values specified in the source FloatIterable are contained
     * in the FloatIterable, and false if they are not.
     */
    default boolean containsAll(FloatIterable source)
    {
        if (this.size() <= 32 || source.size() < 4)
        {
            return source.allSatisfy(this::contains);
        }
        else
        {
            FloatSet set = this instanceof FloatSet ? (FloatSet) this : this.toSet();
            return source.allSatisfy(set::contains);
        }
    }

    /**
     * Returns true if any of the values specified in the source array are contained
     * in the FloatIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(float... source)
    {
        FloatIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof FloatSet))
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
     * Returns true if any of the values specified in the source FloatIterable are contained
     * in the FloatIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(FloatIterable source)
    {
        FloatIterable outside = this;
        FloatIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof FloatSet)
        {
            FloatIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof FloatSet))
        {
            inside = inside.toSet();
        }
        return outside.anySatisfy(inside::contains);
    }

    /**
     * Returns true if none of the values specified in the source array are contained
     * in the FloatIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(float... source)
    {
        FloatIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof FloatSet))
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
     * Returns true if none of the values specified in the source FloatIterable are contained
     * in the FloatIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(FloatIterable source)
    {
        FloatIterable outside = this;
        FloatIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof FloatSet)
        {
            FloatIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof FloatSet))
        {
            inside = inside.toSet();
        }
        return outside.noneSatisfy(inside::contains);
    }

    /**
     * Applies the FloatProcedure to each element in the FloatIterable.
     */
    default void forEach(FloatProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * A synonym for forEach.
     *
     * @since 7.0.
     */
    void each(FloatProcedure procedure);

    /**
     * @since 9.0.
     */
    default FloatIterable tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new FloatIterable with all of the elements in the FloatIterable that
     * return true for the specified predicate.
     */
    FloatIterable select(FloatPredicate predicate);

    /**
     * Returns a new FloatIterable with all of the elements in the FloatIterable that
     * return false for the specified predicate.
     */
    FloatIterable reject(FloatPredicate predicate);

    /**
     * Same as {@link #select(FloatPredicate)} , only the results are added to the target MutableFloatCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableFloatCollection> R select(FloatPredicate predicate, R target)
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
     * Same as {@link #reject(FloatPredicate)} , only the results are added to the target MutableFloatCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableFloatCollection> R reject(FloatPredicate predicate, R target)
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
    <V> RichIterable<V> collect(FloatToObjectFunction<? extends V> function);

    /**
     * Same as {@link #collect(FloatToObjectFunction)} , only the results are added to the target Collection.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R collect(FloatToObjectFunction<? extends V> function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * {@code flatCollect} is a special case of {@link #collect(FloatToObjectFunction)}. With {@code collect}, when the {@link FloatToObjectFunction} returns
     * a collection, the result is a collection of collections. {@code flatCollect} outputs a single "flattened" collection
     * instead.  This method is commonly called flatMap.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R flatCollect(FloatToObjectFunction<? extends Iterable<V>> function, R target)
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
    default <R extends MutableBooleanCollection> R collectBoolean(FloatToBooleanFunction function, R target)
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
    default <R extends MutableByteCollection> R collectByte(FloatToByteFunction function, R target)
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
    default <R extends MutableCharCollection> R collectChar(FloatToCharFunction function, R target)
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
    default <R extends MutableShortCollection> R collectShort(FloatToShortFunction function, R target)
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
    default <R extends MutableIntCollection> R collectInt(FloatToIntFunction function, R target)
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
    default <R extends MutableFloatCollection> R collectFloat(FloatToFloatFunction function, R target)
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
    default <R extends MutableLongCollection> R collectLong(FloatToLongFunction function, R target)
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
    default <R extends MutableDoubleCollection> R collectDouble(FloatToDoubleFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    float detectIfNone(FloatPredicate predicate, float ifNone);

    /**
     * Returns a count of the number of elements in the FloatIterable that return true for the
     * specified predicate.
     */
    int count(FloatPredicate predicate);

    /**
     * Returns true if any of the elements in the FloatIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean anySatisfy(FloatPredicate predicate);

    /**
     * Returns true if all of the elements in the FloatIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean allSatisfy(FloatPredicate predicate);

    /**
     * Returns true if none of the elements in the FloatIterable return true for the
     * specified predicate, otherwise returns false.
     */
    default boolean noneSatisfy(FloatPredicate predicate)
    {
        return !this.anySatisfy(predicate);
    }

    /**
     * Converts the FloatIterable to a new MutableFloatList.
     */
    MutableFloatList toList();

    /**
     * Converts the FloatIterable to a new MutableFloatSet.
     */
    MutableFloatSet toSet();

    /**
     * Converts the FloatIterable to a new MutableFloatBag.
     */
    MutableFloatBag toBag();

    /**
     * Returns a LazyFloatIterable adapter wrapping the source FloatIterable.
     */
    LazyFloatIterable asLazy();

    <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function);

    /**
     * Returns the final boolean result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default boolean injectIntoBoolean(boolean injectedValue, BooleanFloatToBooleanFunction function)
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
    default byte injectIntoByte(byte injectedValue, ByteFloatToByteFunction function)
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
    default char injectIntoChar(char injectedValue, CharFloatToCharFunction function)
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
    default short injectIntoShort(short injectedValue, ShortFloatToShortFunction function)
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
    default int injectIntoInt(int injectedValue, IntFloatToIntFunction function)
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
    default float injectIntoFloat(float injectedValue, FloatFloatToFloatFunction function)
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
    default long injectIntoLong(long injectedValue, LongFloatToLongFunction function)
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
    default double injectIntoDouble(double injectedValue, DoubleFloatToDoubleFunction function)
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
     * @see #reduce(DoubleFloatToDoubleFunction)
     *
     * @since 10.0
     */
    default double reduceIfEmpty(DoubleFloatToDoubleFunction accumulator, double defaultValue)
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
    default double reduce(DoubleFloatToDoubleFunction accumulator)
    {
        boolean[] seenOne = new boolean[1];
        double[] result = new double[1];
        this.each(each ->
        {
            if (seenOne[0])
            {
                result[0] = accumulator.valueOf(result[0], each);
            }
            else
            {
                seenOne[0] = true;
                result[0] = (double) each;
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
     * @return A {@code RichIterable} containing {@code FloatIterable}s of size {@code size}, except the last will be
     * truncated if the elements don't divide evenly.
     *
     * @since 9.2
     */
    default RichIterable<FloatIterable> chunk(int size)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }

    double sum();

    /**
     * @since 8.0
     */
    default DoubleSummaryStatistics summaryStatistics()
    {
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        this.forEach(stats::accept);
        return stats;
    }

    float max();

    float maxIfEmpty(float defaultValue);

    float min();

    float minIfEmpty(float defaultValue);

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

    float[] toSortedArray();

    MutableFloatList toSortedList();

    /**
     * Converts the collection to a MutableFloatList implementation sorted using the provided comparator.
     */
    default MutableFloatList toSortedList(FloatComparator comparator)
    {
        return this.toList().sortThis(comparator);
    }

    /**
     * Converts the collection to a MutableFloatListImplementation sorted based on the natural order of the key returned
     * by {@code function}.
     */
    default <T> MutableFloatList toSortedListBy(FloatToObjectFunction<T> function)
    {
        return this.toList().sortThisBy(function);
    }

    /**
     * Converts the collection to a MutableFloatList implementation, which is sorted based on the key returned by
     * {@code function} using the provided {@code comparator}.
     */
    default <T> MutableFloatList toSortedListBy(FloatToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.toList().sortThisBy(function, comparator);
    }
}
