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
import java.util.Objects;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
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
 * This file was automatically generated from template file unmodifiablePrimitiveStack.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableByteStack
        implements MutableByteStack, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableByteStack stack;

    public UnmodifiableByteStack(MutableByteStack stack)
    {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public void push(byte item)
    {
        throw new UnsupportedOperationException("Cannot call push() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte pop()
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public ByteList pop(int count)
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte peek()
    {
        return this.stack.peek();
    }

    @Override
    public ByteList peek(int count)
    {
        return this.stack.peek(count);
    }

    @Override
    public byte peekAt(int index)
    {
        return this.stack.peekAt(index);
    }

    @Override
    public int size()
    {
        return this.stack.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.stack.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.stack.notEmpty();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean contains(byte value)
    {
        return this.stack.contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.stack.containsAll(source);
    }

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
        this.stack.forEach(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return this.stack.count(predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.stack.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.stack.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.stack.noneSatisfy(predicate);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.stack.detectIfNone(predicate, ifNone);
    }

    @Override
    public MutableByteStack select(BytePredicate predicate)
    {
        return this.stack.select(predicate);
    }

    @Override
    public MutableByteStack reject(BytePredicate predicate)
    {
        return this.stack.reject(predicate);
    }

    @Override
    public <V> MutableStack<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.stack.collect(function);
    }

    @Override
    public long sum()
    {
        return this.stack.sum();
    }

    @Override
    public byte max()
    {
        return this.stack.max();
    }

    @Override
    public byte min()
    {
        return this.stack.min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return this.stack.minIfEmpty(defaultValue);
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.stack.maxIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.stack.average();
    }

    @Override
    public double median()
    {
        return this.stack.median();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return this.stack.toSortedList();
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.stack.toSortedArray();
    }

    @Override
    public byte[] toArray()
    {
        return this.stack.toArray();
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return this.stack.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.stack.toString();
    }

    @Override
    public String makeString()
    {
        return this.stack.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.stack.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.stack.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.stack.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.stack.appendString(appendable, separator);
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        this.stack.appendString(appendable, start, separator, end);
    }

    @Override
    public MutableByteList toList()
    {
        return this.stack.toList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return this.stack.toSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return this.stack.toBag();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        return this.stack.equals(otherStack);
    }

    @Override
    public int hashCode()
    {
        return this.stack.hashCode();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public MutableByteStack asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteStack asSynchronized()
    {
        return new SynchronizedByteStack(this);
    }

    @Override
    public ImmutableByteStack toImmutable()
    {
        return ByteStacks.immutable.withAllReversed(this);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableByteStack newEmpty()
    {
        return this.stack.newEmpty();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return this.stack.chunk(size);
    }

    @Override
    public byte getFirst()
    {
        return this.stack.getFirst();
    }

    @Override
    public int indexOf(byte value)
    {
        return this.stack.indexOf(value);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectIntoWithIndex(injectedValue, function);
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        this.stack.forEachWithIndex(procedure);
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
        return this.stack.collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V, R extends Collection<V>> R collectWithIndex(ByteIntToObjectFunction<? extends V> function, R target)
    {
        return this.stack.collectWithIndex(function, target);
    }
}
