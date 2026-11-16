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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.BooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.BooleanHashBag;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanBags;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyBooleanIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.BooleanArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.BooleanHashSet;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanIntPair;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableBooleanSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanSets;

/**
 * ImmutableBooleanEmptyBag is an optimization for {@link ImmutableBooleanBag} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableBooleanEmptyBag implements ImmutableBooleanBag, Serializable
{
    static final ImmutableBooleanBag INSTANCE = new ImmutableBooleanEmptyBag();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableBooleanBag newWith(boolean element)
    {
        return new ImmutableBooleanSingletonBag(element);
    }

    @Override
    public ImmutableBooleanBag newWithout(boolean element)
    {
        return this;
    }

    @Override
    public ImmutableBooleanBag newWithAll(BooleanIterable elements)
    {
        return BooleanBags.immutable.withAll(elements);
    }

    @Override
    public ImmutableBooleanBag newWithoutAll(BooleanIterable elements)
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
    public boolean contains(boolean value)
    {
        return false;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(boolean... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(BooleanProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
    }

    @Override
    public ImmutableBooleanBag select(BooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableBooleanSet selectUnique()
    {
        return BooleanSets.immutable.empty();
    }

    @Override
    public ImmutableBooleanBag reject(BooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of();
    }

    @Override
    public MutableBooleanList toList()
    {
        return new BooleanArrayList();
    }

    @Override
    public int sizeDistinct()
    {
        return 0;
    }

    @Override
    public int occurrencesOf(boolean item)
    {
        return 0;
    }

    @Override
    public void forEachWithOccurrences(BooleanIntProcedure booleanIntProcedure)
    {
    }

    @Override
    public ImmutableBooleanBag selectByOccurrences(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableList<BooleanIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableList<BooleanIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
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
        if (!(obj instanceof BooleanBag))
        {
            return false;
        }
        BooleanBag bag = (BooleanBag) obj;
        return bag.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return new BooleanHashSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return new BooleanHashBag();
    }

    @Override
    public ImmutableBooleanBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[0];
    }

    @Override
    public boolean[] toArray(boolean[] target)
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
    public BooleanIterator booleanIterator()
    {
        return ImmutableEmptyBooleanIterator.INSTANCE;
    }
}
