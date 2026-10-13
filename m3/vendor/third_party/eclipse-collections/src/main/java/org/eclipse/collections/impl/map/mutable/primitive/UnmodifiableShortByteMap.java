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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortBytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.ImmutableShortByteMap;
import org.eclipse.collections.api.map.primitive.ShortByteMap;
import org.eclipse.collections.api.map.primitive.MutableShortByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteShortMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ShortBytePair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.primitive.ShortByteMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableShortByteMap
        implements MutableShortByteMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortByteMap map;

    public UnmodifiableShortByteMap(MutableShortByteMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableShortByteMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(short key, byte value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ShortBytePair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ShortByteMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ShortByteToByteFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(short key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(short key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte removeKeyIfAbsent(short key, byte value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public byte getIfAbsentPut(short key, byte value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte getAndPut(short key, byte putValue, byte defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte getIfAbsentPut(short key, ByteFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte getIfAbsentPutWithKey(short key, ShortToByteFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> byte getIfAbsentPutWith(short key, ByteFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte updateValue(short key, byte initialValueIfAbsent, ByteToByteFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte get(short key)
    {
        return this.map.get(key);
    }

    @Override
    public byte getIfAbsent(short key, byte ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public byte getOrThrow(short key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(byte value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(ByteProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortByteProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ShortBytePair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableByteShortMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableShortByteMap select(ShortBytePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableShortByteMap reject(ShortBytePredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.map.byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableByteBag select(BytePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableByteBag reject(BytePredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public byte max()
    {
        return this.map.max();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public byte min()
    {
        return this.map.min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
    public byte addToValue(short key, byte toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public byte[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(byte value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableByteList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableShortByteMap withKeyValue(short key, byte value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortByteMap withoutKey(short key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortByteMap withoutAllKeys(ShortIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortByteMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortByteMap asSynchronized()
    {
        return new SynchronizedShortByteMap(this);
    }

    @Override
    public ImmutableShortByteMap toImmutable()
    {
        return ShortByteMaps.immutable.withAll(this);
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
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(this.map.keySet());
    }

    @Override
    public MutableByteCollection values()
    {
        return UnmodifiableByteCollection.of(this.map.values());
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
