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
import java.util.Set;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBooleanPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectBooleanHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableObjectBooleanSingletonMap is an optimization for {@link ImmutableObjectBooleanMap} of size 1.
 * This file was automatically generated from template file immutableObjectPrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableObjectBooleanSingletonMap<K> extends AbstractImmutableObjectBooleanMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final boolean EMPTY_VALUE = false;
    private final K key1;
    private final boolean value1;

    ImmutableObjectBooleanSingletonMap(K key1, boolean value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(ObjectBooleanHashMap.newWithKeysValues(this.key1, this.value1).booleanIterator());
    }

    @Override
    public void forEach(BooleanProcedure procedure)
    {
        this.each(procedure);
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
    public ImmutableBooleanCollection select(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1) ? BooleanLists.immutable.with(this.value1) : BooleanLists.immutable.with();
    }

    @Override
    public ImmutableBooleanCollection reject(BooleanPredicate predicate)
    {
        return predicate.accept(this.value1) ? BooleanLists.immutable.with() : BooleanLists.immutable.with(this.value1);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
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
        return Lists.immutable.with(BooleanLists.immutable.with(this.value1));
    }

    @Override
    public <V> ImmutableCollection<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of(function.valueOf(this.value1));
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
    public ImmutableObjectBooleanMap<K> newWithKeyValue(K key, boolean value)
    {
        return ObjectBooleanMaps.immutable.withAll(ObjectBooleanHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableObjectBooleanMap<K> newWithoutKey(K key)
    {
        return nullSafeEquals(this.key1, key) ? (ImmutableObjectBooleanMap<K>) ImmutableObjectBooleanEmptyMap.INSTANCE : this;
    }

    private static boolean nullSafeEquals(Object value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (other == value || value.equals(other))
        {
            return true;
        }
        return false;
    }

    @Override
    public ImmutableObjectBooleanMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectBooleanHashMap<K> map = new ObjectBooleanHashMap<K>(this);
        for (K key : keys)
        {
            map.removeKey(key);
        }
        return map.toImmutable();
    }

    @Override
    public boolean get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public boolean getOrThrow(Object key)
    {
        if (nullSafeEquals(this.key1, key))
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean getIfAbsent(Object key, boolean ifAbsent)
    {
        return nullSafeEquals(this.key1, key) ? this.value1 : ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return nullSafeEquals(this.key1, key);
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
    public void forEachKey(Procedure<? super K> procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ObjectBooleanProcedure<? super K> objectBooleanProcedure)
    {
        objectBooleanProcedure.value(this.key1, this.value1);
    }

    @Override
    public ImmutableObjectBooleanMap<K> select(ObjectBooleanPredicate<? super K> objectBooleanPredicate)
    {
        return objectBooleanPredicate.accept(this.key1, this.value1) ? ObjectBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableObjectBooleanMap<K>) ImmutableObjectBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableObjectBooleanMap<K> reject(ObjectBooleanPredicate<? super K> objectBooleanPredicate)
    {
        return objectBooleanPredicate.accept(this.key1, this.value1) ? (ImmutableObjectBooleanMap<K>) ImmutableObjectBooleanEmptyMap.INSTANCE
                : ObjectBooleanHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableObjectBooleanMap<K> toImmutable()
    {
        return this;
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
    public Set<K> keySet()
    {
        return Sets.immutable.of(this.key1).castToSet();
    }

    @Override
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(BooleanArrayList.newListWith(this.value1));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return Lists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ObjectBooleanPair<K>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectBooleanMap))
        {
            return false;
        }
        ObjectBooleanMap<K> map = (ObjectBooleanMap<K>) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (this.key1 == null ? 0 : this.key1.hashCode()) ^ (this.value1 ? 1231 : 1237);
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

    private Object writeReplace()
    {
        return new ImmutableObjectBooleanMapSerializationProxy<K>(this);
    }
}
