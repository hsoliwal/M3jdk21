/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import java.util.Collection;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseLongIterable;
import java.util.Spliterator;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableLongList
        extends AbstractUnmodifiableLongCollection
        implements MutableLongList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableLongList(MutableLongList list)
    {
        super(list);
    }

    private MutableLongList getMutableLongList()
    {
        return (MutableLongList) this.getLongCollection();
    }

    @Override
    public long get(int index)
    {
        return this.getMutableLongList().get(index);
    }

    @Override
    public long getFirst()
    {
        return this.getMutableLongList().getFirst();
    }

    @Override
    public long getLast()
    {
        return this.getMutableLongList().getLast();
    }

    @Override
    public int indexOf(long value)
    {
        return this.getMutableLongList().indexOf(value);
    }

    @Override
    public int lastIndexOf(long value)
    {
        return this.getMutableLongList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, long element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, long... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, LongIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public long removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public long set(int index, long element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongList with(long element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongList without(long element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongList withAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongList withoutAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongList select(LongPredicate predicate)
    {
        return this.getMutableLongList().select(predicate);
    }

    @Override
    public MutableLongList reject(LongPredicate predicate)
    {
        return this.getMutableLongList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.getMutableLongList().collect(function);
    }

    @Override
    public MutableLongList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(long value)
    {
        return this.getMutableLongList().binarySearch(value);
    }

    @Override
    public long dotProduct(LongList list)
    {
        return this.getMutableLongList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableLongList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableLongList().hashCode();
    }

    @Override
    public MutableLongList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableLongList asSynchronized()
    {
        return new SynchronizedLongList(this);
    }

    @Override
    public ImmutableLongList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return LongLists.immutable.with();
        }
        if (size == 1)
        {
            return LongLists.immutable.with(this.getFirst());
        }
        return LongLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableLongList newEmpty()
    {
        return this.getMutableLongList().newEmpty();
    }

    @Override
    public MutableLongList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongList toReversed()
    {
        return this.getMutableLongList().toReversed();
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        this.getMutableLongList().forEachWithIndex(procedure);
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableLongList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableLongList distinct()
    {
        return this.getMutableLongList().distinct();
    }

    @Override
    public MutableLongList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<LongLongPair> zipLong(LongIterable iterable)
    {
        return this.getMutableLongList().zipLong(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableLongList().zip(iterable);
    }

    /**
     * Returns a new MutableLongList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongList selectWithIndex(LongIntPredicate predicate)
    {
        return this.getMutableLongList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableLongCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R selectWithIndex(LongIntPredicate predicate, R target)
    {
        return this.getMutableLongList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableLongList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongList rejectWithIndex(LongIntPredicate predicate)
    {
        return this.getMutableLongList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableLongCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R rejectWithIndex(LongIntPredicate predicate, R target)
    {
        return this.getMutableLongList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        return this.getMutableLongList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(LongIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableLongList().collectWithIndex(function, target);
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        return this.getMutableLongList().spliterator();
    }
}
