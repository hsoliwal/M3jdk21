/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleFloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.DoubleFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.DoubleFloatPair;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.factory.primitive.FloatDoubleMaps;
import org.eclipse.collections.impl.iterator.ImmutableEmptyFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyDoubleIterate;

/**
 * ImmutableDoubleFloatEmptyMap is an optimization for {@link ImmutableDoubleFloatMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleFloatEmptyMap implements ImmutableDoubleFloatMap, Serializable
{
    static final ImmutableDoubleFloatMap INSTANCE = new ImmutableDoubleFloatEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final float EMPTY_VALUE = 0.0f;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public float get(double key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public float getIfAbsent(double key, float ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public float getOrThrow(double key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(double key)
    {
        return false;
    }

    @Override
    public boolean containsValue(float value)
    {
        return false;
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(DoubleFloatProcedure procedure)
    {
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return LazyDoubleIterate.empty();
    }

    @Override
    public RichIterable<DoubleFloatPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableFloatDoubleMap flipUniqueValues()
    {
        return FloatDoubleMaps.immutable.empty();
    }

    @Override
    public ImmutableDoubleFloatMap select(DoubleFloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableDoubleFloatMap reject(DoubleFloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableDoubleFloatMap toImmutable()
    {
        return this;
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
    public int count(FloatPredicate predicate)
    {
        return 0;
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
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return FloatBags.immutable.empty();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return FloatBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return ifNone;
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
    public ImmutableDoubleFloatMap newWithKeyValue(double key, float value)
    {
        return new ImmutableDoubleFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleFloatMap newWithoutKey(double key)
    {
        return this;
    }

    @Override
    public ImmutableDoubleFloatMap newWithoutAllKeys(DoubleIterable keys)
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
    public MutableDoubleSet keySet()
    {
        return UnmodifiableDoubleSet.of(new DoubleHashSet());
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(new FloatArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof DoubleFloatMap))
        {
            return false;
        }
        DoubleFloatMap map = (DoubleFloatMap) obj;
        return map.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{}";
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
}
