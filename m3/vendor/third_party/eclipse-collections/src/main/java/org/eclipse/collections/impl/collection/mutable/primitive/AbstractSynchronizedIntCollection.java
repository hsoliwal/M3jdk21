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
import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.LongIntToLongFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;

/**
 * This file was automatically generated from template file abstractSynchronizedPrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractSynchronizedIntCollection
        implements MutableIntCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final MutableIntCollection collection;

    protected AbstractSynchronizedIntCollection(MutableIntCollection collection)
    {
        this(collection, null);
    }

    protected AbstractSynchronizedIntCollection(MutableIntCollection collection, Object newLock)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractSynchronizedIntCollection on a null collection");
        }
        this.collection = collection;
        this.lock = newLock == null ? this : newLock;
    }

    protected Object getLock()
    {
        return this.lock;
    }

    protected MutableIntCollection getIntCollection()
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
    public MutableIntCollection select(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.select(predicate);
        }
    }

    @Override
    public MutableIntCollection reject(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.reject(predicate);
        }
    }

    @Override
    public <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.collection.collect(function);
        }
    }

    @Override
    public MutableIntCollection with(int element)
    {
        synchronized (this.lock)
        {
            this.add(element);
        }
        return this;
    }

    @Override
    public MutableIntCollection without(int element)
    {
        synchronized (this.lock)
        {
            this.remove(element);
        }
        return this;
    }

    @Override
    public MutableIntCollection withAll(IntIterable elements)
    {
        synchronized (this.lock)
        {
            this.addAll(elements);
        }
        return this;
    }

    @Override
    public MutableIntCollection withoutAll(IntIterable elements)
    {
        synchronized (this.lock)
        {
            this.removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableIntCollection asUnmodifiable()
    {
        return new UnmodifiableIntCollection(this);
    }

    @Override
    public MutableIntCollection asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableIntCollection toImmutable()
    {
        synchronized (this.lock)
        {
            return this.collection.toImmutable();
        }
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public boolean contains(int value)
    {
        synchronized (this.lock)
        {
            return this.collection.contains(value);
        }
    }

    @Override
    public boolean containsAll(int... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(int... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(int... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean add(int newItem)
    {
        synchronized (this.lock)
        {
            return this.collection.add(newItem);
        }
    }

    @Override
    public boolean addAll(int... source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean addAll(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean remove(int value)
    {
        synchronized (this.lock)
        {
            return this.collection.remove(value);
        }
    }

    @Override
    public boolean removeIf(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.removeIf(predicate);
        }
    }

    @Override
    public boolean removeAll(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean removeAll(int... source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean retainAll(IntIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    @Override
    public boolean retainAll(int... source)
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
    public MutableIntIterator intIterator()
    {
        return this.collection.intIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.collection.forEach(procedure);
        }
    }

    @Override
    public int count(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.noneSatisfy(predicate);
        }
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
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
    public int max()
    {
        synchronized (this.lock)
        {
            return this.collection.max();
        }
    }

    @Override
    public int min()
    {
        synchronized (this.lock)
        {
            return this.collection.min();
        }
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.minIfEmpty(defaultValue);
        }
    }

    @Override
    public int maxIfEmpty(int defaultValue)
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
    public MutableIntList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedList();
        }
    }

    @Override
    public int[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedArray();
        }
    }

    @Override
    public int[] toArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toArray();
        }
    }

    @Override
    public int[] toArray(int[] target)
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
    public MutableIntList toList()
    {
        synchronized (this.lock)
        {
            return this.collection.toList();
        }
    }

    @Override
    public MutableIntSet toSet()
    {
        synchronized (this.lock)
        {
            return this.collection.toSet();
        }
    }

    @Override
    public MutableIntBag toBag()
    {
        synchronized (this.lock)
        {
            return this.collection.toBag();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.collection.injectInto(injectedValue, function);
        }
    }

    @Override
    public long reduce(LongIntToLongFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.collection.reduce(accumulator);
        }
    }

    @Override
    public long reduceIfEmpty(LongIntToLongFunction accumulator, long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.collection.chunk(size);
        }
    }
}
