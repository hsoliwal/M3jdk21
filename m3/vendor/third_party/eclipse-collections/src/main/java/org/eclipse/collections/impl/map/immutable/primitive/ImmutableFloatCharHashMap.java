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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatCharPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.FloatCharMap;
import org.eclipse.collections.impl.factory.primitive.CharFloatMaps;
import org.eclipse.collections.api.map.primitive.ImmutableFloatCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.FloatCharPair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedFloatCharProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.iterator.UnmodifiableCharIterator;
import org.eclipse.collections.impl.map.mutable.primitive.FloatCharHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;

/**
 * ImmutableFloatCharHashMap is the non-modifiable equivalent of {@link FloatCharHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatCharHashMap implements ImmutableFloatCharMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableFloatCharMap delegate;

    ImmutableFloatCharHashMap(FloatCharMap delegate)
    {
        this.delegate = new FloatCharHashMap(delegate);
    }

    @Override
    public char get(float key)
    {
        return this.delegate.get(key);
    }

    @Override
    public char getIfAbsent(float key, char ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public char getOrThrow(float key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(float key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(char value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(CharProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(FloatCharProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<FloatCharPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableCharFloatMap flipUniqueValues()
    {
        MutableCharFloatMap result = CharFloatMaps.mutable.empty();
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
    public ImmutableFloatCharMap select(FloatCharPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatCharMap reject(FloatCharPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            CharIterator iterator = this.delegate.charIterator();
            while (iterator.hasNext())
            {
                MutableCharBag batch = CharBags.mutable.empty();
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
    public ImmutableFloatCharMap toImmutable()
    {
        return this;
    }

    @Override
    public CharIterator charIterator()
    {
        return new UnmodifiableCharIterator(this.delegate.charIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableCharBag select(CharPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableCharBag reject(CharPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public char max()
    {
        return this.delegate.max();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public char min()
    {
        return this.delegate.min();
    }

    @Override
    public char minIfEmpty(char defaultValue)
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
    public char[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableCharList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public char[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public char[] toArray(char[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(char value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableCharList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableCharSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableCharBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableFloatCharMap newWithKeyValue(float key, char value)
    {
        MutableFloatCharMap map = new FloatCharHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableFloatCharMap newWithoutKey(float key)
    {
        MutableFloatCharMap map = new FloatCharHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableFloatCharMap newWithoutAllKeys(FloatIterable keys)
    {
        MutableFloatCharMap map = new FloatCharHashMap(this.size());
        map.putAll(this);
        FloatIterator iterator = keys.floatIterator();
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
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(this.delegate.keySet());
    }

    @Override
    public MutableCharCollection values()
    {
        return UnmodifiableCharCollection.of(this.delegate.values());
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
        return new ImmutableFloatCharMapSerializationProxy(this);
    }

    protected static class ImmutableFloatCharMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private FloatCharMap map;

        public ImmutableFloatCharMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableFloatCharMapSerializationProxy(FloatCharMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedFloatCharProcedure()
                {
                    @Override
                    public void safeValue(float key, char value) throws IOException
                    {
                        out.writeFloat(key);
                        out.writeChar(value);
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
            MutableFloatCharMap deserializedMap = new FloatCharHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readFloat(), in.readChar());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
