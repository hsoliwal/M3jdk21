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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToBooleanFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortBooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.ImmutableShortBooleanMap;
import org.eclipse.collections.api.map.primitive.ShortBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableShortBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.ShortBooleanPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.primitive.ShortBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableShortBooleanMap
        implements MutableShortBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortBooleanMap map;

    public UnmodifiableShortBooleanMap(MutableShortBooleanMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableShortBooleanMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(short key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ShortBooleanPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ShortBooleanMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ShortBooleanToBooleanFunction function)
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
    public boolean removeKeyIfAbsent(short key, boolean value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public boolean getIfAbsentPut(short key, boolean value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean getAndPut(short key, boolean putValue, boolean defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean getIfAbsentPut(short key, BooleanFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean getIfAbsentPutWithKey(short key, ShortToBooleanFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> boolean getIfAbsentPutWith(short key, BooleanFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public boolean updateValue(short key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean get(short key)
    {
        return this.map.get(key);
    }

    @Override
    public boolean getIfAbsent(short key, boolean ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean getOrThrow(short key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
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
    public void forEachKey(ShortProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortBooleanProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ShortBooleanPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableShortBooleanMap select(ShortBooleanPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableShortBooleanMap reject(ShortBooleanPredicate predicate)
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
    public MutableShortBooleanMap withKeyValue(short key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortBooleanMap withoutKey(short key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortBooleanMap withoutAllKeys(ShortIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortBooleanMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortBooleanMap asSynchronized()
    {
        return new SynchronizedShortBooleanMap(this);
    }

    @Override
    public ImmutableShortBooleanMap toImmutable()
    {
        return ShortBooleanMaps.immutable.withAll(this);
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
