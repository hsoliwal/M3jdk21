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
import org.eclipse.collections.api.block.comparator.primitive.DoubleComparator;

import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToByteFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToShortFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToIntFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToLongFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanDoubleToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteDoubleToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharDoubleToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatDoubleToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntDoubleToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongDoubleToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortDoubleToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;

/**
 * DoubleIterable is an interface which is memory-optimized for double primitives.
 * It is inspired by the interface RichIterable, and contains a subset of the internal iterator methods on RichIterable like collect, sum, etc.
 * The API also includes an external iterator method, which returns an DoubleIterator. DoubleIterator helps iterate over the DoubleIterable without boxing the primitives.
 * This file was automatically generated from template file primitiveIterable.stg.
 */
public interface DoubleIterable extends PrimitiveIterable
{
    /**
     * Returns a primitive iterator that can be used to iterate over the DoubleIterable in an
     * imperative style.
     */
    DoubleIterator doubleIterator();

    /**
     * Converts the DoubleIterable to a primitive double array.
     */
    double[] toArray();

    /**
     * Converts the DoubleIterable to a primitive double array. If the collection fits into the provided array it is used
     * to store its elements and is returned from the method, otherwise a new array of the appropriate size is allocated
     * and returned. If the iterable is empty, the target array is returned unchanged.
     */
    default double[] toArray(double[] target)
    {
        return this.toList().toArray(target);
    }

    /**
     * Returns true if the value is contained in the DoubleIterable, and false if it is not.
     */
    boolean contains(double value);

