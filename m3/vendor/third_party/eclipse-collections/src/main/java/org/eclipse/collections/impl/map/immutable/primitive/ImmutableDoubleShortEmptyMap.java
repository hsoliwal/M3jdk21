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
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.DoubleShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.DoubleShortPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.factory.primitive.ShortDoubleMaps;
import org.eclipse.collections.impl.iterator.ImmutableEmptyShortIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyDoubleIterate;

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
