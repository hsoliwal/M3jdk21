/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.factory.primitive.CharBags;
import jdk.m3.collections.eclipse.api.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableCharSet;
import jdk.m3.collections.eclipse.api.set.primitive.CharSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharCharPair;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharSets;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;

/**
 * ImmutableCharEmptySet is an optimization for {@link ImmutableCharSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharEmptySet implements ImmutableCharSet, Serializable
{
    static final ImmutableCharSet INSTANCE = new ImmutableCharEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableCharSet newWith(char element)
    {
        return new ImmutableCharSingletonSet(element);
    }

    @Override
    public ImmutableCharSet newWithout(char element)
    {
        return this;
    }

    @Override
    public ImmutableCharSet newWithAll(CharIterable elements)
    {
        return CharSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableCharSet newWithoutAll(CharIterable elements)
    {
        return this;
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
    public boolean contains(char value)
    {
        return false;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(char... elements)
    {
        return elements.length == 0;
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
    public ImmutableCharSet select(CharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableCharSet reject(CharPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableCharList toList()
    {
        return CharLists.mutable.empty();
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return ifNone;
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
    public long sum()
    {
        return 0L;
    }

    @Override
    public char min()
    {
        throw new NoSuchElementException();
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
        return CharLists.mutable.empty();
    }

    @Override
    public LazyIterable<CharCharPair> cartesianProduct(CharSet set)
    {
        return CharSets.cartesianProduct(this, set);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return true;
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof CharSet))
        {
            return false;
        }
        CharSet set = (CharSet) obj;
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharSets.mutable.empty();
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharBags.mutable.empty();
    }

    @Override
    public CharSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableCharSet toImmutable()
    {
        return this;
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
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
    public CharIterator charIterator()
    {
        return ImmutableEmptyCharIterator.INSTANCE;
    }
}
