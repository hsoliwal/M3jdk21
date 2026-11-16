/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.LongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.stack.ImmutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableLongStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableLongStack;
import jdk.m3.collections.eclipse.api.stack.primitive.LongStack;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.LongHashBag;
import jdk.m3.collections.eclipse.impl.factory.Stacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyLongIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyLongIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.factory.Lists;

/**
 * ImmutableLongEmptyStack is an optimization for {@link ImmutableLongStack} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyStack.stg.
 */
final class ImmutableLongEmptyStack implements ImmutableLongStack, Serializable
{
    static final ImmutableLongStack INSTANCE = new ImmutableLongEmptyStack();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public LongIterator longIterator()
    {
        return ImmutableEmptyLongIterator.INSTANCE;
    }

    @Override
    public void forEach(LongProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public long peek()
    {
        throw new EmptyStackException();
    }

    @Override
    public LongList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new LongArrayList(0);
        }
        throw new EmptyStackException();
    }

    @Override
    public long peekAt(int index)
    {
        this.checkNegativeCount(index);
        throw new EmptyStackException();
    }

    @Override
    public ImmutableLongStack select(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableLongStack reject(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
    }

    @Override
    public long max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return defaultValue;
    }

    @Override
    public long min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return new long[0];
    }

    @Override
    public MutableLongList toSortedList()
    {
        return new LongArrayList();
    }

    @Override
    public long[] toArray()
    {
        return new long[0];
    }

    @Override
    public long[] toArray(long[] target)
    {
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return false;
    }

    @Override
    public boolean containsAll(long... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableLongList toList()
    {
        return new LongArrayList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return new LongHashSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return new LongHashBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public ImmutableLongStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableLongStack push(long element)
    {
        return LongStacks.immutable.with(element);
    }

    @Override
    public ImmutableLongStack pop()
    {
        throw new EmptyStackException();
    }

    @Override
    public ImmutableLongStack pop(int count)
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
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
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
        if (!(otherStack instanceof LongStack))
        {
            return false;
        }
        LongStack stack = (LongStack) otherStack;
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
    public long getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(long value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public MutableLongStack toStack()
    {
        return LongStacks.mutable.empty();
    }
}
