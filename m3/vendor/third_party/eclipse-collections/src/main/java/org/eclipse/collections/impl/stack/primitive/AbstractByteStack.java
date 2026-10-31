/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.primitive;

import java.util.EmptyStackException;

import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.stack.primitive.ByteStack;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;

/**
 * This file was automatically generated from template file abstractPrimitiveStack.stg.
 */
public abstract class AbstractByteStack implements ByteStack
{
    protected abstract ByteArrayList getDelegate();

    protected void checkEmptyStack()
    {
        if (this.getDelegate().isEmpty())
        {
            throw new EmptyStackException();
        }
    }

    @Override
    public byte peek()
    {
        this.checkEmptyStack();
        return this.getDelegate().getLast();
    }

    @Override
    public ByteList peek(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new ByteArrayList(0);
        }
        MutableByteList subList = new ByteArrayList(count);
        int index = this.getDelegate().size() - 1;
        for (int i = 0; i < count; i++)
        {
            subList.add(this.getDelegate().get(index - i));
        }
        return subList;
    }

    @Override
    public byte peekAt(int index)
    {
        this.rangeCheck(index);
        return this.getDelegate().get(this.getDelegate().size() - 1 - index);
    }

    protected void rangeCheck(int index)
    {
        if (index < 0 || index > this.getDelegate().size() - 1)
        {
            throw new IllegalArgumentException("Index " + index + " out of range.Should be between 0 and " + (this.getDelegate().size() - 1));
        }
    }

    protected void checkPositiveValueForCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    protected void checkSizeLessThanCount(int count)
    {
        if (this.getDelegate().size() < count)
        {
            throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = " + this.getDelegate().size());
        }
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.getDelegate().asReversed().byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.getDelegate().asReversed().forEach(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return this.getDelegate().asReversed().count(predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.getDelegate().asReversed().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.getDelegate().asReversed().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.getDelegate().asReversed().noneSatisfy(predicate);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.getDelegate().asReversed().detectIfNone(predicate, ifNone);
    }

    @Override
    public byte[] toArray()
    {
        return this.getDelegate().asReversed().toArray();
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return this.getDelegate().asReversed().toArray(target);
    }

    @Override
    public boolean contains(byte value)
    {
        return this.getDelegate().asReversed().contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newList(this);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSet(this);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBag(this);
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectByteToObjectFunction<? super V, ? extends V> function)
    {
        return this.getDelegate().toReversed().injectInto(injectedValue, function);
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public int size()
    {
        return this.getDelegate().size();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof ByteStack))
        {
            return false;
        }
        ByteStack stack = (ByteStack) otherStack;
        if (this.size() != stack.size())
        {
            return false;
        }
        for (int i = 0; i < this.size(); i++)
        {
            if (this.peekAt(i) != stack.peekAt(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;
        ByteIterable iterable = this.getDelegate().asReversed();
        ByteIterator iterator = iterable.byteIterator();
        while (iterator.hasNext())
        {
            byte item = iterator.next();
            hashCode = 31 * hashCode + (int) item;
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        return this.getDelegate().asReversed().toString();
    }

    @Override
    public String makeString()
    {
        return this.getDelegate().asReversed().makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.getDelegate().asReversed().makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.getDelegate().asReversed().makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.getDelegate().asReversed().appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.getDelegate().asReversed().appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.getDelegate().asReversed().appendString(appendable, start, separator, end);
    }

    @Override
    public byte getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(byte value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return this.getDelegate().asReversed().chunk(size);
    }

    @Override
    public long sum()
    {
        return this.getDelegate().sum();
    }

    @Override
    public byte max()
    {
        return this.getDelegate().max();
    }

    @Override
    public byte min()
    {
        return this.getDelegate().min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.max();
    }

    @Override
    public double average()
    {
        return this.getDelegate().average();
    }

    @Override
    public double median()
    {
        return this.getDelegate().median();
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.getDelegate().toSortedArray();
    }
}
