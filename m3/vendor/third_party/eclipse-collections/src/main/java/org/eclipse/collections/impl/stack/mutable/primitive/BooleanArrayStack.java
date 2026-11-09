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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.BooleanStack;
import org.eclipse.collections.api.stack.primitive.ImmutableBooleanStack;
import org.eclipse.collections.api.stack.primitive.MutableBooleanStack;
import org.eclipse.collections.impl.factory.primitive.BooleanStacks;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractBooleanStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * BooleanArrayStack is similar to {@link ArrayStack}, and is memory-optimized for boolean primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class BooleanArrayStack extends AbstractBooleanStack
        implements MutableBooleanStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient BooleanArrayList delegate;

    public BooleanArrayStack()
    {
        this.delegate = new BooleanArrayList();
    }

    private BooleanArrayStack(int size)
    {
        this.delegate = new BooleanArrayList(size);
    }

    private BooleanArrayStack(boolean... items)
    {
        this.delegate = new BooleanArrayList(items);
    }

    public static BooleanArrayStack newStackFromTopToBottom(boolean... items)
    {
        BooleanArrayStack stack = new BooleanArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static BooleanArrayStack newStackWith(boolean... items)
    {
        return new BooleanArrayStack(items);
    }

    public static BooleanArrayStack newStack(BooleanIterable items)
    {
        BooleanArrayStack stack = new BooleanArrayStack(items.size());
        stack.delegate = BooleanArrayList.newList(items);
        return stack;
    }

    public static BooleanArrayStack newStackFromTopToBottom(BooleanIterable items)
    {
        BooleanArrayStack stack = new BooleanArrayStack(items.size());
        stack.delegate = BooleanArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected BooleanArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(boolean item)
    {
        this.delegate.add(item);
    }

    @Override
    public boolean pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public BooleanList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new BooleanArrayList(0);
        }
        MutableBooleanList subList = new BooleanArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableBooleanStack select(BooleanPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableBooleanStack reject(BooleanPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableBooleanStack asUnmodifiable()
    {
        return new UnmodifiableBooleanStack(this);
    }

    @Override
    public MutableBooleanStack asSynchronized()
    {
        return new SynchronizedBooleanStack(this);
    }

    @Override
    public ImmutableBooleanStack toImmutable()
    {
        return BooleanStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty BooleanArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public BooleanArrayStack newEmpty()
    {
        return new BooleanArrayStack();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof BooleanStack))
        {
            return false;
        }
        BooleanStack stack = (BooleanStack) otherStack;
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
            boolean item = this.delegate.get(i);
            hashCode = 31 * hashCode + (item ? 1231 : 1237);
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        BooleanIterator iterator = this.delegate.asReversed().booleanIterator();
        while (iterator.hasNext())
        {
            boolean each = iterator.next();
            out.writeBoolean(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        boolean[] array = new boolean[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readBoolean();
        }
        this.delegate = BooleanArrayList.newListWith(array);
    }
}
