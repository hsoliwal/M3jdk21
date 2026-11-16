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

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ObjectLongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ObjectLongProcedure;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableLongCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableLongCollection;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectLongMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectLongPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.LongHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.Sets;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectLongMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongObjectMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableLongIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyLongIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectLongHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableObjectLongSingletonMap is an optimization for {@link ImmutableObjectLongMap} of size 1.
 * This file was automatically generated from template file immutableObjectPrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"serial", "unchecked"})
final class ImmutableObjectLongSingletonMap<K> extends AbstractImmutableObjectLongMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final long EMPTY_VALUE = 0L;
    private final K key1;
    private final long value1;

    ImmutableObjectLongSingletonMap(K key1, long value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(ObjectLongHashMap.newWithKeysValues(this.key1, this.value1).longIterator());
    }

    @Override
    public void forEach(LongProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableLongCollection select(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? LongLists.immutable.with(this.value1) : LongLists.immutable.with();
    }

    @Override
    public ImmutableLongCollection reject(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? LongLists.immutable.with() : LongLists.immutable.with(this.value1);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(LongLists.immutable.with(this.value1));
    }

    @Override
    public <V> ImmutableCollection<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public long min()
    {
        return this.value1;
    }

    @Override
    public long max()
    {
        return this.value1;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.value1;
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return new long[]{this.value1};
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newListWith(this.value1);
    }

    @Override
    public long[] toArray()
    {
        return new long[]{this.value1};
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < 1)
        {
            target = new long[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newListWith(this.value1);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public ImmutableObjectLongMap<K> newWithKeyValue(K key, long value)
    {
        return ObjectLongMaps.immutable.withAll(ObjectLongHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableObjectLongMap<K> newWithoutKey(K key)
    {
        return nullSafeEquals(this.key1, key) ? (ImmutableObjectLongMap<K>) ImmutableObjectLongEmptyMap.INSTANCE : this;
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
    public ImmutableObjectLongMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectLongHashMap<K> map = new ObjectLongHashMap<K>(this);
        for (K key : keys)
        {
            map.removeKey(key);
        }
        return map.toImmutable();
    }

    @Override
    public long get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public long getOrThrow(Object key)
    {
        if (nullSafeEquals(this.key1, key))
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public long getIfAbsent(Object key, long ifAbsent)
    {
        return nullSafeEquals(this.key1, key) ? this.value1 : ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return nullSafeEquals(this.key1, key);
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ObjectLongProcedure<? super K> objectLongProcedure)
    {
        objectLongProcedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    @Override
    public boolean allSatisfyKeyValue(ObjectLongPredicate<K> predicate)
    {
        if (!predicate.accept(this.key1, this.value1))
        {
            return false;
        }
        return true;
    }

    @Override
    public ImmutableObjectLongMap<K> select(ObjectLongPredicate<? super K> objectLongPredicate)
    {
        return objectLongPredicate.accept(this.key1, this.value1) ? ObjectLongHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableObjectLongMap<K>) ImmutableObjectLongEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableObjectLongMap<K> reject(ObjectLongPredicate<? super K> objectLongPredicate)
    {
        return objectLongPredicate.accept(this.key1, this.value1) ? (ImmutableObjectLongMap<K>) ImmutableObjectLongEmptyMap.INSTANCE
                : ObjectLongHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableObjectLongMap<K> toImmutable()
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
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(LongArrayList.newListWith(this.value1));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return Lists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ObjectLongPair<K>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableLongObjectMap<K> flipUniqueValues()
    {
        return LongObjectMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectLongMap))
        {
            return false;
        }
        ObjectLongMap<K> map = (ObjectLongMap<K>) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (this.key1 == null ? 0 : this.key1.hashCode()) ^ (int) (this.value1 ^ this.value1 >>> 32);
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
        return new ImmutableObjectLongMapSerializationProxy<K>(this);
    }
}
