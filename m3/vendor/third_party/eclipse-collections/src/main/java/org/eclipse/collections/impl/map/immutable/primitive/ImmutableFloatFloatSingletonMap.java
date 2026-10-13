/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatFloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.FloatFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatFloatMaps;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.FloatFloatHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableFloatFloatSingletonMap is an optimization for {@link ImmutableFloatFloatMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatFloatSingletonMap implements ImmutableFloatFloatMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_VALUE = 0.0f;
    private final float key1;
    private final float value1;

    ImmutableFloatFloatSingletonMap(float key1, float value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public float get(float key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public float getIfAbsent(float key, float ifAbsent)
    {
        return Float.compare(this.key1, key) == 0 ? this.value1 : ifAbsent;
    }

    @Override
    public float getOrThrow(float key)
    {
        if (Float.compare(this.key1, key) == 0)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(float key)
    {
        return Float.compare(this.key1, key) == 0;
    }

    @Override
    public boolean containsValue(float value)
    {
        return Float.compare(this.value1, value) == 0;
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(FloatFloatProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return FloatLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<FloatFloatPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableFloatFloatMap flipUniqueValues()
    {
        return FloatFloatMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableFloatFloatMap select(FloatFloatPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? FloatFloatHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new FloatFloatHashMap().toImmutable();
    }

    @Override
    public ImmutableFloatFloatMap reject(FloatFloatPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new FloatFloatHashMap().toImmutable()
                : FloatFloatHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(FloatBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableFloatFloatMap toImmutable()
    {
        return this;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(FloatFloatHashMap.newWithKeysValues(this.key1, this.value1).floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public double sum()
    {
        return this.value1;
    }

    @Override
    public float min()
    {
        return this.value1;
    }

    @Override
    public float max()
    {
        return this.value1;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.value1;
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        return this.value1;
    }

    @Override
    public double average()
    {
        return this.value1;
    }

    @Override
    public double median()
    {
        return this.value1;
    }

    @Override
    public float[] toSortedArray()
    {
        return new float[]{this.value1};
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? FloatHashBag.newBagWith(this.value1).toImmutable() : FloatBags.immutable.empty();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? FloatBags.immutable.empty() : FloatHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public float[] toArray()
    {
        return new float[]{this.value1};
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < 1)
        {
            target = new float[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(float value)
    {
        return Float.compare(this.value1, value) == 0;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (Float.compare(this.value1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (Float.compare(this.value1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newListWith(this.value1);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableFloatFloatMap newWithKeyValue(float key, float value)
    {
        return FloatFloatMaps.immutable.withAll(FloatFloatHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableFloatFloatMap newWithoutKey(float key)
    {
        return Float.compare(this.key1, key) == 0 ? FloatFloatMaps.immutable.with() : this;
    }

    @Override
    public ImmutableFloatFloatMap newWithoutAllKeys(FloatIterable keys)
    {
        return keys.contains(this.key1) ? FloatFloatMaps.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(FloatHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(FloatArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof FloatFloatMap))
        {
            return false;
        }
        FloatFloatMap map = (FloatFloatMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && Float.compare(this.value1, map.getOrThrow(this.key1)) == 0;
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(this.key1) ^ Float.floatToIntBits(this.value1);
    }

    @Override
    public String toString()
    {
        return "{" + this.key1 + "=" + this.value1 + "}";
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.value1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
