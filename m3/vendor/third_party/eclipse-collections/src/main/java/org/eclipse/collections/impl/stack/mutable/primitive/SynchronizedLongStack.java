/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.mutable.primitive;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableLongStack;
import org.eclipse.collections.api.stack.primitive.MutableLongStack;
import org.eclipse.collections.impl.factory.primitive.LongStacks;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;

/**
 * A synchronized view of a {@link MutableLongStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link LongIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveStack.stg.
 *
 * @see MutableLongStack#asSynchronized()
 * @see MutableStack#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedLongStack
        implements MutableLongStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableLongStack stack;

    public SynchronizedLongStack(MutableLongStack stack)
    {
        this(stack, null);
    }

    public SynchronizedLongStack(MutableLongStack stack, Object newLock)
    {
        this.stack = stack;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void push(long item)
    {
        synchronized (this.lock)
        {
            this.stack.push(item);
        }
    }

    @Override
    public long pop()
    {
        synchronized (this.lock)
        {
            return this.stack.pop();
        }
    }

    @Override
    public LongList pop(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.pop(count);
        }
    }

    @Override
    public long peek()
    {
        synchronized (this.lock)
        {
            return this.stack.peek();
        }
    }

    @Override
    public LongList peek(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.peek(count);
        }
    }

    @Override
    public long peekAt(int index)
    {
        synchronized (this.lock)
        {
            return this.stack.peekAt(index);
        }
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.stack.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.notEmpty();
        }
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.stack.clear();
        }
    }

    @Override
    public boolean contains(long value)
    {
        synchronized (this.lock)
        {
            return this.stack.contains(value);
        }
    }

    @Override
    public boolean containsAll(long... source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.stack.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEach(procedure);
        }
    }

    @Override
    public int count(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.noneSatisfy(predicate);
        }
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        synchronized (this.lock)
        {
            return this.stack.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public MutableLongStack select(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.select(predicate);
        }
    }

    @Override
    public MutableLongStack reject(LongPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.reject(predicate);
        }
    }

    @Override
    public <V> MutableStack<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.stack.collect(function);
        }
    }

    @Override
    public long sum()
    {
        synchronized (this.lock)
        {
            return this.stack.sum();
        }
    }

    @Override
    public long max()
    {
        synchronized (this.lock)
        {
            return this.stack.max();
        }
    }

    @Override
    public long min()
    {
        synchronized (this.lock)
        {
            return this.stack.min();
        }
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.minIfEmpty(defaultValue);
        }
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public double average()
    {
        synchronized (this.lock)
        {
            return this.stack.average();
        }
    }

    @Override
    public double median()
    {
        synchronized (this.lock)
        {
            return this.stack.median();
        }
    }

    @Override
    public MutableLongList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedList();
        }
    }

    @Override
    public long[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedArray();
        }
    }

    @Override
    public long[] toArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toArray();
        }
    }

    @Override
    public long[] toArray(long[] target)
    {
        synchronized (this.lock)
        {
            return this.stack.toArray(target);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.stack.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.stack.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.stack.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.stack.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.stack.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.stack.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        synchronized (this.lock)
        {
            this.stack.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public MutableLongList toList()
    {
        synchronized (this.lock)
        {
            return this.stack.toList();
        }
    }

    @Override
    public MutableLongSet toSet()
    {
        synchronized (this.lock)
        {
            return this.stack.toSet();
        }
    }

    @Override
    public MutableLongBag toBag()
    {
        synchronized (this.lock)
        {
            return this.stack.toBag();
        }
    }

    @Override
    public boolean equals(Object otherStack)
    {
        synchronized (this.lock)
        {
            return this.stack.equals(otherStack);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.lock)
        {
            return this.stack.hashCode();
        }
    }

    @Override
    public LazyLongIterable asLazy()
    {
        synchronized (this.lock)
        {
            return new LazyLongIterableAdapter(this);
        }
    }

    @Override
    public MutableLongStack asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableLongStack(this);
        }
    }

    @Override
    public MutableLongStack asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableLongStack toImmutable()
    {
        synchronized (this.lock)
        {
            return LongStacks.immutable.withAllReversed(this);
        }
    }

    /**
     * @since 9.2.
     */
    public MutableLongStack newEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.newEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.stack.chunk(size);
        }
    }

    @Override
    public long getFirst()
    {
        synchronized (this.lock)
        {
            return this.stack.getFirst();
        }
    }

    @Override
    public int indexOf(long value)
    {
        synchronized (this.lock)
        {
            return this.stack.indexOf(value);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectIntoWithIndex(injectedValue, function);
        }
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEachWithIndex(procedure);
        }
    }

    /**
     * Returns a new MutableLongStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongStack selectWithIndex(LongIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableLongCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R selectWithIndex(LongIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableLongStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableLongStack rejectWithIndex(LongIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableLongCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableLongCollection> R rejectWithIndex(LongIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableStack using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableStack<V> collectWithIndex(LongIntToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function);
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
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function, target);
        }
    }
}
