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
import java.util.Set;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectByteProcedure;
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
import org.eclipse.collections.impl.factory.primitive.ObjectByteMaps;
import org.eclipse.collections.impl.factory.primitive.ByteObjectMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableObjectByteSingletonMap is an optimization for {@link ImmutableObjectByteMap} of size 1.
 * This file was automatically generated from template file immutableObjectPrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableObjectByteSingletonMap<K> extends AbstractImmutableObjectByteMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_VALUE = (byte) 0;
    private final K key1;
    private final byte value1;

    ImmutableObjectByteSingletonMap(K key1, byte value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(ObjectByteHashMap.newWithKeysValues(this.key1, this.value1).byteIterator());
    }

    @Override
    public void forEach(ByteProcedure procedure)
    {
        this.each(procedure);
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
    public ImmutableByteCollection select(BytePredicate predicate)
    {
        return predicate.accept(this.value1) ? ByteLists.immutable.with(this.value1) : ByteLists.immutable.with();
    }

    @Override
    public ImmutableByteCollection reject(BytePredicate predicate)
    {
        return predicate.accept(this.value1) ? ByteLists.immutable.with() : ByteLists.immutable.with(this.value1);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
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
        return Lists.immutable.with(ByteLists.immutable.with(this.value1));
    }

    @Override
    public <V> ImmutableCollection<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of(function.valueOf(this.value1));
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
    public ImmutableObjectByteMap<K> newWithKeyValue(K key, byte value)
    {
        return ObjectByteMaps.immutable.withAll(ObjectByteHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableObjectByteMap<K> newWithoutKey(K key)
    {
        return nullSafeEquals(this.key1, key) ? (ImmutableObjectByteMap<K>) ImmutableObjectByteEmptyMap.INSTANCE : this;
    }

    private static boolean nullSafeEquals(Object value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (other == value || value.equals(other))
        {
            return true;
        }
        return false;
    }

    @Override
    public ImmutableObjectByteMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectByteHashMap<K> map = new ObjectByteHashMap<K>(this);
        for (K key : keys)
        {
            map.removeKey(key);
        }
        return map.toImmutable();
    }

    @Override
    public byte get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getOrThrow(Object key)
    {
        if (nullSafeEquals(this.key1, key))
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public byte getIfAbsent(Object key, byte ifAbsent)
    {
        return nullSafeEquals(this.key1, key) ? this.value1 : ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return nullSafeEquals(this.key1, key);
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
    public void forEachKey(Procedure<? super K> procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ObjectByteProcedure<? super K> objectByteProcedure)
    {
        objectByteProcedure.value(this.key1, this.value1);
    }

    @Override
    public ImmutableObjectByteMap<K> select(ObjectBytePredicate<? super K> objectBytePredicate)
    {
        return objectBytePredicate.accept(this.key1, this.value1) ? ObjectByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableObjectByteMap<K>) ImmutableObjectByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableObjectByteMap<K> reject(ObjectBytePredicate<? super K> objectBytePredicate)
    {
        return objectBytePredicate.accept(this.key1, this.value1) ? (ImmutableObjectByteMap<K>) ImmutableObjectByteEmptyMap.INSTANCE
                : ObjectByteHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableObjectByteMap<K> toImmutable()
    {
        return this;
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
    public Set<K> keySet()
    {
        return Sets.immutable.of(this.key1).castToSet();
    }

    @Override
    public MutableByteCollection values()
    {
        return UnmodifiableByteCollection.of(ByteArrayList.newListWith(this.value1));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return Lists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ObjectBytePair<K>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableByteObjectMap<K> flipUniqueValues()
    {
        return ByteObjectMaps.immutable.with(this.value1, this.key1);
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
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (this.key1 == null ? 0 : this.key1.hashCode()) ^ (int) this.value1;
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

    private Object writeReplace()
    {
        return new ImmutableObjectByteMapSerializationProxy<K>(this);
    }
}
