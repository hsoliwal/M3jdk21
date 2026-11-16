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
import java.util.Set;

import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ObjectIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ObjectIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableIntCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectIntMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectIntPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.Sets;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntObjectMaps;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyIntIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyIntIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.utility.LazyIterate;

/**
 * ImmutableObjectIntEmptyMap is an optimization for {@link ImmutableObjectIntMap} of size 0.
 * This file was automatically generated from template file immutableObjectPrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("unchecked")
final class ImmutableObjectIntEmptyMap<K> implements ImmutableObjectIntMap<K>, Serializable
{
    static final ImmutableObjectIntMap<?> INSTANCE = new ImmutableObjectIntEmptyMap<>();

    private static final long serialVersionUID = 1L;
    private static final int EMPTY_VALUE = 0;

    private Object readResolve()
    {
        return INSTANCE;
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
    public ImmutableIntCollection select(IntPredicate predicate)
    {
        return IntLists.immutable.with();
    }

    @Override
    public ImmutableIntCollection reject(IntPredicate predicate)
    {
        return IntLists.immutable.with();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableCollection<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
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
    public ImmutableObjectIntMap<K> newWithKeyValue(K key, int value)
    {
        return new ImmutableObjectIntSingletonMap<>(key, value);
    }

    @Override
    public ImmutableObjectIntMap<K> newWithoutKey(K key)
    {
        return this;
    }

    @Override
    public ImmutableObjectIntMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        return this;
    }

    @Override
    public int get(Object key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public int getOrThrow(Object key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public int getIfAbsent(Object key, int ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
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
    public void forEachKey(Procedure<? super K> procedure)
    {
    }

    @Override
    public void forEachKeyValue(ObjectIntProcedure<? super K> objectIntProcedure)
    {
    }

    @Override
    public ImmutableObjectIntMap<K> select(ObjectIntPredicate<? super K> objectIntPredicate)
    {
        return this;
    }

    @Override
    public ImmutableObjectIntMap<K> reject(ObjectIntPredicate<? super K> objectIntPredicate)
    {
        return this;
    }

    @Override
    public ImmutableObjectIntMap<K> toImmutable()
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
    public Set<K> keySet()
    {
        return Sets.immutable.<K>of().castToSet();
    }

    @Override
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(new IntArrayList());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return LazyIterate.empty();
    }

    @Override
    public RichIterable<ObjectIntPair<K>> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableIntObjectMap<K> flipUniqueValues()
    {
        return IntObjectMaps.immutable.empty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectIntMap))
        {
            return false;
        }
        ObjectIntMap<K> map = (ObjectIntMap<K>) obj;
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
