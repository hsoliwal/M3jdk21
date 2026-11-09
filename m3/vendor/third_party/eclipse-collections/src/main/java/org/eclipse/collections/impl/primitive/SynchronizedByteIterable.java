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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.LongByteToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * A synchronized view of a ByteIterable.
 * This file was automatically generated from template file synchronizedPrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public class SynchronizedByteIterable implements ByteIterable, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final ByteIterable iterable;

    protected SynchronizedByteIterable(ByteIterable iterable)
    {
        this(iterable, null);
    }

    protected SynchronizedByteIterable(ByteIterable iterable, Object newLock)
    {
        if (iterable == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedByteIterable on a null collection");
        }
        this.iterable = iterable;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a ByteIterable and wrap it directly in a SynchronizedByteIterable.
     */
    public static SynchronizedByteIterable of(ByteIterable iterable)
    {
        return new SynchronizedByteIterable(iterable);
    }

    /**
     * This method will take a ByteIterable and wrap it directly in a SynchronizedByteIterable. Additionally,
     * a developer specifies which lock to use with the collection.
     */
    public static SynchronizedByteIterable of(ByteIterable iterable, Object lock)
    {
        return new SynchronizedByteIterable(iterable, lock);
    }

    @Override
    public byte[] toArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray();
        }
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray(target);
        }
    }

    @Override
    public boolean contains(byte value)
    {
        synchronized (this.lock)
        {
            return this.iterable.contains(value);
        }
    }

    @Override
    public boolean containsAll(byte... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(byte... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(ByteIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(byte... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(ByteIterable source)
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
    public void each(ByteProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.iterable.forEach(procedure);
        }
    }

    @Override
    public ByteIterable select(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.select(predicate);
        }
    }

    @Override
    public ByteIterable reject(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.reject(predicate);
        }
    }

    @Override
    public <V> RichIterable<V> collect(ByteToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.collect(function);
        }
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        synchronized (this.lock)
        {
            return this.iterable.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public int count(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableByteList toList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toList();
        }
    }

    @Override
    public MutableByteSet toSet()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSet();
        }
    }

    @Override
    public MutableByteBag toBag()
    {
        synchronized (this.lock)
        {
            return this.iterable.toBag();
        }
    }

    @Override
    public LazyByteIterable asLazy()
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
    public byte max()
    {
        synchronized (this.lock)
        {
            return this.iterable.max();
        }
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public byte min()
    {
        synchronized (this.lock)
        {
            return this.iterable.min();
        }
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
    public byte[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedArray();
        }
    }

    @Override
    public MutableByteList toSortedList()
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongByteToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongByteToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
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
    public ByteIterator byteIterator()
    {
        return this.iterable.byteIterator();
    }
}
