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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction0;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToFloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFloatToFloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToFloatFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntFloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntFloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableFloatCollection;
import jdk.m3.collections.eclipse.api.iterator.MutableFloatIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.IntFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatIntMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntFloatPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntFloatMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableFloatIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableIntSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableIntFloatMap
        implements MutableIntFloatMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableIntFloatMap map;

    public UnmodifiableIntFloatMap(MutableIntFloatMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableIntFloatMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(int key, float value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(IntFloatPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(IntFloatMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(IntFloatToFloatFunction function)
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
    public float removeKeyIfAbsent(int key, float value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public float getIfAbsentPut(int key, float value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public float getAndPut(int key, float putValue, float defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public float getIfAbsentPut(int key, FloatFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public float getIfAbsentPutWithKey(int key, IntToFloatFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> float getIfAbsentPutWith(int key, FloatFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public float updateValue(int key, float initialValueIfAbsent, FloatToFloatFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public float get(int key)
    {
        return this.map.get(key);
    }

    @Override
    public float getIfAbsent(int key, float ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public float getOrThrow(int key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(int key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(float value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(IntProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(IntFloatProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyIntIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<IntFloatPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableFloatIntMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableIntFloatMap select(IntFloatPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableIntFloatMap reject(IntFloatPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableFloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.map.floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableFloatBag select(FloatPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableFloatBag reject(FloatPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public double sum()
    {
        return this.map.sum();
    }

    @Override
    public float max()
    {
        return this.map.max();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public float min()
    {
        return this.map.min();
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float addToValue(int key, float toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public float[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public float[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public float[] toArray(float[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(float value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableFloatList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableFloatSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableIntFloatMap withKeyValue(int key, float value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntFloatMap withoutKey(int key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntFloatMap withoutAllKeys(IntIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntFloatMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntFloatMap asSynchronized()
    {
        return new SynchronizedIntFloatMap(this);
    }

    @Override
    public ImmutableIntFloatMap toImmutable()
    {
        return IntFloatMaps.immutable.withAll(this);
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
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(this.map.values());
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
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
