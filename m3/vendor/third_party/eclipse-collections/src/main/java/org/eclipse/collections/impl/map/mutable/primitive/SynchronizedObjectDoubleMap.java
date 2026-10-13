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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectDoublePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectDoubleProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.SynchronizedRichIterable;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.set.mutable.SynchronizedMutableSet;
import org.eclipse.collections.impl.utility.LazyIterate;

/**
 * A synchronized view of a {@link MutableObjectDoubleMap}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * DoubleIterator as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedObjectPrimitiveMap.stg.
 *
 * @see MutableObjectDoubleMap#asSynchronized()
 * @see MutableMap#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedObjectDoubleMap<K>
        implements MutableObjectDoubleMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableObjectDoubleMap<K> map;

    public SynchronizedObjectDoubleMap(MutableObjectDoubleMap<K> map)
    {
        this(map, null);
    }

    public SynchronizedObjectDoubleMap(MutableObjectDoubleMap<K> map, Object newLock)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedObjectDoubleMap on a null map");
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
    public void put(K key, double value)
    {
        synchronized (this.lock)
        {
            this.map.put(key, value);
        }
    }

    @Override
    public void putPair(ObjectDoublePair<K> keyValuePair)
    {
        synchronized (this.lock)
        {
            this.map.put(keyValuePair.getOne(), keyValuePair.getTwo());
        }
    }

    @Override
    public void putAll(ObjectDoubleMap<? extends K> map)
    {
        synchronized (this.lock)
        {
            this.map.putAll(map);
        }
    }

    @Override
    public void updateValues(ObjectDoubleToDoubleFunction<? super K> function)
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
    public double removeKeyIfAbsent(K key, double value)
    {
        synchronized (this.lock)
        {
            return this.map.removeKeyIfAbsent(key, value);
        }
    }

    @Override
    public double getIfAbsentPut(K key, double value)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, value);
        }
    }

    @Override
    public double getAndPut(K key, double putValue, double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.getAndPut(key, putValue, defaultValue);
        }
    }

    @Override
    public double getIfAbsentPut(K key, DoubleFunction0 function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, function);
        }
    }

    @Override
    public double getIfAbsentPutWithKey(K key, DoubleFunction<? super K> function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWithKey(key, function);
        }
    }

    @Override
    public <P> double getIfAbsentPutWith(K key, DoubleFunction<? super P> function, P parameter)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWith(key, function, parameter);
        }
    }

    @Override
    public double updateValue(K key, double initialValueIfAbsent, DoubleToDoubleFunction function)
    {
        synchronized (this.lock)
        {
            return this.map.updateValue(key, initialValueIfAbsent, function);
        }
    }

    @Override
    public double get(Object key)
    {
        synchronized (this.lock)
        {
            return this.map.get(key);
        }
    }

    @Override
    public double getOrThrow(Object key)
    {
        synchronized (this.lock)
        {
            return this.map.getOrThrow(key);
        }
    }

    @Override
    public double getIfAbsent(Object key, double ifAbsent)
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
    public boolean containsValue(double value)
    {
        synchronized (this.lock)
        {
            return this.map.containsValue(value);
        }
    }

    @Override
    public void forEachValue(DoubleProcedure procedure)
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
    public void forEachKeyValue(ObjectDoubleProcedure<? super K> procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachKeyValue(procedure);
        }
    }

    @Override
    public MutableObjectDoubleMap<K> select(ObjectDoublePredicate<? super K> predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableObjectDoubleMap<K> reject(ObjectDoublePredicate<? super K> predicate)
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
    public MutableDoubleIterator doubleIterator()
    {
        return this.map.doubleIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEach(procedure);
        }
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableDoubleCollection select(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableDoubleCollection reject(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.reject(predicate);
        }
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        synchronized (this.lock)
        {
            return this.map.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public <V1> MutableCollection<V1> collect(DoubleToObjectFunction<? extends V1> function)
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
    public double addToValue(K key, double toBeAdded)
    {
        synchronized (this.lock)
        {
            return this.map.addToValue(key, toBeAdded);
        }
    }

    @Override
    public double max()
    {
        synchronized (this.lock)
        {
            return this.map.max();
        }
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public double min()
    {
        synchronized (this.lock)
        {
            return this.map.min();
        }
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.map.toSortedArray();
        }
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.map.toSortedList();
        }
    }

    @Override
    public double[] toArray()
    {
        synchronized (this.lock)
        {
            return this.map.toArray();
        }
    }

    @Override
    public double[] toArray(double[] target)
    {
        synchronized (this.lock)
        {
            return this.map.toArray(target);
        }
    }

    @Override
    public boolean contains(double value)
    {
        synchronized (this.lock)
        {
            return this.map.contains(value);
        }
    }

    @Override
    public boolean containsAll(double... source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public MutableDoubleList toList()
    {
        synchronized (this.lock)
        {
            return this.map.toList();
        }
    }

    @Override
    public MutableDoubleSet toSet()
    {
        synchronized (this.lock)
        {
            return this.map.toSet();
        }
    }

    @Override
    public MutableDoubleBag toBag()
    {
        synchronized (this.lock)
        {
            return this.map.toBag();
        }
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        synchronized (this.lock)
        {
            return this.map.asLazy();
        }
    }

    @Override
    public MutableObjectDoubleMap<K> withKeyValue(K key, double value)
    {
        synchronized (this.lock)
        {
            this.map.withKeyValue(key, value);
        }
        return this;
    }

    @Override
    public MutableObjectDoubleMap<K> withoutKey(K key)
    {
        synchronized (this.lock)
        {
            this.map.withoutKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectDoubleMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        synchronized (this.lock)
        {
            this.map.withoutAllKeys(keys);
        }
        return this;
    }

    @Override
    public MutableObjectDoubleMap<K> asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableObjectDoubleMap<>(this);
        }
    }

    @Override
    public MutableObjectDoubleMap<K> asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableObjectDoubleMap<K> toImmutable()
    {
        synchronized (this.lock)
        {
            return ObjectDoubleMaps.immutable.withAll(this);
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
    public MutableDoubleCollection values()
    {
        synchronized (this.lock)
        {
            return SynchronizedDoubleCollection.of(this.map.values(), this.lock);
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return LazyIterate.adapt(this.keySet());
    }

    @Override
    public RichIterable<ObjectDoublePair<K>> keyValuesView()
    {
        synchronized (this.lock)
        {
            return SynchronizedRichIterable.of(this.map.keyValuesView(), this.lock).asLazy();
        }
    }

    @Override
    public MutableDoubleObjectMap<K> flipUniqueValues()
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
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.map.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.map.chunk(size);
        }
    }
}
