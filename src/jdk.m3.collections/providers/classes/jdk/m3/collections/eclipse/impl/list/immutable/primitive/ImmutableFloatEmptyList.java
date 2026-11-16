/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.FloatList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableFloatStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.FloatHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatStacks;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyFloatIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseFloatIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;

/**
 * ImmutableFloatEmptyList is an optimization for {@link ImmutableFloatList} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyList.stg.
 */
final class ImmutableFloatEmptyList implements ImmutableFloatList, Serializable
{
    static final ImmutableFloatList INSTANCE = new ImmutableFloatEmptyList();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public float get(int index)
    {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public float getFirst()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public float getLast()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int indexOf(float value)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(float value)
    {
        return -1;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return ImmutableEmptyFloatIterator.INSTANCE;
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
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
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
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableFloatList select(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatList reject(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public double sum()
    {
        return 0;
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
    public float min()
    {
        throw new NoSuchElementException();
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
    public int binarySearch(float value)
    {
        return -1;
    }

    @Override
    public double dotProduct(FloatList list)
    {
        if (!list.isEmpty())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return 0;
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
    public boolean contains(float value)
    {
        return false;
    }

    @Override
    public boolean containsAll(float... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public MutableFloatList toList()
    {
        return new FloatArrayList();
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
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableFloatEmptyList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableFloatList newWith(float element)
    {
        return FloatLists.immutable.with(element);
    }

    @Override
    public ImmutableFloatList newWithout(float element)
    {
        return this;
    }

    @Override
    public ImmutableFloatList newWithAll(FloatIterable elements)
    {
        return FloatLists.immutable.withAll(elements);
    }

    @Override
    public ImmutableFloatList newWithoutAll(FloatIterable elements)
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
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof FloatList))
        {
            return false;
        }
        FloatList list = (FloatList) otherList;
        return list.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 1;
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

    /**
     * @since 6.0.
     */
    @Override
    public ImmutableFloatList distinct()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableFloatList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        return Lists.immutable.empty();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        return Lists.immutable.empty();
    }

    @Override
    public MutableFloatStack toStack()
    {
        return FloatStacks.mutable.empty();
    }
}
