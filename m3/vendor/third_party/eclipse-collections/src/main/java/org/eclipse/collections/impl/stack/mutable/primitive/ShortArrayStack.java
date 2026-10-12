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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.ShortList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ShortStack;
import org.eclipse.collections.api.stack.primitive.ImmutableShortStack;
import org.eclipse.collections.api.stack.primitive.MutableShortStack;
import org.eclipse.collections.impl.factory.primitive.ShortStacks;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractShortStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * ShortArrayStack is similar to {@link ArrayStack}, and is memory-optimized for short primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class ShortArrayStack extends AbstractShortStack
        implements MutableShortStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient ShortArrayList delegate;

    public ShortArrayStack()
    {
        this.delegate = new ShortArrayList();
    }

    private ShortArrayStack(int size)
    {
        this.delegate = new ShortArrayList(size);
    }

    private ShortArrayStack(short... items)
    {
        this.delegate = new ShortArrayList(items);
    }

    public static ShortArrayStack newStackFromTopToBottom(short... items)
    {
        ShortArrayStack stack = new ShortArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static ShortArrayStack newStackWith(short... items)
    {
        return new ShortArrayStack(items);
    }

    public static ShortArrayStack newStack(ShortIterable items)
    {
        ShortArrayStack stack = new ShortArrayStack(items.size());
        stack.delegate = ShortArrayList.newList(items);
        return stack;
    }

    public static ShortArrayStack newStackFromTopToBottom(ShortIterable items)
    {
        ShortArrayStack stack = new ShortArrayStack(items.size());
        stack.delegate = ShortArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected ShortArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(short item)
    {
        this.delegate.add(item);
    }

    @Override
    public short pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public ShortList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new ShortArrayList(0);
        }
        MutableShortList subList = new ShortArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableShortStack select(ShortPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableShortStack reject(ShortPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newList(this).sortThis();
    }

    @Override
    public MutableShortStack asUnmodifiable()
    {
        return new UnmodifiableShortStack(this);
    }

    @Override
    public MutableShortStack asSynchronized()
    {
        return new SynchronizedShortStack(this);
    }

    @Override
    public ImmutableShortStack toImmutable()
    {
        return ShortStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty ShortArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public ShortArrayStack newEmpty()
    {
        return new ShortArrayStack();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof ShortStack))
        {
            return false;
        }
        ShortStack stack = (ShortStack) otherStack;
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
            short item = this.delegate.get(i);
            hashCode = 31 * hashCode + (int) item;
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        ShortIterator iterator = this.delegate.asReversed().shortIterator();
        while (iterator.hasNext())
        {
            short each = iterator.next();
            out.writeShort(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        short[] array = new short[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readShort();
        }
        this.delegate = ShortArrayList.newListWith(array);
    }
}
