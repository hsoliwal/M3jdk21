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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortBooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.ShortBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.ShortBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ShortBooleanMaps;
import org.eclipse.collections.impl.factory.primitive.ShortLists;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ShortBooleanHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableShortBooleanSingletonMap is an optimization for {@link ImmutableShortBooleanMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortBooleanSingletonMap implements ImmutableShortBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;
    private final short key1;
    private final boolean value1;

    ImmutableShortBooleanSingletonMap(short key1, boolean value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public boolean get(short key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public boolean getIfAbsent(short key, boolean ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public boolean getOrThrow(short key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(short key)
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
    public void forEachKey(ShortProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ShortBooleanProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return ShortLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ShortBooleanPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableShortBooleanMap select(ShortBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? ShortBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new ShortBooleanHashMap().toImmutable();
    }

    @Override
    public ImmutableShortBooleanMap reject(ShortBooleanPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new ShortBooleanHashMap().toImmutable()
                : ShortBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
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
    public ImmutableShortBooleanMap toImmutable()
    {
        return this;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(ShortBooleanHashMap.newWithKeysValues(this.key1, this.value1).booleanIterator());
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
    public ImmutableShortBooleanMap newWithKeyValue(short key, boolean value)
    {
        return ShortBooleanMaps.immutable.withAll(ShortBooleanHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableShortBooleanMap newWithoutKey(short key)
    {
        return this.key1 == key ? ShortBooleanMaps.immutable.with() : this;
    }

    @Override
    public ImmutableShortBooleanMap newWithoutAllKeys(ShortIterable keys)
    {
        return keys.contains(this.key1) ? ShortBooleanMaps.immutable.with() : this;
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
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(ShortHashSet.newSetWith(this.key1));
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
        if (!(obj instanceof ShortBooleanMap))
        {
            return false;
        }
        ShortBooleanMap map = (ShortBooleanMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) this.key1 ^ (this.value1 ? 1231 : 1237);
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
