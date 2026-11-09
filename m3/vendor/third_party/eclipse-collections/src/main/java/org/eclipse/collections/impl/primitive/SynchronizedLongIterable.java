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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * A synchronized view of a LongIterable.
 * This file was automatically generated from template file synchronizedPrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public class SynchronizedLongIterable implements LongIterable, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final LongIterable iterable;

    protected SynchronizedLongIterable(LongIterable iterable)
    {
        this(iterable, null);
    }

    protected SynchronizedLongIterable(LongIterable iterable, Object newLock)
    {
        if (iterable == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedLongIterable on a null collection");
        }
        this.iterable = iterable;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a LongIterable and wrap it directly in a SynchronizedLongIterable.
     */
    public static SynchronizedLongIterable of(LongIterable iterable)
    {
        return new SynchronizedLongIterable(iterable);
    }

    /**
     * This method will take a LongIterable and wrap it directly in a SynchronizedLongIterable. Additionally,
     * a developer specifies which lock to use with the collection.
     */
    public static SynchronizedLongIterable of(LongIterable iterable, Object lock)
    {
        return new SynchronizedLongIterable(iterable, lock);
    }

    @Override
    public long[] toArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray();
        }
    }

    @Override
    public long[] toArray(long[] target)
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray(target);
        }
    }

    @Override
    public boolean contains(long value)
    {
        synchronized (this.lock)
        {
            return this.iterable.contains(value);
        }
    }

    @Override
    public boolean containsAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(long... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(long... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(LongIterable source)
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
    public void each(LongProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.iterable.forEach(procedure);
        }
    }

    @Override
    public LongIterable select(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.select(predicate);
        }
    }

    @Override
    public LongIterable reject(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.reject(predicate);
        }
    }

    @Override
    public <V> RichIterable<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.collect(function);
        }
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        synchronized (this.lock)
        {
            return this.iterable.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public int count(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableLongList toList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toList();
        }
    }

    @Override
    public MutableLongSet toSet()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSet();
        }
    }

    @Override
    public MutableLongBag toBag()
    {
        synchronized (this.lock)
        {
            return this.iterable.toBag();
        }
    }

    @Override
    public LazyLongIterable asLazy()
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
    public long max()
    {
        synchronized (this.lock)
        {
            return this.iterable.max();
        }
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public long min()
    {
        synchronized (this.lock)
        {
            return this.iterable.min();
        }
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedArray();
        }
    }

    @Override
    public MutableLongList toSortedList()
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
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongLongToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongLongToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
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
    public LongIterator longIterator()
    {
        return this.iterable.longIterator();
    }
}
