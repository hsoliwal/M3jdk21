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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoubleDoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleDoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.primitive.DoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.DoubleDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.DoubleDoubleHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableDoubleDoubleSingletonMap is an optimization for {@link ImmutableDoubleDoubleMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleDoubleSingletonMap implements ImmutableDoubleDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final double EMPTY_VALUE = 0.0;
    private final double key1;
    private final double value1;

    ImmutableDoubleDoubleSingletonMap(double key1, double value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public double get(double key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public double getIfAbsent(double key, double ifAbsent)
    {
        return Double.compare(this.key1, key) == 0 ? this.value1 : ifAbsent;
    }

    @Override
    public double getOrThrow(double key)
    {
        if (Double.compare(this.key1, key) == 0)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(double key)
    {
        return Double.compare(this.key1, key) == 0;
    }

    @Override
    public boolean containsValue(double value)
    {
        return Double.compare(this.value1, value) == 0;
    }

    @Override
    public void forEachValue(DoubleProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(DoubleDoubleProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return DoubleLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<DoubleDoublePair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableDoubleDoubleMap flipUniqueValues()
    {
        return DoubleDoubleMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableDoubleDoubleMap select(DoubleDoublePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? DoubleDoubleHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new DoubleDoubleHashMap().toImmutable();
    }

    @Override
    public ImmutableDoubleDoubleMap reject(DoubleDoublePredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new DoubleDoubleHashMap().toImmutable()
                : DoubleDoubleHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(DoubleBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableDoubleDoubleMap toImmutable()
    {
        return this;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(DoubleDoubleHashMap.newWithKeysValues(this.key1, this.value1).doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public double sum()
    {
        return this.value1;
    }

    @Override
    public double min()
    {
        return this.value1;
    }

    @Override
    public double max()
    {
        return this.value1;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.value1;
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return new double[]{this.value1};
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableDoubleBag select(DoublePredicate predicate)
    {
        return predicate.accept(this.value1) ? DoubleHashBag.newBagWith(this.value1).toImmutable() : DoubleBags.immutable.empty();
    }

    @Override
    public ImmutableDoubleBag reject(DoublePredicate predicate)
    {
        return predicate.accept(this.value1) ? DoubleBags.immutable.empty() : DoubleHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public double[] toArray()
    {
        return new double[]{this.value1};
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < 1)
        {
            target = new double[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(double value)
    {
        return Double.compare(this.value1, value) == 0;
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double value : source)
        {
            if (Double.compare(this.value1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (Double.compare(this.value1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newListWith(this.value1);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleDoubleMap newWithKeyValue(double key, double value)
    {
        return DoubleDoubleMaps.immutable.withAll(DoubleDoubleHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableDoubleDoubleMap newWithoutKey(double key)
    {
        return Double.compare(this.key1, key) == 0 ? DoubleDoubleMaps.immutable.with() : this;
    }

    @Override
    public ImmutableDoubleDoubleMap newWithoutAllKeys(DoubleIterable keys)
    {
        return keys.contains(this.key1) ? DoubleDoubleMaps.immutable.with() : this;
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
    public MutableDoubleSet keySet()
    {
        return UnmodifiableDoubleSet.of(DoubleHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableDoubleCollection values()
    {
        return UnmodifiableDoubleCollection.of(DoubleArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof DoubleDoubleMap))
        {
            return false;
        }
        DoubleDoubleMap map = (DoubleDoubleMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && Double.compare(this.value1, map.getOrThrow(this.key1)) == 0;
    }

    @Override
    public int hashCode()
    {
        return (int) (Double.doubleToLongBits(this.key1) ^ Double.doubleToLongBits(this.key1) >>> 32) ^ (int) (Double.doubleToLongBits(this.value1) ^ Double.doubleToLongBits(this.value1) >>> 32);
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
