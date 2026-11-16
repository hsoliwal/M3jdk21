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

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharFloatPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharFloatProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableFloatCollection;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.map.primitive.CharFloatMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatCharMaps;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableFloatCharMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharFloatPair;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedCharFloatProcedure;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableFloatIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.CharFloatHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * ImmutableCharFloatHashMap is the non-modifiable equivalent of {@link CharFloatHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableCharFloatHashMap implements ImmutableCharFloatMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharFloatMap delegate;

    ImmutableCharFloatHashMap(CharFloatMap delegate)
    {
        this.delegate = new CharFloatHashMap(delegate);
    }

    @Override
    public float get(char key)
    {
        return this.delegate.get(key);
    }

    @Override
    public float getIfAbsent(char key, float ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public float getOrThrow(char key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(char key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(float value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharFloatProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(CharFloatPredicate predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<CharFloatPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableFloatCharMap flipUniqueValues()
    {
        MutableFloatCharMap result = FloatCharMaps.mutable.empty();
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
    public ImmutableCharFloatMap select(CharFloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableCharFloatMap reject(CharFloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            FloatIterator iterator = this.delegate.floatIterator();
            while (iterator.hasNext())
            {
                MutableFloatBag batch = FloatBags.mutable.empty();
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
    public ImmutableCharFloatMap toImmutable()
    {
        return this;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.delegate.floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public double sum()
    {
        return this.delegate.sum();
    }

    @Override
    public float max()
    {
        return this.delegate.max();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public float min()
    {
        return this.delegate.min();
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public float[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public float[] toArray(float[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(float value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableFloatList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableFloatSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableCharFloatMap newWithKeyValue(char key, float value)
    {
        MutableCharFloatMap map = new CharFloatHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharFloatMap newWithoutKey(char key)
    {
        MutableCharFloatMap map = new CharFloatHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharFloatMap newWithoutAllKeys(CharIterable keys)
    {
        MutableCharFloatMap map = new CharFloatHashMap(this.size());
        map.putAll(this);
        CharIterator iterator = keys.charIterator();
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
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(this.delegate.keySet());
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(this.delegate.values());
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
        return new ImmutableCharFloatMapSerializationProxy(this);
    }

    protected static class ImmutableCharFloatMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private CharFloatMap map;

        public ImmutableCharFloatMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableCharFloatMapSerializationProxy(CharFloatMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedCharFloatProcedure()
                {
                    @Override
                    public void safeValue(char key, float value) throws IOException
                    {
                        out.writeChar(key);
                        out.writeFloat(value);
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
            MutableCharFloatMap deserializedMap = new CharFloatHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readChar(), in.readFloat());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
