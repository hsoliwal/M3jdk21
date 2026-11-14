/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;
import org.eclipse.collections.api.stack.primitive.ByteStack;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.ByteStacks;
import org.eclipse.collections.impl.iterator.ImmutableEmptyByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.factory.Lists;

/**
 * ImmutableByteEmptyStack is an optimization for {@link ImmutableByteStack} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyStack.stg.
 */
final class ImmutableByteEmptyStack implements ImmutableByteStack, Serializable
{
    static final ImmutableByteStack INSTANCE = new ImmutableByteEmptyStack();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return ImmutableEmptyByteIterator.INSTANCE;
    }

    @Override
    public void forEach(ByteProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public byte peek()
    {
        throw new EmptyStackException();
    }

    @Override
    public ByteList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new ByteArrayList(0);
        }
        throw new EmptyStackException();
    }

    @Override
    public byte peekAt(int index)
    {
        this.checkNegativeCount(index);
        throw new EmptyStackException();
    }

    @Override
    public ImmutableByteStack select(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteStack reject(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
    }

    @Override
    public byte max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return defaultValue;
    }

    @Override
    public byte min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double average()
    {
        throw new ArithmeticException();
    }

    @Override
    public double median()
    {
        throw new ArithmeticException();
    }

    @Override
    public byte[] toSortedArray()
    {
        return new byte[0];
    }

    @Override
    public MutableByteList toSortedList()
    {
        return new ByteArrayList();
    }

    @Override
    public byte[] toArray()
    {
        return new byte[0];
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        return false;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableByteList toList()
    {
        return new ByteArrayList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return new ByteHashSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return new ByteHashBag();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public ImmutableByteStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableByteStack push(byte element)
    {
        return ByteStacks.immutable.with(element);
    }

    @Override
    public ImmutableByteStack pop()
    {
        throw new EmptyStackException();
    }

    @Override
    public ImmutableByteStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        throw new EmptyStackException();
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof ByteStack))
        {
            return false;
        }
        ByteStack stack = (ByteStack) otherStack;
        return stack.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    @Override
    public String toString()
    {
        return "[]";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(byte value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }
}
