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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteLongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.ByteLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongByteMap;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.ByteLongPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.factory.primitive.LongByteMaps;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableByteSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyByteIterate;

/**
 * ImmutableByteLongEmptyMap is an optimization for {@link ImmutableByteLongMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteLongEmptyMap implements ImmutableByteLongMap, Serializable
{
    static final ImmutableByteLongMap INSTANCE = new ImmutableByteLongEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final long EMPTY_VALUE = 0L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public long get(byte key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public long getIfAbsent(byte key, long ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public long getOrThrow(byte key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(byte key)
    {
        return false;
    }

    @Override
    public boolean containsValue(long value)
    {
        return false;
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
    }

    @Override
    public void forEachKey(ByteProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(ByteLongProcedure procedure)
    {
    }

    @Override
    public LazyByteIterable keysView()
    {
        return LazyByteIterate.empty();
    }

    @Override
    public RichIterable<ByteLongPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableLongByteMap flipUniqueValues()
    {
        return LongByteMaps.immutable.empty();
    }

    @Override
    public ImmutableByteLongMap select(ByteLongPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteLongMap reject(ByteLongPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteLongMap toImmutable()
    {
        return this;
    }

    @Override
    public LongIterator longIterator()
    {
        return ImmutableEmptyLongIterator.INSTANCE;
    }

    @Override
    public void forEach(LongProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return 0;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public long min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return defaultValue;
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return new long[0];
    }

    @Override
    public MutableLongList toSortedList()
    {
        return new LongArrayList();
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableLongBag select(LongPredicate predicate)
    {
        return LongBags.immutable.empty();
    }

    @Override
    public ImmutableLongBag reject(LongPredicate predicate)
    {
        return LongBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public long[] toArray()
    {
        return new long[0];
    }

    @Override
    public long[] toArray(long[] target)
    {
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return false;
    }

    @Override
    public boolean containsAll(long... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableLongList toList()
    {
        return new LongArrayList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return new LongHashSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return new LongHashBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public ImmutableByteLongMap newWithKeyValue(byte key, long value)
    {
        return new ImmutableByteLongSingletonMap(key, value);
    }

    @Override
    public ImmutableByteLongMap newWithoutKey(byte key)
    {
        return this;
    }

    @Override
    public ImmutableByteLongMap newWithoutAllKeys(ByteIterable keys)
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
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(new LongArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ByteLongMap))
        {
            return false;
        }
        ByteLongMap map = (ByteLongMap) obj;
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
