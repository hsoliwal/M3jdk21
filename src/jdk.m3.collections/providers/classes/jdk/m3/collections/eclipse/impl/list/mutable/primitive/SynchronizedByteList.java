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

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.block.comparator.primitive.ByteComparator;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectByteIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ByteIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteByteProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableByteCollection;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ByteList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableByteStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteBytePair;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteObjectPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedByteCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyByteIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseByteIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.SynchronizedMutableList;

/**
 * A synchronized view of a {@link MutableByteList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableByteList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedByteList
        extends AbstractSynchronizedByteCollection
        implements MutableByteList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedByteList(MutableByteList list)
    {
        super(list);
    }

    public SynchronizedByteList(MutableByteList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableByteList getMutableByteList()
    {
        return (MutableByteList) this.getByteCollection();
    }

    @Override
    public byte get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().get(index);
        }
    }

    @Override
    public byte getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().getFirst();
        }
    }

    @Override
    public byte getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().getLast();
        }
    }

    @Override
    public int indexOf(byte value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(byte value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, byte... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, ByteIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().addAllAtIndex(index, source);
        }
    }

    @Override
    public byte removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().removeAtIndex(index);
        }
    }

    @Override
    public byte set(int index, byte element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().set(index, element);
        }
    }

    @Override
    public SynchronizedByteList with(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedByteList without(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedByteList withAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedByteList withoutAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableByteList select(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().select(predicate);
        }
    }

    @Override
    public MutableList<Byte> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableList.of(new BoxedMutableByteList(this), this.getLock());
        }
    }

    @Override
    public MutableByteList reject(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().collect(function);
        }
    }

    @Override
    public MutableByteList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().sortThis();
        }
        return this;
    }

    @Override
    public MutableByteList sortThis(ByteComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableByteList sortThisBy(ByteToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableByteList sortThisBy(ByteToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableByteList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableByteList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(byte value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().binarySearch(value);
        }
    }

    @Override
    public long dotProduct(ByteList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().hashCode();
        }
    }

    @Override
    public LazyByteIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyByteIterableAdapter(this);
        }
    }

    @Override
    public MutableByteList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableByteList(this);
        }
    }

    @Override
    public MutableByteList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableByteList toImmutable()
    {
        synchronized (this.getLock())
        {
            int size = this.size();
            if (size == 0)
            {
                return ByteLists.immutable.with();
            }
            if (size == 1)
            {
                return ByteLists.immutable.with(this.getFirst());
            }
            return ByteLists.immutable.with(this.toArray());
        }
    }

    /**
     * @since 9.2.
     */
    public MutableByteList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().newEmpty();
        }
    }

    @Override
    public MutableByteList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableByteList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().toReversed();
        }
    }

    @Override
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(ByteList other, ByteByteProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableByteList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().distinct();
        }
    }

    @Override
    public MutableByteList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().zipByte(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableByteList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteList selectWithIndex(ByteIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableByteCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R selectWithIndex(ByteIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableByteList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteList rejectWithIndex(ByteIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableByteCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R rejectWithIndex(ByteIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().rejectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(ByteIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteList().collectWithIndex(function, target);
        }
    }

    @Override
    public MutableByteStack toStack()
    {
        synchronized (this.getLock())
        {
            return ByteStacks.mutable.withAll(this);
        }
    }
}
