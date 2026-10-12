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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ByteStack;
import org.eclipse.collections.api.stack.primitive.ImmutableByteStack;
import org.eclipse.collections.api.stack.primitive.MutableByteStack;
import org.eclipse.collections.impl.factory.primitive.ByteStacks;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractByteStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * ByteArrayStack is similar to {@link ArrayStack}, and is memory-optimized for byte primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class ByteArrayStack extends AbstractByteStack
        implements MutableByteStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient ByteArrayList delegate;

    public ByteArrayStack()
    {
        this.delegate = new ByteArrayList();
    }

    private ByteArrayStack(int size)
    {
        this.delegate = new ByteArrayList(size);
    }

    private ByteArrayStack(byte... items)
    {
        this.delegate = new ByteArrayList(items);
    }

    public static ByteArrayStack newStackFromTopToBottom(byte... items)
    {
        ByteArrayStack stack = new ByteArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static ByteArrayStack newStackWith(byte... items)
    {
        return new ByteArrayStack(items);
    }

    public static ByteArrayStack newStack(ByteIterable items)
    {
        ByteArrayStack stack = new ByteArrayStack(items.size());
        stack.delegate = ByteArrayList.newList(items);
        return stack;
    }

    public static ByteArrayStack newStackFromTopToBottom(ByteIterable items)
    {
        ByteArrayStack stack = new ByteArrayStack(items.size());
        stack.delegate = ByteArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected ByteArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(byte item)
    {
        this.delegate.add(item);
    }

    @Override
    public byte pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public ByteList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new ByteArrayList(0);
        }
        MutableByteList subList = new ByteArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableByteStack select(BytePredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableByteStack reject(BytePredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newList(this).sortThis();
    }

    @Override
    public MutableByteStack asUnmodifiable()
    {
        return new UnmodifiableByteStack(this);
    }

    @Override
    public MutableByteStack asSynchronized()
    {
        return new SynchronizedByteStack(this);
    }

    @Override
    public ImmutableByteStack toImmutable()
    {
        return ByteStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty ByteArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public ByteArrayStack newEmpty()
    {
        return new ByteArrayStack();
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
        for (int i = this.size() - 1; i >= 0; i--)
        {
            byte item = this.delegate.get(i);
            hashCode = 31 * hashCode + (int) item;
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        ByteIterator iterator = this.delegate.asReversed().byteIterator();
        while (iterator.hasNext())
        {
            byte each = iterator.next();
            out.writeByte(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        byte[] array = new byte[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readByte();
        }
        this.delegate = ByteArrayList.newListWith(array);
    }
}
