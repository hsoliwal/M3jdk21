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

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoubleShortPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.map.primitive.DoubleShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableDoubleShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortDoubleMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleShortPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortDoubleMaps;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyShortIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.DoubleHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import jdk.m3.collections.eclipse.impl.utility.LazyIterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LazyDoubleIterate;

/**
 * ImmutableDoubleShortEmptyMap is an optimization for {@link ImmutableDoubleShortMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleShortEmptyMap implements ImmutableDoubleShortMap, Serializable
{
    static final ImmutableDoubleShortMap INSTANCE = new ImmutableDoubleShortEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final short EMPTY_VALUE = (short) 0;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public short get(double key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public short getIfAbsent(double key, short ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public short getOrThrow(double key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(double key)
    {
        return false;
    }

    @Override
    public boolean containsValue(short value)
    {
        return false;
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(DoubleShortProcedure procedure)
    {
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(DoubleShortPredicate predicate)
    {
        return true;
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return LazyDoubleIterate.empty();
    }

    @Override
    public RichIterable<DoubleShortPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableShortDoubleMap flipUniqueValues()
    {
        return ShortDoubleMaps.immutable.empty();
    }

    @Override
    public ImmutableDoubleShortMap select(DoubleShortPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableDoubleShortMap reject(DoubleShortPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableDoubleShortMap toImmutable()
    {
        return this;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return ImmutableEmptyShortIterator.INSTANCE;
    }

    @Override
    public void forEach(ShortProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return 0;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public short min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public short max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return defaultValue;
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return new short[0];
    }

    @Override
    public MutableShortList toSortedList()
    {
        return new ShortArrayList();
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return ShortBags.immutable.empty();
    }

    @Override
    public ImmutableShortBag reject(ShortPredicate predicate)
    {
        return ShortBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public short[] toArray()
    {
        return new short[0];
    }

    @Override
    public short[] toArray(short[] target)
    {
        return target;
    }

    @Override
    public boolean contains(short value)
    {
        return false;
    }

    @Override
    public boolean containsAll(short... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableShortList toList()
    {
        return new ShortArrayList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return new ShortHashSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return new ShortHashBag();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleShortMap newWithKeyValue(double key, short value)
    {
        return new ImmutableDoubleShortSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleShortMap newWithoutKey(double key)
    {
        return this;
    }

    @Override
    public ImmutableDoubleShortMap newWithoutAllKeys(DoubleIterable keys)
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
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(new ShortArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof DoubleShortMap))
        {
            return false;
        }
        DoubleShortMap map = (DoubleShortMap) obj;
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