    /**
     * Returns true if all of the values specified in the source array are contained
     * in the DoubleIterable, and false if they are not.
     */
    default boolean containsAll(double... source)
    {
        if (this.size() <= 32 || source.length < 4)
        {
            for (double item : source)
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
            DoubleSet set = this instanceof DoubleSet ? (DoubleSet) this : this.toSet();
            for (double item : source)
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
     * Returns true if all of the values specified in the source DoubleIterable are contained
     * in the DoubleIterable, and false if they are not.
     */
    default boolean containsAll(DoubleIterable source)
    {
        if (this.size() <= 32 || source.size() < 4)
        {
            return source.allSatisfy(this::contains);
        }
        else
        {
            DoubleSet set = this instanceof DoubleSet ? (DoubleSet) this : this.toSet();
            return source.allSatisfy(set::contains);
        }
    }

    /**
     * Returns true if any of the values specified in the source array are contained
     * in the DoubleIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(double... source)
    {
        DoubleIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof DoubleSet))
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
     * Returns true if any of the values specified in the source DoubleIterable are contained
     * in the DoubleIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(DoubleIterable source)
    {
        DoubleIterable outside = this;
        DoubleIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof DoubleSet)
        {
            DoubleIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof DoubleSet))
        {
            inside = inside.toSet();
        }
        return outside.anySatisfy(inside::contains);
    }

    /**
     * Returns true if none of the values specified in the source array are contained
     * in the DoubleIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(double... source)
    {
        DoubleIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof DoubleSet))
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
     * Returns true if none of the values specified in the source DoubleIterable are contained
     * in the DoubleIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(DoubleIterable source)
    {
        DoubleIterable outside = this;
        DoubleIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof DoubleSet)
        {
            DoubleIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof DoubleSet))
        {
            inside = inside.toSet();
        }
        return outside.noneSatisfy(inside::contains);
    }

    /**
     * Applies the DoubleProcedure to each element in the DoubleIterable.
     */
    default void forEach(DoubleProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * A synonym for forEach.
     *
     * @since 7.0.
     */
    void each(DoubleProcedure procedure);

    /**
     * @since 9.0.
     */
    default DoubleIterable tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new DoubleIterable with all of the elements in the DoubleIterable that
     * return true for the specified predicate.
     */
    DoubleIterable select(DoublePredicate predicate);

    /**
     * Returns a new DoubleIterable with all of the elements in the DoubleIterable that
     * return false for the specified predicate.
     */
    DoubleIterable reject(DoublePredicate predicate);

    /**
     * Same as {@link #select(DoublePredicate)} , only the results are added to the target MutableDoubleCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableDoubleCollection> R select(DoublePredicate predicate, R target)
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
     * Same as {@link #reject(DoublePredicate)} , only the results are added to the target MutableDoubleCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableDoubleCollection> R reject(DoublePredicate predicate, R target)
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
    <V> RichIterable<V> collect(DoubleToObjectFunction<? extends V> function);

    /**
     * Same as {@link #collect(DoubleToObjectFunction)} , only the results are added to the target Collection.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R collect(DoubleToObjectFunction<? extends V> function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * {@code flatCollect} is a special case of {@link #collect(DoubleToObjectFunction)}. With {@code collect}, when the {@link DoubleToObjectFunction} returns
     * a collection, the result is a collection of collections. {@code flatCollect} outputs a single "flattened" collection
     * instead.  This method is commonly called flatMap.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R flatCollect(DoubleToObjectFunction<? extends Iterable<V>> function, R target)
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
    default <R extends MutableBooleanCollection> R collectBoolean(DoubleToBooleanFunction function, R target)
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
    default <R extends MutableByteCollection> R collectByte(DoubleToByteFunction function, R target)
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
    default <R extends MutableCharCollection> R collectChar(DoubleToCharFunction function, R target)
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
    default <R extends MutableShortCollection> R collectShort(DoubleToShortFunction function, R target)
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
    default <R extends MutableIntCollection> R collectInt(DoubleToIntFunction function, R target)
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
    default <R extends MutableFloatCollection> R collectFloat(DoubleToFloatFunction function, R target)
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
    default <R extends MutableLongCollection> R collectLong(DoubleToLongFunction function, R target)
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
    default <R extends MutableDoubleCollection> R collectDouble(DoubleToDoubleFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    double detectIfNone(DoublePredicate predicate, double ifNone);

    /**
     * Returns a count of the number of elements in the DoubleIterable that return true for the
     * specified predicate.
     */
    int count(DoublePredicate predicate);

    /**
     * Returns true if any of the elements in the DoubleIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean anySatisfy(DoublePredicate predicate);

    /**
     * Returns true if all of the elements in the DoubleIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean allSatisfy(DoublePredicate predicate);

    /**
     * Returns true if none of the elements in the DoubleIterable return true for the
     * specified predicate, otherwise returns false.
     */
    default boolean noneSatisfy(DoublePredicate predicate)
    {
        return !this.anySatisfy(predicate);
    }

    /**
     * Converts the DoubleIterable to a new MutableDoubleList.
     */
    MutableDoubleList toList();

    /**
     * Converts the DoubleIterable to a new MutableDoubleSet.
     */
    MutableDoubleSet toSet();

    /**
     * Converts the DoubleIterable to a new MutableDoubleBag.
     */
    MutableDoubleBag toBag();

    /**
     * Returns a LazyDoubleIterable adapter wrapping the source DoubleIterable.
     */
    LazyDoubleIterable asLazy();

    <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function);

    /**
     * Returns the final boolean result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default boolean injectIntoBoolean(boolean injectedValue, BooleanDoubleToBooleanFunction function)
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
    default byte injectIntoByte(byte injectedValue, ByteDoubleToByteFunction function)
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
    default char injectIntoChar(char injectedValue, CharDoubleToCharFunction function)
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
    default short injectIntoShort(short injectedValue, ShortDoubleToShortFunction function)
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
    default int injectIntoInt(int injectedValue, IntDoubleToIntFunction function)
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
    default float injectIntoFloat(float injectedValue, FloatDoubleToFloatFunction function)
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
    default long injectIntoLong(long injectedValue, LongDoubleToLongFunction function)
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
    default double injectIntoDouble(double injectedValue, DoubleDoubleToDoubleFunction function)
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
     * @see #reduce(DoubleDoubleToDoubleFunction)
     *
     * @since 10.0
     */
    default double reduceIfEmpty(DoubleDoubleToDoubleFunction accumulator, double defaultValue)
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
    default double reduce(DoubleDoubleToDoubleFunction accumulator)
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
     * @return A {@code RichIterable} containing {@code DoubleIterable}s of size {@code size}, except the last will be
     * truncated if the elements don't divide evenly.
     *
     * @since 9.2
     */
    default RichIterable<DoubleIterable> chunk(int size)
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

    double max();

    double maxIfEmpty(double defaultValue);

    double min();

    double minIfEmpty(double defaultValue);

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

    double[] toSortedArray();

    MutableDoubleList toSortedList();

    /**
     * Converts the collection to a MutableDoubleList implementation sorted using the provided comparator.
     */
    default MutableDoubleList toSortedList(DoubleComparator comparator)
    {
        return this.toList().sortThis(comparator);
    }

    /**
     * Converts the collection to a MutableDoubleListImplementation sorted based on the natural order of the key returned
     * by {@code function}.
     */
    default <T> MutableDoubleList toSortedListBy(DoubleToObjectFunction<T> function)
    {
        return this.toList().sortThisBy(function);
    }

    /**
     * Converts the collection to a MutableDoubleList implementation, which is sorted based on the key returned by
     * {@code function} using the provided {@code comparator}.
     */
    default <T> MutableDoubleList toSortedListBy(DoubleToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        return this.toList().sortThisBy(function, comparator);
    }
}
