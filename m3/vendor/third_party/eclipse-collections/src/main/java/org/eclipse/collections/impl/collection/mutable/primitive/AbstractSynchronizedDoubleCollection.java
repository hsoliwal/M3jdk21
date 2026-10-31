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
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;

/**
 * This file was automatically generated from template file abstractSynchronizedPrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractSynchronizedDoubleCollection
        implements MutableDoubleCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final MutableDoubleCollection collection;

    protected AbstractSynchronizedDoubleCollection(MutableDoubleCollection collection)
    {
        this(collection, null);
    }

    protected AbstractSynchronizedDoubleCollection(MutableDoubleCollection collection, Object newLock)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractSynchronizedDoubleCollection on a null collection");
        }
        this.collection = collection;
        this.lock = newLock == null ? this : newLock;
    }

    protected Object getLock()
    {
        return this.lock;
    }

    protected MutableDoubleCollection getDoubleCollection()
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
    public MutableDoubleCollection select(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.select(predicate);
        }
    }

    @Override
    public MutableDoubleCollection reject(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.reject(predicate);
        }
    }

    @Override
    public <V> MutableCollection<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.collection.collect(function);
        }
    }

    @Override
    public MutableDoubleCollection with(double element)
    {
        synchronized (this.lock)
        {
            this.add(element);
        }
        return this;
    }

    @Override
    public MutableDoubleCollection without(double element)
    {
        synchronized (this.lock)
        {
            this.remove(element);
        }
        return this;
    }

    @Override
    public MutableDoubleCollection withAll(DoubleIterable elements)
    {
        synchronized (this.lock)
        {
            this.addAll(elements);
        }
        return this;
    }

    @Override
    public MutableDoubleCollection withoutAll(DoubleIterable elements)
    {
        synchronized (this.lock)
        {
            this.removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableDoubleCollection asUnmodifiable()
    {
        return new UnmodifiableDoubleCollection(this);
    }

    @Override
    public MutableDoubleCollection asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableDoubleCollection toImmutable()
    {
        synchronized (this.lock)
        {
            return this.collection.toImmutable();
        }
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public boolean contains(double value)
    {
        synchronized (this.lock)
        {
            return this.collection.contains(value);
        }
    }

    @Override
    public boolean containsAll(double... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(double... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(double... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean add(double newItem)
    {
        synchronized (this.lock)
        {
            return this.collection.add(newItem);
        }
    }

    @Override
    public boolean addAll(double... source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean remove(double value)
    {
        synchronized (this.lock)
        {
            return this.collection.remove(value);
        }
    }

    @Override
    public boolean removeIf(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.removeIf(predicate);
        }
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean removeAll(double... source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean retainAll(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    @Override
    public boolean retainAll(double... source)
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
    public MutableDoubleIterator doubleIterator()
    {
        return this.collection.doubleIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.collection.forEach(procedure);
        }
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.noneSatisfy(predicate);
        }
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        synchronized (this.lock)
        {
            return this.collection.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public double sum()
    {
        synchronized (this.lock)
        {
            return this.collection.sum();
        }
    }

    @Override
    public double max()
    {
        synchronized (this.lock)
        {
            return this.collection.max();
        }
    }

    @Override
    public double min()
    {
        synchronized (this.lock)
        {
            return this.collection.min();
        }
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.minIfEmpty(defaultValue);
        }
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
    public MutableDoubleList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedList();
        }
    }

    @Override
    public double[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toSortedArray();
        }
    }

    @Override
    public double[] toArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toArray();
        }
    }

    @Override
    public double[] toArray(double[] target)
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
    public MutableDoubleList toList()
    {
        synchronized (this.lock)
        {
            return this.collection.toList();
        }
    }

    @Override
    public MutableDoubleSet toSet()
    {
        synchronized (this.lock)
        {
            return this.collection.toSet();
        }
    }

    @Override
    public MutableDoubleBag toBag()
    {
        synchronized (this.lock)
        {
            return this.collection.toBag();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.collection.injectInto(injectedValue, function);
        }
    }

    @Override
    public double reduce(DoubleDoubleToDoubleFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.collection.reduce(accumulator);
        }
    }

    @Override
    public double reduceIfEmpty(DoubleDoubleToDoubleFunction accumulator, double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.collection.chunk(size);
        }
    }
}
