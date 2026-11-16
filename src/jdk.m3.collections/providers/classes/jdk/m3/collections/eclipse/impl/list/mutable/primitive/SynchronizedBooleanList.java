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
import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanBooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableBooleanCollection;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.BooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableBooleanStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanBooleanPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanObjectPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseBooleanIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.SynchronizedMutableList;

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
    public MutableList<Boolean> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableList.of(new BoxedMutableBooleanList(this), this.getLock());
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

    @Override
    public MutableBooleanStack toStack()
    {
        synchronized (this.getLock())
        {
            return BooleanStacks.mutable.withAll(this);
        }
    }
}
