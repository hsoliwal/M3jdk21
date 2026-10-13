/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.primitive.ByteBags;
import org.eclipse.collections.api.factory.primitive.ByteLists;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.impl.iterator.SingletonByteIterator;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;

/**
 * ImmutableByteSingletonSet is an optimization for {@link ImmutableByteSet} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonSet.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteSingletonSet implements ImmutableByteSet, Serializable
{
    private static final long serialVersionUID = 1L;

    private final byte element;

    ImmutableByteSingletonSet(byte element)
    {
        this.element = element;
    }

    @Override
    public ImmutableByteSet newWith(byte element)
    {
        return ByteSets.immutable.with(this.element, element);
    }

    @Override
    public ImmutableByteSet newWithout(byte element)
    {
        return this.element == element ? ByteSets.immutable.with() : this;
    }

    @Override
    public ImmutableByteSet newWithAll(ByteIterable elements)
    {
        return ByteHashSet.newSet(elements).with(this.element).toImmutable();
    }

    @Override
    public ImmutableByteSet newWithoutAll(ByteIterable elements)
    {
        return elements.contains(this.element) ? ByteSets.immutable.with() : this;
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
    public boolean contains(byte value)
    {
        return this.element == value;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (this.element != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (this.element != value)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        procedure.value(this.element);
    }

    @Override
    public ImmutableByteSet select(BytePredicate predicate)
    {
        return predicate.accept(this.element) ? ByteHashSet.newSetWith(this.element).toImmutable()
                : new ByteHashSet().toImmutable();
    }

    @Override
    public ImmutableByteSet reject(BytePredicate predicate)
    {
        return predicate.accept(this.element) ? new ByteHashSet().toImmutable()
                : ByteHashSet.newSetWith(this.element).toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return Sets.immutable.with(function.valueOf(this.element));
    }

    @Override
    public MutableByteList toList()
    {
        return ByteLists.mutable.with(this.element);
    }

    public int sizeDistinct()
    {
        return 1;
    }

    public int occurrencesOf(byte item)
    {
        return this.element == item ? 1 : 0;
    }

    public void forEachWithOccurrences(ByteIntProcedure byteIntProcedure)
    {
        byteIntProcedure.value(this.element, 1);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return predicate.accept(this.element) ? this.element : ifNone;
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return predicate.accept(this.element) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public long sum()
    {
        return this.element;
    }

    @Override
    public byte min()
    {
        return this.element;
    }

    @Override
    public byte max()
    {
        return this.element;
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.element;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return this.element;
    }

    @Override
    public double average()
    {
        return this.element;
    }

    @Override
    public double median()
    {
        return this.element;
    }

    @Override
    public LazyIterable<ByteBytePair> cartesianProduct(ByteSet set)
    {
        return ByteSets.cartesianProduct(this, set);
    }

    @Override
    public byte[] toSortedArray()
    {
        return new byte[]{this.element};
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteLists.mutable.with(this.element);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return !predicate.accept(this.element);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return predicate.accept(this.element);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element);
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
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ByteSet))
        {
            return false;
        }
        ByteSet set = (ByteSet) obj;
        if (set.size() != 1)
        {
            return false;
        }
        return set.contains(this.element);
    }

    @Override
    public int hashCode()
    {
        return (int) this.element;
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteSets.mutable.with(this.element);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteBags.mutable.with(this.element);
    }

    @Override
    public ByteSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableByteSet toImmutable()
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
        return new byte[]{this.element};
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < 1)
        {
            target = new byte[]{this.element};
        }
        else
        {
            target[0] = this.element;
        }
        return target;
    }

    @Override
    public String toString()
    {
        return '[' + this.makeString() + ']';
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
            appendable.append(String.valueOf(this.element));
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
        return new SingletonByteIterator(element);
    }
}
