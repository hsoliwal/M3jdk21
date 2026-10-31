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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.IntStack;
import org.eclipse.collections.api.stack.primitive.ImmutableIntStack;
import org.eclipse.collections.api.stack.primitive.MutableIntStack;
import org.eclipse.collections.impl.factory.primitive.IntStacks;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractIntStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * IntArrayStack is similar to {@link ArrayStack}, and is memory-optimized for int primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class IntArrayStack extends AbstractIntStack
        implements MutableIntStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient IntArrayList delegate;

    public IntArrayStack()
    {
        this.delegate = new IntArrayList();
    }

    private IntArrayStack(int size)
    {
        this.delegate = new IntArrayList(size);
    }

    private IntArrayStack(int... items)
    {
        this.delegate = new IntArrayList(items);
    }

    public static IntArrayStack newStackFromTopToBottom(int... items)
    {
        IntArrayStack stack = new IntArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static IntArrayStack newStackWith(int... items)
    {
        return new IntArrayStack(items);
    }

    public static IntArrayStack newStack(IntIterable items)
    {
        IntArrayStack stack = new IntArrayStack(items.size());
        stack.delegate = IntArrayList.newList(items);
        return stack;
    }

    public static IntArrayStack newStackFromTopToBottom(IntIterable items)
    {
        IntArrayStack stack = new IntArrayStack(items.size());
        stack.delegate = IntArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected IntArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(int item)
    {
        this.delegate.add(item);
    }

    @Override
    public int pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public IntList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new IntArrayList(0);
        }
        MutableIntList subList = new IntArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableIntStack select(IntPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableIntStack reject(IntPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(IntToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntArrayList.newList(this).sortThis();
    }

    @Override
    public MutableIntStack asUnmodifiable()
    {
        return new UnmodifiableIntStack(this);
    }

    @Override
    public MutableIntStack asSynchronized()
    {
        return new SynchronizedIntStack(this);
    }

    @Override
    public ImmutableIntStack toImmutable()
    {
        return IntStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty IntArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public IntArrayStack newEmpty()
    {
        return new IntArrayStack();
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
        for (int i = this.size() - 1; i >= 0; i--)
        {
            int item = this.delegate.get(i);
            hashCode = 31 * hashCode + item;
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        IntIterator iterator = this.delegate.asReversed().intIterator();
        while (iterator.hasNext())
        {
            int each = iterator.next();
            out.writeInt(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        int[] array = new int[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readInt();
        }
        this.delegate = IntArrayList.newListWith(array);
    }
}
