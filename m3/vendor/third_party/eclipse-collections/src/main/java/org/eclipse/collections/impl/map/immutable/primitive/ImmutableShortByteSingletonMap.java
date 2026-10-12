/*
 * Copyright (c) 2022 Goldman Sachs and others.
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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortBytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.ShortByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteShortMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ShortBytePair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ShortByteMaps;
import org.eclipse.collections.impl.factory.primitive.ByteShortMaps;
import org.eclipse.collections.impl.factory.primitive.ShortLists;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ShortByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableShortByteSingletonMap is an optimization for {@link ImmutableShortByteMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortByteSingletonMap implements ImmutableShortByteMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_VALUE = (byte) 0;
    private final short key1;
    private final byte value1;

    ImmutableShortByteSingletonMap(short key1, byte value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public byte get(short key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getIfAbsent(short key, byte ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public byte getOrThrow(short key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(short key)
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
    public void forEachKey(ShortProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ShortByteProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return ShortLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ShortBytePair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableByteShortMap flipUniqueValues()
    {
        return ByteShortMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableShortByteMap select(ShortBytePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? ShortByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new ShortByteHashMap().toImmutable();
    }

    @Override
    public ImmutableShortByteMap reject(ShortBytePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new ShortByteHashMap().toImmutable()
                : ShortByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
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
    public ImmutableShortByteMap toImmutable()
    {
        return this;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(ShortByteHashMap.newWithKeysValues(this.key1, this.value1).byteIterator());
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
    public ImmutableShortByteMap newWithKeyValue(short key, byte value)
    {
        return ShortByteMaps.immutable.withAll(ShortByteHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableShortByteMap newWithoutKey(short key)
    {
        return this.key1 == key ? ShortByteMaps.immutable.with() : this;
    }

    @Override
    public ImmutableShortByteMap newWithoutAllKeys(ShortIterable keys)
    {
        return keys.contains(this.key1) ? ShortByteMaps.immutable.with() : this;
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
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(ShortHashSet.newSetWith(this.key1));
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
        if (!(obj instanceof ShortByteMap))
        {
            return false;
        }
        ShortByteMap map = (ShortByteMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) this.key1 ^ (int) this.value1;
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
