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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;

/**
 * ImmutableLongEmptySet is an optimization for {@link ImmutableLongSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongEmptySet implements ImmutableLongSet, Serializable
{
    static final ImmutableLongSet INSTANCE = new ImmutableLongEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableLongSet newWith(long element)
    {
        return new ImmutableLongSingletonSet(element);
    }

    @Override
    public ImmutableLongSet newWithout(long element)
    {
        return this;
    }

    @Override
    public ImmutableLongSet newWithAll(LongIterable elements)
    {
        return LongSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableLongSet newWithoutAll(LongIterable elements)
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
    public boolean contains(long value)
    {
        return false;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(long... elements)
    {
        return elements.length == 0;
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
    public ImmutableLongSet select(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableLongSet reject(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableLongList toList()
    {
        return LongLists.mutable.empty();
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return ifNone;
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
    public long sum()
    {
        return 0L;
    }

    @Override
    public long min()
    {
        throw new NoSuchElementException();
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
        return LongLists.mutable.empty();
    }

    @Override
    public LazyIterable<LongLongPair> cartesianProduct(LongSet set)
    {
        return LongSets.cartesianProduct(this, set);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return true;
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongSet))
        {
            return false;
        }
        LongSet set = (LongSet) obj;
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongSets.mutable.empty();
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongBags.mutable.empty();
    }

    @Override
    public LongSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableLongSet toImmutable()
    {
        return this;
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
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
    public LongIterator longIterator()
    {
        return ImmutableEmptyLongIterator.INSTANCE;
    }
}
