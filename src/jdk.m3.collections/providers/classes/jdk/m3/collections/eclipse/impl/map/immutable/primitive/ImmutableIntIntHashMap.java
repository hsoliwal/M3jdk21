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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntIntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.map.primitive.IntIntMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntIntMaps;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableIntIntMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntIntPair;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedIntIntProcedure;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableIntIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.IntIntHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableIntSet;

/**
 * ImmutableIntIntHashMap is the non-modifiable equivalent of {@link IntIntHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableIntIntHashMap implements ImmutableIntIntMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableIntIntMap delegate;

    ImmutableIntIntHashMap(IntIntMap delegate)
    {
        this.delegate = new IntIntHashMap(delegate);
    }

    @Override
    public int get(int key)
    {
        return this.delegate.get(key);
    }

    @Override
    public int getIfAbsent(int key, int ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public int getOrThrow(int key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(int key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(int value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(IntProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(IntProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(IntIntProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(IntIntPredicate predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public LazyIntIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<IntIntPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableIntIntMap flipUniqueValues()
    {
        MutableIntIntMap result = IntIntMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            if (result.containsKey(value))
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + result.get(value) + " and key: " + key);
            }
            result.put(value, key);
        });
        return result.toImmutable();
    }

    @Override
    public ImmutableIntIntMap select(IntIntPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableIntIntMap reject(IntIntPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            IntIterator iterator = this.delegate.intIterator();
            while (iterator.hasNext())
            {
                MutableIntBag batch = IntBags.mutable.empty();
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    batch.add(iterator.next());
                }
                result.add(batch.toImmutable());
            }
        }
        return result.toImmutable();
    }

    @Override
    public ImmutableIntIntMap toImmutable()
    {
        return this;
    }

    @Override
    public IntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.delegate.intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableIntBag select(IntPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableIntBag reject(IntPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public int max()
    {
        return this.delegate.max();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public int min()
    {
        return this.delegate.min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.delegate.average();
    }

    @Override
    public double median()
    {
        return this.delegate.median();
    }

    @Override
    public int[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public int[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(int value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableIntList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableIntIntMap newWithKeyValue(int key, int value)
    {
        MutableIntIntMap map = new IntIntHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableIntIntMap newWithoutKey(int key)
    {
        MutableIntIntMap map = new IntIntHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableIntIntMap newWithoutAllKeys(IntIterable keys)
    {
        MutableIntIntMap map = new IntIntHashMap(this.size());
        map.putAll(this);
        IntIterator iterator = keys.intIterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    @Override
    public MutableIntSet keySet()
    {
        return UnmodifiableIntSet.of(this.delegate.keySet());
    }

    @Override
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(this.delegate.values());
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    private Object writeReplace()
    {
        return new ImmutableIntIntMapSerializationProxy(this);
    }

    protected static class ImmutableIntIntMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private IntIntMap map;

        public ImmutableIntIntMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableIntIntMapSerializationProxy(IntIntMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedIntIntProcedure()
                {
                    @Override
                    public void safeValue(int key, int value) throws IOException
                    {
                        out.writeInt(key);
                        out.writeInt(value);
                    }
                });
            }
            catch (RuntimeException e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            MutableIntIntMap deserializedMap = new IntIntHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readInt(), in.readInt());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
