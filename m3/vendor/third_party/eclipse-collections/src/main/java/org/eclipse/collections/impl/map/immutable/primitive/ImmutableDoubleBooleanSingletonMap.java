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
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoubleBooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.DoubleBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.DoubleBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.DoubleBooleanMaps;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.DoubleBooleanHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableDoubleBooleanSingletonMap is an optimization for {@link ImmutableDoubleBooleanMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleBooleanSingletonMap implements ImmutableDoubleBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;
    private final double key1;
    private final boolean value1;

    ImmutableDoubleBooleanSingletonMap(double key1, boolean value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public boolean get(double key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public boolean getIfAbsent(double key, boolean ifAbsent)
    {
        return Double.compare(this.key1, key) == 0 ? this.value1 : ifAbsent;
    }

    @Override
    public boolean getOrThrow(double key)
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
    public boolean containsValue(boolean value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(DoubleBooleanProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return DoubleLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<DoubleBooleanPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableDoubleBooleanMap select(DoubleBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? DoubleBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new DoubleBooleanHashMap().toImmutable();
    }

    @Override
    public ImmutableDoubleBooleanMap reject(DoubleBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new DoubleBooleanHashMap().toImmutable()
                : DoubleBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(BooleanBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableDoubleBooleanMap toImmutable()
    {
        return this;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(DoubleBooleanHashMap.newWithKeysValues(this.key1, this.value1).booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableBooleanBag select(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1) ? BooleanHashBag.newBagWith(this.value1).toImmutable() : BooleanBags.immutable.empty();
    }

    @Override
    public ImmutableBooleanBag reject(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1) ? BooleanBags.immutable.empty() : BooleanHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[]{this.value1};
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        if (target.length < 1)
        {
            target = new boolean[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        for (boolean value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        for (BooleanIterator iterator = source.booleanIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableBooleanList toList()
    {
        return BooleanArrayList.newListWith(this.value1);
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return BooleanHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return BooleanHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleBooleanMap newWithKeyValue(double key, boolean value)
    {
        return DoubleBooleanMaps.immutable.withAll(DoubleBooleanHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableDoubleBooleanMap newWithoutKey(double key)
    {
        return Double.compare(this.key1, key) == 0 ? DoubleBooleanMaps.immutable.with() : this;
    }

    @Override
    public ImmutableDoubleBooleanMap newWithoutAllKeys(DoubleIterable keys)
    {
        return keys.contains(this.key1) ? DoubleBooleanMaps.immutable.with() : this;
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
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(BooleanArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof DoubleBooleanMap))
        {
            return false;
        }
        DoubleBooleanMap map = (DoubleBooleanMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) (Double.doubleToLongBits(this.key1) ^ Double.doubleToLongBits(this.key1) >>> 32) ^ (this.value1 ? 1231 : 1237);
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
