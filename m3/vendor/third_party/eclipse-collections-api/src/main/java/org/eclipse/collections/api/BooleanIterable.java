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

import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToByteFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToShortFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToCharFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToIntFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToLongFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteBooleanToByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharBooleanToCharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleBooleanToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatBooleanToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntBooleanToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongBooleanToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortBooleanToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;

/**
 * BooleanIterable is an interface which is memory-optimized for boolean primitives.
 * It is inspired by the interface RichIterable, and contains a subset of the internal iterator methods on RichIterable like collect, sum, etc.
 * The API also includes an external iterator method, which returns an BooleanIterator. BooleanIterator helps iterate over the BooleanIterable without boxing the primitives.
 * This file was automatically generated from template file primitiveIterable.stg.
 */
public interface BooleanIterable extends PrimitiveIterable
{
    /**
     * Returns a primitive iterator that can be used to iterate over the BooleanIterable in an
     * imperative style.
     */
    BooleanIterator booleanIterator();

    /**
     * Converts the BooleanIterable to a primitive boolean array.
     */
    boolean[] toArray();

    /**
     * Converts the BooleanIterable to a primitive boolean array. If the collection fits into the provided array it is used
     * to store its elements and is returned from the method, otherwise a new array of the appropriate size is allocated
     * and returned. If the iterable is empty, the target array is returned unchanged.
     */
    default boolean[] toArray(boolean[] target)
    {
        return this.toList().toArray(target);
    }

    /**
     * Returns true if the value is contained in the BooleanIterable, and false if it is not.
     */
    boolean contains(boolean value);

    /**
     * Returns true if all of the values specified in the source array are contained
     * in the BooleanIterable, and false if they are not.
     */
    default boolean containsAll(boolean... source)
    {
        if (this.size() <= 32 || source.length < 4)
        {
            for (boolean item : source)
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
            BooleanSet set = this instanceof BooleanSet ? (BooleanSet) this : this.toSet();
            for (boolean item : source)
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
     * Returns true if all of the values specified in the source BooleanIterable are contained
     * in the BooleanIterable, and false if they are not.
     */
    default boolean containsAll(BooleanIterable source)
    {
        if (this.size() <= 32 || source.size() < 4)
        {
            return source.allSatisfy(this::contains);
        }
        else
        {
            BooleanSet set = this instanceof BooleanSet ? (BooleanSet) this : this.toSet();
            return source.allSatisfy(set::contains);
        }
    }

    /**
     * Returns true if any of the values specified in the source array are contained
     * in the BooleanIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(boolean... source)
    {
        BooleanIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof BooleanSet))
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
     * Returns true if any of the values specified in the source BooleanIterable are contained
     * in the BooleanIterable, and false if they are not.
     *
     * @since 11.0
     */
    default boolean containsAny(BooleanIterable source)
    {
        BooleanIterable outside = this;
        BooleanIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof BooleanSet)
        {
            BooleanIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof BooleanSet))
        {
            inside = inside.toSet();
        }
        return outside.anySatisfy(inside::contains);
    }

    /**
     * Returns true if none of the values specified in the source array are contained
     * in the BooleanIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(boolean... source)
    {
        BooleanIterable inside = this;
        if (this.size() > 32 && source.length > 32 && !(this instanceof BooleanSet))
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
     * Returns true if none of the values specified in the source BooleanIterable are contained
     * in the BooleanIterable, and false if they are.
     *
     * @since 11.0
     */
    default boolean containsNone(BooleanIterable source)
    {
        BooleanIterable outside = this;
        BooleanIterable inside = source;
        if (this.size() < source.size())
        {
            outside = source;
            inside = this;
        }
        if (outside instanceof BooleanSet)
        {
            BooleanIterable temp = outside;
            outside = inside;
            inside = temp;
        }
        else if (inside.size() > 32 && !(inside instanceof BooleanSet))
        {
            inside = inside.toSet();
        }
        return outside.noneSatisfy(inside::contains);
    }

