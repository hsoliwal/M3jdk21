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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectLongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;

/**
 * This file was automatically generated from template file unmodifiableObjectPrimitiveMap.stg.
 *
 * @since 3.2
 */
public class UnmodifiableObjectLongMap<K>
        implements MutableObjectLongMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectLongMap<K> map;

    public UnmodifiableObjectLongMap(MutableObjectLongMap<K> map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableObjectLongMap on a null map");
        }

        this.map = map;
    }

    private boolean isAbsent(long result, K key)
    {
        return result == ObjectLongHashMap.EMPTY_VALUE && !this.containsKey(key);
    }

    private long getIfAbsentThrow(K key)
    {
        long result = this.map.get(key);
        if (this.isAbsent(result, key))
        {
            throw new UnsupportedOperationException("Cannot add to an " + this.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public long getAndPut(K key, long defaultValue, long putValue)
    {
        throw new UnsupportedOperationException("Cannot call getAndPut() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(K key, long value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ObjectLongPair<K> keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ObjectLongMap<? extends K> map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ObjectLongToLongFunction<? super K> function)
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
    public long removeKeyIfAbsent(K key, long value)
    {
        throw new UnsupportedOperationException("Cannot call removeKeyIfAbsent() on " + this.getClass().getSimpleName());
    }

    @Override
    public long getIfAbsentPut(K key, long value)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public long getIfAbsentPut(K key, LongFunction0 function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public long getIfAbsentPutWithKey(K key, LongFunction<? super K> function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public <P> long getIfAbsentPutWith(K key, LongFunction<? super P> function, P parameter)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public long updateValue(K key, long initialValueIfAbsent, LongToLongFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    public long addToValue(K key, long toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public long get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public long getOrThrow(Object key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public long getIfAbsent(Object key, long ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectLongProcedure<? super K> procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public MutableObjectLongMap<K> select(ObjectLongPredicate<? super K> predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableObjectLongMap<K> reject(ObjectLongPredicate<? super K> predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.map.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableLongCollection select(LongPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableLongCollection reject(LongPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public <V1> MutableCollection<V1> collect(LongToObjectFunction<? extends V1> function)
    {
        return this.map.collect(function);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public long max()
    {
        return this.map.max();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public long min()
    {
        return this.map.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public long[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public long[] toArray(long[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(long value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableObjectLongMap<K> withKeyValue(K key, long value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectLongMap<K> withoutKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectLongMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectLongMap<K> asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableObjectLongMap<K> asSynchronized()
    {
        return new SynchronizedObjectLongMap<>(this);
    }

    @Override
    public ImmutableObjectLongMap<K> toImmutable()
    {
        return ObjectLongMaps.immutable.withAll(this);
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
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(this.map.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ObjectLongPair<K>> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableLongObjectMap<K> flipUniqueValues()
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
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
