/*
 * Copyright (c) 2022 Goldman Sachs.
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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.ByteBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.ByteBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.iterator.ImmutableEmptyBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableByteSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyByteIterate;

/**
 * ImmutableByteBooleanEmptyMap is an optimization for {@link ImmutableByteBooleanMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteBooleanEmptyMap implements ImmutableByteBooleanMap, Serializable
{
    static final ImmutableByteBooleanMap INSTANCE = new ImmutableByteBooleanEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public boolean get(byte key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public boolean getIfAbsent(byte key, boolean ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public boolean getOrThrow(byte key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(byte key)
    {
        return false;
    }

    @Override
    public boolean containsValue(boolean value)
    {
        return false;
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
    }

    @Override
    public void forEachKey(ByteProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(ByteBooleanProcedure procedure)
    {
    }

    @Override
    public LazyByteIterable keysView()
    {
        return LazyByteIterate.empty();
    }

    @Override
    public RichIterable<ByteBooleanPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableByteBooleanMap select(ByteBooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteBooleanMap reject(ByteBooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteBooleanMap toImmutable()
    {
        return this;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return ImmutableEmptyBooleanIterator.INSTANCE;
    }

    @Override
    public void forEach(BooleanProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableBooleanBag select(BooleanPredicate predicate)
    {
        return BooleanBags.immutable.empty();
    }

    @Override
    public ImmutableBooleanBag reject(BooleanPredicate predicate)
    {
        return BooleanBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean[] toArray()
    {
        return new boolean[0];
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return target;
    }

    @Override
    public boolean contains(boolean value)
    {
        return false;
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableBooleanList toList()
    {
        return new BooleanArrayList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return new BooleanHashSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return new BooleanHashBag();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public ImmutableByteBooleanMap newWithKeyValue(byte key, boolean value)
    {
        return new ImmutableByteBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableByteBooleanMap newWithoutKey(byte key)
    {
        return this;
    }

    @Override
    public ImmutableByteBooleanMap newWithoutAllKeys(ByteIterable keys)
    {
        return this;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public MutableByteSet keySet()
    {
        return UnmodifiableByteSet.of(new ByteHashSet());
    }

    @Override
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(new BooleanArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ByteBooleanMap))
        {
            return false;
        }
        ByteBooleanMap map = (ByteBooleanMap) obj;
        return map.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{}";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
