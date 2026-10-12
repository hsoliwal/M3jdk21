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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.impl.factory.primitive.FloatSets;
import org.eclipse.collections.impl.iterator.ImmutableEmptyFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;

/**
 * ImmutableFloatEmptySet is an optimization for {@link ImmutableFloatSet} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptySet.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatEmptySet implements ImmutableFloatSet, Serializable
{
    static final ImmutableFloatSet INSTANCE = new ImmutableFloatEmptySet();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableFloatSet newWith(float element)
    {
        return new ImmutableFloatSingletonSet(element);
    }

    @Override
    public ImmutableFloatSet newWithout(float element)
    {
        return this;
    }

    @Override
    public ImmutableFloatSet newWithAll(FloatIterable elements)
    {
        return FloatSets.immutable.withAll(elements);
    }

    @Override
    public ImmutableFloatSet newWithoutAll(FloatIterable elements)
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
    public ImmutableFloatSet select(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatSet reject(FloatPredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableSet<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Sets.immutable.of();
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatLists.mutable.empty();
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
        return FloatLists.mutable.empty();
    }

    @Override
    public LazyIterable<FloatFloatPair> cartesianProduct(FloatSet set)
    {
        return FloatSets.cartesianProduct(this, set);
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
        if (!(obj instanceof FloatSet))
        {
            return false;
        }
        FloatSet set = (FloatSet) obj;
        return set.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatSets.mutable.empty();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatBags.mutable.empty();
    }

    @Override
    public FloatSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableFloatSet toImmutable()
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
