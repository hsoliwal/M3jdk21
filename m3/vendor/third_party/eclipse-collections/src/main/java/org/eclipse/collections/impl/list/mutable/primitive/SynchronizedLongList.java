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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.block.comparator.primitive.LongComparator;
import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedLongCollection;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseLongIterable;
import java.util.Spliterator;

/**
 * A synchronized view of a {@link MutableLongList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableLongList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedLongList
        extends AbstractSynchronizedLongCollection
        implements MutableLongList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedLongList(MutableLongList list)
    {
        super(list);
    }

    public SynchronizedLongList(MutableLongList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableLongList getMutableLongList()
    {
        return (MutableLongList) this.getLongCollection();
    }

    @Override
    public long get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().get(index);
        }
    }

    @Override
    public long getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().getFirst();
        }
    }

    @Override
    public long getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().getLast();
        }
    }

    @Override
    public int indexOf(long value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(long value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, long... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, LongIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().addAllAtIndex(index, source);
        }
    }

    @Override
    public long removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().removeAtIndex(index);
        }
    }

    @Override
    public long set(int index, long element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().set(index, element);
        }
    }

    @Override
    public SynchronizedLongList with(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongList without(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongList withAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedLongList withoutAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableLongList select(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().select(predicate);
        }
    }

    @Override
    public MutableLongList reject(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().collect(function);
        }
    }

    @Override
    public MutableLongList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().sortThis();
        }
        return this;
    }

    @Override
    public MutableLongList sortThis(LongComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableLongList sortThisBy(LongToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableLongList sortThisBy(LongToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableLongList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableLongList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(long value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().binarySearch(value);
        }
    }

    @Override
    public long dotProduct(LongList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().hashCode();
        }
    }

    @Override
    public LazyLongIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyLongIterableAdapter(this);
        }
    }

    @Override
    public MutableLongList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableLongList(this);
        }
    }

    @Override
    public MutableLongList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableLongList toImmutable()
    {
        synchronized (this.getLock())
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
    }

    /**
     * @since 9.2.
     */
    public MutableLongList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().newEmpty();
        }
    }

    @Override
    public MutableLongList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableLongList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().toReversed();
        }
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(LongList other, LongLongProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableLongList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().distinct();
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableLongList().zipLong(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableLongList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongList selectWithIndex(LongIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableLongCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R selectWithIndex(LongIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableLongList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongList rejectWithIndex(LongIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableLongCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R rejectWithIndex(LongIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().rejectWithIndex(predicate, target);
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableLongList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(LongIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongList().collectWithIndex(function, target);
        }
    }

    /**
    * This function needs to be synchronized manually
    */
    @Override
    public Spliterator.OfLong spliterator()
    {
        return this.getMutableLongList().spliterator();
    }
}
