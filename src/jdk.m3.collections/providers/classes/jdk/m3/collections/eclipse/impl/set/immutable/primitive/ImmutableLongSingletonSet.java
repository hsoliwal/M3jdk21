/*
 * Copyright (c) 2026 Goldman Sachs and others.
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

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.factory.primitive.LongBags;
import jdk.m3.collections.eclipse.api.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.LongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.impl.iterator.SingletonLongIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongSets;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyLongIterableAdapter;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;

/**
 * ImmutableLongSingletonSet is an optimization for {@link ImmutableLongSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongSingletonSet implements ImmutableLongSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final long element;

    ImmutableLongSingletonSet(long element)
    {
        this.element = element;
    }

    @Override
    public ImmutableLongSet newWith(long element)
    {
        return LongSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableLongSet newWithout(long element)
    {
        return this.element == element ? LongSets.immutable.with() : this;
    }

    @Override
    public ImmutableLongSet newWithAll(LongIterable elements)
    {
        return LongHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableLongSet newWithoutAll(LongIterable elements)
    {
        return elements.contains(this.element) ? LongSets.immutable.with() : this;
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
        return this.element == value;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (this.element != iterator.next())
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
            if (this.element != value)
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
        procedure.value(this.element);
    }

    @Override
    public ImmutableLongSet select(LongPredicate predicate)
    {
        return predicate.accept(this.element) ? LongHashSet.newSetWith(this.element).toImmutable()
                : new LongHashSet().toImmutable();
    }

    @Override
    public ImmutableLongSet reject(LongPredicate predicate)
    {
        return predicate.accept(this.element) ? new LongHashSet().toImmutable()
                : LongHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableLongList toList()
    {
        return LongLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(long item)
    {
        return this.element == item ? 1 : 0;
    }

    public void forEachWithOccurrences(LongIntProcedure longIntProcedure)
    {
        longIntProcedure.value(this.element, 1);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public long sum()
    {
        return this.element;
    }

    @Override
    public long min()
    {
        return this.element;
    }

    @Override
    public long max()
    {
        return this.element;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.element;
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.element;
    }

    @Override
    public double average()
    {
        return this.element;
    }

    @Override
    public double median()
    {
        return this.element;
    }

    @Override
    public LazyIterable<LongLongPair> cartesianProduct(LongSet set)
    {
        return LongSets.cartesianProduct(this, set);
    }

    @Override
    public long[] toSortedArray()
    {
        return new long[]{this.element};
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
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
        if (!(obj instanceof LongSet))
        {
            return false;
        }
        LongSet set = (LongSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.element ^ this.element >>> 32);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongSets.mutable.with(this.element);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongBags.mutable.with(this.element);
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
        return new long[]{this.element};
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < 1)
        {
            target = new long[]{this.element};
        }
        else
        {
            target[0] = this.element;
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
            appendable.append(String.valueOf(this.element));
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
        return new SingletonLongIterator(element);
    }
}
