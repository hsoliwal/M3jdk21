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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.block.comparator.primitive.DoubleComparator;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseDoubleIterable;
import java.util.Spliterator;

/**
 * A synchronized view of a {@link MutableDoubleList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableDoubleList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedDoubleList
        extends AbstractSynchronizedDoubleCollection
        implements MutableDoubleList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedDoubleList(MutableDoubleList list)
    {
        super(list);
    }

    public SynchronizedDoubleList(MutableDoubleList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableDoubleList getMutableDoubleList()
    {
        return (MutableDoubleList) this.getDoubleCollection();
    }

    @Override
    public double get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().get(index);
        }
    }

    @Override
    public double getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().getFirst();
        }
    }

    @Override
    public double getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().getLast();
        }
    }

    @Override
    public int indexOf(double value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(double value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, double... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, DoubleIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().addAllAtIndex(index, source);
        }
    }

    @Override
    public double removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().removeAtIndex(index);
        }
    }

    @Override
    public double set(int index, double element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().set(index, element);
        }
    }

    @Override
    public SynchronizedDoubleList with(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedDoubleList without(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedDoubleList withAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedDoubleList withoutAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableDoubleList select(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().select(predicate);
        }
    }

    @Override
    public MutableDoubleList reject(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().collect(function);
        }
    }

    @Override
    public MutableDoubleList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().sortThis();
        }
        return this;
    }

    @Override
    public MutableDoubleList sortThis(DoubleComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableDoubleList sortThisBy(DoubleToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableDoubleList sortThisBy(DoubleToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableDoubleList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableDoubleList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(double value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().binarySearch(value);
        }
    }

    @Override
    public double dotProduct(DoubleList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().hashCode();
        }
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyDoubleIterableAdapter(this);
        }
    }

    @Override
    public MutableDoubleList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableDoubleList(this);
        }
    }

    @Override
    public MutableDoubleList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableDoubleList toImmutable()
    {
        synchronized (this.getLock())
        {
            int size = this.size();
            if (size == 0)
            {
                return DoubleLists.immutable.with();
            }
            if (size == 1)
            {
                return DoubleLists.immutable.with(this.getFirst());
            }
            return DoubleLists.immutable.with(this.toArray());
        }
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().newEmpty();
        }
    }

    @Override
    public MutableDoubleList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableDoubleList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().toReversed();
        }
    }

    @Override
    public LazyDoubleIterable asReversed()
    {
        return ReverseDoubleIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(DoubleList other, DoubleDoubleProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableDoubleList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().distinct();
        }
    }

    @Override
    public MutableDoubleList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().zipDouble(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableDoubleList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleList selectWithIndex(DoubleIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableDoubleCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R selectWithIndex(DoubleIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableDoubleList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleList rejectWithIndex(DoubleIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableDoubleCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R rejectWithIndex(DoubleIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().rejectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(DoubleIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleList().collectWithIndex(function, target);
        }
    }

    /**
    * This function needs to be synchronized manually
    */
    @Override
    public Spliterator.OfDouble spliterator()
    {
        return this.getMutableDoubleList().spliterator();
    }
}
