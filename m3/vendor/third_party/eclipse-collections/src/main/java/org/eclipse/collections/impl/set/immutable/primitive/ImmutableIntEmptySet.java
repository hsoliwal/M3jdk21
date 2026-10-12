/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.IntBags;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.iterator.ImmutableEmptyIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;

/**
 * ImmutableIntEmptySet is an optimization for {@link ImmutableIntSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntEmptySet implements ImmutableIntSet, Serializable
{
    static final ImmutableIntSet INSTANCE = new ImmutableIntEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableIntSet newWith(int element)
    {
        return new ImmutableIntSingletonSet(element);
    }

    @Override
    public ImmutableIntSet newWithout(int element)
    {
        return this;
    }

    @Override
    public ImmutableIntSet newWithAll(IntIterable elements)
    {
        return IntSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableIntSet newWithoutAll(IntIterable elements)
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
    public boolean contains(int value)
    {
        return false;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(int... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(IntProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
    }

    @Override
    public ImmutableIntSet select(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableIntSet reject(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableIntList toList()
    {
        return IntLists.mutable.empty();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return false;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public int min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return defaultValue;
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return new int[0];
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntLists.mutable.empty();
    }

    @Override
    public LazyIterable<IntIntPair> cartesianProduct(IntSet set)
    {
        return IntSets.cartesianProduct(this, set);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
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
        if (!(obj instanceof IntSet))
        {
            return false;
        }
        IntSet set = (IntSet) obj;
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntSets.mutable.empty();
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntBags.mutable.empty();
    }

    @Override
    public IntSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableIntSet toImmutable()
    {
        return this;
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public int[] toArray()
    {
        return new int[0];
    }

    @Override
    public int[] toArray(int[] target)
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
    public IntIterator intIterator()
    {
        return ImmutableEmptyIntIterator.INSTANCE;
    }
}
