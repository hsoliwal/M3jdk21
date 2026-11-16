/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongBooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongBooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableBooleanCollection;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.map.primitive.LongBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongBooleanMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongBooleanPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.BooleanHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongBooleanMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableBooleanIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.BooleanArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.LongBooleanHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.BooleanHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableLongSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableLongBooleanSingletonMap is an optimization for {@link ImmutableLongBooleanMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongBooleanSingletonMap implements ImmutableLongBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;
    private final long key1;
    private final boolean value1;

    ImmutableLongBooleanSingletonMap(long key1, boolean value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public boolean get(long key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public boolean getIfAbsent(long key, boolean ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public boolean getOrThrow(long key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(long key)
    {
        return this.key1 == key;
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
    public void forEachKey(LongProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(LongBooleanProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(LongBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LongLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<LongBooleanPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableLongBooleanMap select(LongBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? LongBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new LongBooleanHashMap().toImmutable();
    }

    @Override
    public ImmutableLongBooleanMap reject(LongBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new LongBooleanHashMap().toImmutable()
                : LongBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
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
    public ImmutableLongBooleanMap toImmutable()
    {
        return this;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(LongBooleanHashMap.newWithKeysValues(this.key1, this.value1).booleanIterator());
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
    public ImmutableLongBooleanMap newWithKeyValue(long key, boolean value)
    {
        return LongBooleanMaps.immutable.withAll(LongBooleanHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableLongBooleanMap newWithoutKey(long key)
    {
        return this.key1 == key ? LongBooleanMaps.immutable.with() : this;
    }

    @Override
    public ImmutableLongBooleanMap newWithoutAllKeys(LongIterable keys)
    {
        return keys.contains(this.key1) ? LongBooleanMaps.immutable.with() : this;
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
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(LongHashSet.newSetWith(this.key1));
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
        if (!(obj instanceof LongBooleanMap))
        {
            return false;
        }
        LongBooleanMap map = (LongBooleanMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.key1 ^ this.key1 >>> 32) ^ (this.value1 ? 1231 : 1237);
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
