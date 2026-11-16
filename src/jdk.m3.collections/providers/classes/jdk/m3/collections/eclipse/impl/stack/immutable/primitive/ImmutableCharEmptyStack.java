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

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.CharList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.stack.ImmutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableCharStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableCharStack;
import jdk.m3.collections.eclipse.api.stack.primitive.CharStack;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.CharHashBag;
import jdk.m3.collections.eclipse.impl.factory.Stacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharStacks;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.factory.Lists;

/**
 * ImmutableCharEmptyStack is an optimization for {@link ImmutableCharStack} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyStack.stg.
 */
final class ImmutableCharEmptyStack implements ImmutableCharStack, Serializable
{
    static final ImmutableCharStack INSTANCE = new ImmutableCharEmptyStack();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public CharIterator charIterator()
    {
        return ImmutableEmptyCharIterator.INSTANCE;
    }

    @Override
    public void forEach(CharProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return true;
    }

    @Override
    public char peek()
    {
        throw new EmptyStackException();
    }

    @Override
    public CharList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new CharArrayList(0);
        }
        throw new EmptyStackException();
    }

    @Override
    public char peekAt(int index)
    {
        this.checkNegativeCount(index);
        throw new EmptyStackException();
    }

    @Override
    public ImmutableCharStack select(CharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableCharStack reject(CharPredicate predicate)
    {
        return this;
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
    }

    @Override
    public char max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return defaultValue;
    }

    @Override
    public char min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public char minIfEmpty(char defaultValue)
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
    public char[] toSortedArray()
    {
        return new char[0];
    }

    @Override
    public MutableCharList toSortedList()
    {
        return new CharArrayList();
    }

    @Override
    public char[] toArray()
    {
        return new char[0];
    }

    @Override
    public char[] toArray(char[] target)
    {
        return target;
    }

    @Override
    public boolean contains(char value)
    {
        return false;
    }

    @Override
    public boolean containsAll(char... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableCharList toList()
    {
        return new CharArrayList();
    }

    @Override
    public MutableCharSet toSet()
    {
        return new CharHashSet();
    }

    @Override
    public MutableCharBag toBag()
    {
        return new CharHashBag();
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public ImmutableCharStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableCharStack push(char element)
    {
        return CharStacks.immutable.with(element);
    }

    @Override
    public ImmutableCharStack pop()
    {
        throw new EmptyStackException();
    }

    @Override
    public ImmutableCharStack pop(int count)
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
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
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
        if (!(otherStack instanceof CharStack))
        {
            return false;
        }
        CharStack stack = (CharStack) otherStack;
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
    public char getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(char value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public MutableCharStack toStack()
    {
        return CharStacks.mutable.empty();
    }
}
