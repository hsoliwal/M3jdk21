/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import java.io.Serializable;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction0;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntDoubleToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToDoubleFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntDoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntDoubleProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableDoubleCollection;
import jdk.m3.collections.eclipse.api.iterator.MutableDoubleIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.IntDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableDoubleIntMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntDoublePair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntDoubleMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableDoubleIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableIntSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableIntDoubleMap
        implements MutableIntDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableIntDoubleMap map;

    public UnmodifiableIntDoubleMap(MutableIntDoubleMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableIntDoubleMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(int key, double value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(IntDoublePair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(IntDoubleMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(IntDoubleToDoubleFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(int key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(int key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public double removeKeyIfAbsent(int key, double value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public double getIfAbsentPut(int key, double value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double getAndPut(int key, double putValue, double defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public double getIfAbsentPut(int key, DoubleFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double getIfAbsentPutWithKey(int key, IntToDoubleFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> double getIfAbsentPutWith(int key, DoubleFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public double updateValue(int key, double initialValueIfAbsent, DoubleToDoubleFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public double get(int key)
    {
        return this.map.get(key);
    }

    @Override
    public double getIfAbsent(int key, double ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public double getOrThrow(int key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(int key)
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
    public void forEachKey(IntProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(IntDoubleProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyIntIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<IntDoublePair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableDoubleIntMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableIntDoubleMap select(IntDoublePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableIntDoubleMap reject(IntDoublePredicate predicate)
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
    public double addToValue(int key, double toBeAdded)
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
    public MutableIntDoubleMap withKeyValue(int key, double value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntDoubleMap withoutKey(int key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntDoubleMap withoutAllKeys(IntIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntDoubleMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntDoubleMap asSynchronized()
    {
        return new SynchronizedIntDoubleMap(this);
    }

    @Override
    public ImmutableIntDoubleMap toImmutable()
    {
        return IntDoubleMaps.immutable.withAll(this);
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
    public MutableIntSet keySet()
    {
        return UnmodifiableIntSet.of(this.map.keySet());
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
