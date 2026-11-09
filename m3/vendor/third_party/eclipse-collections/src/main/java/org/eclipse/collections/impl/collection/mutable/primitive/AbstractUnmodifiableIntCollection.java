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
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableIntCollection
        implements MutableIntCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableIntCollection collection;

    protected AbstractUnmodifiableIntCollection(MutableIntCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableIntCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableIntCollection getIntCollection()
    {
        return this.collection;
    }

    @Override
    public int size()
    {
        return this.collection.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.collection.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.collection.notEmpty();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean contains(int value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(int... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(IntIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(int... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(IntIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(int newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(int... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(IntIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(int value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(IntPredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(IntIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(int... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(IntIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(int... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.collection.intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableIntCollection select(IntPredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableIntCollection reject(IntPredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableIntCollection with(int element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntCollection without(int element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntCollection withAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntCollection withoutAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntCollection asSynchronized()
    {
        return new SynchronizedIntCollection(this);
    }

    @Override
    public ImmutableIntCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.collection.sum();
    }

    @Override
    public int max()
    {
        return this.collection.max();
    }

    @Override
    public int min()
    {
        return this.collection.min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.collection.maxIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.collection.average();
    }

    @Override
    public double median()
    {
        return this.collection.median();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public int[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public int[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.collection.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.collection.toString();
    }

    @Override
    public String makeString()
    {
        return this.collection.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.collection.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.collection.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.collection.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.collection.appendString(appendable, separator);
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        this.collection.appendString(appendable, start, separator, end);
    }

    @Override
    public MutableIntList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public long reduce(LongIntToLongFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public long reduceIfEmpty(LongIntToLongFunction accumulator, long defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
