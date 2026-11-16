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

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableIntBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ByteIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.map.primitive.ByteIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableByteIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntByteMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteIntPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntBags;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntByteMaps;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyIntIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyIntIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ByteHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableByteSet;
import jdk.m3.collections.eclipse.impl.utility.LazyIterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LazyByteIterate;

/**
 * ImmutableByteIntEmptyMap is an optimization for {@link ImmutableByteIntMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteIntEmptyMap implements ImmutableByteIntMap, Serializable
{
    static final ImmutableByteIntMap INSTANCE = new ImmutableByteIntEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final int EMPTY_VALUE = 0;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public int get(byte key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public int getIfAbsent(byte key, int ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public int getOrThrow(byte key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(byte key)
    {
        return false;
    }

    @Override
    public boolean containsValue(int value)
    {
        return false;
    }

    @Override
    public void forEachValue(IntProcedure procedure)
    {
    }

    @Override
    public void forEachKey(ByteProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(ByteIntProcedure procedure)
    {
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(ByteIntPredicate predicate)
    {
        return true;
    }

    @Override
    public LazyByteIterable keysView()
    {
        return LazyByteIterate.empty();
    }

    @Override
    public RichIterable<ByteIntPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableIntByteMap flipUniqueValues()
    {
        return IntByteMaps.immutable.empty();
    }

    @Override
    public ImmutableByteIntMap select(ByteIntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteIntMap reject(ByteIntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteIntMap toImmutable()
    {
        return this;
    }

    @Override
    public IntIterator intIterator()
    {
        return ImmutableEmptyIntIterator.INSTANCE;
    }

    @Override
    public void forEach(IntProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return 0;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public int min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return defaultValue;
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return new int[0];
    }

    @Override
    public MutableIntList toSortedList()
    {
        return new IntArrayList();
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableIntBag select(IntPredicate predicate)
    {
        return IntBags.immutable.empty();
    }

    @Override
    public ImmutableIntBag reject(IntPredicate predicate)
    {
        return IntBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public int[] toArray()
    {
        return new int[0];
    }

    @Override
    public int[] toArray(int[] target)
    {
        return target;
    }

    @Override
    public boolean contains(int value)
    {
        return false;
    }

    @Override
    public boolean containsAll(int... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableIntList toList()
    {
        return new IntArrayList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return new IntHashSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return new IntHashBag();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public ImmutableByteIntMap newWithKeyValue(byte key, int value)
    {
        return new ImmutableByteIntSingletonMap(key, value);
    }

    @Override
    public ImmutableByteIntMap newWithoutKey(byte key)
    {
        return this;
    }

    @Override
    public ImmutableByteIntMap newWithoutAllKeys(ByteIterable keys)
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
    public MutableByteSet keySet()
    {
        return UnmodifiableByteSet.of(new ByteHashSet());
    }

    @Override
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(new IntArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ByteIntMap))
        {
            return false;
        }
        ByteIntMap map = (ByteIntMap) obj;
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
