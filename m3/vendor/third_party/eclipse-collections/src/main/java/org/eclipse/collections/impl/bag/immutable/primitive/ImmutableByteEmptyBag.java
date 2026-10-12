/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.ByteBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.iterator.ImmutableEmptyByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;

/**
 * ImmutableByteEmptyBag is an optimization for {@link ImmutableByteBag} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteEmptyBag implements ImmutableByteBag, Serializable
{
    static final ImmutableByteBag INSTANCE = new ImmutableByteEmptyBag();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableByteBag newWith(byte element)
    {
        return new ImmutableByteSingletonBag(element);
    }

    @Override
    public ImmutableByteBag newWithout(byte element)
    {
        return this;
    }

    @Override
    public ImmutableByteBag newWithAll(ByteIterable elements)
    {
        return ByteBags.immutable.withAll(elements);
    }

    @Override
    public ImmutableByteBag newWithoutAll(ByteIterable elements)
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
    public boolean contains(byte value)
    {
        return false;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAll(byte... elements)
    {
        return elements.length == 0;
    }

    @Override
    public void forEach(ByteProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
    }

    @Override
    public ImmutableByteBag select(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteSet selectUnique()
    {
        return ByteSets.immutable.empty();
    }

    @Override
    public ImmutableByteBag reject(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public <V> ImmutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of();
    }

    @Override
    public MutableByteList toList()
    {
        return new ByteArrayList();
    }

    @Override
    public int sizeDistinct()
    {
        return 0;
    }

    @Override
    public int occurrencesOf(byte item)
    {
        return 0;
    }

    @Override
    public void forEachWithOccurrences(ByteIntProcedure byteIntProcedure)
    {
    }

    @Override
    public ImmutableByteBag selectByOccurrences(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableList<ByteIntPair> topOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableList<ByteIntPair> bottomOccurrences(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Cannot use a value of count < 0");
        }
        return Lists.immutable.empty();
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return ifNone;
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return false;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public byte min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return defaultValue;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double average()
    {
        throw new ArithmeticException();
    }

    @Override
    public double median()
    {
        throw new ArithmeticException();
    }

    @Override
    public byte[] toSortedArray()
    {
        return new byte[0];
    }

    @Override
    public MutableByteList toSortedList()
    {
        return new ByteArrayList();
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ByteBag))
        {
            return false;
        }
        ByteBag bag = (ByteBag) obj;
        return bag.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public MutableByteSet toSet()
    {
        return new ByteHashSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return new ByteHashBag();
    }

    @Override
    public ImmutableByteBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public byte[] toArray()
    {
        return new byte[0];
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return target;
    }

    @Override
    public String toString()
    {
        return "[]";
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

    @Override
    public ByteIterator byteIterator()
    {
        return ImmutableEmptyByteIterator.INSTANCE;
    }
}
