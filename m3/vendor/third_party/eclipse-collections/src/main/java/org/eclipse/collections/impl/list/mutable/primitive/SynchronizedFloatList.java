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
import java.util.Comparator;
import java.util.Random;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.block.comparator.primitive.FloatComparator;
import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedFloatCollection;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseFloatIterable;

/**
 * A synchronized view of a {@link MutableFloatList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableFloatList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedFloatList
        extends AbstractSynchronizedFloatCollection
        implements MutableFloatList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedFloatList(MutableFloatList list)
    {
        super(list);
    }

    public SynchronizedFloatList(MutableFloatList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableFloatList getMutableFloatList()
    {
        return (MutableFloatList) this.getFloatCollection();
    }

    @Override
    public float get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().get(index);
        }
    }

    @Override
    public float getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().getFirst();
        }
    }

    @Override
    public float getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().getLast();
        }
    }

    @Override
    public int indexOf(float value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(float value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, float... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, FloatIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().addAllAtIndex(index, source);
        }
    }

    @Override
    public float removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().removeAtIndex(index);
        }
    }

    @Override
    public float set(int index, float element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().set(index, element);
        }
    }

    @Override
    public SynchronizedFloatList with(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedFloatList without(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedFloatList withAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedFloatList withoutAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableFloatList select(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().select(predicate);
        }
    }

    @Override
    public MutableFloatList reject(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().collect(function);
        }
    }

    @Override
    public MutableFloatList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().sortThis();
        }
        return this;
    }

    @Override
    public MutableFloatList sortThis(FloatComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableFloatList sortThisBy(FloatToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableFloatList sortThisBy(FloatToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableFloatList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableFloatList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(float value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().binarySearch(value);
        }
    }

    @Override
    public double dotProduct(FloatList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().hashCode();
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyFloatIterableAdapter(this);
        }
    }

    @Override
    public MutableFloatList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableFloatList(this);
        }
    }

    @Override
    public MutableFloatList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        synchronized (this.getLock())
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
    }

    /**
     * @since 9.2.
     */
    public MutableFloatList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().newEmpty();
        }
    }

    @Override
    public MutableFloatList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableFloatList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().toReversed();
        }
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(FloatList other, FloatFloatProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableFloatList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().distinct();
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().zipFloat(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableFloatList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatList selectWithIndex(FloatIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableFloatCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R selectWithIndex(FloatIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableFloatList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatList rejectWithIndex(FloatIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableFloatCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R rejectWithIndex(FloatIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().rejectWithIndex(predicate, target);
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(FloatIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatList().collectWithIndex(function, target);
        }
    }
}
