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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.api.list.primitive.ShortList;
import org.eclipse.collections.api.list.primitive.ImmutableShortList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.primitive.ShortLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseShortIterable;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableShortList
        extends AbstractUnmodifiableShortCollection
        implements MutableShortList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableShortList(MutableShortList list)
    {
        super(list);
    }

    private MutableShortList getMutableShortList()
    {
        return (MutableShortList) this.getShortCollection();
    }

    @Override
    public short get(int index)
    {
        return this.getMutableShortList().get(index);
    }

    @Override
    public short getFirst()
    {
        return this.getMutableShortList().getFirst();
    }

    @Override
    public short getLast()
    {
        return this.getMutableShortList().getLast();
    }

    @Override
    public int indexOf(short value)
    {
        return this.getMutableShortList().indexOf(value);
    }

    @Override
    public int lastIndexOf(short value)
    {
        return this.getMutableShortList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, short element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, short... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, ShortIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public short removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public short set(int index, short element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortList with(short element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortList without(short element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortList withAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortList withoutAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortList select(ShortPredicate predicate)
    {
        return this.getMutableShortList().select(predicate);
    }

    @Override
    public MutableShortList reject(ShortPredicate predicate)
    {
        return this.getMutableShortList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.getMutableShortList().collect(function);
    }

    @Override
    public MutableShortList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(short value)
    {
        return this.getMutableShortList().binarySearch(value);
    }

    @Override
    public long dotProduct(ShortList list)
    {
        return this.getMutableShortList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableShortList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableShortList().hashCode();
    }

    @Override
    public MutableShortList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortList asSynchronized()
    {
        return new SynchronizedShortList(this);
    }

    @Override
    public ImmutableShortList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return ShortLists.immutable.with();
        }
        if (size == 1)
        {
            return ShortLists.immutable.with(this.getFirst());
        }
        return ShortLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableShortList newEmpty()
    {
        return this.getMutableShortList().newEmpty();
    }

    @Override
    public MutableShortList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortList toReversed()
    {
        return this.getMutableShortList().toReversed();
    }

    @Override
    public void forEachWithIndex(ShortIntProcedure procedure)
    {
        this.getMutableShortList().forEachWithIndex(procedure);
    }

    @Override
    public LazyShortIterable asReversed()
    {
        return ReverseShortIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableShortList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableShortList distinct()
    {
        return this.getMutableShortList().distinct();
    }

    @Override
    public MutableShortList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<ShortShortPair> zipShort(ShortIterable iterable)
    {
        return this.getMutableShortList().zipShort(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<ShortObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableShortList().zip(iterable);
    }

    /**
     * Returns a new MutableShortList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableShortList selectWithIndex(ShortIntPredicate predicate)
    {
        return this.getMutableShortList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableShortCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableShortCollection> R selectWithIndex(ShortIntPredicate predicate, R target)
    {
        return this.getMutableShortList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableShortList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableShortList rejectWithIndex(ShortIntPredicate predicate)
    {
        return this.getMutableShortList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableShortCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableShortCollection> R rejectWithIndex(ShortIntPredicate predicate, R target)
    {
        return this.getMutableShortList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
    {
        return this.getMutableShortList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(ShortIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableShortList().collectWithIndex(function, target);
    }
}
