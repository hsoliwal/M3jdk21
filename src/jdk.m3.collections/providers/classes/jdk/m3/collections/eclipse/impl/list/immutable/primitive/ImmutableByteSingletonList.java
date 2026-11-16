/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableByteBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectByteIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.ByteList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableByteStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteBytePair;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ByteHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteStacks;
import jdk.m3.collections.eclipse.impl.iterator.SingletonByteIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyByteIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseByteIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ByteArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ByteHashSet;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableByteSingletonList is an optimization for {@link ImmutableByteList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableByteSingletonList implements ImmutableByteList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final byte element1;

    ImmutableByteSingletonList(byte element)
    {
        this.element1 = element;
    }

    @Override
    public byte get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public byte getFirst()
    {
        return this.element1;
    }

    @Override
    public byte getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(byte value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public int lastIndexOf(byte value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new SingletonByteIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableByteList select(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? ByteArrayList.newListWith(this.element1).toImmutable()
                : new ByteArrayList().toImmutable();
    }

    @Override
    public ImmutableByteList reject(BytePredicate predicate)
    {
        return predicate.accept(this.element1) ? new ByteArrayList().toImmutable()
                : ByteArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public byte max()
    {
        return this.element1;
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.element1;
    }

    @Override
    public byte min()
    {
        return this.element1;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public byte[] toSortedArray()
    {
        return new byte[]{this.element1};
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(byte value)
    {
        if (this.element1 == value)
        {
            return 0;
        }
        if (this.element1 < value)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public long dotProduct(ByteList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return (long) this.element1 * list.getFirst();
    }

    @Override
    public byte[] toArray()
    {
        return new byte[]{this.element1};
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < 1)
        {
            target = new byte[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newListWith(this.element1);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBagWith(this.element1);
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
    public ImmutableByteSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableByteList newWith(byte element)
    {
        return ByteLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableByteList newWithout(byte element)
    {
        return this.element1 == element ? ByteLists.immutable.with() : this;
    }

    @Override
    public ImmutableByteList newWithAll(ByteIterable elements)
    {
        ByteArrayList arrayList = ByteArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableByteList newWithoutAll(ByteIterable elements)
    {
        return elements.contains(this.element1) ? ByteLists.immutable.with() : this;
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(this);
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
        if (list.size() != 1)
        {
            return false;
        }
        return this.element1 == list.get(0);
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) this.element1;
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
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
            appendable.append(String.valueOf(this.element1));
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
        return this;
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
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.byteIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public MutableByteStack toStack()
    {
        return ByteStacks.mutable.with(this.element1);
    }
}
