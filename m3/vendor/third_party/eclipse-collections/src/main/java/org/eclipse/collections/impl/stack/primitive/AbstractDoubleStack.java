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

import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.stack.primitive.DoubleStack;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;

/**
 * This file was automatically generated from template file abstractPrimitiveStack.stg.
 */
public abstract class AbstractDoubleStack implements DoubleStack
{
    protected abstract DoubleArrayList getDelegate();

    protected void checkEmptyStack()
    {
        if (this.getDelegate().isEmpty())
        {
            throw new EmptyStackException();
        }
    }

    @Override
    public double peek()
    {
        this.checkEmptyStack();
        return this.getDelegate().getLast();
    }

    @Override
    public DoubleList peek(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new DoubleArrayList(0);
        }
        MutableDoubleList subList = new DoubleArrayList(count);
        int index = this.getDelegate().size() - 1;
        for (int i = 0; i < count; i++)
        {
            subList.add(this.getDelegate().get(index - i));
        }
        return subList;
    }

    @Override
    public double peekAt(int index)
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
    public DoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(this.getDelegate().asReversed().doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.getDelegate().asReversed().forEach(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return this.getDelegate().asReversed().count(predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.getDelegate().asReversed().anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.getDelegate().asReversed().allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.getDelegate().asReversed().noneSatisfy(predicate);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.getDelegate().asReversed().detectIfNone(predicate, ifNone);
    }

    @Override
    public double[] toArray()
    {
        return this.getDelegate().asReversed().toArray();
    }

    @Override
    public double[] toArray(double[] target)
    {
        return this.getDelegate().asReversed().toArray(target);
    }

    @Override
    public boolean contains(double value)
    {
        return this.getDelegate().asReversed().contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return this.getDelegate().asReversed().containsAll(source);
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newList(this);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSet(this);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBag(this);
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectDoubleToObjectFunction<? super V, ? extends V> function)
    {
        return this.getDelegate().toReversed().injectInto(injectedValue, function);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
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
        if (!(otherStack instanceof DoubleStack))
        {
            return false;
        }
        DoubleStack stack = (DoubleStack) otherStack;
        if (this.size() != stack.size())
        {
            return false;
        }
        for (int i = 0; i < this.size(); i++)
        {
            if (Double.compare(this.peekAt(i), stack.peekAt(i)) != 0)
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
        DoubleIterable iterable = this.getDelegate().asReversed();
        DoubleIterator iterator = iterable.doubleIterator();
        while (iterator.hasNext())
        {
            double item = iterator.next();
            hashCode = 31 * hashCode + (int) (Double.doubleToLongBits(item) ^ Double.doubleToLongBits(item) >>> 32);
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
    public double getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(double value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return this.getDelegate().asReversed().chunk(size);
    }

    @Override
    public double sum()
    {
        return this.getDelegate().sum();
    }

    @Override
    public double max()
    {
        return this.getDelegate().max();
    }

    @Override
    public double min()
    {
        return this.getDelegate().min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return this.getDelegate().toSortedArray();
    }
}
