/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.mutable.primitive;

import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.block.comparator.primitive.ShortComparator;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ShortList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableShortList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableShortStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortShortPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortObjectPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedShortCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseShortIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.SynchronizedMutableList;

/**
 * A synchronized view of a {@link MutableShortList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableShortList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedShortList
        extends AbstractSynchronizedShortCollection
        implements MutableShortList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedShortList(MutableShortList list)
    {
        super(list);
    }

    public SynchronizedShortList(MutableShortList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableShortList getMutableShortList()
    {
        return (MutableShortList) this.getShortCollection();
    }

    @Override
    public short get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().get(index);
        }
    }

    @Override
    public short getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().getFirst();
        }
    }

    @Override
    public short getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().getLast();
        }
    }

    @Override
    public int indexOf(short value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(short value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, short... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, ShortIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().addAllAtIndex(index, source);
        }
    }

    @Override
    public short removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().removeAtIndex(index);
        }
    }

    @Override
    public short set(int index, short element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().set(index, element);
        }
    }

    @Override
    public SynchronizedShortList with(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedShortList without(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedShortList withAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedShortList withoutAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableShortList select(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().select(predicate);
        }
    }

    @Override
    public MutableList<Short> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableList.of(new BoxedMutableShortList(this), this.getLock());
        }
    }

    @Override
    public MutableShortList reject(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(ShortToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().collect(function);
        }
    }

    @Override
    public MutableShortList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().sortThis();
        }
        return this;
    }

    @Override
    public MutableShortList sortThis(ShortComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableShortList sortThisBy(ShortToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableShortList sortThisBy(ShortToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableShortList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableShortList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(short value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().binarySearch(value);
        }
    }

    @Override
    public long dotProduct(ShortList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().hashCode();
        }
    }

    @Override
    public LazyShortIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyShortIterableAdapter(this);
        }
    }

    @Override
    public MutableShortList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableShortList(this);
        }
    }

    @Override
    public MutableShortList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableShortList toImmutable()
    {
        synchronized (this.getLock())
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
    }

    /**
     * @since 9.2.
     */
    public MutableShortList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().newEmpty();
        }
    }

    @Override
    public MutableShortList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableShortList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().toReversed();
        }
    }

    @Override
    public LazyShortIterable asReversed()
    {
        return ReverseShortIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(ShortList other, ShortShortProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(ShortIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableShortList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().distinct();
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableShortList().zipShort(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<ShortObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableShortList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableShortList selectWithIndex(ShortIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableShortCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableShortCollection> R selectWithIndex(ShortIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableShortList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableShortList rejectWithIndex(ShortIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableShortCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableShortCollection> R rejectWithIndex(ShortIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().rejectWithIndex(predicate, target);
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableShortList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(ShortIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortList().collectWithIndex(function, target);
        }
    }

    @Override
    public MutableShortStack toStack()
    {
        synchronized (this.getLock())
        {
            return ShortStacks.mutable.withAll(this);
        }
    }
}
