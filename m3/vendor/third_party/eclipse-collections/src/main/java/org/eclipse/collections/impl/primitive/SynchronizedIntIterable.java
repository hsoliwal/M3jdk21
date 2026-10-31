/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.primitive;

import java.io.Serializable;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.LongIntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * A synchronized view of a IntIterable.
 * This file was automatically generated from template file synchronizedPrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public class SynchronizedIntIterable implements IntIterable, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final IntIterable iterable;

    protected SynchronizedIntIterable(IntIterable iterable)
    {
        this(iterable, null);
    }

    protected SynchronizedIntIterable(IntIterable iterable, Object newLock)
    {
        if (iterable == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedIntIterable on a null collection");
        }
        this.iterable = iterable;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a IntIterable and wrap it directly in a SynchronizedIntIterable.
     */
    public static SynchronizedIntIterable of(IntIterable iterable)
    {
        return new SynchronizedIntIterable(iterable);
    }

    /**
     * This method will take a IntIterable and wrap it directly in a SynchronizedIntIterable. Additionally,
     * a developer specifies which lock to use with the collection.
     */
    public static SynchronizedIntIterable of(IntIterable iterable, Object lock)
    {
        return new SynchronizedIntIterable(iterable, lock);
    }

    @Override
    public int[] toArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray();
        }
    }

    @Override
    public int[] toArray(int[] target)
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray(target);
        }
    }

    @Override
    public boolean contains(int value)
    {
        synchronized (this.lock)
        {
            return this.iterable.contains(value);
        }
    }

    @Override
    public boolean containsAll(int... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(int... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(int... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.iterable.forEach(procedure);
        }
    }

    @Override
    public IntIterable select(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.select(predicate);
        }
    }

    @Override
    public IntIterable reject(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.reject(predicate);
        }
    }

    @Override
    public <V> RichIterable<V> collect(IntToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.collect(function);
        }
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        synchronized (this.lock)
        {
            return this.iterable.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public int count(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableIntList toList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toList();
        }
    }

    @Override
    public MutableIntSet toSet()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSet();
        }
    }

    @Override
    public MutableIntBag toBag()
    {
        synchronized (this.lock)
        {
            return this.iterable.toBag();
        }
    }

    @Override
    public LazyIntIterable asLazy()
    {
        synchronized (this.lock)
        {
            return this.iterable.asLazy();
        }
    }

    @Override
    public long sum()
    {
        synchronized (this.lock)
        {
            return this.iterable.sum();
        }
    }

    @Override
    public int max()
    {
        synchronized (this.lock)
        {
            return this.iterable.max();
        }
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public int min()
    {
        synchronized (this.lock)
        {
            return this.iterable.min();
        }
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.minIfEmpty(defaultValue);
        }
    }

    @Override
    public double average()
    {
        synchronized (this.lock)
        {
            return this.iterable.average();
        }
    }

    @Override
    public double median()
    {
        synchronized (this.lock)
        {
            return this.iterable.median();
        }
    }

    @Override
    public int[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedArray();
        }
    }

    @Override
    public MutableIntList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedList();
        }
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.iterable.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.iterable.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.iterable.notEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongIntToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongIntToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.iterable.chunk(size);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.iterable.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.iterable.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.iterable.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.iterable.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.iterable.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.iterable.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            this.iterable.appendString(appendable, start, separator, end);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public IntIterator intIterator()
    {
        return this.iterable.intIterator();
    }
}
