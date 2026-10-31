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

import java.util.Collections;
import java.io.Serializable;
import java.util.Set;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction0;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectShortPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectShortProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.MutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ObjectShortPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectShortMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;

/**
 * This file was automatically generated from template file unmodifiableObjectPrimitiveMap.stg.
 *
 * @since 3.2
 */
public class UnmodifiableObjectShortMap<K>
        implements MutableObjectShortMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectShortMap<K> map;

    public UnmodifiableObjectShortMap(MutableObjectShortMap<K> map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableObjectShortMap on a null map");
        }

        this.map = map;
    }

    private boolean isAbsent(short result, K key)
    {
        return result == ObjectShortHashMap.EMPTY_VALUE && !this.containsKey(key);
    }

    private short getIfAbsentThrow(K key)
    {
        short result = this.map.get(key);
        if (this.isAbsent(result, key))
        {
            throw new UnsupportedOperationException("Cannot add to an " + this.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public short getAndPut(K key, short defaultValue, short putValue)
    {
        throw new UnsupportedOperationException("Cannot call getAndPut() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(K key, short value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ObjectShortPair<K> keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ObjectShortMap<? extends K> map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ObjectShortToShortFunction<? super K> function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(Object key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public short removeKeyIfAbsent(K key, short value)
    {
        throw new UnsupportedOperationException("Cannot call removeKeyIfAbsent() on " + this.getClass().getSimpleName());
    }

    @Override
    public short getIfAbsentPut(K key, short value)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public short getIfAbsentPut(K key, ShortFunction0 function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public short getIfAbsentPutWithKey(K key, ShortFunction<? super K> function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public <P> short getIfAbsentPutWith(K key, ShortFunction<? super P> function, P parameter)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public short updateValue(K key, short initialValueIfAbsent, ShortToShortFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    public short addToValue(K key, short toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public short get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public short getOrThrow(Object key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public short getIfAbsent(Object key, short ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(short value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectShortProcedure<? super K> procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public MutableObjectShortMap<K> select(ObjectShortPredicate<? super K> predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableObjectShortMap<K> reject(ObjectShortPredicate<? super K> predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.map.shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableShortCollection select(ShortPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableShortCollection reject(ShortPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public <V1> MutableCollection<V1> collect(ShortToObjectFunction<? extends V1> function)
    {
        return this.map.collect(function);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public short max()
    {
        return this.map.max();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public short min()
    {
        return this.map.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        return this.map.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.map.average();
    }

    @Override
    public double median()
    {
        return this.map.median();
    }

    @Override
    public short[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableShortList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public short[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public short[] toArray(short[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(short value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableShortList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableObjectShortMap<K> withKeyValue(K key, short value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectShortMap<K> withoutKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectShortMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectShortMap<K> asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableObjectShortMap<K> asSynchronized()
    {
        return new SynchronizedObjectShortMap<>(this);
    }

    @Override
    public ImmutableObjectShortMap<K> toImmutable()
    {
        return ObjectShortMaps.immutable.withAll(this);
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.map.notEmpty();
    }

    @Override
    public Set<K> keySet()
    {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    @Override
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(this.map.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ObjectShortPair<K>> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableShortObjectMap<K> flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.map.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.map.hashCode();
    }

    @Override
    public String toString()
    {
        return this.map.toString();
    }

    @Override
    public String makeString()
    {
        return this.map.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.map.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.map.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.map.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.map.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.map.appendString(appendable, start, separator, end);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
