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
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.LongShortToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableShortCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;

/**
 * This file was automatically generated from template file abstractSynchronizedPrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractSynchronizedShortCollection
        implements MutableShortCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final MutableShortCollection collection;

    protected AbstractSynchronizedShortCollection(MutableShortCollection collection)
    {
        this(collection, null);
    }

    protected AbstractSynchronizedShortCollection(MutableShortCollection collection, Object newLock)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractSynchronizedShortCollection on a null collection");
        }
        this.collection = collection;
        this.lock = newLock == null ? this : newLock;
    }

    protected Object getLock()
    {
        return this.lock;
    }

    protected MutableShortCollection getShortCollection()
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
    public MutableShortCollection select(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.select(predicate);
        }
    }

    @Override
    public MutableShortCollection reject(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.reject(predicate);
        }
    }

    @Override
    public <V> MutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.collection.collect(function);
        }
    }

    @Override
    public MutableShortCollection with(short element)
    {
        synchronized (this.lock)
        {
            this.add(element);
        }
        return this;
    }

    @Override
    public MutableShortCollection without(short element)
    {
        synchronized (this.lock)
        {
            this.remove(element);
        }
        return this;
    }

    @Override
    public MutableShortCollection withAll(ShortIterable elements)
    {
        synchronized (this.lock)
        {
            this.addAll(elements);
        }
        return this;
    }

    @Override
    public MutableShortCollection withoutAll(ShortIterable elements)
    {
        synchronized (this.lock)
        {
            this.removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableShortCollection asUnmodifiable()
    {
        return new UnmodifiableShortCollection(this);
    }

    @Override
    public MutableShortCollection asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableShortCollection toImmutable()
    {
        synchronized (this.lock)
        {
            return this.collection.toImmutable();
        }
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public boolean contains(short value)
    {
        synchronized (this.lock)
        {
            return this.collection.contains(value);
        }
    }

    @Override
    public boolean containsAll(short... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(short... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(short... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean add(short newItem)
    {
        synchronized (this.lock)
        {
            return this.collection.add(newItem);
        }
    }

    @Override
    public boolean addAll(short... source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean addAll(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean remove(short value)
    {
        synchronized (this.lock)
        {
            return this.collection.remove(value);
        }
    }

    @Override
    public boolean removeIf(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.removeIf(predicate);
        }
    }

    @Override
    public boolean removeAll(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean removeAll(short... source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean retainAll(ShortIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    @Override
    public boolean retainAll(short... source)
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
    public MutableShortIterator shortIterator()
    {
        return this.collection.shortIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.collection.forEach(procedure);
        }
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.noneSatisfy(predicate);
        }
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
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
    public short max()
    {
        synchronized (this.lock)
        {
            return this.collection.max();
        }
    }

    @Override
    public short min()
    {
        synchronized (this.lock)
        {
            return this.collection.min();
        }
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.minIfEmpty(defaultValue);
        }
    }

    @Override
    public short maxIfEmpty(short defaultValue)
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
    public MutableShortList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedList();
        }
    }

    @Override
    public short[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedArray();
        }
    }

    @Override
    public short[] toArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toArray();
        }
    }

    @Override
    public short[] toArray(short[] target)
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
    public MutableShortList toList()
    {
        synchronized (this.lock)
        {
            return this.collection.toList();
        }
    }

    @Override
    public MutableShortSet toSet()
    {
        synchronized (this.lock)
        {
            return this.collection.toSet();
        }
    }

    @Override
    public MutableShortBag toBag()
    {
        synchronized (this.lock)
        {
            return this.collection.toBag();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.collection.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongShortToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.collection.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongShortToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.collection.chunk(size);
        }
    }
}
