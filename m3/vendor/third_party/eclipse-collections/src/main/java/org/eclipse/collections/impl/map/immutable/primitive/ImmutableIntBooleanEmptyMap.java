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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.IntBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.IntBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.iterator.ImmutableEmptyBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableIntSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyIntIterate;

/**
 * ImmutableIntBooleanEmptyMap is an optimization for {@link ImmutableIntBooleanMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntBooleanEmptyMap implements ImmutableIntBooleanMap, Serializable
{
    static final ImmutableIntBooleanMap INSTANCE = new ImmutableIntBooleanEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public boolean get(int key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public boolean getIfAbsent(int key, boolean ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public boolean getOrThrow(int key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(int key)
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
    public void forEachKey(IntProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(IntBooleanProcedure procedure)
    {
    }

    @Override
    public LazyIntIterable keysView()
    {
        return LazyIntIterate.empty();
    }

    @Override
    public RichIterable<IntBooleanPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableIntBooleanMap select(IntBooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableIntBooleanMap reject(IntBooleanPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableIntBooleanMap toImmutable()
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
    public ImmutableIntBooleanMap newWithKeyValue(int key, boolean value)
    {
        return new ImmutableIntBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableIntBooleanMap newWithoutKey(int key)
    {
        return this;
    }

    @Override
    public ImmutableIntBooleanMap newWithoutAllKeys(IntIterable keys)
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
    public MutableIntSet keySet()
    {
        return UnmodifiableIntSet.of(new IntHashSet());
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
        if (!(obj instanceof IntBooleanMap))
        {
            return false;
        }
        IntBooleanMap map = (IntBooleanMap) obj;
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
