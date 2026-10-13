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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.DoubleFloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * A synchronized view of a FloatIterable.
 * This file was automatically generated from template file synchronizedPrimitiveIterable.stg.
 *
 * @since 5.0.
 */
public class SynchronizedFloatIterable implements FloatIterable, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final FloatIterable iterable;

    protected SynchronizedFloatIterable(FloatIterable iterable)
    {
        this(iterable, null);
    }

    protected SynchronizedFloatIterable(FloatIterable iterable, Object newLock)
    {
        if (iterable == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedFloatIterable on a null collection");
        }
        this.iterable = iterable;
        this.lock = newLock == null ? this : newLock;
    }

    /**
     * This method will take a FloatIterable and wrap it directly in a SynchronizedFloatIterable.
     */
    public static SynchronizedFloatIterable of(FloatIterable iterable)
    {
        return new SynchronizedFloatIterable(iterable);
    }

    /**
     * This method will take a FloatIterable and wrap it directly in a SynchronizedFloatIterable. Additionally,
     * a developer specifies which lock to use with the collection.
     */
    public static SynchronizedFloatIterable of(FloatIterable iterable, Object lock)
    {
        return new SynchronizedFloatIterable(iterable, lock);
    }

    @Override
    public float[] toArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray();
        }
    }

    @Override
    public float[] toArray(float[] target)
    {
        synchronized (this.lock)
        {
            return this.iterable.toArray(target);
        }
    }

    @Override
    public boolean contains(float value)
    {
        synchronized (this.lock)
        {
            return this.iterable.contains(value);
        }
    }

    @Override
    public boolean containsAll(float... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(float... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(FloatIterable source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(float... source)
    {
        synchronized (this.lock)
        {
            return this.iterable.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(FloatIterable source)
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
    public void each(FloatProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.iterable.forEach(procedure);
        }
    }

    @Override
    public FloatIterable select(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.select(predicate);
        }
    }

    @Override
    public FloatIterable reject(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.reject(predicate);
        }
    }

    @Override
    public <V> RichIterable<V> collect(FloatToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.collect(function);
        }
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        synchronized (this.lock)
        {
            return this.iterable.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.iterable.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableFloatList toList()
    {
        synchronized (this.lock)
        {
            return this.iterable.toList();
        }
    }

    @Override
    public MutableFloatSet toSet()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSet();
        }
    }

    @Override
    public MutableFloatBag toBag()
    {
        synchronized (this.lock)
        {
            return this.iterable.toBag();
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        synchronized (this.lock)
        {
            return this.iterable.asLazy();
        }
    }

    @Override
    public double sum()
    {
        synchronized (this.lock)
        {
            return this.iterable.sum();
        }
    }

    @Override
    public float max()
    {
        synchronized (this.lock)
        {
            return this.iterable.max();
        }
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public float min()
    {
        synchronized (this.lock)
        {
            return this.iterable.min();
        }
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.iterable.toSortedArray();
        }
    }

    @Override
    public MutableFloatList toSortedList()
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
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.iterable.injectInto(injectedValue, function);
        }
    }

    @Override
    public double reduce(DoubleFloatToDoubleFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduce(accumulator);
        }
    }

    @Override
    public double reduceIfEmpty(DoubleFloatToDoubleFunction accumulator, double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.iterable.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
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
    public FloatIterator floatIterator()
    {
        return this.iterable.floatIterator();
    }
}
