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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction0;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharIntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.ImmutableCharIntMap;
import org.eclipse.collections.api.map.primitive.CharIntMap;
import org.eclipse.collections.api.map.primitive.MutableCharIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import org.eclipse.collections.impl.factory.primitive.CharIntMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharIntMap
        implements MutableCharIntMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharIntMap map;

    public UnmodifiableCharIntMap(MutableCharIntMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableCharIntMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(char key, int value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(CharIntPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(CharIntMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(CharIntToIntFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(char key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(char key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public int removeKeyIfAbsent(char key, int value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public int getIfAbsentPut(char key, int value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public int getAndPut(char key, int putValue, int defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public int getIfAbsentPut(char key, IntFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public int getIfAbsentPutWithKey(char key, CharToIntFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> int getIfAbsentPutWith(char key, IntFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public int updateValue(char key, int initialValueIfAbsent, IntToIntFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public int get(char key)
    {
        return this.map.get(key);
    }

    @Override
    public int getIfAbsent(char key, int ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public int getOrThrow(char key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(char key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(int value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(IntProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharIntProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<CharIntPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableIntCharMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableCharIntMap select(CharIntPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableCharIntMap reject(CharIntPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableIntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.map.intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableIntBag select(IntPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableIntBag reject(IntPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public int max()
    {
        return this.map.max();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public int min()
    {
        return this.map.min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int addToValue(char key, int toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public int[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public int[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(int value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableIntList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableCharIntMap withKeyValue(char key, int value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharIntMap withoutKey(char key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharIntMap withoutAllKeys(CharIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharIntMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharIntMap asSynchronized()
    {
        return new SynchronizedCharIntMap(this);
    }

    @Override
    public ImmutableCharIntMap toImmutable()
    {
        return CharIntMaps.immutable.withAll(this);
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
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(this.map.keySet());
    }

    @Override
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(this.map.values());
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
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
