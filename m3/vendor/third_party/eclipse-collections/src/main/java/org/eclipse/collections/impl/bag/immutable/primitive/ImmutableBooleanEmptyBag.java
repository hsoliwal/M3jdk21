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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.BooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.iterator.ImmutableEmptyBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.set.primitive.ImmutableBooleanSet;
import org.eclipse.collections.impl.factory.primitive.BooleanSets;

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
