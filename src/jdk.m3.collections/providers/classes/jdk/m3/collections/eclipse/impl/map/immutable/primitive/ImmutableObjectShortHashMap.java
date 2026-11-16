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
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectShortMap;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.MutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableShortCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableShortCollection;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortObjectMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectShortPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableShortIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectShortHashMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortObjectMaps;
import jdk.m3.collections.eclipse.impl.set.mutable.UnmodifiableMutableSet;

/**
 * ImmutableObjectShortHashMap is the non-modifiable equivalent of {@link ObjectShortHashMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableObjectShortHashMap<K> extends AbstractImmutableObjectShortMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectShortMap<K> delegate;

    ImmutableObjectShortHashMap(ObjectShortMap<? extends K> delegate)
    {
        this.delegate = new ObjectShortHashMap<>(delegate);
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.delegate.shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableShortCollection select(ShortPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortCollection reject(ShortPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            ShortIterator iterator = this.shortIterator();
            while (iterator.hasNext())
            {
                MutableShortBag batch = ShortBags.mutable.empty();
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
    public <V> ImmutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
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
    public short max()
    {
        return this.delegate.max();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public short min()
    {
        return this.delegate.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableShortList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public short[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public short[] toArray(short[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(short value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableShortList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableObjectShortMap<K> newWithKeyValue(K key, short value)
    {
        ObjectShortHashMap<K> map = ObjectShortHashMap.newMap();
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectShortMap<K> newWithoutKey(K key)
    {
        ObjectShortHashMap<K> map = ObjectShortHashMap.newMap();
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectShortMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectShortHashMap<K> map = ObjectShortHashMap.newMap();
        map.putAll(this);
        Iterator<? extends K> iterator = keys.iterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public short get(Object key)
    {
        return this.delegate.get(key);
    }

    @Override
    public short getOrThrow(Object key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public short getIfAbsent(Object key, short ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(short value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectShortProcedure<? super K> objectShortProcedure)
    {
        this.delegate.forEachKeyValue(objectShortProcedure);
    }

    /**
     * @since 12.0
     */
    @Override
    public boolean allSatisfyKeyValue(ObjectShortPredicate<K> predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public ImmutableObjectShortMap<K> select(ObjectShortPredicate<? super K> objectShortPredicate)
    {
        return this.delegate.select(objectShortPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectShortMap<K> reject(ObjectShortPredicate<? super K> objectShortPredicate)
    {
        return this.delegate.reject(objectShortPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectShortMap<K> toImmutable()
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
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(this.delegate.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ObjectShortPair<K>> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableShortObjectMap<K> flipUniqueValues()
    {
        MutableShortObjectMap<K> result = ShortObjectMaps.mutable.empty();
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
        return new ImmutableObjectShortMapSerializationProxy<K>(this);
    }
}
