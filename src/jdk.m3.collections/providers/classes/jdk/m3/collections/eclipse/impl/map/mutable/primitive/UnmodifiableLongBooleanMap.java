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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction0;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToBooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongBooleanToBooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToBooleanFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongBooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongBooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableBooleanCollection;
import jdk.m3.collections.eclipse.api.iterator.MutableBooleanIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableLongBooleanMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongBooleanPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongBooleanMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableBooleanIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableLongSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableLongBooleanMap
        implements MutableLongBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableLongBooleanMap map;

    public UnmodifiableLongBooleanMap(MutableLongBooleanMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableLongBooleanMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(long key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(LongBooleanPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(LongBooleanMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(LongBooleanToBooleanFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(long key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(long key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeKeyIfAbsent(long key, boolean value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public boolean getIfAbsentPut(long key, boolean value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean getAndPut(long key, boolean putValue, boolean defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean getIfAbsentPut(long key, BooleanFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean getIfAbsentPutWithKey(long key, LongToBooleanFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> boolean getIfAbsentPutWith(long key, BooleanFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean updateValue(long key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean get(long key)
    {
        return this.map.get(key);
    }

    @Override
    public boolean getIfAbsent(long key, boolean ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean getOrThrow(long key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(long key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(boolean value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(LongBooleanProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<LongBooleanPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableLongBooleanMap select(LongBooleanPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableLongBooleanMap reject(LongBooleanPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.map.booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableBooleanBag select(BooleanPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableBooleanBag reject(BooleanPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public boolean[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableBooleanList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableLongBooleanMap withKeyValue(long key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongBooleanMap withoutKey(long key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongBooleanMap withoutAllKeys(LongIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongBooleanMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableLongBooleanMap asSynchronized()
    {
        return new SynchronizedLongBooleanMap(this);
    }

    @Override
    public ImmutableLongBooleanMap toImmutable()
    {
        return LongBooleanMaps.immutable.withAll(this);
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
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(this.map.keySet());
    }

    @Override
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(this.map.values());
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
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