    /**
     * Applies the BooleanProcedure to each element in the BooleanIterable.
     */
    default void forEach(BooleanProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * A synonym for forEach.
     *
     * @since 7.0.
     */
    void each(BooleanProcedure procedure);

    /**
     * @since 9.0.
     */
    default BooleanIterable tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Returns a new BooleanIterable with all of the elements in the BooleanIterable that
     * return true for the specified predicate.
     */
    BooleanIterable select(BooleanPredicate predicate);

    /**
     * Returns a new BooleanIterable with all of the elements in the BooleanIterable that
     * return false for the specified predicate.
     */
    BooleanIterable reject(BooleanPredicate predicate);

    /**
     * Same as {@link #select(BooleanPredicate)} , only the results are added to the target MutableBooleanCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableBooleanCollection> R select(BooleanPredicate predicate, R target)
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
     * Same as {@link #reject(BooleanPredicate)} , only the results are added to the target MutableBooleanCollection.
     *
     * @since 8.1.
     */
    default <R extends MutableBooleanCollection> R reject(BooleanPredicate predicate, R target)
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
    <V> RichIterable<V> collect(BooleanToObjectFunction<? extends V> function);

    /**
     * Same as {@link #collect(BooleanToObjectFunction)} , only the results are added to the target Collection.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R collect(BooleanToObjectFunction<? extends V> function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    /**
     * {@code flatCollect} is a special case of {@link #collect(BooleanToObjectFunction)}. With {@code collect}, when the {@link BooleanToObjectFunction} returns
     * a collection, the result is a collection of collections. {@code flatCollect} outputs a single "flattened" collection
     * instead.  This method is commonly called flatMap.
     *
     * @since 8.1.
     */
    default <V, R extends Collection<V>> R flatCollect(BooleanToObjectFunction<? extends Iterable<V>> function, R target)
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
    default <R extends MutableBooleanCollection> R collectBoolean(BooleanToBooleanFunction function, R target)
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
    default <R extends MutableByteCollection> R collectByte(BooleanToByteFunction function, R target)
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
    default <R extends MutableCharCollection> R collectChar(BooleanToCharFunction function, R target)
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
    default <R extends MutableShortCollection> R collectShort(BooleanToShortFunction function, R target)
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
    default <R extends MutableIntCollection> R collectInt(BooleanToIntFunction function, R target)
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
    default <R extends MutableFloatCollection> R collectFloat(BooleanToFloatFunction function, R target)
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
    default <R extends MutableLongCollection> R collectLong(BooleanToLongFunction function, R target)
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
    default <R extends MutableDoubleCollection> R collectDouble(BooleanToDoubleFunction function, R target)
    {
        this.each(each -> target.add(function.valueOf(each)));
        return target;
    }

    boolean detectIfNone(BooleanPredicate predicate, boolean ifNone);

    /**
     * Returns a count of the number of elements in the BooleanIterable that return true for the
     * specified predicate.
     */
    int count(BooleanPredicate predicate);

    /**
     * Returns true if any of the elements in the BooleanIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean anySatisfy(BooleanPredicate predicate);

    /**
     * Returns true if all of the elements in the BooleanIterable return true for the
     * specified predicate, otherwise returns false.
     */
    boolean allSatisfy(BooleanPredicate predicate);

    /**
     * Returns true if none of the elements in the BooleanIterable return true for the
     * specified predicate, otherwise returns false.
     */
    default boolean noneSatisfy(BooleanPredicate predicate)
    {
        return !this.anySatisfy(predicate);
    }

    /**
     * Converts the BooleanIterable to a new MutableBooleanList.
     */
    MutableBooleanList toList();

    /**
     * Converts the BooleanIterable to a new MutableBooleanSet.
     */
    MutableBooleanSet toSet();

    /**
     * Converts the BooleanIterable to a new MutableBooleanBag.
     */
    MutableBooleanBag toBag();

    /**
     * Returns a LazyBooleanIterable adapter wrapping the source BooleanIterable.
     */
    LazyBooleanIterable asLazy();

    <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function);

    /**
     * Returns the final boolean result of evaluating function using each element of the iterable and the previous evaluation
     * result as the parameters. The injected value is used for the first parameter of the first evaluation, and the current
     * item in the iterable is used as the second parameter.
     *
     * @since 11.1
     */
    default boolean injectIntoBoolean(boolean injectedValue, BooleanBooleanToBooleanFunction function)
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
    default byte injectIntoByte(byte injectedValue, ByteBooleanToByteFunction function)
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
    default char injectIntoChar(char injectedValue, CharBooleanToCharFunction function)
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
    default short injectIntoShort(short injectedValue, ShortBooleanToShortFunction function)
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
    default int injectIntoInt(int injectedValue, IntBooleanToIntFunction function)
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
    default float injectIntoFloat(float injectedValue, FloatBooleanToFloatFunction function)
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
    default long injectIntoLong(long injectedValue, LongBooleanToLongFunction function)
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
    default double injectIntoDouble(double injectedValue, DoubleBooleanToDoubleFunction function)
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
     * @see #reduce(BooleanBooleanToBooleanFunction)
     *
     * @since 10.0
     */
    default boolean reduceIfEmpty(BooleanBooleanToBooleanFunction accumulator, boolean defaultValue)
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
    default boolean reduce(BooleanBooleanToBooleanFunction accumulator)
    {
        boolean[] seenOne = new boolean[1];
        boolean[] result = new boolean[1];
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
     * @return A {@code RichIterable} containing {@code BooleanIterable}s of size {@code size}, except the last will be
     * truncated if the elements don't divide evenly.
     *
     * @since 9.2
     */
    default RichIterable<BooleanIterable> chunk(int size)
    {
        throw new UnsupportedOperationException("Default method to prevent breaking backwards compatibility");
    }
}
