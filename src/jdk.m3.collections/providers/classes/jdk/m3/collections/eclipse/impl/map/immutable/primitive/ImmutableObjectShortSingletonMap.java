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
import java.util.Set;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ObjectShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ObjectShortProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableShortCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectShortMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectShortPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.Sets;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectShortMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortObjectMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableShortIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectShortHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableObjectShortSingletonMap is an optimization for {@link ImmutableObjectShortMap} of size 1.
 * This file was automatically generated from template file immutableObjectPrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"serial", "unchecked"})
final class ImmutableObjectShortSingletonMap<K> extends AbstractImmutableObjectShortMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_VALUE = (short) 0;
    private final K key1;
    private final short value1;

    ImmutableObjectShortSingletonMap(K key1, short value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(ObjectShortHashMap.newWithKeysValues(this.key1, this.value1).shortIterator());
    }

    @Override
    public void forEach(ShortProcedure procedure)
    {
        this.each(procedure);
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
    public ImmutableShortCollection select(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortLists.immutable.with(this.value1) : ShortLists.immutable.with();
    }

    @Override
    public ImmutableShortCollection reject(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortLists.immutable.with() : ShortLists.immutable.with(this.value1);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
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
        return Lists.immutable.with(ShortLists.immutable.with(this.value1));
    }

    @Override
    public <V> ImmutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of(function.valueOf(this.value1));
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
    public ImmutableObjectShortMap<K> newWithKeyValue(K key, short value)
    {
        return ObjectShortMaps.immutable.withAll(ObjectShortHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableObjectShortMap<K> newWithoutKey(K key)
    {
        return nullSafeEquals(this.key1, key) ? (ImmutableObjectShortMap<K>) ImmutableObjectShortEmptyMap.INSTANCE : this;
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
    public ImmutableObjectShortMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectShortHashMap<K> map = new ObjectShortHashMap<K>(this);
        for (K key : keys)
        {
            map.removeKey(key);
        }
        return map.toImmutable();
    }

    @Override
    public short get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public short getOrThrow(Object key)
    {
        if (nullSafeEquals(this.key1, key))
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public short getIfAbsent(Object key, short ifAbsent)
    {
        return nullSafeEquals(this.key1, key) ? this.value1 : ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return nullSafeEquals(this.key1, key);
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
    public void forEachKey(Procedure<? super K> procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ObjectShortProcedure<? super K> objectShortProcedure)
    {
        objectShortProcedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    @Override
    public boolean allSatisfyKeyValue(ObjectShortPredicate<K> predicate)
    {
        if (!predicate.accept(this.key1, this.value1))
        {
            return false;
        }
        return true;
    }

    @Override
    public ImmutableObjectShortMap<K> select(ObjectShortPredicate<? super K> objectShortPredicate)
    {
        return objectShortPredicate.accept(this.key1, this.value1) ? ObjectShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableObjectShortMap<K>) ImmutableObjectShortEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableObjectShortMap<K> reject(ObjectShortPredicate<? super K> objectShortPredicate)
    {
        return objectShortPredicate.accept(this.key1, this.value1) ? (ImmutableObjectShortMap<K>) ImmutableObjectShortEmptyMap.INSTANCE
                : ObjectShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableObjectShortMap<K> toImmutable()
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
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(ShortArrayList.newListWith(this.value1));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return Lists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ObjectShortPair<K>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableShortObjectMap<K> flipUniqueValues()
    {
        return ShortObjectMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectShortMap))
        {
            return false;
        }
        ObjectShortMap<K> map = (ObjectShortMap<K>) obj;
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
        return new ImmutableObjectShortMapSerializationProxy<K>(this);
    }
}
