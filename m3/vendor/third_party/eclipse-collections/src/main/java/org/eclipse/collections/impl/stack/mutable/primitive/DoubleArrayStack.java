/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.DoubleStack;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import org.eclipse.collections.api.stack.primitive.MutableDoubleStack;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractDoubleStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * DoubleArrayStack is similar to {@link ArrayStack}, and is memory-optimized for double primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class DoubleArrayStack extends AbstractDoubleStack
        implements MutableDoubleStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient DoubleArrayList delegate;

    public DoubleArrayStack()
    {
        this.delegate = new DoubleArrayList();
    }

    private DoubleArrayStack(int size)
    {
        this.delegate = new DoubleArrayList(size);
    }

    private DoubleArrayStack(double... items)
    {
        this.delegate = new DoubleArrayList(items);
    }

    public static DoubleArrayStack newStackFromTopToBottom(double... items)
    {
        DoubleArrayStack stack = new DoubleArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static DoubleArrayStack newStackWith(double... items)
    {
        return new DoubleArrayStack(items);
    }

    public static DoubleArrayStack newStack(DoubleIterable items)
    {
        DoubleArrayStack stack = new DoubleArrayStack(items.size());
        stack.delegate = DoubleArrayList.newList(items);
        return stack;
    }

    public static DoubleArrayStack newStackFromTopToBottom(DoubleIterable items)
    {
        DoubleArrayStack stack = new DoubleArrayStack(items.size());
        stack.delegate = DoubleArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected DoubleArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(double item)
    {
        this.delegate.add(item);
    }

    @Override
    public double pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public DoubleList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new DoubleArrayList(0);
        }
        MutableDoubleList subList = new DoubleArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableDoubleStack select(DoublePredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableDoubleStack reject(DoublePredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newList(this).sortThis();
    }

    @Override
    public MutableDoubleStack asUnmodifiable()
    {
        return new UnmodifiableDoubleStack(this);
    }

    @Override
    public MutableDoubleStack asSynchronized()
    {
        return new SynchronizedDoubleStack(this);
    }

    @Override
    public ImmutableDoubleStack toImmutable()
    {
        return DoubleStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty DoubleArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public DoubleArrayStack newEmpty()
    {
        return new DoubleArrayStack();
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
        for (int i = this.size() - 1; i >= 0; i--)
        {
            double item = this.delegate.get(i);
            hashCode = 31 * hashCode + (int) (Double.doubleToLongBits(item) ^ Double.doubleToLongBits(item) >>> 32);
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        DoubleIterator iterator = this.delegate.asReversed().doubleIterator();
        while (iterator.hasNext())
        {
            double each = iterator.next();
            out.writeDouble(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        double[] array = new double[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readDouble();
        }
        this.delegate = DoubleArrayList.newListWith(array);
    }
}
