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
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;

/**
 * This file was automatically generated from template file unmodifiableObjectPrimitiveMap.stg.
 *
 * @since 3.2
 */
public class UnmodifiableObjectDoubleMap<K>
        implements MutableObjectDoubleMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectDoubleMap<K> map;

    public UnmodifiableObjectDoubleMap(MutableObjectDoubleMap<K> map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableObjectDoubleMap on a null map");
        }

        this.map = map;
    }

    private boolean isAbsent(double result, K key)
    {
        return result == ObjectDoubleHashMap.EMPTY_VALUE && !this.containsKey(key);
    }

    private double getIfAbsentThrow(K key)
    {
        double result = this.map.get(key);
        if (this.isAbsent(result, key))
        {
            throw new UnsupportedOperationException("Cannot add to an " + this.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public double getAndPut(K key, double defaultValue, double putValue)
    {
        throw new UnsupportedOperationException("Cannot call getAndPut() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(K key, double value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ObjectDoublePair<K> keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ObjectDoubleMap<? extends K> map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ObjectDoubleToDoubleFunction<? super K> function)
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
    public double removeKeyIfAbsent(K key, double value)
    {
        throw new UnsupportedOperationException("Cannot call removeKeyIfAbsent() on " + this.getClass().getSimpleName());
    }

    @Override
    public double getIfAbsentPut(K key, double value)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public double getIfAbsentPut(K key, DoubleFunction0 function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public double getIfAbsentPutWithKey(K key, DoubleFunction<? super K> function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public <P> double getIfAbsentPutWith(K key, DoubleFunction<? super P> function, P parameter)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public double updateValue(K key, double initialValueIfAbsent, DoubleToDoubleFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    public double addToValue(K key, double toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public double get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public double getOrThrow(Object key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public double getIfAbsent(Object key, double ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(double value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(DoubleProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectDoubleProcedure<? super K> procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public MutableObjectDoubleMap<K> select(ObjectDoublePredicate<? super K> predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableObjectDoubleMap<K> reject(ObjectDoublePredicate<? super K> predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(this.map.doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableDoubleCollection select(DoublePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableDoubleCollection reject(DoublePredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public <V1> MutableCollection<V1> collect(DoubleToObjectFunction<? extends V1> function)
    {
        return this.map.collect(function);
    }

    @Override
    public double sum()
    {
        return this.map.sum();
    }

    @Override
    public double max()
    {
        return this.map.max();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public double min()
    {
        return this.map.min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public double[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public double[] toArray(double[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(double value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableDoubleList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableObjectDoubleMap<K> withKeyValue(K key, double value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectDoubleMap<K> withoutKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectDoubleMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectDoubleMap<K> asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableObjectDoubleMap<K> asSynchronized()
    {
        return new SynchronizedObjectDoubleMap<>(this);
    }

    @Override
    public ImmutableObjectDoubleMap<K> toImmutable()
    {
        return ObjectDoubleMaps.immutable.withAll(this);
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
    public MutableDoubleCollection values()
    {
        return UnmodifiableDoubleCollection.of(this.map.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ObjectDoublePair<K>> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableDoubleObjectMap<K> flipUniqueValues()
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
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
