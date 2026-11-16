/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyShortIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortIntPair;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableShortSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;

/**
 * ImmutableShortEmptyBag is an optimization for {@link ImmutableShortBag} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortEmptyBag implements ImmutableShortBag, Serializable
{
    static final ImmutableShortBag INSTANCE = new ImmutableShortEmptyBag();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableShortBag newWith(short element)
    {
        return new ImmutableShortSingletonBag(element);
    }

    @Override
    public ImmutableShortBag newWithout(short element)
    {
        return this;
    }

    @Override
    public ImmutableShortBag newWithAll(ShortIterable elements)
    {
        return ShortBags.immutable.withAll(elements);
    }

    @Override
    public ImmutableShortBag newWithoutAll(ShortIterable elements)
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
    public boolean contains(short value)
    {
        return false;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(short... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(ShortProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
    }

    @Override
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableShortSet selectUnique()
    {
        return ShortSets.immutable.empty();
    }

    @Override
    public ImmutableShortBag reject(ShortPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of();
    }

    @Override
    public MutableShortList toList()
    {
        return new ShortArrayList();
    }

    @Override
    public int sizeDistinct()
    {
        return 0;
    }

    @Override
    public int occurrencesOf(short item)
    {
        return 0;
    }

    @Override
    public void forEachWithOccurrences(ShortIntProcedure shortIntProcedure)
    {
    }

    @Override
    public ImmutableShortBag selectByOccurrences(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableList<ShortIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableList<ShortIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return false;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public short min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public short max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return defaultValue;
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return new short[0];
    }

    @Override
    public MutableShortList toSortedList()
    {
        return new ShortArrayList();
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
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
        if (!(obj instanceof ShortBag))
        {
            return false;
        }
        ShortBag bag = (ShortBag) obj;
        return bag.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableShortSet toSet()
    {
        return new ShortHashSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return new ShortHashBag();
    }

    @Override
    public ImmutableShortBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public short[] toArray()
    {
        return new short[0];
    }

    @Override
    public short[] toArray(short[] target)
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
    public ShortIterator shortIterator()
    {
        return ImmutableEmptyShortIterator.INSTANCE;
    }
}
