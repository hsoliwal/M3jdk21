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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseFloatIterable;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableFloatList
        extends AbstractUnmodifiableFloatCollection
        implements MutableFloatList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableFloatList(MutableFloatList list)
    {
        super(list);
    }

    private MutableFloatList getMutableFloatList()
    {
        return (MutableFloatList) this.getFloatCollection();
    }

    @Override
    public float get(int index)
    {
        return this.getMutableFloatList().get(index);
    }

    @Override
    public float getFirst()
    {
        return this.getMutableFloatList().getFirst();
    }

    @Override
    public float getLast()
    {
        return this.getMutableFloatList().getLast();
    }

    @Override
    public int indexOf(float value)
    {
        return this.getMutableFloatList().indexOf(value);
    }

    @Override
    public int lastIndexOf(float value)
    {
        return this.getMutableFloatList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, float element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, float... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, FloatIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public float removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public float set(int index, float element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatList with(float element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatList without(float element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatList withAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatList withoutAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatList select(FloatPredicate predicate)
    {
        return this.getMutableFloatList().select(predicate);
    }

    @Override
    public MutableFloatList reject(FloatPredicate predicate)
    {
        return this.getMutableFloatList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.getMutableFloatList().collect(function);
    }

    @Override
    public MutableFloatList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(float value)
    {
        return this.getMutableFloatList().binarySearch(value);
    }

    @Override
    public double dotProduct(FloatList list)
    {
        return this.getMutableFloatList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableFloatList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableFloatList().hashCode();
    }

    @Override
    public MutableFloatList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableFloatList asSynchronized()
    {
        return new SynchronizedFloatList(this);
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return FloatLists.immutable.with();
        }
        if (size == 1)
        {
            return FloatLists.immutable.with(this.getFirst());
        }
        return FloatLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableFloatList newEmpty()
    {
        return this.getMutableFloatList().newEmpty();
    }

    @Override
    public MutableFloatList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatList toReversed()
    {
        return this.getMutableFloatList().toReversed();
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        this.getMutableFloatList().forEachWithIndex(procedure);
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableFloatList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableFloatList distinct()
    {
        return this.getMutableFloatList().distinct();
    }

    @Override
    public MutableFloatList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        return this.getMutableFloatList().zipFloat(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableFloatList().zip(iterable);
    }

    /**
     * Returns a new MutableFloatList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatList selectWithIndex(FloatIntPredicate predicate)
    {
        return this.getMutableFloatList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableFloatCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R selectWithIndex(FloatIntPredicate predicate, R target)
    {
        return this.getMutableFloatList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableFloatList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatList rejectWithIndex(FloatIntPredicate predicate)
    {
        return this.getMutableFloatList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableFloatCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R rejectWithIndex(FloatIntPredicate predicate, R target)
    {
        return this.getMutableFloatList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
    {
        return this.getMutableFloatList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(FloatIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableFloatList().collectWithIndex(function, target);
    }
}
