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
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableLongCollection
        implements MutableLongCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableLongCollection collection;

    protected AbstractUnmodifiableLongCollection(MutableLongCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableLongCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableLongCollection getLongCollection()
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
    public boolean contains(long value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(long... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(LongIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(long... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(LongIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(long newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(long... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(LongIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(long value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(LongPredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(LongIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(long... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(LongIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(long... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.collection.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableLongCollection select(LongPredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableLongCollection reject(LongPredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableLongCollection with(long element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongCollection without(long element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongCollection withAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongCollection withoutAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableLongCollection asSynchronized()
    {
        return new SynchronizedLongCollection(this);
    }

    @Override
    public ImmutableLongCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.collection.sum();
    }

    @Override
    public long max()
    {
        return this.collection.max();
    }

    @Override
    public long min()
    {
        return this.collection.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public long maxIfEmpty(long defaultValue)
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
    public MutableLongList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public long[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public long[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public long[] toArray(long[] target)
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
    public MutableLongList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public long reduce(LongLongToLongFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public long reduceIfEmpty(LongLongToLongFunction accumulator, long defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
