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

import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.ObjectIntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.stack.primitive.IntStack;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/**
 * This file was automatically generated from template file abstractPrimitiveStack.stg.
 */
public abstract class AbstractIntStack implements IntStack
{
    protected abstract IntArrayList getDelegate();

    protected void checkEmptyStack()
    {
        if (this.getDelegate().isEmpty())
        {
            throw new EmptyStackException();
        }
    }

    @Override
    public int peek()
    {
        this.checkEmptyStack();
        return this.getDelegate().getLast();
    }

    @Override
    public IntList peek(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new IntArrayList(0);
        }
        MutableIntList subList = new IntArrayList(count);
        int index = this.getDelegate().size() - 1;
        for (int i = 0; i < count; i++)
        {
            subList.add(this.getDelegate().get(index - i));
        }
        return subList;
    }

    @Override
    public int peekAt(int index)
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
    public IntIterator intIterator()
    {
        return new UnmodifiableIntIterator(this.getDelegate().asReversed().intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.getDelegate().asReversed().forEach(procedure);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return this.getDelegate().asReversed().count(predicate);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return this.getDelegate().asReversed().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return this.getDelegate().asReversed().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return this.getDelegate().asReversed().noneSatisfy(predicate);
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return this.getDelegate().asReversed().detectIfNone(predicate, ifNone);
    }

    @Override
    public int[] toArray()
    {
        return this.getDelegate().asReversed().toArray();
    }

    @Override
    public int[] toArray(int[] target)
    {
        return this.getDelegate().asReversed().toArray(target);
    }

    @Override
    public boolean contains(int value)
    {
        return this.getDelegate().asReversed().contains(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public MutableIntList toList()
    {
        return IntArrayList.newList(this);
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntHashSet.newSet(this);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntHashBag.newBag(this);
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectIntToObjectFunction<? super V, ? extends V> function)
    {
        return this.getDelegate().toReversed().injectInto(injectedValue, function);
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
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
        if (!(otherStack instanceof IntStack))
        {
            return false;
        }
        IntStack stack = (IntStack) otherStack;
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
        IntIterable iterable = this.getDelegate().asReversed();
        IntIterator iterator = iterable.intIterator();
        while (iterator.hasNext())
        {
            int item = iterator.next();
            hashCode = 31 * hashCode + item;
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
    public int getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(int value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return this.getDelegate().asReversed().chunk(size);
    }

    @Override
    public long sum()
    {
        return this.getDelegate().sum();
    }

    @Override
    public int max()
    {
        return this.getDelegate().max();
    }

    @Override
    public int min()
    {
        return this.getDelegate().min();
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return this.getDelegate().toSortedArray();
    }
}
