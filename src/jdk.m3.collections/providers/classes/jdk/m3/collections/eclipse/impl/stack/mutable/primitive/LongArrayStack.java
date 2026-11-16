/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.LongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.stack.MutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.LongStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableLongStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableLongStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.stack.primitive.AbstractLongStack;
import jdk.m3.collections.eclipse.impl.stack.mutable.ArrayStack;

/**
 * LongArrayStack is similar to {@link ArrayStack}, and is memory-optimized for long primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class LongArrayStack extends AbstractLongStack
        implements MutableLongStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient LongArrayList delegate;

    public LongArrayStack()
    {
        this.delegate = new LongArrayList();
    }

    private LongArrayStack(int size)
    {
        this.delegate = new LongArrayList(size);
    }

    private LongArrayStack(long... items)
    {
        this.delegate = new LongArrayList(items);
    }

    public static LongArrayStack newStackFromTopToBottom(long... items)
    {
        LongArrayStack stack = new LongArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static LongArrayStack newStackWith(long... items)
    {
        return new LongArrayStack(items);
    }

    public static LongArrayStack newStack(LongIterable items)
    {
        LongArrayStack stack = new LongArrayStack(items.size());
        stack.delegate = LongArrayList.newList(items);
        return stack;
    }

    public static LongArrayStack newStackFromTopToBottom(LongIterable items)
    {
        LongArrayStack stack = new LongArrayStack(items.size());
        stack.delegate = LongArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected LongArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(long item)
    {
        this.delegate.add(item);
    }

    @Override
    public long pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public LongList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new LongArrayList(0);
        }
        MutableLongList subList = new LongArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableLongStack select(LongPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableLongStack reject(LongPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(LongToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newList(this).sortThis();
    }

    @Override
    public MutableLongStack asUnmodifiable()
    {
        return new UnmodifiableLongStack(this);
    }

    @Override
    public MutableLongStack asSynchronized()
    {
        return new SynchronizedLongStack(this);
    }

    @Override
    public ImmutableLongStack toImmutable()
    {
        return LongStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty LongArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public LongArrayStack newEmpty()
    {
        return new LongArrayStack();
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
        for (int i = this.size() - 1; i >= 0; i--)
        {
            long item = this.delegate.get(i);
            hashCode = 31 * hashCode + (int) (item ^ item >>> 32);
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        LongIterator iterator = this.delegate.asReversed().longIterator();
        while (iterator.hasNext())
        {
            long each = iterator.next();
            out.writeLong(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        long[] array = new long[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readLong();
        }
        this.delegate = LongArrayList.newListWith(array);
    }

    @Override
    public MutableLongStack toStack()
    {
        return LongStacks.mutable.withAllReversed(this);
    }
}
