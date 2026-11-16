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
import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableByteBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableByteBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongBytePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongByteProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableByteCollection;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.map.primitive.LongByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableByteLongMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongBytePair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ByteHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongByteMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteLongMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableByteIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyByteIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ByteArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.LongByteHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ByteHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableLongSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableLongByteSingletonMap is an optimization for {@link ImmutableLongByteMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongByteSingletonMap implements ImmutableLongByteMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_VALUE = (byte) 0;
    private final long key1;
    private final byte value1;

    ImmutableLongByteSingletonMap(long key1, byte value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public byte get(long key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getIfAbsent(long key, byte ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public byte getOrThrow(long key)
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
    public boolean containsValue(byte value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(ByteProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(LongByteProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(LongBytePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LongLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<LongBytePair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableByteLongMap flipUniqueValues()
    {
        return ByteLongMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableLongByteMap select(LongBytePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? LongByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new LongByteHashMap().toImmutable();
    }

    @Override
    public ImmutableLongByteMap reject(LongBytePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new LongByteHashMap().toImmutable()
                : LongByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(ByteBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableLongByteMap toImmutable()
    {
        return this;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(LongByteHashMap.newWithKeysValues(this.key1, this.value1).byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public byte min()
    {
        return this.value1;
    }

    @Override
    public byte max()
    {
        return this.value1;
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.value1;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
    public byte[] toSortedArray()
    {
        return new byte[]{this.value1};
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableByteBag select(BytePredicate predicate)
    {
        return predicate.accept(this.value1) ? ByteHashBag.newBagWith(this.value1).toImmutable() : ByteBags.immutable.empty();
    }

    @Override
    public ImmutableByteBag reject(BytePredicate predicate)
    {
        return predicate.accept(this.value1) ? ByteBags.immutable.empty() : ByteHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public byte[] toArray()
    {
        return new byte[]{this.value1};
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < 1)
        {
            target = new byte[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newListWith(this.value1);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public ImmutableLongByteMap newWithKeyValue(long key, byte value)
    {
        return LongByteMaps.immutable.withAll(LongByteHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableLongByteMap newWithoutKey(long key)
    {
        return this.key1 == key ? LongByteMaps.immutable.with() : this;
    }

    @Override
    public ImmutableLongByteMap newWithoutAllKeys(LongIterable keys)
    {
        return keys.contains(this.key1) ? LongByteMaps.immutable.with() : this;
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
    public MutableByteCollection values()
    {
        return UnmodifiableByteCollection.of(ByteArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongByteMap))
        {
            return false;
        }
        LongByteMap map = (LongByteMap) obj;
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
