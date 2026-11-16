/*
 * Copyright (c) 2026 Goldman Sachs and others.
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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableIntBag;
import jdk.m3.collections.eclipse.api.bag.primitive.IntBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntBags;
import jdk.m3.collections.eclipse.impl.iterator.SingletonIntIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyIntIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntIntPair;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableIntSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntSets;

/**
 * ImmutableIntSingletonBag is an optimization for {@link ImmutableIntBag} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntSingletonBag implements ImmutableIntBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final int element1;

    ImmutableIntSingletonBag(int element)
    {
        this.element1 = element;
    }

    @Override
    public ImmutableIntBag newWith(int element)
    {
        return IntBags.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableIntBag newWithout(int element)
    {
        return this.element1 == element ? IntBags.immutable.with() : this;
    }

    @Override
    public ImmutableIntBag newWithAll(IntIterable elements)
    {
        return IntHashBag.newBag(elements).with(this.element1).toImmutable();
    }

    @Override
    public ImmutableIntBag newWithoutAll(IntIterable elements)
    {
        return elements.contains(this.element1) ? IntBags.immutable.with() : this;
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
    public boolean contains(int value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        for (IntIterator iterator = source.intIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int value : source)
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
    public void each(IntProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public ImmutableIntBag select(IntPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? IntBags.immutable.with(this.element1)
            : IntBags.immutable.empty();
    }

    @Override
    public ImmutableIntBag selectByOccurrences(IntPredicate predicate)
    {
        return predicate.accept(1)
            ? IntBags.immutable.with(this.element1)
            : IntBags.immutable.empty();
    }

    @Override
    public ImmutableIntSet selectUnique()
    {
        return IntSets.immutable.of(this.element1);
    }

    @Override
    public ImmutableList<IntIntPair> topOccurrences(int count)
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
    public ImmutableList<IntIntPair> bottomOccurrences(int count)
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
    public ImmutableIntBag reject(IntPredicate predicate)
    {
        return predicate.accept(this.element1)
            ? IntBags.immutable.empty()
            : IntBags.immutable.with(this.element1);
    }

    @Override
    public <V> ImmutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Bags.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public MutableIntList toList()
    {
        return IntArrayList.newListWith(this.element1);
    }

    @Override
    public int sizeDistinct()
    {
        return 1;
    }

    @Override
    public int occurrencesOf(int item)
    {
        return this.element1 == item ? 1 : 0;
    }

    @Override
    public void forEachWithOccurrences(IntIntProcedure intIntProcedure)
    {
        intIntProcedure.value(this.element1, 1);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public int min()
    {
        return this.element1;
    }

    @Override
    public int max()
    {
        return this.element1;
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.element1;
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return new int[]{this.element1};
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntArrayList.newListWith(this.element1);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
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
        if (!(obj instanceof IntBag))
        {
            return false;
        }
        IntBag bag = (IntBag) obj;
        if (bag.size() != 1)
        {
            return false;
        }
        return this.occurrencesOf(this.element1) == bag.occurrencesOf(this.element1);
    }

    @Override
    public int hashCode()
    {
        return this.element1 ^ 1;
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntHashBag.newBagWith(this.element1);
    }

    @Override
    public ImmutableIntBag toImmutable()
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
        return new int[]{this.element1};
    }

    @Override
    public int[] toArray(int[] target)
    {
        if (target.length < 1)
        {
            target = new int[]{this.element1};
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
    public IntIterator intIterator()
    {
        return new SingletonIntIterator(element1);
    }
}
