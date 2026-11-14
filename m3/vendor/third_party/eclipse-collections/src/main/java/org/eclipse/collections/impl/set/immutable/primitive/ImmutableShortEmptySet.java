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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.impl.factory.primitive.ShortSets;
import org.eclipse.collections.impl.iterator.ImmutableEmptyShortIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;

/**
 * ImmutableShortEmptySet is an optimization for {@link ImmutableShortSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortEmptySet implements ImmutableShortSet, Serializable
{
    static final ImmutableShortSet INSTANCE = new ImmutableShortEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableShortSet newWith(short element)
    {
        return new ImmutableShortSingletonSet(element);
    }

    @Override
    public ImmutableShortSet newWithout(short element)
    {
        return this;
    }

    @Override
    public ImmutableShortSet newWithAll(ShortIterable elements)
    {
        return ShortSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableShortSet newWithoutAll(ShortIterable elements)
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
    public ImmutableShortSet select(ShortPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableShortSet reject(ShortPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableShortList toList()
    {
        return ShortLists.mutable.empty();
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
        return ShortLists.mutable.empty();
    }

    @Override
    public LazyIterable<ShortShortPair> cartesianProduct(ShortSet set)
    {
        return ShortSets.cartesianProduct(this, set);
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
        if (!(obj instanceof ShortSet))
        {
            return false;
        }
        ShortSet set = (ShortSet) obj;
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortSets.mutable.empty();
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortBags.mutable.empty();
    }

    @Override
    public ShortSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableShortSet toImmutable()
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
