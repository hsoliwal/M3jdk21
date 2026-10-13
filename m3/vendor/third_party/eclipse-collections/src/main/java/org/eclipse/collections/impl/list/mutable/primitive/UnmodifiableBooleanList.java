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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseBooleanIterable;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableBooleanList
        extends AbstractUnmodifiableBooleanCollection
        implements MutableBooleanList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableBooleanList(MutableBooleanList list)
    {
        super(list);
    }

    private MutableBooleanList getMutableBooleanList()
    {
        return (MutableBooleanList) this.getBooleanCollection();
    }

    @Override
    public boolean get(int index)
    {
        return this.getMutableBooleanList().get(index);
    }

    @Override
    public boolean getFirst()
    {
        return this.getMutableBooleanList().getFirst();
    }

    @Override
    public boolean getLast()
    {
        return this.getMutableBooleanList().getLast();
    }

    @Override
    public int indexOf(boolean value)
    {
        return this.getMutableBooleanList().indexOf(value);
    }

    @Override
    public int lastIndexOf(boolean value)
    {
        return this.getMutableBooleanList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, boolean element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, boolean... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, BooleanIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean set(int index, boolean element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanList with(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanList without(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanList withAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanList withoutAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanList select(BooleanPredicate predicate)
    {
        return this.getMutableBooleanList().select(predicate);
    }

    @Override
    public MutableBooleanList reject(BooleanPredicate predicate)
    {
        return this.getMutableBooleanList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.getMutableBooleanList().collect(function);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableBooleanList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableBooleanList().hashCode();
    }

    @Override
    public MutableBooleanList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableBooleanList asSynchronized()
    {
        return new SynchronizedBooleanList(this);
    }

    @Override
    public ImmutableBooleanList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return BooleanLists.immutable.with();
        }
        if (size == 1)
        {
            return BooleanLists.immutable.with(this.getFirst());
        }
        return BooleanLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableBooleanList newEmpty()
    {
        return this.getMutableBooleanList().newEmpty();
    }

    @Override
    public MutableBooleanList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanList toReversed()
    {
        return this.getMutableBooleanList().toReversed();
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
    {
        this.getMutableBooleanList().forEachWithIndex(procedure);
    }

    @Override
    public LazyBooleanIterable asReversed()
    {
        return ReverseBooleanIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableBooleanList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableBooleanList distinct()
    {
        return this.getMutableBooleanList().distinct();
    }

    @Override
    public MutableBooleanList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<BooleanBooleanPair> zipBoolean(BooleanIterable iterable)
    {
        return this.getMutableBooleanList().zipBoolean(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<BooleanObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableBooleanList().zip(iterable);
    }

    /**
     * Returns a new MutableBooleanList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableBooleanList selectWithIndex(BooleanIntPredicate predicate)
    {
        return this.getMutableBooleanList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableBooleanCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableBooleanCollection> R selectWithIndex(BooleanIntPredicate predicate, R target)
    {
        return this.getMutableBooleanList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableBooleanList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableBooleanList rejectWithIndex(BooleanIntPredicate predicate)
    {
        return this.getMutableBooleanList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableBooleanCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableBooleanCollection> R rejectWithIndex(BooleanIntPredicate predicate, R target)
    {
        return this.getMutableBooleanList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(BooleanIntToObjectFunction<? extends V> function)
    {
        return this.getMutableBooleanList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(BooleanIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableBooleanList().collectWithIndex(function, target);
    }
}
