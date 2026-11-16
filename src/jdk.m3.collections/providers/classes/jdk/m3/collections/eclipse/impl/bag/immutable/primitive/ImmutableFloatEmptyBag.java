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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableFloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.FloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.FloatHashBag;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatBags;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyFloatIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatIntPair;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableFloatSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatSets;

/**
 * ImmutableFloatEmptyBag is an optimization for {@link ImmutableFloatBag} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatEmptyBag implements ImmutableFloatBag, Serializable
{
    static final ImmutableFloatBag INSTANCE = new ImmutableFloatEmptyBag();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableFloatBag newWith(float element)
    {
        return new ImmutableFloatSingletonBag(element);
    }

    @Override
    public ImmutableFloatBag newWithout(float element)
    {
        return this;
    }

    @Override
    public ImmutableFloatBag newWithAll(FloatIterable elements)
    {
        return FloatBags.immutable.withAll(elements);
    }

    @Override
    public ImmutableFloatBag newWithoutAll(FloatIterable elements)
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
    public boolean contains(float value)
    {
        return false;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(float... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(FloatProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
    }

    @Override
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatSet selectUnique()
    {
        return FloatSets.immutable.empty();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of();
    }

    @Override
    public MutableFloatList toList()
    {
        return new FloatArrayList();
    }

    @Override
    public int sizeDistinct()
    {
        return 0;
    }

    @Override
    public int occurrencesOf(float item)
    {
        return 0;
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure floatIntProcedure)
    {
    }

    @Override
    public ImmutableFloatBag selectByOccurrences(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableList<FloatIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableList<FloatIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return false;
    }

    @Override
    public double sum()
    {
        return 0.0;
    }

    @Override
    public float min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public float max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return defaultValue;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float[] toSortedArray()
    {
        return new float[0];
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return new FloatArrayList();
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
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
        if (!(obj instanceof FloatBag))
        {
            return false;
        }
        FloatBag bag = (FloatBag) obj;
        return bag.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableFloatSet toSet()
    {
        return new FloatHashSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return new FloatHashBag();
    }

    @Override
    public ImmutableFloatBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public float[] toArray()
    {
        return new float[0];
    }

    @Override
    public float[] toArray(float[] target)
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
    public FloatIterator floatIterator()
    {
        return ImmutableEmptyFloatIterator.INSTANCE;
    }
}
