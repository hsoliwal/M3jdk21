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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.CharStack;
import org.eclipse.collections.api.stack.primitive.ImmutableCharStack;
import org.eclipse.collections.api.stack.primitive.MutableCharStack;
import org.eclipse.collections.impl.factory.primitive.CharStacks;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.stack.primitive.AbstractCharStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

/**
 * CharArrayStack is similar to {@link ArrayStack}, and is memory-optimized for char primitives.
 * This file was automatically generated from template file primitiveArrayStack.stg.
 */
public class CharArrayStack extends AbstractCharStack
        implements MutableCharStack, Externalizable
{
    private static final long serialVersionUID = 1L;

    private transient CharArrayList delegate;

    public CharArrayStack()
    {
        this.delegate = new CharArrayList();
    }

    private CharArrayStack(int size)
    {
        this.delegate = new CharArrayList(size);
    }

    private CharArrayStack(char... items)
    {
        this.delegate = new CharArrayList(items);
    }

    public static CharArrayStack newStackFromTopToBottom(char... items)
    {
        CharArrayStack stack = new CharArrayStack(items.length);
        for (int i = items.length - 1; i >= 0; i--)
        {
            stack.push(items[i]);
        }
        return stack;
    }

    public static CharArrayStack newStackWith(char... items)
    {
        return new CharArrayStack(items);
    }

    public static CharArrayStack newStack(CharIterable items)
    {
        CharArrayStack stack = new CharArrayStack(items.size());
        stack.delegate = CharArrayList.newList(items);
        return stack;
    }

    public static CharArrayStack newStackFromTopToBottom(CharIterable items)
    {
        CharArrayStack stack = new CharArrayStack(items.size());
        stack.delegate = CharArrayList.newList(items).reverseThis();
        return stack;
    }

    @Override
    protected CharArrayList getDelegate()
    {
        return this.delegate;
    }

    @Override
    public void push(char item)
    {
        this.delegate.add(item);
    }

    @Override
    public char pop()
    {
        this.checkEmptyStack();
        return this.delegate.removeAtIndex(this.delegate.size() - 1);
    }

    @Override
    public CharList pop(int count)
    {
        this.checkPositiveValueForCount(count);
        this.checkSizeLessThanCount(count);
        if (count == 0)
        {
            return new CharArrayList(0);
        }
        MutableCharList subList = new CharArrayList(count);
        while (count > 0)
        {
            subList.add(this.pop());
            count--;
        }
        return subList;
    }

    @Override
    public MutableCharStack select(CharPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().select(predicate));
    }

    @Override
    public MutableCharStack reject(CharPredicate predicate)
    {
        return newStackFromTopToBottom(this.delegate.asReversed().reject(predicate));
    }

    @Override
    public <V> MutableStack<V> collect(CharToObjectFunction<? extends V> function)
    {
        return ArrayStack.newStackFromTopToBottom(this.delegate.asReversed().collect(function));
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharArrayList.newList(this).sortThis();
    }

    @Override
    public MutableCharStack asUnmodifiable()
    {
        return new UnmodifiableCharStack(this);
    }

    @Override
    public MutableCharStack asSynchronized()
    {
        return new SynchronizedCharStack(this);
    }

    @Override
    public ImmutableCharStack toImmutable()
    {
        return CharStacks.immutable.withAll(this.delegate);
    }

    /**
     * Creates a new empty CharArrayStack.
     *
     * @since 9.2.
     */
    @Override
    public CharArrayStack newEmpty()
    {
        return new CharArrayStack();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof CharStack))
        {
            return false;
        }
        CharStack stack = (CharStack) otherStack;
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
            char item = this.delegate.get(i);
            hashCode = 31 * hashCode + (int) item;
        }
        return hashCode;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        CharIterator iterator = this.delegate.asReversed().charIterator();
        while (iterator.hasNext())
        {
            char each = iterator.next();
            out.writeChar(each);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();
        char[] array = new char[size];
        for (int i = size - 1; i >= 0; i--)
        {
            array[i] = in.readChar();
        }
        this.delegate = CharArrayList.newListWith(array);
    }
}
