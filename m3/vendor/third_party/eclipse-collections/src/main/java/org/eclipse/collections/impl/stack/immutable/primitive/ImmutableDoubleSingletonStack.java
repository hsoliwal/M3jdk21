/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.DoubleStack;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import org.eclipse.collections.impl.iterator.SingletonDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;

/**
 * ImmutableDoubleSingletonStack is an optimization for {@link ImmutableDoubleStack} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonStack.stg.
 */
final class ImmutableDoubleSingletonStack implements ImmutableDoubleStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final double element1;

    ImmutableDoubleSingletonStack(double element)
    {
        this.element1 = element;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new SingletonDoubleIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public double peek()
    {
        return this.element1;
    }

    @Override
    public DoubleList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new DoubleArrayList(0);
        }
        if (count == 1)
        {
            return DoubleArrayList.newListWith(this.element1);
        }
        throw new IllegalArgumentException("Count must be less than or equal to size: Count = " + count + " Size = 1");
    }

    @Override
    public double peekAt(int index)
    {
        this.checkNegativeCount(index);
        if (index == 0)
        {
            return this.element1;
        }
        throw new IllegalArgumentException("Index must be less than size: Index = " + index + " Size = 1");
    }

    @Override
    public ImmutableDoubleStack select(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? DoubleStacks.immutable.with(this.element1)
                : DoubleStacks.immutable.with();
    }

    @Override
    public ImmutableDoubleStack reject(DoublePredicate predicate)
    {
        return predicate.accept(this.element1) ? DoubleStacks.immutable.with()
                : DoubleStacks.immutable.with(this.element1);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public double sum()
    {
        return this.element1;
    }

    @Override
    public double max()
    {
        return this.element1;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.element1;
    }

    @Override
    public double min()
    {
        return this.element1;
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return new double[]{this.element1};
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newListWith(this.element1);
    }

    @Override
    public double[] toArray()
    {
        return new double[]{this.element1};
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < 1)
        {
            target = new double[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(double value)
    {
        return Double.compare(this.element1, value) == 0;
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double value : source)
        {
            if (Double.compare(this.element1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (Double.compare(this.element1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newListWith(this.element1);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableDoubleStack push(double element)
    {
        return DoubleStacks.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableDoubleStack pop()
    {
        return DoubleStacks.immutable.with();
    }

    @Override
    public ImmutableDoubleStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        if (count == 1)
        {
            return DoubleStacks.immutable.with();
        }
        throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = 1");
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
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
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        return Lists.immutable.with(this);
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
        if (stack.size() != 1)
        {
            return false;
        }
        return Double.compare(this.element1, stack.peek()) == 0;
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) (Double.doubleToLongBits(this.element1) ^ Double.doubleToLongBits(this.element1) >>> 32);
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
}
