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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.LongShortToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * A synchronized view of a ShortIterable.
 * This file was automatically generated from template file synchronizedPrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public class SynchronizedShortIterable implements ShortIterable, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final ShortIterable iterable;

    protected SynchronizedShortIterable(ShortIterable iterable)
    {
        this(iterable, null);
    }

    protected SynchronizedShortIterable(ShortIterable iterable, Object newLock)
    {
        if (iterable == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedShortIterable on a null collection");
        }
        this.iterable = iterable;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a ShortIterable and wrap it directly in a SynchronizedShortIterable.
     */
    public static SynchronizedShortIterable of(ShortIterable iterable)
    {
        return new SynchronizedShortIterable(iterable);
    }

    /**
     * This method will take a ShortIterable and wrap it directly in a SynchronizedShortIterable. Additionally,
     * a developer specifies which lock to use with the collection.
     */
    public static SynchronizedShortIterable of(ShortIterable iterable, Object lock)
    {
        return new SynchronizedShortIterable(iterable, lock);
    }

    @Override
    public short[] toArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray();
        }
    }

    @Override
    public short[] toArray(short[] target)
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray(target);
        }
    }

    @Override
    public boolean contains(short value)
    {
        synchronized (this.lock)
        {
            return this.iterable.contains(value);
        }
    }

    @Override
    public boolean containsAll(short... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(short... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(short... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(ShortIterable source)
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
    public void each(ShortProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.iterable.forEach(procedure);
        }
    }

    @Override
    public ShortIterable select(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.select(predicate);
        }
    }

    @Override
    public ShortIterable reject(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.reject(predicate);
        }
    }

    @Override
    public <V> RichIterable<V> collect(ShortToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.collect(function);
        }
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        synchronized (this.lock)
        {
            return this.iterable.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableShortList toList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toList();
        }
    }

    @Override
    public MutableShortSet toSet()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSet();
        }
    }

    @Override
    public MutableShortBag toBag()
    {
        synchronized (this.lock)
        {
            return this.iterable.toBag();
        }
    }

    @Override
    public LazyShortIterable asLazy()
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
    public short max()
    {
        synchronized (this.lock)
        {
            return this.iterable.max();
        }
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public short min()
    {
        synchronized (this.lock)
        {
            return this.iterable.min();
        }
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedArray();
        }
    }

    @Override
    public MutableShortList toSortedList()
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
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongShortToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongShortToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
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
    public ShortIterator shortIterator()
    {
        return this.iterable.shortIterator();
    }
}
