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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ObjectIntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ObjectIntProcedure;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectIntMap;
import jdk.m3.collections.eclipse.api.collection.ImmutableCollection;
import jdk.m3.collections.eclipse.api.collection.MutableCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableIntCollection;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntObjectMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableIntIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ObjectIntHashMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntObjectMaps;
import jdk.m3.collections.eclipse.impl.set.mutable.UnmodifiableMutableSet;

/**
 * ImmutableObjectIntHashMap is the non-modifiable equivalent of {@link ObjectIntHashMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableObjectIntHashMap<K> extends AbstractImmutableObjectIntMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectIntMap<K> delegate;

    ImmutableObjectIntHashMap(ObjectIntMap<? extends K> delegate)
    {
        this.delegate = new ObjectIntHashMap<>(delegate);
    }

    @Override
    public IntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.delegate.intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableIntCollection select(IntPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableIntCollection reject(IntPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            IntIterator iterator = this.intIterator();
            while (iterator.hasNext())
            {
                MutableIntBag batch = IntBags.mutable.empty();
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
    public <V> ImmutableCollection<V> collect(IntToObjectFunction<? extends V> function)
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
    public int max()
    {
        return this.delegate.max();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public int min()
    {
        return this.delegate.min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public int[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(int value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableIntList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableObjectIntMap<K> newWithKeyValue(K key, int value)
    {
        ObjectIntHashMap<K> map = ObjectIntHashMap.newMap();
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectIntMap<K> newWithoutKey(K key)
    {
        ObjectIntHashMap<K> map = ObjectIntHashMap.newMap();
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectIntMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectIntHashMap<K> map = ObjectIntHashMap.newMap();
        map.putAll(this);
        Iterator<? extends K> iterator = keys.iterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public int get(Object key)
    {
        return this.delegate.get(key);
    }

    @Override
    public int getOrThrow(Object key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public int getIfAbsent(Object key, int ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(int value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(IntProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectIntProcedure<? super K> objectIntProcedure)
    {
        this.delegate.forEachKeyValue(objectIntProcedure);
    }

    /**
     * @since 12.0
     */
    @Override
    public boolean allSatisfyKeyValue(ObjectIntPredicate<K> predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public ImmutableObjectIntMap<K> select(ObjectIntPredicate<? super K> objectIntPredicate)
    {
        return this.delegate.select(objectIntPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectIntMap<K> reject(ObjectIntPredicate<? super K> objectIntPredicate)
    {
        return this.delegate.reject(objectIntPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectIntMap<K> toImmutable()
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
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(this.delegate.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ObjectIntPair<K>> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableIntObjectMap<K> flipUniqueValues()
    {
        MutableIntObjectMap<K> result = IntObjectMaps.mutable.empty();
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
        return new ImmutableObjectIntMapSerializationProxy<K>(this);
    }
}
