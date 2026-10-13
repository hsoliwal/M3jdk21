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
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoubleBytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.DoubleByteMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.DoubleBytePair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleByteMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableDoubleByteMap
        implements MutableDoubleByteMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableDoubleByteMap map;

    public UnmodifiableDoubleByteMap(MutableDoubleByteMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableDoubleByteMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(double key, byte value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(DoubleBytePair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(DoubleByteMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(DoubleByteToByteFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(double key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(double key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte removeKeyIfAbsent(double key, byte value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public byte getIfAbsentPut(double key, byte value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte getAndPut(double key, byte putValue, byte defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte getIfAbsentPut(double key, ByteFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte getIfAbsentPutWithKey(double key, DoubleToByteFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> byte getIfAbsentPutWith(double key, ByteFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public byte updateValue(double key, byte initialValueIfAbsent, ByteToByteFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte get(double key)
    {
        return this.map.get(key);
    }

    @Override
    public byte getIfAbsent(double key, byte ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public byte getOrThrow(double key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(double key)
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
    public void forEachKey(DoubleProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(DoubleByteProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<DoubleBytePair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableByteDoubleMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableDoubleByteMap select(DoubleBytePredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableDoubleByteMap reject(DoubleBytePredicate predicate)
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
    public byte addToValue(double key, byte toBeAdded)
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
    public MutableDoubleByteMap withKeyValue(double key, byte value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleByteMap withoutKey(double key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleByteMap withoutAllKeys(DoubleIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleByteMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleByteMap asSynchronized()
    {
        return new SynchronizedDoubleByteMap(this);
    }

    @Override
    public ImmutableDoubleByteMap toImmutable()
    {
        return DoubleByteMaps.immutable.withAll(this);
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
    public MutableDoubleSet keySet()
    {
        return UnmodifiableDoubleSet.of(this.map.keySet());
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
