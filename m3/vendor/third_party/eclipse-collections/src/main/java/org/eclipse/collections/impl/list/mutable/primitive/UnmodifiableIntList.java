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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableIntCollection;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseIntIterable;
import java.util.Spliterator;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableIntList
        extends AbstractUnmodifiableIntCollection
        implements MutableIntList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableIntList(MutableIntList list)
    {
        super(list);
    }

    private MutableIntList getMutableIntList()
    {
        return (MutableIntList) this.getIntCollection();
    }

    @Override
    public int get(int index)
    {
        return this.getMutableIntList().get(index);
    }

    @Override
    public int getFirst()
    {
        return this.getMutableIntList().getFirst();
    }

    @Override
    public int getLast()
    {
        return this.getMutableIntList().getLast();
    }

    @Override
    public int indexOf(int value)
    {
        return this.getMutableIntList().indexOf(value);
    }

    @Override
    public int lastIndexOf(int value)
    {
        return this.getMutableIntList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, int element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, int... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, IntIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public int removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public int set(int index, int element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntList with(int element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntList without(int element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntList withAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntList withoutAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntList select(IntPredicate predicate)
    {
        return this.getMutableIntList().select(predicate);
    }

    @Override
    public MutableIntList reject(IntPredicate predicate)
    {
        return this.getMutableIntList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.getMutableIntList().collect(function);
    }

    @Override
    public MutableIntList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(int value)
    {
        return this.getMutableIntList().binarySearch(value);
    }

    @Override
    public long dotProduct(IntList list)
    {
        return this.getMutableIntList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableIntList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableIntList().hashCode();
    }

    @Override
    public MutableIntList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntList asSynchronized()
    {
        return new SynchronizedIntList(this);
    }

    @Override
    public ImmutableIntList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return IntLists.immutable.with();
        }
        if (size == 1)
        {
            return IntLists.immutable.with(this.getFirst());
        }
        return IntLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableIntList newEmpty()
    {
        return this.getMutableIntList().newEmpty();
    }

    @Override
    public MutableIntList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntList toReversed()
    {
        return this.getMutableIntList().toReversed();
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
        this.getMutableIntList().forEachWithIndex(procedure);
    }

    @Override
    public LazyIntIterable asReversed()
    {
        return ReverseIntIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableIntList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableIntList distinct()
    {
        return this.getMutableIntList().distinct();
    }

    @Override
    public MutableIntList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<IntIntPair> zipInt(IntIterable iterable)
    {
        return this.getMutableIntList().zipInt(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableIntList().zip(iterable);
    }

    /**
     * Returns a new MutableIntList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableIntList selectWithIndex(IntIntPredicate predicate)
    {
        return this.getMutableIntList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableIntCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableIntCollection> R selectWithIndex(IntIntPredicate predicate, R target)
    {
        return this.getMutableIntList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableIntList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableIntList rejectWithIndex(IntIntPredicate predicate)
    {
        return this.getMutableIntList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableIntCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableIntCollection> R rejectWithIndex(IntIntPredicate predicate, R target)
    {
        return this.getMutableIntList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(IntIntToObjectFunction<? extends V> function)
    {
        return this.getMutableIntList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(IntIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableIntList().collectWithIndex(function, target);
    }

    @Override
    public Spliterator.OfInt spliterator()
    {
        return this.getMutableIntList().spliterator();
    }
}
