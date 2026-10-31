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
import java.util.Set;

import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.factory.primitive.ByteObjectMaps;
import org.eclipse.collections.impl.iterator.ImmutableEmptyByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.utility.LazyIterate;

/**
 * ImmutableObjectByteEmptyMap is an optimization for {@link ImmutableObjectByteMap} of size 0.
 * This file was automatically generated from template file immutableObjectPrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableObjectByteEmptyMap<K> implements ImmutableObjectByteMap<K>, Serializable
{
    static final ImmutableObjectByteMap<?> INSTANCE = new ImmutableObjectByteEmptyMap<>();

    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_VALUE = (byte) 0;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return ImmutableEmptyByteIterator.INSTANCE;
    }

    @Override
    public void forEach(ByteProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableByteCollection select(BytePredicate predicate)
    {
        return ByteLists.immutable.with();
    }

    @Override
    public ImmutableByteCollection reject(BytePredicate predicate)
    {
        return ByteLists.immutable.with();
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableCollection<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public byte min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return defaultValue;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
    public byte[] toSortedArray()
    {
        return new byte[0];
    }

    @Override
    public MutableByteList toSortedList()
    {
        return new ByteArrayList();
    }

    @Override
    public byte[] toArray()
    {
        return new byte[0];
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        return false;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableByteList toList()
    {
        return new ByteArrayList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return new ByteHashSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return new ByteHashBag();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public ImmutableObjectByteMap<K> newWithKeyValue(K key, byte value)
    {
        return new ImmutableObjectByteSingletonMap<>(key, value);
    }

    @Override
    public ImmutableObjectByteMap<K> newWithoutKey(K key)
    {
        return this;
    }

    @Override
    public ImmutableObjectByteMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        return this;
    }

    @Override
    public byte get(Object key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public byte getOrThrow(Object key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public byte getIfAbsent(Object key, byte ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return false;
    }

    @Override
    public boolean containsValue(byte value)
    {
        return false;
    }

    @Override
    public void forEachValue(ByteProcedure procedure)
    {
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
    }

    @Override
    public void forEachKeyValue(ObjectByteProcedure<? super K> objectByteProcedure)
    {
    }

    @Override
    public ImmutableObjectByteMap<K> select(ObjectBytePredicate<? super K> objectBytePredicate)
    {
        return this;
    }

    @Override
    public ImmutableObjectByteMap<K> reject(ObjectBytePredicate<? super K> objectBytePredicate)
    {
        return this;
    }

    @Override
    public ImmutableObjectByteMap<K> toImmutable()
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public Set<K> keySet()
    {
        return Sets.immutable.<K>of().castToSet();
    }

    @Override
    public MutableByteCollection values()
    {
        return UnmodifiableByteCollection.of(new ByteArrayList());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return LazyIterate.empty();
    }

    @Override
    public RichIterable<ObjectBytePair<K>> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableByteObjectMap<K> flipUniqueValues()
    {
        return ByteObjectMaps.immutable.empty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectByteMap))
        {
            return false;
        }
        ObjectByteMap<K> map = (ObjectByteMap<K>) obj;
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
