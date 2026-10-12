/*
 * Copyright (c) 2022 Goldman Sachs and others.
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
import org.eclipse.collections.api.stack.primitive.ByteStack;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.ByteStacks;
import org.eclipse.collections.impl.iterator.SingletonByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;

/**
 * ImmutableByteSingletonStack is an optimization for {@link ImmutableByteStack} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonStack.stg.
 */
final class ImmutableByteSingletonStack implements ImmutableByteStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final byte element1;

    ImmutableByteSingletonStack(byte element)
    {
        this.element1 = element;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new SingletonByteIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public byte peek()
    {
        return this.element1;
    }

    @Override
    public ByteList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new ByteArrayList(0);
        }
        if (count == 1)
        {
            return ByteArrayList.newListWith(this.element1);
        }
        throw new IllegalArgumentException("Count must be less than or equal to size: Count = " + count + " Size = 1");
    }

    @Override
    public byte peekAt(int index)
    {
        this.checkNegativeCount(index);
        if (index == 0)
        {
            return this.element1;
        }
        throw new IllegalArgumentException("Index must be less than size: Index = " + index + " Size = 1");
    }

    @Override
    public ImmutableByteStack select(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? ByteStacks.immutable.with(this.element1)
                : ByteStacks.immutable.with();
    }

    @Override
    public ImmutableByteStack reject(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? ByteStacks.immutable.with()
                : ByteStacks.immutable.with(this.element1);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public byte max()
    {
        return this.element1;
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.element1;
    }

    @Override
    public byte min()
    {
        return this.element1;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public byte[] toSortedArray()
    {
        return new byte[]{this.element1};
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newListWith(this.element1);
    }

    @Override
    public byte[] toArray()
    {
        return new byte[]{this.element1};
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < 1)
        {
            target = new byte[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newListWith(this.element1);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBagWith(this.element1);
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
        return ByteStacks.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableByteStack pop()
    {
        return ByteStacks.immutable.with();
    }

    @Override
    public ImmutableByteStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        if (count == 1)
        {
            return ByteStacks.immutable.with();
        }
        throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = 1");
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
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        return Lists.immutable.with(this);
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
        if (stack.size() != 1)
        {
            return false;
        }
        return this.element1 == stack.peek();
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) this.element1;
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.element1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
