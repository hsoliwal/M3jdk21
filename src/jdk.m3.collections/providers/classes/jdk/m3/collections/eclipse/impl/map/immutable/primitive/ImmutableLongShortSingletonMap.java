/*
 * Copyright (c) 2026 Goldman Sachs and others.
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

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.map.primitive.LongShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortLongMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongShortPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongShortMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLongMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableShortIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.LongShortHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableLongSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableLongShortSingletonMap is an optimization for {@link ImmutableLongShortMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongShortSingletonMap implements ImmutableLongShortMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_VALUE = (short) 0;
    private final long key1;
    private final short value1;

    ImmutableLongShortSingletonMap(long key1, short value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public short get(long key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public short getIfAbsent(long key, short ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public short getOrThrow(long key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(long key)
    {
        return this.key1 == key;
    }

    @Override
    public boolean containsValue(short value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(LongShortProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(LongShortPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LongLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<LongShortPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableShortLongMap flipUniqueValues()
    {
        return ShortLongMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableLongShortMap select(LongShortPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? LongShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new LongShortHashMap().toImmutable();
    }

    @Override
    public ImmutableLongShortMap reject(LongShortPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new LongShortHashMap().toImmutable()
                : LongShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(ShortBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableLongShortMap toImmutable()
    {
        return this;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(LongShortHashMap.newWithKeysValues(this.key1, this.value1).shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public short min()
    {
        return this.value1;
    }

    @Override
    public short max()
    {
        return this.value1;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.value1;
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        return this.value1;
    }

    @Override
    public double average()
    {
        return this.value1;
    }

    @Override
    public double median()
    {
        return this.value1;
    }

    @Override
    public short[] toSortedArray()
    {
        return new short[]{this.value1};
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortHashBag.newBagWith(this.value1).toImmutable() : ShortBags.immutable.empty();
    }

    @Override
    public ImmutableShortBag reject(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortBags.immutable.empty() : ShortHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public short[] toArray()
    {
        return new short[]{this.value1};
    }

    @Override
    public short[] toArray(short[] target)
    {
        if (target.length < 1)
        {
            target = new short[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(short value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        for (ShortIterator iterator = source.shortIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableShortList toList()
    {
        return ShortArrayList.newListWith(this.value1);
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public ImmutableLongShortMap newWithKeyValue(long key, short value)
    {
        return LongShortMaps.immutable.withAll(LongShortHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableLongShortMap newWithoutKey(long key)
    {
        return this.key1 == key ? LongShortMaps.immutable.with() : this;
    }

    @Override
    public ImmutableLongShortMap newWithoutAllKeys(LongIterable keys)
    {
        return keys.contains(this.key1) ? LongShortMaps.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(LongHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(ShortArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongShortMap))
        {
            return false;
        }
        LongShortMap map = (LongShortMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.key1 ^ this.key1 >>> 32) ^ (int) this.value1;
    }

    @Override
    public String toString()
    {
        return "{" + this.key1 + "=" + this.value1 + "}";
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.value1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
