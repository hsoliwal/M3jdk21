/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.ShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.iterator.ImmutableEmptyShortIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.ShortIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.impl.factory.primitive.ShortSets;

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
