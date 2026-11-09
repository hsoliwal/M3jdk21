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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction0;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.ImmutableCharShortMap;
import org.eclipse.collections.api.map.primitive.CharShortMap;
import org.eclipse.collections.api.map.primitive.MutableCharShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.CharShortPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.primitive.CharShortMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharShortMap
        implements MutableCharShortMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharShortMap map;

    public UnmodifiableCharShortMap(MutableCharShortMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableCharShortMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(char key, short value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(CharShortPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(CharShortMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(CharShortToShortFunction function)
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
    public short removeKeyIfAbsent(char key, short value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public short getIfAbsentPut(char key, short value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public short getAndPut(char key, short putValue, short defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public short getIfAbsentPut(char key, ShortFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public short getIfAbsentPutWithKey(char key, CharToShortFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> short getIfAbsentPutWith(char key, ShortFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public short updateValue(char key, short initialValueIfAbsent, ShortToShortFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public short get(char key)
    {
        return this.map.get(key);
    }

    @Override
    public short getIfAbsent(char key, short ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public short getOrThrow(char key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(char key)
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
    public void forEachKey(CharProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharShortProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<CharShortPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableShortCharMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableCharShortMap select(CharShortPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableCharShortMap reject(CharShortPredicate predicate)
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
    public MutableShortBag select(ShortPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableShortBag reject(ShortPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
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
    public short addToValue(char key, short toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
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
    public MutableCharShortMap withKeyValue(char key, short value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharShortMap withoutKey(char key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharShortMap withoutAllKeys(CharIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharShortMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharShortMap asSynchronized()
    {
        return new SynchronizedCharShortMap(this);
    }

    @Override
    public ImmutableCharShortMap toImmutable()
    {
        return CharShortMaps.immutable.withAll(this);
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
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(this.map.values());
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
