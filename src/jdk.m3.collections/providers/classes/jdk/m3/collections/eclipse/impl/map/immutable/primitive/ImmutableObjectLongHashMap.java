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

import java.io.Serializable;
import java.util.Iterator;
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
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectLongMap;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.MutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableLongCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableLongCollection;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableLongObjectMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectLongPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableLongIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectLongHashMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongObjectMaps;
import jdk.m3.collections.eclipse.impl.set.mutable.UnmodifiableMutableSet;

/**
 * ImmutableObjectLongHashMap is the non-modifiable equivalent of {@link ObjectLongHashMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableObjectLongHashMap<K> extends AbstractImmutableObjectLongMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectLongMap<K> delegate;

    ImmutableObjectLongHashMap(ObjectLongMap<? extends K> delegate)
    {
        this.delegate = new ObjectLongHashMap<>(delegate);
    }

    @Override
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.delegate.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableLongCollection select(LongPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableLongCollection reject(LongPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            LongIterator iterator = this.longIterator();
            while (iterator.hasNext())
            {
                MutableLongBag batch = LongBags.mutable.empty();
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    batch.add(iterator.next());
                }
                result.add(batch.toImmutable());
            }
        }
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableCollection<V> collect(LongToObjectFunction<? extends V> function)
    {
        MutableCollection<V> collection = this.delegate.collect(function);
        return collection.toImmutable();
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public long max()
    {
        return this.delegate.max();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public long min()
    {
        return this.delegate.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.delegate.average();
    }

    @Override
    public double median()
    {
        return this.delegate.median();
    }

    @Override
    public long[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public long[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public long[] toArray(long[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(long value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableObjectLongMap<K> newWithKeyValue(K key, long value)
    {
        ObjectLongHashMap<K> map = ObjectLongHashMap.newMap();
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectLongMap<K> newWithoutKey(K key)
    {
        ObjectLongHashMap<K> map = ObjectLongHashMap.newMap();
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectLongMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectLongHashMap<K> map = ObjectLongHashMap.newMap();
        map.putAll(this);
        Iterator<? extends K> iterator = keys.iterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public long get(Object key)
    {
        return this.delegate.get(key);
    }

    @Override
    public long getOrThrow(Object key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public long getIfAbsent(Object key, long ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectLongProcedure<? super K> objectLongProcedure)
    {
        this.delegate.forEachKeyValue(objectLongProcedure);
    }

    /**
     * @since 12.0
     */
    @Override
    public boolean allSatisfyKeyValue(ObjectLongPredicate<K> predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public ImmutableObjectLongMap<K> select(ObjectLongPredicate<? super K> objectLongPredicate)
    {
        return this.delegate.select(objectLongPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectLongMap<K> reject(ObjectLongPredicate<? super K> objectLongPredicate)
    {
        return this.delegate.reject(objectLongPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectLongMap<K> toImmutable()
    {
        return this;
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public Set<K> keySet()
    {
        return UnmodifiableMutableSet.of(this.delegate.keySet());
    }

    @Override
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(this.delegate.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ObjectLongPair<K>> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableLongObjectMap<K> flipUniqueValues()
    {
        MutableLongObjectMap<K> result = LongObjectMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            K oldKey = result.put(value, key);
            if (oldKey != null)
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + oldKey + " and key: " + key);
            }
        });
        return result.toImmutable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    private Object writeReplace()
    {
        return new ImmutableObjectLongMapSerializationProxy<K>(this);
    }
}
