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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.FloatStack;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;
import org.eclipse.collections.api.stack.primitive.MutableFloatStack;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractFloatStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * FloatArrayStack is similar to {@link ArrayStack}, and is memory-optimized for float primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class FloatArrayStack extends AbstractFloatStack
        implements MutableFloatStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient FloatArrayList delegate;

    public FloatArrayStack()
    {
        this.delegate = new FloatArrayList();
    }

    private FloatArrayStack(int size)
    {
        this.delegate = new FloatArrayList(size);
    }

    private FloatArrayStack(float... items)
    {
        this.delegate = new FloatArrayList(items);
    }

    public static FloatArrayStack newStackFromTopToBottom(float... items)
    {
        FloatArrayStack stack = new FloatArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static FloatArrayStack newStackWith(float... items)
    {
        return new FloatArrayStack(items);
    }

    public static FloatArrayStack newStack(FloatIterable items)
    {
        FloatArrayStack stack = new FloatArrayStack(items.size());
        stack.delegate = FloatArrayList.newList(items);
        return stack;
    }

    public static FloatArrayStack newStackFromTopToBottom(FloatIterable items)
    {
        FloatArrayStack stack = new FloatArrayStack(items.size());
        stack.delegate = FloatArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected FloatArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(float item)
    {
        this.delegate.add(item);
    }

    @Override
    public float pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public FloatList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new FloatArrayList(0);
        }
        MutableFloatList subList = new FloatArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableFloatStack select(FloatPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableFloatStack reject(FloatPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newList(this).sortThis();
    }

    @Override
    public MutableFloatStack asUnmodifiable()
    {
        return new UnmodifiableFloatStack(this);
    }

    @Override
    public MutableFloatStack asSynchronized()
    {
        return new SynchronizedFloatStack(this);
    }

    @Override
    public ImmutableFloatStack toImmutable()
    {
        return FloatStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty FloatArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public FloatArrayStack newEmpty()
    {
        return new FloatArrayStack();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof FloatStack))
        {
            return false;
        }
        FloatStack stack = (FloatStack) otherStack;
        if (this.size() != stack.size())
        {
            return false;
        }
        for (int i = 0; i < this.size(); i++)
        {
            if (Float.compare(this.peekAt(i), stack.peekAt(i)) != 0)
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
            float item = this.delegate.get(i);
            hashCode = 31 * hashCode + Float.floatToIntBits(item);
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        FloatIterator iterator = this.delegate.asReversed().floatIterator();
        while (iterator.hasNext())
        {
            float each = iterator.next();
            out.writeFloat(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        float[] array = new float[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readFloat();
        }
        this.delegate = FloatArrayList.newListWith(array);
    }
}
