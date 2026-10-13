/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction0;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectFloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.SynchronizedRichIterable;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedFloatCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectFloatMaps;
import org.eclipse.collections.impl.set.mutable.SynchronizedMutableSet;
import org.eclipse.collections.impl.utility.LazyIterate;

/**
 * A synchronized view of a {@link MutableObjectFloatMap}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * FloatIterator as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedObjectPrimitiveMap.stg.
 *
 * @see MutableObjectFloatMap#asSynchronized()
 * @see MutableMap#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedObjectFloatMap<K>
        implements MutableObjectFloatMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableObjectFloatMap<K> map;

    public SynchronizedObjectFloatMap(MutableObjectFloatMap<K> map)
    {
        this(map, null);
    }

    public SynchronizedObjectFloatMap(MutableObjectFloatMap<K> map, Object newLock)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedObjectFloatMap on a null map");
        }

        this.map = map;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.map.clear();
        }
    }

    @Override
    public void put(K key, float value)
    {
        synchronized (this.lock)
        {
            this.map.put(key, value);
        }
    }

    @Override
    public void putPair(ObjectFloatPair<K> keyValuePair)
    {
        synchronized (this.lock)
        {
            this.map.put(keyValuePair.getOne(), keyValuePair.getTwo());
        }
    }

    @Override
    public void putAll(ObjectFloatMap<? extends K> map)
    {
        synchronized (this.lock)
        {
            this.map.putAll(map);
        }
    }

    @Override
    public void updateValues(ObjectFloatToFloatFunction<? super K> function)
    {
        synchronized (this.lock)
        {
            this.map.updateValues(function);
        }
    }

    @Override
    public void removeKey(K key)
    {
        synchronized (this.lock)
        {
            this.map.removeKey(key);
        }
    }

    @Override
    public void remove(Object key)
    {
        synchronized (this.lock)
        {
            this.map.removeKey((K) key);
        }
    }

    @Override
    public float removeKeyIfAbsent(K key, float value)
    {
        synchronized (this.lock)
        {
            return this.map.removeKeyIfAbsent(key, value);
        }
    }

    @Override
    public float getIfAbsentPut(K key, float value)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, value);
        }
    }

    @Override
    public float getAndPut(K key, float putValue, float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.getAndPut(key, putValue, defaultValue);
        }
    }

    @Override
    public float getIfAbsentPut(K key, FloatFunction0 function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, function);
        }
    }

    @Override
    public float getIfAbsentPutWithKey(K key, FloatFunction<? super K> function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWithKey(key, function);
        }
    }

    @Override
    public <P> float getIfAbsentPutWith(K key, FloatFunction<? super P> function, P parameter)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWith(key, function, parameter);
        }
    }

    @Override
    public float updateValue(K key, float initialValueIfAbsent, FloatToFloatFunction function)
    {
        synchronized (this.lock)
        {
            return this.map.updateValue(key, initialValueIfAbsent, function);
        }
    }

    @Override
    public float get(Object key)
    {
        synchronized (this.lock)
        {
            return this.map.get(key);
        }
    }

    @Override
    public float getOrThrow(Object key)
    {
        synchronized (this.lock)
        {
            return this.map.getOrThrow(key);
        }
    }

    @Override
    public float getIfAbsent(Object key, float ifAbsent)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsent(key, ifAbsent);
        }
    }

    @Override
    public boolean containsKey(Object key)
    {
        synchronized (this.lock)
        {
            return this.map.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(float value)
    {
        synchronized (this.lock)
        {
            return this.map.containsValue(value);
        }
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachValue(procedure);
        }
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachKey(procedure);
        }
    }

    @Override
    public void forEachKeyValue(ObjectFloatProcedure<? super K> procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachKeyValue(procedure);
        }
    }

    @Override
    public MutableObjectFloatMap<K> select(ObjectFloatPredicate<? super K> predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableObjectFloatMap<K> reject(ObjectFloatPredicate<? super K> predicate)
    {
        synchronized (this.lock)
        {
            return this.map.reject(predicate);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public MutableFloatIterator floatIterator()
    {
        return this.map.floatIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEach(procedure);
        }
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableFloatCollection select(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableFloatCollection reject(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.reject(predicate);
        }
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        synchronized (this.lock)
        {
            return this.map.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public <V1> MutableCollection<V1> collect(FloatToObjectFunction<? extends V1> function)
    {
        synchronized (this.lock)
        {
            return this.map.collect(function);
        }
    }

    @Override
    public double sum()
    {
        synchronized (this.lock)
        {
            return this.map.sum();
        }
    }

    @Override
    public float addToValue(K key, float toBeAdded)
    {
        synchronized (this.lock)
        {
            return this.map.addToValue(key, toBeAdded);
        }
    }

    @Override
    public float max()
    {
        synchronized (this.lock)
        {
            return this.map.max();
        }
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public float min()
    {
        synchronized (this.lock)
        {
            return this.map.min();
        }
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.minIfEmpty(defaultValue);
        }
    }

    @Override
    public double average()
    {
        synchronized (this.lock)
        {
            return this.map.average();
        }
    }

    @Override
    public double median()
    {
        synchronized (this.lock)
        {
            return this.map.median();
        }
    }

    @Override
    public float[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.map.toSortedArray();
        }
    }

    @Override
    public MutableFloatList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.map.toSortedList();
        }
    }

    @Override
    public float[] toArray()
    {
        synchronized (this.lock)
        {
            return this.map.toArray();
        }
    }

    @Override
    public float[] toArray(float[] target)
    {
        synchronized (this.lock)
        {
            return this.map.toArray(target);
        }
    }

    @Override
    public boolean contains(float value)
    {
        synchronized (this.lock)
        {
            return this.map.contains(value);
        }
    }

    @Override
    public boolean containsAll(float... source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public MutableFloatList toList()
    {
        synchronized (this.lock)
        {
            return this.map.toList();
        }
    }

    @Override
    public MutableFloatSet toSet()
    {
        synchronized (this.lock)
        {
            return this.map.toSet();
        }
    }

    @Override
    public MutableFloatBag toBag()
    {
        synchronized (this.lock)
        {
            return this.map.toBag();
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        synchronized (this.lock)
        {
            return this.map.asLazy();
        }
    }

    @Override
    public MutableObjectFloatMap<K> withKeyValue(K key, float value)
    {
        synchronized (this.lock)
        {
            this.map.withKeyValue(key, value);
        }
        return this;
    }

    @Override
    public MutableObjectFloatMap<K> withoutKey(K key)
    {
        synchronized (this.lock)
        {
            this.map.withoutKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectFloatMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        synchronized (this.lock)
        {
            this.map.withoutAllKeys(keys);
        }
        return this;
    }

    @Override
    public MutableObjectFloatMap<K> asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableObjectFloatMap<>(this);
        }
    }

    @Override
    public MutableObjectFloatMap<K> asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableObjectFloatMap<K> toImmutable()
    {
        synchronized (this.lock)
        {
            return ObjectFloatMaps.immutable.withAll(this);
        }
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.map.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.map.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.map.notEmpty();
        }
    }

    @Override
    public Set<K> keySet()
    {
        synchronized (this.lock)
        {
            return SynchronizedMutableSet.of(this.map.keySet(), this.lock);
        }
    }

    @Override
    public MutableFloatCollection values()
    {
        synchronized (this.lock)
        {
            return SynchronizedFloatCollection.of(this.map.values(), this.lock);
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return LazyIterate.adapt(this.keySet());
    }

    @Override
    public RichIterable<ObjectFloatPair<K>> keyValuesView()
    {
        synchronized (this.lock)
        {
            return SynchronizedRichIterable.of(this.map.keyValuesView(), this.lock).asLazy();
        }
    }

    @Override
    public MutableFloatObjectMap<K> flipUniqueValues()
    {
        synchronized (this.lock)
        {
            return this.map.flipUniqueValues();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        synchronized (this.lock)
        {
            return this.map.equals(obj);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.lock)
        {
            return this.map.hashCode();
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.map.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.map.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.map.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.map.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.map.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.map.chunk(size);
        }
    }
}
