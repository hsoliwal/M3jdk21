/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableFloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatFloatPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatFloatProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableFloatCollection;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.map.primitive.FloatFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatFloatMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.FloatHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatBags;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatFloatMaps;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyFloatIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableFloatSet;
import jdk.m3.collections.eclipse.impl.utility.LazyIterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LazyFloatIterate;

/**
 * ImmutableFloatFloatEmptyMap is an optimization for {@link ImmutableFloatFloatMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatFloatEmptyMap implements ImmutableFloatFloatMap, Serializable
{
    static final ImmutableFloatFloatMap INSTANCE = new ImmutableFloatFloatEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final float EMPTY_VALUE = 0.0f;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public float get(float key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public float getIfAbsent(float key, float ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public float getOrThrow(float key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(float key)
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
    public void forEachKey(FloatProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(FloatFloatProcedure procedure)
    {
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(FloatFloatPredicate predicate)
    {
        return true;
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return LazyFloatIterate.empty();
    }

    @Override
    public RichIterable<FloatFloatPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableFloatFloatMap flipUniqueValues()
    {
        return FloatFloatMaps.immutable.empty();
    }

    @Override
    public ImmutableFloatFloatMap select(FloatFloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatFloatMap reject(FloatFloatPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatFloatMap toImmutable()
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
    public ImmutableFloatFloatMap newWithKeyValue(float key, float value)
    {
        return new ImmutableFloatFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatFloatMap newWithoutKey(float key)
    {
        return this;
    }

    @Override
    public ImmutableFloatFloatMap newWithoutAllKeys(FloatIterable keys)
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
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(new FloatHashSet());
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
        if (!(obj instanceof FloatFloatMap))
        {
            return false;
        }
        FloatFloatMap map = (FloatFloatMap) obj;
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
