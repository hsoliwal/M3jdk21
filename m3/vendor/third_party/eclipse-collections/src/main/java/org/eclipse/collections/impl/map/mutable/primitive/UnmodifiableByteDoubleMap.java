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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToDoubleFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ByteDoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.primitive.ImmutableByteDoubleMap;
import org.eclipse.collections.api.map.primitive.ByteDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableByteDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleByteMap;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.ByteDoublePair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.ByteDoubleMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableByteSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableByteDoubleMap
        implements MutableByteDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableByteDoubleMap map;

    public UnmodifiableByteDoubleMap(MutableByteDoubleMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableByteDoubleMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(byte key, double value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ByteDoublePair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ByteDoubleMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ByteDoubleToDoubleFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(byte key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(byte key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public double removeKeyIfAbsent(byte key, double value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public double getIfAbsentPut(byte key, double value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double getAndPut(byte key, double putValue, double defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public double getIfAbsentPut(byte key, DoubleFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double getIfAbsentPutWithKey(byte key, ByteToDoubleFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> double getIfAbsentPutWith(byte key, DoubleFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double updateValue(byte key, double initialValueIfAbsent, DoubleToDoubleFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public double get(byte key)
    {
        return this.map.get(key);
    }

    @Override
    public double getIfAbsent(byte key, double ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public double getOrThrow(byte key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(byte key)
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
    public void forEachKey(ByteProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ByteDoubleProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyByteIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ByteDoublePair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableDoubleByteMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableByteDoubleMap select(ByteDoublePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableByteDoubleMap reject(ByteDoublePredicate predicate)
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
    public MutableDoubleBag select(DoublePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableDoubleBag reject(DoublePredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
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
    public double addToValue(byte key, double toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
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
    public MutableByteDoubleMap withKeyValue(byte key, double value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteDoubleMap withoutKey(byte key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteDoubleMap withoutAllKeys(ByteIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteDoubleMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteDoubleMap asSynchronized()
    {
        return new SynchronizedByteDoubleMap(this);
    }

    @Override
    public ImmutableByteDoubleMap toImmutable()
    {
        return ByteDoubleMaps.immutable.withAll(this);
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
    public MutableByteSet keySet()
    {
        return UnmodifiableByteSet.of(this.map.keySet());
    }

    @Override
    public MutableDoubleCollection values()
    {
        return UnmodifiableDoubleCollection.of(this.map.values());
    }

    @Override
    public boolean equals(Object otherMap)
    {
        return this.map.equals(otherMap);
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
