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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.ShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.impl.iterator.SingletonShortIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortShortPair;

/**
 * ImmutableShortSingletonSet is an optimization for {@link ImmutableShortSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortSingletonSet implements ImmutableShortSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final short element;

    ImmutableShortSingletonSet(short element)
    {
        this.element = element;
    }

    @Override
    public ImmutableShortSet newWith(short element)
    {
        return ShortSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableShortSet newWithout(short element)
    {
        return this.element == element ? ShortSets.immutable.with() : this;
    }

    @Override
    public ImmutableShortSet newWithAll(ShortIterable elements)
    {
        return ShortHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableShortSet newWithoutAll(ShortIterable elements)
    {
        return elements.contains(this.element) ? ShortSets.immutable.with() : this;
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
    public boolean contains(short value)
    {
        return this.element == value;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        for (ShortIterator iterator = source.shortIterator(); iterator.hasNext(); )
        {
            if (this.element != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short value : source)
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
    public void each(ShortProcedure procedure)
    {
        procedure.value(this.element);
    }

    @Override
    public ImmutableShortSet select(ShortPredicate predicate)
    {
        return predicate.accept(this.element) ? ShortHashSet.newSetWith(this.element).toImmutable()
                : new ShortHashSet().toImmutable();
    }

    @Override
    public ImmutableShortSet reject(ShortPredicate predicate)
    {
        return predicate.accept(this.element) ? new ShortHashSet().toImmutable()
                : ShortHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableShortList toList()
    {
        return ShortLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(short item)
    {
        return this.element == item ? 1 : 0;
    }

    public void forEachWithOccurrences(ShortIntProcedure shortIntProcedure)
    {
        shortIntProcedure.value(this.element, 1);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public long sum()
    {
        return this.element;
    }

    @Override
    public short min()
    {
        return this.element;
    }

    @Override
    public short max()
    {
        return this.element;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.element;
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public LazyIterable<ShortShortPair> cartesianProduct(ShortSet set)
    {
        return ShortSets.cartesianProduct(this, set);
    }

    @Override
    public short[] toSortedArray()
    {
        return new short[]{this.element};
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
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
        if (!(obj instanceof ShortSet))
        {
            return false;
        }
        ShortSet set = (ShortSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return (int) this.element;
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortSets.mutable.with(this.element);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortBags.mutable.with(this.element);
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
        return new short[]{this.element};
    }

    @Override
    public short[] toArray(short[] target)
    {
        if (target.length < 1)
        {
            target = new short[]{this.element};
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
    public ShortIterator shortIterator()
    {
        return new SingletonShortIterator(element);
    }
}
