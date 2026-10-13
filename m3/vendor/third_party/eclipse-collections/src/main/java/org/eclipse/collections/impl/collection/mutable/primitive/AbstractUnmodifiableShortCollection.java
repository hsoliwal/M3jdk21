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
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableShortCollection
        implements MutableShortCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableShortCollection collection;

    protected AbstractUnmodifiableShortCollection(MutableShortCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableShortCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableShortCollection getShortCollection()
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
    public boolean contains(short value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(short... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(ShortIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(short... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(ShortIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(short newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(short... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(ShortIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(short value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(ShortPredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(ShortIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(short... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(ShortIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(short... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.collection.shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableShortCollection select(ShortPredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableShortCollection reject(ShortPredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableShortCollection with(short element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCollection without(short element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCollection withAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCollection withoutAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortCollection asSynchronized()
    {
        return new SynchronizedShortCollection(this);
    }

    @Override
    public ImmutableShortCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.collection.sum();
    }

    @Override
    public short max()
    {
        return this.collection.max();
    }

    @Override
    public short min()
    {
        return this.collection.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public short maxIfEmpty(short defaultValue)
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
    public MutableShortList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public short[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public short[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public short[] toArray(short[] target)
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
    public MutableShortList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public long reduce(LongShortToLongFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public long reduceIfEmpty(LongShortToLongFunction accumulator, long defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
