/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.collection.mutable.primitive;

import java.io.Serializable;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;

/**
 * This file was automatically generated from template file abstractSynchronizedPrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractSynchronizedLongCollection
        implements MutableLongCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final MutableLongCollection collection;

    protected AbstractSynchronizedLongCollection(MutableLongCollection collection)
    {
        this(collection, null);
    }

    protected AbstractSynchronizedLongCollection(MutableLongCollection collection, Object newLock)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractSynchronizedLongCollection on a null collection");
        }
        this.collection = collection;
        this.lock = newLock == null ? this : newLock;
    }

    protected Object getLock()
    {
        return this.lock;
    }

    protected MutableLongCollection getLongCollection()
    {
        return this.collection;
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.collection.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.collection.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.collection.notEmpty();
        }
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.collection.clear();
        }
    }

    @Override
    public MutableLongCollection select(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.select(predicate);
        }
    }

    @Override
    public MutableLongCollection reject(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.reject(predicate);
        }
    }

    @Override
    public <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.collection.collect(function);
        }
    }

    @Override
    public MutableLongCollection with(long element)
    {
        synchronized (this.lock)
        {
            this.add(element);
        }
        return this;
    }

    @Override
    public MutableLongCollection without(long element)
    {
        synchronized (this.lock)
        {
            this.remove(element);
        }
        return this;
    }

    @Override
    public MutableLongCollection withAll(LongIterable elements)
    {
        synchronized (this.lock)
        {
            this.addAll(elements);
        }
        return this;
    }

    @Override
    public MutableLongCollection withoutAll(LongIterable elements)
    {
        synchronized (this.lock)
        {
            this.removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableLongCollection asUnmodifiable()
    {
        return new UnmodifiableLongCollection(this);
    }

    @Override
    public MutableLongCollection asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableLongCollection toImmutable()
    {
        synchronized (this.lock)
        {
            return this.collection.toImmutable();
        }
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public boolean contains(long value)
    {
        synchronized (this.lock)
        {
            return this.collection.contains(value);
        }
    }

    @Override
    public boolean containsAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean add(long newItem)
    {
        synchronized (this.lock)
        {
            return this.collection.add(newItem);
        }
    }

    @Override
    public boolean addAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean addAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean remove(long value)
    {
        synchronized (this.lock)
        {
            return this.collection.remove(value);
        }
    }

    @Override
    public boolean removeIf(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.removeIf(predicate);
        }
    }

    @Override
    public boolean removeAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean removeAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean retainAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    @Override
    public boolean retainAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public MutableLongIterator longIterator()
    {
        return this.collection.longIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.collection.forEach(procedure);
        }
    }

    @Override
    public int count(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.noneSatisfy(predicate);
        }
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        synchronized (this.lock)
        {
            return this.collection.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public long sum()
    {
        synchronized (this.lock)
        {
            return this.collection.sum();
        }
    }

    @Override
    public long max()
    {
        synchronized (this.lock)
        {
            return this.collection.max();
        }
    }

    @Override
    public long min()
    {
        synchronized (this.lock)
        {
            return this.collection.min();
        }
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.minIfEmpty(defaultValue);
        }
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public double average()
    {
        synchronized (this.lock)
        {
            return this.collection.average();
        }
    }

    @Override
    public double median()
    {
        synchronized (this.lock)
        {
            return this.collection.median();
        }
    }

    @Override
    public MutableLongList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedList();
        }
    }

    @Override
    public long[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedArray();
        }
    }

    @Override
    public long[] toArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toArray();
        }
    }

    @Override
    public long[] toArray(long[] target)
    {
        synchronized (this.lock)
        {
            return this.collection.toArray(target);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.collection.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.collection.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.collection.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.collection.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public MutableLongList toList()
    {
        synchronized (this.lock)
        {
            return this.collection.toList();
        }
    }

    @Override
    public MutableLongSet toSet()
    {
        synchronized (this.lock)
        {
            return this.collection.toSet();
        }
    }

    @Override
    public MutableLongBag toBag()
    {
        synchronized (this.lock)
        {
            return this.collection.toBag();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.collection.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongLongToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.collection.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongLongToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.collection.chunk(size);
        }
    }
}
