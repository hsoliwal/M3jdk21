/*
 * Copyright (c) 2022 Goldman Sachs and others.
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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.LongBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.iterator.SingletonLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;

/**
 * ImmutableLongSingletonBag is an optimization for {@link ImmutableLongBag} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongSingletonBag implements ImmutableLongBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final long element1;

    ImmutableLongSingletonBag(long element)
    {
        this.element1 = element;
    }

    @Override
    public ImmutableLongBag newWith(long element)
    {
        return LongBags.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableLongBag newWithout(long element)
    {
        return this.element1 == element ? LongBags.immutable.with() : this;
    }

    @Override
    public ImmutableLongBag newWithAll(LongIterable elements)
    {
        return LongHashBag.newBag(elements).with(this.element1).toImmutable();
    }

    @Override
    public ImmutableLongBag newWithoutAll(LongIterable elements)
    {
        return elements.contains(this.element1) ? LongBags.immutable.with() : this;
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
    public boolean contains(long value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public ImmutableLongBag select(LongPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? LongBags.immutable.with(this.element1)
            : LongBags.immutable.empty();
    }

    @Override
    public ImmutableLongBag selectByOccurrences(IntPredicate predicate)
    {
        return predicate.accept(1)
            ? LongBags.immutable.with(this.element1)
            : LongBags.immutable.empty();
    }

    @Override
    public ImmutableLongSet selectUnique()
    {
        return LongSets.immutable.of(this.element1);
    }

    @Override
    public ImmutableList<LongIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableList<LongIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        if (count == 0)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, 1));
    }

    @Override
    public ImmutableLongBag reject(LongPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? LongBags.immutable.empty()
            : LongBags.immutable.with(this.element1);
    }

    @Override
    public <V> ImmutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Bags.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newListWith(this.element1);
    }

    @Override
    public int sizeDistinct()
    {
        return 1;
    }

    @Override
    public int occurrencesOf(long item)
    {
        return this.element1 == item ? 1 : 0;
    }

    @Override
    public void forEachWithOccurrences(LongIntProcedure longIntProcedure)
    {
        longIntProcedure.value(this.element1, 1);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public long min()
    {
        return this.element1;
    }

    @Override
    public long max()
    {
        return this.element1;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.element1;
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return new long[]{this.element1};
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newListWith(this.element1);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        return Lists.immutable.with(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongBag))
        {
            return false;
        }
        LongBag bag = (LongBag) obj;
        if (bag.size() != 1)
        {
            return false;
        }
        return this.occurrencesOf(this.element1) == bag.occurrencesOf(this.element1);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.element1 ^ this.element1 >>> 32) ^ 1;
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBagWith(this.element1);
    }

    @Override
    public ImmutableLongBag toImmutable()
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
        return new long[]{this.element1};
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < 1)
        {
            target = new long[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public String toString()
    {
        return '[' + this.makeString() + ']';
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

    @Override
    public LongIterator longIterator()
    {
        return new SingletonLongIterator(element1);
    }
}
