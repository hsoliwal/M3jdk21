/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.iterator.ImmutableEmptyByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseByteIterable;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;

/**
 * ImmutableByteEmptyList is an optimization for {@link ImmutableByteList} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyList.stg.
 */
final class ImmutableByteEmptyList implements ImmutableByteList, Serializable
{
    static final ImmutableByteList INSTANCE = new ImmutableByteEmptyList();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public byte get(int index)
    {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public byte getFirst()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public byte getLast()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int indexOf(byte value)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(byte value)
    {
        return -1;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return ImmutableEmptyByteIterator.INSTANCE;
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
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
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
    public boolean allSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableByteList select(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableByteList reject(BytePredicate predicate)
    {
        return this;
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
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
    public byte min()
    {
        throw new NoSuchElementException();
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
    public int binarySearch(byte value)
    {
        return -1;
    }

    @Override
    public long dotProduct(ByteList list)
    {
        if (!list.isEmpty())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return 0;
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
    public boolean contains(byte value)
    {
        return false;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public MutableByteList toList()
    {
        return new ByteArrayList();
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
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public ImmutableByteList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableByteEmptyList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableByteList newWith(byte element)
    {
        return ByteLists.immutable.with(element);
    }

    @Override
    public ImmutableByteList newWithout(byte element)
    {
        return this;
    }

    @Override
    public ImmutableByteList newWithAll(ByteIterable elements)
    {
        return ByteLists.immutable.withAll(elements);
    }

    @Override
    public ImmutableByteList newWithoutAll(ByteIterable elements)
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof ByteList))
        {
            return false;
        }
        ByteList list = (ByteList) otherList;
        return list.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 1;
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

    /**
     * @since 6.0.
     */
    @Override
    public ImmutableByteList distinct()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableByteList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        return Lists.immutable.empty();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        return Lists.immutable.empty();
    }
}
