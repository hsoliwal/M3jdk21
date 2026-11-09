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
import org.eclipse.collections.api.block.procedure.primitive.BooleanBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedBooleanCollection;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseBooleanIterable;

/**
 * A synchronized view of a {@link MutableBooleanList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableBooleanList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedBooleanList
        extends AbstractSynchronizedBooleanCollection
        implements MutableBooleanList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedBooleanList(MutableBooleanList list)
    {
        super(list);
    }

    public SynchronizedBooleanList(MutableBooleanList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableBooleanList getMutableBooleanList()
    {
        return (MutableBooleanList) this.getBooleanCollection();
    }

    @Override
    public boolean get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().get(index);
        }
    }

    @Override
    public boolean getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().getFirst();
        }
    }

    @Override
    public boolean getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().getLast();
        }
    }

    @Override
    public int indexOf(boolean value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(boolean value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, boolean... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, BooleanIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().removeAtIndex(index);
        }
    }

    @Override
    public boolean set(int index, boolean element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().set(index, element);
        }
    }

    @Override
    public SynchronizedBooleanList with(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedBooleanList without(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedBooleanList withAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedBooleanList withoutAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableBooleanList select(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().select(predicate);
        }
    }

    @Override
    public MutableBooleanList reject(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().hashCode();
        }
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyBooleanIterableAdapter(this);
        }
    }

    @Override
    public MutableBooleanList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableBooleanList(this);
        }
    }

    @Override
    public MutableBooleanList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableBooleanList toImmutable()
    {
        synchronized (this.getLock())
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
    }

    /**
     * @since 9.2.
     */
    public MutableBooleanList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().newEmpty();
        }
    }

    @Override
    public MutableBooleanList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableBooleanList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().toReversed();
        }
    }

    @Override
    public LazyBooleanIterable asReversed()
    {
        return ReverseBooleanIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(BooleanList other, BooleanBooleanProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableBooleanList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().distinct();
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().zipBoolean(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<BooleanObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableBooleanList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableBooleanList selectWithIndex(BooleanIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableBooleanCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableBooleanCollection> R selectWithIndex(BooleanIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableBooleanList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableBooleanList rejectWithIndex(BooleanIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableBooleanCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableBooleanCollection> R rejectWithIndex(BooleanIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().rejectWithIndex(predicate, target);
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(BooleanIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanList().collectWithIndex(function, target);
        }
    }
}
