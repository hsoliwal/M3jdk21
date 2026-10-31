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

import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.ObjectLongIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.stack.primitive.LongStack;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

/**
 * This file was automatically generated from template file abstractPrimitiveStack.stg.
 */
public abstract class AbstractLongStack implements LongStack
{
    protected abstract LongArrayList getDelegate();

    protected void checkEmptyStack()
    {
        if (this.getDelegate().isEmpty())
        {
            throw new EmptyStackException();
        }
    }

    @Override
    public long peek()
    {
        this.checkEmptyStack();
        return this.getDelegate().getLast();
    }

    @Override
    public LongList peek(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new LongArrayList(0);
        }
        MutableLongList subList = new LongArrayList(count);
        int index = this.getDelegate().size() - 1;
        for (int i = 0; i < count; i++)
        {
            subList.add(this.getDelegate().get(index - i));
        }
        return subList;
    }

    @Override
    public long peekAt(int index)
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
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.getDelegate().asReversed().longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.getDelegate().asReversed().forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.getDelegate().asReversed().count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.getDelegate().asReversed().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.getDelegate().asReversed().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.getDelegate().asReversed().noneSatisfy(predicate);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.getDelegate().asReversed().detectIfNone(predicate, ifNone);
    }

    @Override
    public long[] toArray()
    {
        return this.getDelegate().asReversed().toArray();
    }

    @Override
    public long[] toArray(long[] target)
    {
        return this.getDelegate().asReversed().toArray(target);
    }

    @Override
    public boolean contains(long value)
    {
        return this.getDelegate().asReversed().contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newList(this);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSet(this);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBag(this);
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectLongToObjectFunction<? super V, ? extends V> function)
    {
        return this.getDelegate().toReversed().injectInto(injectedValue, function);
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
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
        if (!(otherStack instanceof LongStack))
        {
            return false;
        }
        LongStack stack = (LongStack) otherStack;
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
        LongIterable iterable = this.getDelegate().asReversed();
        LongIterator iterator = iterable.longIterator();
        while (iterator.hasNext())
        {
            long item = iterator.next();
            hashCode = 31 * hashCode + (int) (item ^ item >>> 32);
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
    public long getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(long value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return this.getDelegate().asReversed().chunk(size);
    }

    @Override
    public long sum()
    {
        return this.getDelegate().sum();
    }

    @Override
    public long max()
    {
        return this.getDelegate().max();
    }

    @Override
    public long min()
    {
        return this.getDelegate().min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return this.getDelegate().toSortedArray();
    }
}
