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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;
import org.eclipse.collections.api.stack.primitive.MutableByteStack;
import org.eclipse.collections.impl.factory.primitive.ByteStacks;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;

/**
 * A synchronized view of a {@link MutableByteStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link ByteIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveStack.stg.
 *
 * @see MutableByteStack#asSynchronized()
 * @see MutableStack#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedByteStack
        implements MutableByteStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableByteStack stack;

    public SynchronizedByteStack(MutableByteStack stack)
    {
        this(stack, null);
    }

    public SynchronizedByteStack(MutableByteStack stack, Object newLock)
    {
        this.stack = stack;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void push(byte item)
    {
        synchronized (this.lock)
        {
            this.stack.push(item);
        }
    }

    @Override
    public byte pop()
    {
        synchronized (this.lock)
        {
            return this.stack.pop();
        }
    }

    @Override
    public ByteList pop(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.pop(count);
        }
    }

    @Override
    public byte peek()
    {
        synchronized (this.lock)
        {
            return this.stack.peek();
        }
    }

    @Override
    public ByteList peek(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.peek(count);
        }
    }

    @Override
    public byte peekAt(int index)
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
    public boolean contains(byte value)
    {
        synchronized (this.lock)
        {
            return this.stack.contains(value);
        }
    }

    @Override
    public boolean containsAll(byte... source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(ByteIterable source)
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
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.stack.byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEach(procedure);
        }
    }

    @Override
    public int count(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.noneSatisfy(predicate);
        }
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        synchronized (this.lock)
        {
            return this.stack.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public MutableByteStack select(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.select(predicate);
        }
    }

    @Override
    public MutableByteStack reject(BytePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.reject(predicate);
        }
    }

    @Override
    public <V> MutableStack<V> collect(ByteToObjectFunction<? extends V> function)
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
    public byte max()
    {
        synchronized (this.lock)
        {
            return this.stack.max();
        }
    }

    @Override
    public byte min()
    {
        synchronized (this.lock)
        {
            return this.stack.min();
        }
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.minIfEmpty(defaultValue);
        }
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
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
    public MutableByteList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedList();
        }
    }

    @Override
    public byte[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedArray();
        }
    }

    @Override
    public byte[] toArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toArray();
        }
    }

    @Override
    public byte[] toArray(byte[] target)
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
    public MutableByteList toList()
    {
        synchronized (this.lock)
        {
            return this.stack.toList();
        }
    }

    @Override
    public MutableByteSet toSet()
    {
        synchronized (this.lock)
        {
            return this.stack.toSet();
        }
    }

    @Override
    public MutableByteBag toBag()
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
    public LazyByteIterable asLazy()
    {
        synchronized (this.lock)
        {
            return new LazyByteIterableAdapter(this);
        }
    }

    @Override
    public MutableByteStack asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableByteStack(this);
        }
    }

    @Override
    public MutableByteStack asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableByteStack toImmutable()
    {
        synchronized (this.lock)
        {
            return ByteStacks.immutable.withAllReversed(this);
        }
    }

    /**
     * @since 9.2.
     */
    public MutableByteStack newEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.newEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.stack.chunk(size);
        }
    }

    @Override
    public byte getFirst()
    {
        synchronized (this.lock)
        {
            return this.stack.getFirst();
        }
    }

    @Override
    public int indexOf(byte value)
    {
        synchronized (this.lock)
        {
            return this.stack.indexOf(value);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectIntoWithIndex(injectedValue, function);
        }
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEachWithIndex(procedure);
        }
    }

    /**
     * Returns a new MutableByteStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteStack selectWithIndex(ByteIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableByteCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R selectWithIndex(ByteIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableByteStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteStack rejectWithIndex(ByteIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableByteCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R rejectWithIndex(ByteIntPredicate predicate, R target)
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
    public <V> MutableStack<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(ByteIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function, target);
        }
    }
}
